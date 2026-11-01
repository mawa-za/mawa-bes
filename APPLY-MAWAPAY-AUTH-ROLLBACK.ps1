# Run from this file's location after extracting the overlay into mawa-bes.
# Deletes ONLY the emergency authentication bypass implementation and its tests.
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not (Test-Path -LiteralPath (Join-Path $backendRoot 'src/main/java/za/co/mawa/bes/configuration/jwt/JwtRequestFilter.java')) -or
    -not (Test-Path -LiteralPath (Join-Path $backendRoot 'src/main/resources/application-prod.properties'))) {
    throw 'This script must be inside the mawa-bes backend repository root.'
}

$obsoleteFiles = @(
    'src/main/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyAccessPolicy.java',
    'src/main/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyBearerPolicy.java',
    'src/main/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyNoAuthPolicy.java',
    'src/main/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyMasterDataPolicy.java',
    'src/main/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyRefreshPolicy.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/JwtRefreshServiceEmergencyTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/JwtRequestFilterEmergencyAccessTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/JwtRequestFilterEmergencyBearerTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/JwtRequestFilterEmergencyNoAuthTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyAccessPolicyTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyBearerPolicyTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyNoAuthPolicyTest.java',
    'src/test/java/za/co/mawa/bes/configuration/jwt/MawaPayEmergencyRefreshPolicyTest.java',
    'src/test/java/za/co/mawa/bes/controller/v2/RefreshTokenControllerV2EmergencyBearerTest.java',
    'src/test/java/za/co/mawa/bes/controller/v2/RefreshTokenControllerV2EmergencyNoAuthTest.java',
    'README-MAWAPAY-EMERGENCY-BEARER.md',
    'README-MAWAPAY-EMERGENCY-NOAUTH.md'
)

foreach ($relativePath in $obsoleteFiles) {
    $target = Join-Path $backendRoot $relativePath
    if (Test-Path -LiteralPath $target -PathType Leaf) {
        Remove-Item -LiteralPath $target -Force
        Write-Output "Removed: $relativePath"
    }
}

Write-Output 'Emergency authentication bypass rollback completed. Verify using git status and run the JWT tests.'
