package com.oddlabs.matchservlet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

public final class ContextInitializer implements ServletContextListener {
	private final static int KEY_SIZE = 1024;
	private final static String KEY_ALGORITHM = "RSA";

	public final void contextDestroyed(ServletContextEvent sce) {
	}

	private final static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator keygen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
			keygen.initialize(KEY_SIZE);
			return keygen.generateKeyPair();
		} catch (GeneralSecurityException e) {
			throw new RuntimeException (e);
		}
	}

	public final void contextInitialized(ServletContextEvent sce) {
		ServletContext ctx = sce.getServletContext();
		ctx.setAttribute("db", createDatabasePool());
		KeyPair key_pair = generateKeyPair();
		ctx.setAttribute("private_key", key_pair.getPrivate());
		ctx.setAttribute("public_key", key_pair.getPublic());
	}

	private static DataSource createDatabasePool() {
		try {
			Context envCtx = (Context)new InitialContext().lookup("java:comp/env");
			return (DataSource)envCtx.lookup("jdbc/matchDB");
		} catch(NamingException e) {
			throw new RuntimeException(e);
		}
	}
}
