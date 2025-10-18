package com.oddlabs.matchservlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;

strictfp interface SQLAction {
	void run() throws SQLException, ServletException, IOException;
}
