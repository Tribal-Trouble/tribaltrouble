package com.oddlabs.registration;

import java.io.Serial;

public final class RegistrationKeyFormatException extends NumberFormatException {
	@Serial
	private static final long serialVersionUID = 3673901824484153336L;

	public static final int TYPE_INVALID_CHAR = 0;
	public static final int TYPE_INVALID_LENGTH = 1;
	public static final int TYPE_INVALID_KEY = 2;

	private final int type;

	private int stripped_length;
	private char invalid_char;

	public RegistrationKeyFormatException(int type) {
		this.type = type;
	}

	public RegistrationKeyFormatException(int type, char invalid_char) {
		this(type);
		this.invalid_char = invalid_char;
	}

	public RegistrationKeyFormatException(int type, int stripped_length) {
		this(type);
		this.stripped_length = stripped_length;
	}

	public int getType() {
		return type;
	}

	public char getInvalidChar() {
		return invalid_char;
	}

	public int getStrippedLength() {
		return stripped_length;
	}
}
