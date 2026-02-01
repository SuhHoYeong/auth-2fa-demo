package com.suhhoyeong.backend.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class TotpService {
  private static final String HMAC_ALGO = "HmacSHA1";
  private static final int PERIOD_SECONDS = 30;
  private static final int DIGITS = 6;
  private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

  // Generate a 160-bit secret and return Base32 (no padding)
  public String generateSecret() {
    byte[] key = new byte[20];
    new SecureRandom().nextBytes(key);
    return base32Encode(key);
  }

  // Verify a TOTP code against a Base32-encoded secret
  public boolean verify(String base32Secret, int code) {
    if (base32Secret == null || base32Secret.isBlank())
      return false;
    byte[] key = base32Decode(base32Secret);
    long counter = Instant.now().getEpochSecond() / PERIOD_SECONDS;
    return generateOtp(key, counter) == code
        || generateOtp(key, counter - 1) == code
        || generateOtp(key, counter + 1) == code;
  }

  private int generateOtp(byte[] key, long counter) {
    try {
      Mac mac = Mac.getInstance(HMAC_ALGO);
      mac.init(new SecretKeySpec(key, HMAC_ALGO));
      byte[] data = new byte[8];
      for (int i = 7; i >= 0; i--) {
        data[i] = (byte) (counter & 0xFF);
        counter >>= 8;
      }
      byte[] hash = mac.doFinal(data);
      int offset = hash[hash.length - 1] & 0x0F;
      int binary = ((hash[offset] & 0x7f) << 24)
          | ((hash[offset + 1] & 0xff) << 16)
          | ((hash[offset + 2] & 0xff) << 8)
          | (hash[offset + 3] & 0xff);
      int otp = binary % (int) Math.pow(10, DIGITS);
      return otp;
    } catch (Exception e) {
      throw new IllegalStateException("TOTP generation failed", e);
    }
  }

  // --- Minimal Base32 (RFC 4648) encode/decode without padding ---
  private String base32Encode(byte[] data) {
    StringBuilder sb = new StringBuilder((data.length * 8 + 4) / 5);
    int buffer = 0, bitsLeft = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        int index = (buffer >> (bitsLeft - 5)) & 0x1F;
        sb.append(BASE32_ALPHABET[index]);
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) {
      int index = (buffer << (5 - bitsLeft)) & 0x1F;
      sb.append(BASE32_ALPHABET[index]);
    }
    return sb.toString();
  }

  private byte[] base32Decode(String s) {
    String str = s.trim().replace("=", "").toUpperCase();
    int outLen = (str.length() * 5) / 8;
    byte[] out = new byte[outLen];
    int buffer = 0, bitsLeft = 0, pos = 0;
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      int val = base32CharValue(c);
      if (val < 0)
        continue; // ignore invalid/whitespace
      buffer = (buffer << 5) | val;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        out[pos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
        bitsLeft -= 8;
      }
    }
    if (pos == outLen)
      return out;
    byte[] resized = new byte[pos];
    System.arraycopy(out, 0, resized, 0, pos);
    return resized;
  }

  private int base32CharValue(char c) {
    if (c >= 'A' && c <= 'Z')
      return c - 'A';
    if (c >= '2' && c <= '7')
      return (c - '2') + 26;
    return -1;
  }
}
