package com.oddlabs.http;

import org.jspecify.annotations.NonNull;

final class OkResponse implements HttpResponse {
	private final Object result;

	OkResponse(Object result) {
		this.result = result;
	}

        @Override
	public void notify(@NonNull HttpCallback callback) {
		callback.success(result);
	}
}
