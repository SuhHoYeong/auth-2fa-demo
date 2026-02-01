package com.suhhoyeong.backend.auth.exception;

public class WrongProviderException extends RuntimeException {
  public WrongProviderException(String message) {
    super(message);
  }
}
