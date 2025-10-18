package com.oddlabs.converter;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public final class GeometryErrorHandler implements ErrorHandler {
	public void fatalError(SAXParseException exception) {
		// ignore fatal errors (an exception is guaranteed)
	}

	// treat validation errors as fatal
	public void error(SAXParseException e) throws SAXParseException {
		throw e;
	}

	// dump warnings too
	public void warning(SAXParseException err) {
		System.out.println ("** Warning, line " + err.getLineNumber () + ", uri " + err.getSystemId ());
		System.out.println("   " + err.getMessage ());
	}
}

