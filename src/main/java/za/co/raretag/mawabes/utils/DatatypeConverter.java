package za.co.raretag.mawabes.utils;

import java.util.Base64;

public class DatatypeConverter {
    public static String printBase64Binary(String input){
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    public static String parseBase64Binary(String input){
        byte[] decodedBytes = Base64.getDecoder().decode(input);
        return new String(decodedBytes);
    }
}
