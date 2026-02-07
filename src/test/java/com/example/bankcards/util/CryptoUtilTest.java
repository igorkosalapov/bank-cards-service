package com.example.bankcards.util;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilTest {

    @Test
    void encryptDecrypt_roundTrip() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        CryptoUtil crypto = new CryptoUtil(key);

        String plaintext = "4111111111111111";
        String token = crypto.encrypt(plaintext);
        assertNotNull(token);
        assertNotEquals(plaintext, token);

        String decrypted = crypto.decrypt(token);
        assertEquals(plaintext, decrypted);
    }
}
