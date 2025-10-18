package com.oddlabs.matchmaking;

import com.oddlabs.util.Utils;

import java.io.Serializable;

public final class LoginDetails implements Serializable {
	private final static long serialVersionUID = 1;

	public final static int MAX_EMAIL_LENGTH = 60;

	private final String email;

	public LoginDetails(String email) {
		this.email = email;
	}

        @Override
	public boolean equals(Object other) {
		if (!(other instanceof LoginDetails))
			return false;
		LoginDetails other_login = (LoginDetails)other;
		return other_login.getEmail().equals(email);
	}

        @Override
	public int hashCode() {
		return email.hashCode();
	}

	public boolean isValid() {
		return email != null && email.length() <= MAX_EMAIL_LENGTH && Utils.EMAIL_PATTERN.matcher(email).matches();
	}

	public String getEmail() {
		return email;
	}
}
