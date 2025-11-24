package com.oddlabs.tt.gui;

import org.jspecify.annotations.NonNull;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import java.util.logging.Level;
import java.util.logging.Logger;

final class GUIErrorHandler implements ErrorHandler {
    private static final Logger logger = Logger.getLogger("SAXParseError");

	@Override
	public void fatalError(@NonNull SAXParseException e) {
        logger.log(Level.SEVERE,"fatal line " + e.getLineNumber() + ", uri " + e.getSystemId(), e);
		// ignore fatal errors (an exception is guaranteed)
	}

	// treat validation errors as fatal
	@Override
	public void error(@NonNull SAXParseException e) throws SAXParseException {
        logger.log(Level.SEVERE,"error line " + e.getLineNumber() + ", uri " + e.getSystemId(), e);
		throw e;
	}

	// dump warnings too
	@Override
	public void warning(@NonNull SAXParseException err) {
       logger.log(Level.WARNING,"line " + err.getLineNumber() + ", uri " + err.getSystemId(), err);
	}
}

