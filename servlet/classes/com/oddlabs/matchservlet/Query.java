package com.oddlabs.matchservlet;

import java.sql.ResultSet;
import java.sql.SQLException;

interface Query {
	Object process(ResultSet result) throws SQLException;
}
