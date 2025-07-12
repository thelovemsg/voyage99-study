package kr.hhplus.be.server.common.utils;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Component
public class CryptoUtils {

    private static final String SECRET_KEY = "HHPLUS_SECRET_2024";

    /**
     * 보안 토큰 생성 (UUID + 현재시간)
     */
    public String generateSecureToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                System.currentTimeMillis();
    }

    /**
     * 토큰 암호화 (SHA-256)
     */
    public String encrypt(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String combined = rawToken + SECRET_KEY;
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // byte array를 hex string으로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("토큰 암호화 실패", e);
        }
    }
}