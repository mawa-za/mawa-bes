package za.co.mawa.bes.dto.v2.payapp;

public record DeviceIdentityResponse(String deviceId, String syncToken, long expiresInSeconds) {}
