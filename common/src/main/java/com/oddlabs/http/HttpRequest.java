package com.oddlabs.http;

import com.oddlabs.net.Callable;
import com.oddlabs.net.Task;
import com.oddlabs.net.TaskThread;
import com.oddlabs.util.CryptUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

public final class HttpRequest {
    public static @NonNull Task doPost(@NonNull TaskThread task_thread, @NonNull HttpRequestParameters parameters, @NonNull HttpResponseParser parser, @NonNull HttpCallback callback) {
        try {
            URL url = new URL(parameters.url);
            return spawnPostRequest(task_thread, url, parameters, parser, callback);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static @NonNull Task doGet(@NonNull TaskThread task_thread, @NonNull HttpRequestParameters parameters, @NonNull HttpResponseParser parser, @NonNull HttpCallback callback) {
        URL url = constructURL(parameters);
        return spawnGetRequest(task_thread, url, parser, callback);
    }

    private static @NonNull Task spawnPostRequest(@NonNull TaskThread task_thread, final @NonNull URL url, final @NonNull HttpRequestParameters parameters, final @NonNull HttpResponseParser parser, final @NonNull HttpCallback callback) {
        return task_thread.addTask(new Callable<HttpResponse>() {
            @Override
            public @NonNull HttpResponse call() throws IOException {
                return runPostRequest(url, parameters, parser);
            }

            @Override
            public void taskCompleted(@NonNull HttpResponse result) {
                result.notify(callback);
            }

            @Override
            public void taskFailed(@NonNull Throwable e) {
                callback.error((IOException) e);
            }
        });
    }

    private static @NonNull Task spawnGetRequest(@NonNull TaskThread task_thread, final @NonNull URL url, final @NonNull HttpResponseParser parser, final @NonNull HttpCallback callback) {
        return task_thread.addTask(new Callable<HttpResponse>() {
            @Override
            public @NonNull HttpResponse call() throws IOException {
                return runGetRequest(url, parser);
            }

            @Override
            public void taskCompleted(@NonNull HttpResponse result) {
                result.notify(callback);
            }

            @Override
            public void taskFailed(@NonNull Throwable e) {
                callback.error((IOException) e);
            }
        });
    }

    private static void copy(@NonNull InputStream is, @Nullable OutputStream os) throws IOException {
        byte[] buf = new byte[4096];
        int ret;
        while ((ret = is.read(buf)) > 0) {
            if (os != null)
                os.write(buf, 0, ret);
        }
    }

    private static @NonNull HttpResponse runPostRequest(@NonNull URL url, @NonNull HttpRequestParameters parameters, @NonNull HttpResponseParser parser) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) openConnection(url);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        ByteArrayOutputStream byte_os = new ByteArrayOutputStream();
        try (OutputStreamWriter out = new OutputStreamWriter(byte_os, StandardCharsets.UTF_8)) {
            String query_string = parameters.createQueryString();
            out.write(query_string, 0, query_string.length());
        }

        conn.setRequestProperty("Content-Length", String.valueOf(byte_os.size()));
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        byte_os.writeTo(conn.getOutputStream());
        return readResponse(conn, parser);
    }

    private static @NonNull HttpResponse runGetRequest(@NonNull URL url, @NonNull HttpResponseParser parser) throws IOException {
        URLConnection conn = openConnection(url);
        return readResponse(conn, parser);
    }

    public static URLConnection openConnection(@NonNull URL url) throws IOException {
        URLConnection conn = url.openConnection();
        try {
            if (conn instanceof HttpsURLConnection connection)
                CryptUtils.setupHttpsConnection(connection);
            return conn;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static @NonNull HttpResponse readResponse(@NonNull URLConnection conn, @NonNull HttpResponseParser parser) throws IOException {
        try {
            try (InputStream is = conn.getInputStream()) {
                HttpResponse response = new OkResponse(parser.parse(is));
                copy(is, null); // Make sure the entire stream is read
                return response;
            }
        } catch (IOException _) {
            int response_code = ((HttpURLConnection) conn).getResponseCode();
            String response_message = ((HttpURLConnection) conn).getResponseMessage();
            ByteArrayOutputStream byte_os = new ByteArrayOutputStream();
            try (InputStream es = ((HttpURLConnection) conn).getErrorStream()) {
                copy(es, byte_os);
            }
            return new ErrorResponse(response_code, response_message);
        }
    }

    private static @NonNull URL constructURL(@NonNull HttpRequestParameters parameters) {
        try {
            return new URL(parameters.url + "?" + parameters.createQueryString());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

/*	private static void testLogin(Deterministic deterministic, HttpResponseParser parser, HttpCallback callback) {
		Map parameters = new java.util.HashMap();
		parameters.put("reg_key", "K6AA-Y33X-C7ZT-K4TF");
		parameters.put("username", "blah");
		parameters.put("password", "blah");
		doGet(deterministic, new HttpRequestParameters("https://localhost/matchservlet/login", parameters), parser, callback);
	}

	private static void testCreateUser(Deterministic deterministic, HttpResponseParser parser, HttpCallback callback) {
		Map parameters = new java.util.HashMap();
		parameters.put("reg_key", "K6AA-Y33X-C7ZT-K4TF");
		parameters.put("username", "blah");
		parameters.put("password", "blah");
		parameters.put("email", "blah@blah.com");
		doPost(deterministic, new HttpRequestParameters("https://localhost/matchservlet/login", parameters), parser, callback);
	}

	public static final void main(String[] args) throws IOException {
		final Deterministic deterministic = new com.oddlabs.event.NotDeterministic();
		final HttpResponseParser parser = new HttpResponseParser() {
			public final Object parse(InputStream in) throws IOException {
				return "OK!";
			}

		};
		testCreateUser(deterministic, parser, new DefaultHttpCallback() {
			public final void success(Object response) {
				testLogin(deterministic, parser, new DefaultHttpCallback() {
					public final void success(Object response) {
System.out.println("response = " + response);
					}

					public final void error(IOException e) {
						System.out.println("e = " + e);
					}
				});
			}

			public final void error(IOException e) {
				System.out.println("e = " + e);
			}
		});
		com.oddlabs.net.NetworkSelector.initSelector();
		while (true) {
			NetworkSelector.processTasks();
		}
	}*/

    private HttpRequest() {
    }
}
