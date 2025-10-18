package com.oddlabs.matchserver;

public final class InvalidUsernameException extends Exception {
	private final int error_code;

	public InvalidUsernameException(int error_code) {
		this.error_code = error_code;
	}

	public int getErrorCode() {
		return error_code;
	}
}
