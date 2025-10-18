package com.oddlabs.http;

final class ErrorResponse implements HttpResponse {
	private final int error_code;
	private final String error_message;

	ErrorResponse(int error_code, String error_message) {
		this.error_code = error_code;
		this.error_message = error_message;
	}

        @Override
	public String toString() {
		return error_code + " " + error_message;
	}

        @Override
	public void notify(HttpCallback callback) {
		callback.error(error_code, error_message);
	}
}
