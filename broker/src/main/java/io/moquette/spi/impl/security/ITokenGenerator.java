package io.moquette.spi.impl.security;

public interface ITokenGenerator {
	public String generateToken(String username);
}
