package com.oddlabs.net;

import java.io.IOException;

interface Handler {
	void handle() throws IOException;
	void handleError(IOException e) throws IOException;
}
