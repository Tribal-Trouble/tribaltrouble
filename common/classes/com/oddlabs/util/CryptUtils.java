package com.oddlabs.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public final class CryptUtils {
	private final static MessageDigest digest;
	public final static int PASSWORD_DIGEST_LENGTH;

	static {
		try {
			digest = MessageDigest.getInstance("SHA-1");
			// The digest length is the length of the digest encoded as a unsigned hex string
			PASSWORD_DIGEST_LENGTH = digest.getDigestLength()*2;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static String digest(byte[] message_bytes) {
		byte[] digest_bytes = digest.digest(message_bytes);
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < digest_bytes.length; i++) {
			int b = ((int)digest_bytes[i]) & 0xff;
			int nibble0 = (b & 0xf0) >> 4;
			buf.append(Integer.toHexString(nibble0));
			int nibble1 = b & 0xf;
			buf.append(Integer.toHexString(nibble1));
		}
		return buf.toString();
	}

	private static String buggyDigest(byte[] message_bytes) {
		byte[] digest_bytes = digest.digest(message_bytes);
		// Pad array with one zeroed byte to make the hash unsigned
		byte[] unsigned_digest_bytes = new byte[digest_bytes.length + 1];
		System.arraycopy(digest_bytes, 0, unsigned_digest_bytes, 1, digest_bytes.length);
		BigInteger b = new BigInteger(unsigned_digest_bytes);
		return b.toString(16);
	}

	public static String digest(String str) {
		try {
			byte[] message_bytes = str.getBytes("UTF-8");
			return buggyDigest(message_bytes);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static void  setupHttpsConnection(HttpsURLConnection https_connection) throws Exception {
		SSLContext ssl_context = SSLContext.getInstance("SSL");
		ssl_context.init(null, new TrustManager[]{new X509TrustManager() {
                        @Override
			public final void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

                        @Override
			public final void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

                        @Override
			public final X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[]{};
			}
		}}, null);
		https_connection.setSSLSocketFactory(ssl_context.getSocketFactory());
		https_connection.setHostnameVerifier((String hostname, SSLSession session) -> true);
	}
/*
	public final static void main(String[] args) {
//		String bla = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_.:;?+={}[]()/&%¤#!1§<\\>'*";
System.out.println("		digest(\"xarerenlamer4kasbdvljh\") = " + 		digest("xarerenlamer4kasbdvljh"));
//		digest(bla);
	}
	*/

    private CryptUtils() {
    }
}
