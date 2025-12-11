package com.oddlabs.matchmaking;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;


public final class Login implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final String username;
	private final String password_digest;

	public Login(String username, String password_digest) {
		this.username = username;
		this.password_digest = password_digest;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof Login))
			return false;
		Login other_login = (Login)other;
		return other_login.getUsername().equals(username) && other_login.getPasswordDigest().equals(password_digest);
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}

	public boolean isValid() {
		return username != null && password_digest != null;
	}
	
	public String getUsername() {
		return username;
	}

	public String getPasswordDigest() {
		return password_digest;
	}
}
