package com.nutritrack.util;

import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
  private static byte[] derive(String p, byte[] s) {
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
          .generateSecret(new PBEKeySpec(p.toCharArray(), s, 120000, 256))
          .getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static String hash(String password) {
    byte[] salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    return Base64.getEncoder().encodeToString(salt)
        + ":"
        + Base64.getEncoder().encodeToString(derive(password, salt));
  }

  public static boolean matches(String password, String stored) {
    String[] parts = stored.split(":");
    return MessageDigest.isEqual(
        derive(password, Base64.getDecoder().decode(parts[0])),
        Base64.getDecoder().decode(parts[1]));
  }
}
