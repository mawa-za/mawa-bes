package za.co.mawa.bes.utils;

import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
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

}