package com.oddlabs.matchservlet;

import java.sql.ResultSet;
import java.sql.SQLException;

final class GetFirstIntQuery implements Query {
    public Object process(ResultSet result) throws SQLException {
        result.first();
        return result.getInt(1);
    }
}
