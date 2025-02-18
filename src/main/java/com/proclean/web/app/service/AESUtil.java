package com.proclean.web.app.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AESUtil {

    @Value("${aes.secret-key}")
    private String secretKey;

    private static final String ALGORITHM = "AES";

    public String encrypt(String data) throws Exception {
        SecretKey key = getKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public String decrypt(String encryptedData) throws Exception {
        SecretKey key = getKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedBytes);
    }

    private SecretKey getKey() {
        byte[] keyBytes = secretKey.getBytes();


        if (keyBytes.length < 16) {
            keyBytes = Arrays.copyOf(keyBytes, 16);
        } else if (keyBytes.length < 24) {
            keyBytes = Arrays.copyOf(keyBytes, 24);
        } else if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        } else {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }

        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
