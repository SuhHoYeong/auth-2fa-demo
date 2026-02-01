package com.suhhoyeong.backend.auth.service;

import com.suhhoyeong.backend.auth.exception.InvalidCredentialsException;
import com.suhhoyeong.backend.auth.exception.WrongProviderException;
import com.suhhoyeong.backend.auth.mapper.UserMapper;
import com.suhhoyeong.backend.auth.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserMapper userMapper;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public AuthService(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public User loginLocal(String email, String password) {
    User user = userMapper.findByEmail(email);
    if (user == null) {
      throw new InvalidCredentialsException("invalid credentials");
    }
    if (!"LOCAL".equalsIgnoreCase(user.getProvider())) {
      throw new WrongProviderException("use social login");
    }
    if (user.getPasswordHash() == null || !encoder.matches(password, user.getPasswordHash())) {
      throw new InvalidCredentialsException("invalid credentials");
    }
    return user;
  }

  public User getByEmail(String email) {
    return userMapper.findByEmail(email);
  }

  public void setTwofaSecret(String email, String secret) {
    userMapper.updateTwofaSecretByEmail(email, secret);
  }

  public void setTwofaEnabled(String email, boolean enabled) {
    userMapper.setTwofaEnabledByEmail(email, enabled);
  }
}
