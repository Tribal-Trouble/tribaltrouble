package com.oddlabs.graphservlet;

import javax.imageio.ImageIO;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public final class GraphServlet extends HttpServlet {
    private final int IMAGE_WIDTH = 532;
    private final int IMAGE_HEIGHT = 200;

    private final int BACKGROUND_COLOR = 0xFFFFFF;

    private static Connection getConnection() {
        try {
            // Obtain our environment naming context
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            // Look up our data source
            DataSource ds = (DataSource) envCtx.lookup("jdbc/graphDB");
            // Allocate and use a connection from the pool
            return ds.getConnection();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int[][] getGameData(Connection conn, int game_id) throws SQLException {
        // Find the max team_index so we know how many columns to produce
        PreparedStatement countStmt = conn.prepareStatement(
                "SELECT COALESCE(MAX(team_index), -1) FROM game_report_teams WHERE game_id = ?");
        countStmt.setInt(1, game_id);
        ResultSet countResult = countStmt.executeQuery();
        countResult.next();
        int maxTeam = countResult.getInt(1);
        int numTeams = maxTeam + 1;

        // Pivot normalized rows back into [tick, team0_score, team1_score, ...] arrays
        PreparedStatement stmt = conn.prepareStatement(
                "SELECT tick, team_index, score FROM game_report_teams WHERE game_id = ? ORDER BY tick, team_index");
        stmt.setInt(1, game_id);
        ResultSet result = stmt.executeQuery();

        // Group by tick
        var tickMap = new java.util.LinkedHashMap<Integer, int[]>();
        while (result.next()) {
            int tick = result.getInt("tick");
            int teamIndex = result.getInt("team_index");
            int score = result.getInt("score");
            int[] row = tickMap.computeIfAbsent(tick, _ -> {
                int[] r = new int[numTeams + 1];
                r[0] = tick;
                return r;
            });
            if (teamIndex + 1 < row.length) {
                row[teamIndex + 1] = score;
            }
        }

        return tickMap.values().toArray(int[][]::new);
    }

    private void printResult(OutputStream out, int[][] data) {
        BufferedImage img = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(BACKGROUND_COLOR));
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        g.setColor(Color.BLACK);
        g.drawLine(0, IMAGE_HEIGHT - 1, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1);
        g.drawLine(0, 0, 0, IMAGE_HEIGHT - 1);


        if (data.length > 0) {
            int max_x = 0;
            int max_y = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i][0] > max_x)
                    max_x = data[i][0];
                for (int j = 1; j < data[i].length; j++) {
                    if (data[i][j] > max_y)
                        max_y = data[i][j];
                }
            }
            for (int i = 0; i < data.length; i++) {
                int a = i + 1;
                int x = data[i][0] * IMAGE_WIDTH / max_x;
                int y = IMAGE_HEIGHT - 1;
                if ((a % 30) == 0) {
                    g.drawLine(x, y, x, y - 10);
                } else if ((a % 15) == 0) {
                    g.drawLine(x, y, x, y - 7);
                } else if ((a % 3) == 0) {
                    g.drawLine(x, y, x, y - 3);
                }
            }
            Color[] base_colors = new Color[]{
                    new Color(1f, .75f, 0f),
                    new Color(0f, .5f, 1f),
                    new Color(1f, 0f, .25f),
                    new Color(0f, 1f, .75f),
                    new Color(.75f, 0f, 1f),
                    new Color(.75f, 1f, 0f)};

            for (int j = 1; j < data[0].length; j++) {
                g.setColor(base_colors[(j - 1) % base_colors.length]);
                int last_x = 0;
                int last_y = IMAGE_HEIGHT;
                for (int i = 0; i < data.length; i++) {
                    int x = data[i][0] * IMAGE_WIDTH / max_x;
                    int y = IMAGE_HEIGHT - data[i][j] * IMAGE_HEIGHT / max_y;
                    g.drawLine(last_x, last_y, x, y);
                    last_x = x;
                    last_y = y;
                }
            }
        }
        try {
            ImageIO.write(img, "png", out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("image/png");

        String game_id_string = req.getParameter("game_id");
        int game_id;
        try {
            game_id = Integer.parseInt(game_id_string);
        } catch (NumberFormatException e) {
            res.sendError(500, e.getMessage());
            return;
        }

        try {
            Connection conn = getConnection();
            try {
                int[][] data = getGameData(conn, game_id);
                printResult(res.getOutputStream(), data);
            } finally {
                conn.close();
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
