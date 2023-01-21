package za.co.mawa.bes.utils;

import java.security.Key;

public interface KeyGenerator {

    Key generateKey(String keyString);
}