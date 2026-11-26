package za.co.mawa.bes.xero;

import java.io.IOException;

/** Runtime configuration changed after claiming work; keep the queue item pending. */
public class XeroDispatchPausedException extends IOException {
    public XeroDispatchPausedException() { super("Xero integration was disabled while processing queued work"); }
}
