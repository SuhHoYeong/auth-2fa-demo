package com.suhhoyeong.backend.auth.model;

import lombok.Data;

@Data
public class User {
  private Long id;
  private String email;
  private String passwordHash; // users.password_hash
  private String provider; // LOCAL / GOOGLE
  private String providerSub; // GOOGLE sub
  private String phone;
  private Boolean twofaEnabled;
  private String twofaSecret; // base32 TOTP secret
}
