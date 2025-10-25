package com.oddlabs.http;

final class OkResponse implements HttpResponse {
	private final Object result;

	OkResponse(Object result) {
		this.result = result;
	}

        @Override
	public void notify(HttpCallback callback) {
		callback.success(result);
	}
}
