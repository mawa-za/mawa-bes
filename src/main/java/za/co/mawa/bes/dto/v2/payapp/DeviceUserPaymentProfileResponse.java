package za.co.mawa.bes.dto.v2.payapp;

public record DeviceUserPaymentProfileResponse(
        String userId,
        String username,
        String cardTerminalId
) {
}
