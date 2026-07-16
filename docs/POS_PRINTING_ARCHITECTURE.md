# MAWA POS Printing Implementation

## Scope

This release replaces IP-address print routing with a tenant-scoped registered printing domain. A MAWA ERP browser terminal is assigned to an enrolled Windows agent and an exact Windows printer queue. IP addresses are stored only for diagnostics.

## Implemented components

### mawa-bes

Dedicated domain-specific entities, repositories, services and v2 controllers were added for:

- One-time print-agent enrollment
- Revocable scoped agent credentials
- Agent heartbeat and version reporting
- Exact Windows printer discovery and registration
- Per-printer paper width and cutter configuration
- Browser terminal registration and printer assignment
- Idempotent receipt print requests
- Atomic pessimistic job claiming with a 90-second lease
- QUEUED, CLAIMED, SPOOLED and FAILED job states
- Exponential retry and auditable print attempts
- Test-print jobs
- Admin summary, revoke, terminal enable/disable and retry operations

Receipt records are now marked printed only after the local agent reports that Windows accepted the print job. Previewing a receipt no longer changes print state.

The legacy IP-routed `/print-job` endpoint now returns HTTP 410 Gone. Legacy premium print requests have been redirected into the registered terminal/printer queue.

### mawa-flyway-runner

Migration `V202607160004__pos_printing_agent.sql` creates:

- `pos_print_enrollment`
- `pos_print_agent`
- `pos_printer`
- `pos_terminal`
- `pos_print_job`
- `pos_print_attempt`

It also corrects new receipt defaults to `printed = false` and `print_count = 0`.

### mawa_erp

System Configuration now includes **POS Printing**, where an authorised user can:

- Register the current browser as a terminal
- Create a one-time Windows agent setup code
- Select an enrolled agent
- Select the exact Windows printer queue
- Configure 58 mm/80 mm receipt width per printer
- Enable or disable ESC/POS paper cutting per printer
- Save the terminal assignment
- Queue a test print

Premium receipt printing first uses the registered Windows queue. Bluetooth remains an explicit fallback; MAWA separately reports when paper printed but the server acknowledgement failed.

### mawa-admin-bes and mawa_erp_admin

The Admin Console can inspect each tenant's:

- Enrolled and online agents
- Discovered printer queues
- ERP terminals
- Recent print jobs and failures

It can create enrollment codes, revoke agents, enable or disable terminals and retry failed jobs.

### Windows agent

The former Spring Boot proof of concept was replaced by a dependency-free Java 17 agent with:

- No embedded web server and no copied ERP DTOs
- HTTPS-only remote API access, except localhost development
- One-time enrollment and durable scoped credentials
- Exact Windows queue matching
- Raw ESC/POS byte spooling with byte-array and input-stream print flavour support
- Printer discovery and regular synchronization
- Scheduled polling, heartbeat and printer sync
- Connection and request timeouts
- Single-instance locking
- Durable duplicate-output suppression
- Rolling logs and machine-readable status
- Graceful shutdown and automatic startup recovery

## Job flow

1. MAWA ERP registers or loads its stable terminal ID.
2. The user requests a receipt print.
3. mawa-bes resolves the terminal's assigned agent and printer.
4. The receipt is rendered for that printer's configured width.
5. A tenant-local job is inserted with a deterministic initial-print idempotency key.
6. The assigned agent atomically claims the oldest eligible job.
7. The agent submits the ESC/POS bytes to the exact Windows queue.
8. The agent reports `SPOOLED` or `FAILED` using the claim token.
9. mawa-bes updates the attempt audit and increments receipt print count only on `SPOOLED`.

## Deployment order

1. Deploy and run the updated `mawa-flyway-runner` for all tenant schemas.
2. Deploy `mawa-bes`.
3. Deploy `mawa-admin-bes`.
4. Deploy `mawa_erp` and `mawa_erp_admin`.
5. In ERP or Admin Console, create a one-time agent setup code.
6. On each Windows POS computer, install the required printer in Windows and verify a Windows test page.
7. Run the MAWA agent installer as Administrator.
8. Refresh ERP POS Printing, select the exact queue, set its width/cutter capability and save.
9. Run **Test print** before processing a live receipt.

## Windows setup packages

The supplied JAR-only setup package contains the compiled runnable agent and installation scripts. It requires a 64-bit Java 17 or newer runtime on the target computer.

For a package with a private Java runtime, run `installer/windows/Build-Windows-Setup.ps1` on a Windows build machine with JDK 17+ and `jpackage`. This is platform-specific and cannot be generated correctly on Linux.

The installer registers a hidden SYSTEM scheduled task named **MAWA POS Printer Agent**, restricts `%ProgramData%\Mawa\PosPrinterAgent` to SYSTEM and Administrators, verifies enrollment, and supplies status/restart/uninstall commands.

## Configuration and operations

Data is stored under:

- Configuration: `%ProgramData%\Mawa\PosPrinterAgent\config`
- Logs: `%ProgramData%\Mawa\PosPrinterAgent\logs`
- Status: `%ProgramData%\Mawa\PosPrinterAgent\state\status.properties`
- Installation: `%ProgramFiles%\Mawa\PosPrinterAgent`

Support commands:

- `Status.cmd`
- `Restart.cmd`
- `Uninstall.cmd`

## Operational meaning of SPOOLED

`SPOOLED` confirms that the Windows print subsystem accepted the job. It cannot guarantee physical paper output for printers or drivers that do not expose hardware completion status. Offline, out-of-paper and cover-open conditions should therefore still be monitored on the printer itself.

## Required existing configuration

No new GCP secret is required. Admin Console proxy operations use the existing:

- `mawa.erp.api.url` or `mawa.api.url`
- `mawa.internal.service-token`

The Windows agent requires only:

- MAWA API URL
- Tenant host/ID
- One-time enrollment code

Normal tenant usernames and passwords are no longer stored on the POS computer.

## Validation performed

- Windows agent compiled with `javac --release 17`.
- Runnable JAR generated and manifest verified.
- Agent self-tests passed, including JSON escaping, Unicode and numeric parsing.
- New/changed Java sources passed a javac syntax pass; unresolved dependency diagnostics were expected because Maven dependencies could not be downloaded in the isolated environment.
- Modified Dart sources passed delimiter, string and interpolation structural checks.
- Maven POM files parsed as XML.
- Flyway migration received structural and terminator checks.

Full Maven and Flutter builds must still run in the normal connected CI environment because this sandbox has no Maven dependency cache, cannot reach Maven Central and does not include Flutter/Dart.
