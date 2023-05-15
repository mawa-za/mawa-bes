package za.co.mawa.bes.utils;

import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Random;
import java.util.UUID;
@Component
public class SimpleKeyGenerator implements KeyGenerator {

    @Override
    public Key generateKey(String keyString) {
        Key key = new SecretKeySpec(keyString.getBytes(), 0, keyString.getBytes().length, "HMAC");
        return key;
    }

    @Override
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    @Override
    public String generatePassword() {
        String letters = "";
        for (char c = 'a'; c <= 'z'; c++) {
            letters = letters + c;
        }
        String numbers = "1234567890";
        String specialChar = "!@#$%^&*)<?{}~_-+={];(>";
        String combination = letters.toUpperCase() + numbers + letters + specialChar;
        int len = 24;
        char [] password = new char[len];
        Random r =new Random();
        for(int x = 0;x<len;x++)
        {
            password[x] = combination.charAt(r.nextInt(combination.length()));
        }
        return new String(password);
    }

}