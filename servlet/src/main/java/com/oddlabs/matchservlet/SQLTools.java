package com.oddlabs.matchservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

final class SQLTools {
	static void doSQL(HttpServletResponse res, SQLAction action) throws ServletException, IOException {
		try {
			action.run();
		} catch (SQLException e) {
			e.printStackTrace();
			res.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
		}
	}
}
