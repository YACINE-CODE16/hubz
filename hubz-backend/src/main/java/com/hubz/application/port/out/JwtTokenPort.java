package com.hubz.application.port.out;

public interface JwtTokenPort {

    String generateToken(String email);
}
