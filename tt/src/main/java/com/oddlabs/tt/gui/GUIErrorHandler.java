package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

public final class GUIErrorHandler implements ErrorHandler {
        @Override
	public void fatalError(SAXParseException exception) {
		// ignore fatal errors (an exception is guaranteed)
	}

	// treat validation errors as fatal
        @Override
	public void error(@NonNull SAXParseException e) throws SAXParseException {
		throw e;
	}

	// dump warnings too
        @Override
	public void warning(@NonNull SAXParseException err) {
			IO.println("** Warning, line " + err.getLineNumber() + ", uri " + err.getSystemId());
			IO.println("   " + err.getMessage());
	}
}

