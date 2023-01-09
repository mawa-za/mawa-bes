//package za.co.raretag.mawabes;
//
//import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
//import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import za.co.raretag.mawabes.entity.UserEntity;
//import za.co.raretag.mawabes.service.UserService;
//
//import java.util.List;
//
//public class testPasswordEncryption {
//
//    @Autowired
//    UserService userService;
//    @Test
//    public void testPasswordEncryption() {
//        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
//        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
//        config.setPassword("fourizar"); // encryptor's private key
//        config.setAlgorithm("PBEWithMD5AndDES");
//        config.setKeyObtentionIterations("1000");
//        config.setPoolSize("1");
//        config.setProviderName("SunJCE");
//        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
//        config.setStringOutputType("base64");
//        encryptor.setConfig(config);
//        String plainText = "Rrtg@6365";
//        String encryptedPassword = encryptor.encrypt(plainText);
//            System.out.println("encryptedPassword : " + encryptedPassword);
//
//        List<UserEntity> userEntityList = userService.getAll();
//        System.out.println(userEntityList.iterator().next().getId());
//    }
//}
