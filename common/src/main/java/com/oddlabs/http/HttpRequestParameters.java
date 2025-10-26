package com.oddlabs.http;

import org.jspecify.annotations.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

public final class HttpRequestParameters {
	final String url;
	public final Map<String,String> parameters;

	public HttpRequestParameters(String url, Map<String,String> parameters) {
		this.url = url;
		this.parameters = parameters;
	}

	@NonNull String createQueryString() {
		if (parameters == null || parameters.isEmpty())
			return "";
		StringBuilder buffer = new StringBuilder();
		Iterator<Map.Entry<String,String>> parameter_entries = parameters.entrySet().iterator();
        while (parameter_entries.hasNext()) {
            Map.Entry<String,String> parameter = parameter_entries.next();
            buffer.append(parameter.getKey());
            buffer.append('=');
            try {
                buffer.append(URLEncoder.encode(parameter.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException never) { }
            if (parameter_entries.hasNext())
                buffer.append('&');
        }
        return buffer.toString();
	}
}
