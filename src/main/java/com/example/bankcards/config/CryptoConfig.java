package com.example.bankcards.config;

import com.example.bankcards.util.CryptoUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

@Configuration
public class CryptoConfig {

    @Bean
    public CryptoUtil cryptoUtil(CryptoProperties cryptoProperties) {
        String b64 = cryptoProperties.getAesKeyBase64();
        if (b64 == null || b64.isBlank()) {
            throw new IllegalStateException("crypto.aes-key-base64 must be set");
        }
        byte[] rawKey = Base64.getDecoder().decode(b64);
        if (rawKey.length != 16 && rawKey.length != 24 && rawKey.length != 32) {
            throw new IllegalStateException("AES key must be 16/24/32 bytes after Base64 decode");
        }
        return new CryptoUtil(rawKey);
    }
}
