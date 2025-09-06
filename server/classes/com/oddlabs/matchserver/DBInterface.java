package com.oddlabs.matchserver;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GamePlayer;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.Login;
import com.oddlabs.matchmaking.LoginDetails;
import com.oddlabs.matchmaking.Participant;
import com.oddlabs.matchmaking.Profile;
import com.oddlabs.matchmaking.RankingEntry;
import com.oddlabs.matchserver.models.GameDataModel;
import com.oddlabs.matchserver.models.GamePlayerModel;
import com.oddlabs.matchserver.models.VersusMatchupModel;
import com.oddlabs.matchserver.models.VersusMatchupResultModel;
import com.oddlabs.util.CryptUtils;
import com.oddlabs.util.DBUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final strictfp class DBInterface {

    public static final boolean usernameExists(String username) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT username FROM registrations R WHERE lower(R.username) ="
                                    + " lower(?)");
            try {
                stmt.setString(1, username);
                ResultSet result = stmt.executeQuery();
                try {
                    boolean user_exists = result.next();
                    return user_exists;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "usernameExists", e);
            throw new RuntimeException(e);
        }
    }

    public static final void createUser(Login login, LoginDetails login_details, String reg_key) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "INSERT INTO registrations (username, email, password) values (?, ?,"
                                    + " ?)");
            try {
                stmt.setString(1, login.getUsername());
                stmt.setString(2, login_details.getEmail());
                stmt.setString(3, CryptUtils.digest(login.getPasswordDigest()));
                int row_count = stmt.executeUpdate();
                assert row_count == 1;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "createUser", e);
            throw new RuntimeException(e);
        }
    }

    public static final boolean queryUser(String username, String password) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT username, password FROM registrations R WHERE lower(R.username)"
                                    + " = lower(?) AND R.password = ? AND NOT R.disabled AND NOT"
                                    + " R.banned");
            try {
                stmt.setString(1, username);
                stmt.setString(2, CryptUtils.digest(password));
                ResultSet result = stmt.executeQuery();
                try {
                    boolean user_exists = result.next();
                    return user_exists;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "queryUser", e);
            throw new RuntimeException(e);
        }
    }

    public static final Profile[] getProfiles(String username, int revision) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT nick, rating, wins, losses, invalid FROM profiles P,"
                                    + " registrations R WHERE P.reg_id = R.id AND R.username = ?");
            try {
                stmt.setString(1, username);
                ResultSet result = stmt.executeQuery();
                try {
                    List profiles = new ArrayList();
                    int index = 1;
                    while (result.next()) {
                        String nick = result.getString("nick").trim();
                        int rating = result.getInt("rating");
                        int wins = result.getInt("wins");
                        int losses = result.getInt("losses");
                        int invalid = result.getInt("invalid");
                        profiles.add(new Profile(nick, rating, wins, losses, invalid, revision));
                    }
                    Profile[] profile_array = new Profile[profiles.size()];
                    for (int i = 0; i < profile_array.length; i++)
                        profile_array[i] = (Profile) profiles.get(i);

                    return profile_array;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "getProfiles", e);
            throw new RuntimeException(e);
        }
    }

    public static final Profile getProfile(String username, String nick, int revision) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT rating, wins, losses, invalid FROM profiles P, registrations R"
                                    + " WHERE P.reg_id = R.id AND R.username = ? AND P.nick = ?");
            try {
                stmt.setString(1, username);
                stmt.setString(2, nick);
                ResultSet result = stmt.executeQuery();
                try {
                    result.next();
                    int rating = result.getInt("rating");
                    int wins = result.getInt("wins");
                    int losses = result.getInt("losses");
                    int invalid = result.getInt("invalid");
                    return new Profile(nick, rating, wins, losses, invalid, revision);
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            return null;
        }
    }

    public static final void setLastUsedProfile(String username, String nick) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE registrations R SET last_used_profile = ? WHERE R.username ="
                                    + " ?");
            try {
                stmt.setString(1, nick);
                stmt.setString(2, username);
                int row_count = stmt.executeUpdate();
                assert row_count == 1;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "setLastUsedProfile", e);
            throw new RuntimeException(e);
        }
    }

    public static final String getLastUsedProfile(String username) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT last_used_profile FROM registrations R WHERE R.username = ?");
            try {
                stmt.setString(1, username);
                ResultSet result = stmt.executeQuery();
                try {
                    result.next();
                    String nick = result.getString("last_used_profile");
                    return nick;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getLastUsedProfile", e);
            throw new RuntimeException(e);
        }
    }

    private static final int getRegID(String username) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement("SELECT id FROM registrations R WHERE R.username = ?");
            try {
                stmt.setString(1, username);
                ResultSet result = stmt.executeQuery();
                try {
                    result.next();
                    return result.getInt("id");
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "private getRegID", e);
            throw new RuntimeException(e);
        }
    }

    public static final boolean nickExists(String nick) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT nick FROM profiles P WHERE lower(P.nick) = lower(?)");
            try {
                stmt.setString(1, nick);
                ResultSet result = stmt.executeQuery();
                try {
                    boolean nick_exists = result.next();
                    return nick_exists;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "queryUser", e);
            throw new RuntimeException(e);
        }
    }

    public static final void saveGameReport(int game_id, int tick, int[] team_score) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "INSERT INTO game_reports (game_id, tick, team1, team2, team3, team4,"
                                    + " team5, team6) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            try {
                stmt.setInt(1, game_id);
                stmt.setInt(2, tick);
                stmt.setInt(3, team_score[0]);
                stmt.setInt(4, team_score[1]);
                stmt.setInt(5, team_score[2]);
                stmt.setInt(6, team_score[3]);
                stmt.setInt(7, team_score[4]);
                stmt.setInt(8, team_score[5]);
                int row_count = stmt.executeUpdate();
                assert row_count == 1;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "saveGameReport", e);
            throw new RuntimeException(e);
        }
    }

    public static final void logPriority(int game_id, String nick1, String nick2, int priority) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "INSERT INTO connections (game_id, nick1, nick2, priority) "
                                    + "VALUES (?, ?, ?, ?)");
            try {
                stmt.setInt(1, game_id);
                stmt.setString(2, nick1);
                stmt.setString(3, nick2);
                stmt.setInt(4, priority);
                int row_count = stmt.executeUpdate();
                assert row_count == 1;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "logPriority", e);
            throw new RuntimeException(e);
        }
    }

    public static final void createProfile(String username, String nick) {
        int reg_id = getRegID(username);
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "INSERT INTO profiles (reg_id, nick, rating, wins, losses, invalid) "
                                    + "VALUES (?, ?, 1000, 0, 0, 0)");
            try {
                stmt.setInt(1, reg_id);
                stmt.setString(2, nick);
                int row_count = stmt.executeUpdate();
                assert row_count == 1;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "createProfile", e);
            throw new RuntimeException(e);
        }
    }

    public static final void deleteProfile(String username, String nick) {
        Profile profile = getProfile(username, nick, -1);
        if (profile != null) {
            int reg_id = getRegID(username);
            try {
                PreparedStatement stmt =
                        DBUtils.createStatement(
                                "INSERT INTO deleted_profiles (reg_id, nick, rating, wins, losses,"
                                        + " invalid) VALUES (?, ?, ?, ?, ?, ?)");
                try {
                    stmt.setInt(1, reg_id);
                    stmt.setString(2, profile.getNick());
                    stmt.setInt(3, profile.getRating());
                    stmt.setInt(4, profile.getWins());
                    stmt.setInt(5, profile.getLosses());
                    stmt.setInt(6, profile.getInvalid());
                    int row_count = stmt.executeUpdate();
                    assert row_count == 1 : row_count;
                } finally {
                    stmt.getConnection().close();
                }
            } catch (SQLException e) {
                System.out.println("Exception: " + e);
                MatchmakingServer.getLogger()
                        .throwing(DBInterface.class.getName(), "deleteProfile INSERT", e);
            }
            // drop profile
            try {
                PreparedStatement stmt =
                        DBUtils.createStatement("DELETE FROM profiles WHERE nick = ?");
                try {
                    stmt.setString(1, nick);
                    int row_count = stmt.executeUpdate();
                    assert row_count == 1 : row_count;
                } finally {
                    stmt.getConnection().close();
                }
            } catch (SQLException e) {
                System.out.println("Exception: " + e);
                MatchmakingServer.getLogger()
                        .throwing(DBInterface.class.getName(), "deleteProfile DELETE", e);
            }
        }
    }

    public static final void increaseLosses(String nick) {
        increaseField("losses", nick);
    }

    public static final void increaseWins(String nick) {
        increaseField("wins", nick);
    }

    public static final void increaseInvalidGames(String nick) {
        increaseField("invalid", nick);
    }

    public static final void increaseField(String field, String nick) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE profiles P SET "
                                    + field
                                    + " = "
                                    + field
                                    + " + 1 WHERE P.nick = ?");
            try {
                stmt.setString(1, nick);
                int result = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "update" + field, e);
        }
    }

    public static final void updateRating(String nick, int rating_delta) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE profiles P SET rating = rating + ? WHERE P.nick = ?");
            try {
                stmt.setInt(1, rating_delta);
                stmt.setString(2, nick);
                int result = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "updateRating", e);
        }
    }

    public static final int getWins(String nick) throws SQLException {
        return getIntField("wins", nick);
    }

    public static final int getRating(String nick) throws SQLException {
        return getIntField("rating", nick);
    }

    public static final int getIntField(String int_field, String nick) throws SQLException {
        // try {
        PreparedStatement stmt =
                DBUtils.createStatement(
                        "SELECT " + int_field + " FROM profiles P WHERE P.nick = ?");
        try {
            stmt.setString(1, nick);
            ResultSet result = stmt.executeQuery();
            try {
                result.next();
                return result.getInt(int_field);
            } finally {
                result.close();
            }
        } finally {
            stmt.getConnection().close();
        }
        /*
         * } catch (SQLException e) {
         * MatchmakingServer.getLogger().throwing(DBInterface.class.getName(),
         * "getIntField", e);
         * return 0;
         * }
         */
    }

    public static final String getSetting(String setting) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement("SELECT value FROM settings S WHERE S.property = ?");
            try {
                stmt.setString(1, setting);
                ResultSet result = stmt.executeQuery();
                try {
                    result.next();
                    return result.getString("value");
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "getSetting", e);
            throw new RuntimeException(e);
        }
    }

    public static final int getSettingsInt(String setting) {
        try {
            String value = getSetting(setting);
            return (Integer.valueOf(value)).intValue();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getSettingsInt", e);
            throw new RuntimeException(e);
        }
    }

    public static final RankingEntry[] getRankings(String nick, int radius) {
        try {
            String sql =
                    "SELECT nick, rating, wins, losses, invalid, row_num FROM (  SELECT nick,"
                        + " rating, wins, losses, invalid, ROW_NUMBER() OVER (ORDER BY rating DESC,"
                        + " (wins - losses) DESC, wins DESC) AS row_num   FROM profiles) ranked"
                        + " WHERE ABS(CAST(row_num AS SIGNED) - (  SELECT CAST(row_num AS SIGNED)"
                        + " FROM (    SELECT nick, ROW_NUMBER() OVER (ORDER BY rating DESC, (wins -"
                        + " losses) DESC, wins DESC) AS row_num FROM profiles  ) sub WHERE nick ="
                        + " ?)) <= ? ORDER BY row_num";

            PreparedStatement stmt = DBUtils.createStatement(sql);
            stmt.setString(1, nick);
            stmt.setInt(2, radius);
            ResultSet result = stmt.executeQuery();
            try {
                List<RankingEntry> rankings = new ArrayList<>();
                while (result.next()) {
                    String nick_name = result.getString("nick");
                    int rating = result.getInt("rating");
                    int wins = result.getInt("wins");
                    int losses = result.getInt("losses");
                    int invalid = result.getInt("invalid");
                    int ranking = result.getInt("row_num");
                    rankings.add(
                            new RankingEntry(ranking, nick_name, rating, wins, losses, invalid));
                }
                RankingEntry[] ranking_array = new RankingEntry[rankings.size()];
                for (int i = 0; i < ranking_array.length; i++) ranking_array[i] = rankings.get(i);
                return ranking_array;
            } finally {
                result.close();
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getRankings(nick, radius)", e);
            return new RankingEntry[0];
        }
    }

    public static final RankingEntry[] getRankings(int start, int count) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT nick, rating, wins, losses, invalid FROM profiles P WHERE"
                                    + " (P.wins > 0 OR P.losses > 0) ORDER BY rating DESC, (wins -"
                                    + " losses) DESC, wins DESC LIMIT ? OFFSET ?");
            try {
                stmt.setInt(1, count);
                stmt.setInt(2, start);
                ResultSet result = stmt.executeQuery();
                try {
                    List rankings = new ArrayList();
                    int index = 1;
                    while (result.next()) {
                        String nick = result.getString("nick").trim();
                        int rating = result.getInt("rating");
                        int wins = result.getInt("wins");
                        int losses = result.getInt("losses");
                        int invalid = result.getInt("invalid");
                        rankings.add(
                                new RankingEntry(index++, nick, rating, wins, losses, invalid));
                    }
                    RankingEntry[] ranking_array = new RankingEntry[rankings.size()];
                    for (int i = 0; i < ranking_array.length; i++)
                        ranking_array[i] = (RankingEntry) rankings.get(i);

                    return ranking_array;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getTopRankings", e);
            return new RankingEntry[0];
        }
    }

    public static final RankingEntry[] getTopRankings(int number) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT nick, rating, wins, losses, invalid FROM profiles P WHERE"
                                    + " P.wins >= "
                                    + GameSession.MIN_WINS_FOR_RANKING
                                    + " ORDER BY rating DESC, (wins - losses) DESC, wins DESC LIMIT"
                                    + " ?");
            try {
                stmt.setInt(1, number);
                ResultSet result = stmt.executeQuery();
                try {
                    List rankings = new ArrayList();
                    int index = 1;
                    while (result.next()) {
                        String nick = result.getString("nick").trim();
                        int rating = result.getInt("rating");
                        int wins = result.getInt("wins");
                        int losses = result.getInt("losses");
                        int invalid = result.getInt("invalid");
                        rankings.add(
                                new RankingEntry(index++, nick, rating, wins, losses, invalid));
                    }
                    RankingEntry[] ranking_array = new RankingEntry[rankings.size()];
                    for (int i = 0; i < ranking_array.length; i++)
                        ranking_array[i] = (RankingEntry) rankings.get(i);

                    return ranking_array;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getTopRankings", e);
            return new RankingEntry[0];
        }
    }

    public static final void createGame(Game game, String nick) {
        try {
            Connection conn = DBUtils.createDatabaseConnection();
            PreparedStatement stmt =
                    conn.prepareStatement(
                            "INSERT INTO games (time_create, name, rated, speed, size, hills,"
                                + " trees, resources, mapcode, status) VALUES (?, ?, ?, ?, ?, ?, ?,"
                                + " ?, ?, ?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS);

            try {
                stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stmt.setString(2, game.getName());
                if (game.isRated()) stmt.setString(3, "Y");
                else stmt.setString(3, "N");
                stmt.setString(4, "" + game.getGamespeed());
                stmt.setString(5, "" + (game.getSize() + 1));
                stmt.setInt(6, game.getHills());
                stmt.setInt(7, game.getTrees());
                stmt.setInt(8, game.getSupplies());
                stmt.setString(9, game.getMapcode());
                stmt.setString(10, "created");

                int row_count = stmt.executeUpdate();
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int gameId = generatedKeys.getInt(1);
                    // System.out.println("Game created with ID: " + gameId);
                    game.setDatabaseID(gameId);
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception(createGame): " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "createGame", e);
            return;
        }
    }

    public static final GameDataModel getGame(int game_id, boolean get_player_data) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT"
                                + " time_create,name,rated,speed,size,hills,trees,resources,mapcode,status,id,winner,time_stop,time_start"
                                + " FROM games G WHERE G.id = ?");
            try {
                stmt.setInt(1, game_id);
                ResultSet result = stmt.executeQuery();
                try {
                    if (result.next()) {
                        GameDataModel gameData = new GameDataModel();
                        gameData.setTimeCreate(result.getTimestamp("time_create"));
                        gameData.setName(result.getString("name"));
                        gameData.setRated(result.getString("rated"));
                        int speed = result.getInt("speed");
                        if (!result.wasNull()) {
                            gameData.setSpeed(speed);
                        }
                        int size = result.getInt("size");
                        if (!result.wasNull()) {
                            gameData.setSize(size);
                        }
                        int hills = result.getInt("hills");
                        if (!result.wasNull()) {
                            gameData.setHills(hills);
                        }
                        int trees = result.getInt("trees");
                        if (!result.wasNull()) {
                            gameData.setTrees(trees);
                        }
                        int resources = result.getInt("resources");
                        if (!result.wasNull()) {
                            gameData.setResources(resources);
                        }
                        gameData.setMapcode(result.getString("mapcode"));
                        gameData.setStatus(result.getString("status"));
                        int id = result.getInt("id");
                        if (!result.wasNull()) {
                            gameData.setId(id);
                        }
                        int winner = result.getInt("winner");
                        if (!result.wasNull()) {
                            gameData.setWinner(winner);
                        }
                        gameData.setTimeStop(result.getTimestamp("time_stop"));
                        gameData.setTimeStart(result.getTimestamp("time_start"));

                        if (get_player_data) {
                            System.out.println("Fetching player data for game ID: " + game_id);
                            PreparedStatement game_players_stmt =
                                    DBUtils.createStatement(
                                            "SELECT * FROM game_players WHERE game_id = ?");
                            game_players_stmt.setInt(1, game_id);
                            ResultSet game_players_result = game_players_stmt.executeQuery();
                            ArrayList<GamePlayerModel> nicks = new ArrayList<>();
                            while (game_players_result.next()) {
                                GamePlayerModel player =
                                        new GamePlayerModel(
                                                game_players_result.getString("nick"),
                                                game_players_result.getString("race"),
                                                game_players_result.getInt("team"));
                                nicks.add(player);
                            }
                            com.oddlabs.matchmaking.Profile[] fetchedProfiles =
                                    DBInterface.getProfilesByNick(
                                            nicks.stream()
                                                    .map(GamePlayerModel::getPlayerName)
                                                    .toArray(String[]::new));
                            System.out.println(
                                    "Fetched "
                                            + fetchedProfiles.length
                                            + " profiles for game ID: "
                                            + game_id);
                            // Map nick to Profile
                            java.util.Map<String, Profile> nickToProfile =
                                    new java.util.HashMap<>();
                            for (Profile pf : fetchedProfiles) {
                                if (pf != null && pf.getNick() != null) {
                                    nickToProfile.put(pf.getNick(), pf);
                                }
                            }

                            // Map player names to their profiles
                            for (GamePlayerModel player : nicks) {
                                Profile profile = nickToProfile.get(player.getPlayerName());
                                if (profile != null) {
                                    player.setProfile(profile);
                                }
                            }
                        }

                        return gameData;
                    }
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "getGame", e);
        }
        return null;
    }

    // Helper function to create IN clause with placeholders
    private static final String createInClause(int parameterCount) {
        if (parameterCount <= 0) {
            return "()";
        }

        StringBuilder inClause = new StringBuilder("(");
        for (int i = 0; i < parameterCount; i++) {
            if (i > 0) {
                inClause.append(",");
            }
            inClause.append("?");
        }
        inClause.append(")");
        return inClause.toString();
    }

    public static final VersusMatchupResultModel getMatchupStats(
            String player1, String player2, boolean only1v1Matchups) {
        String query =
                "WITH two_player_games AS (   SELECT game_id FROM game_players GROUP BY game_id"
                    + " HAVING COUNT(*) = 2 ) SELECT g.id AS game_id, CASE   WHEN g.winner ="
                    + " gp.team THEN 'Player1'   WHEN g.winner = gp2.team THEN 'Player2'   ELSE"
                    + " 'Neither' END AS vsResult, gp.nick AS player1_name, gp2.nick AS"
                    + " player2_name,  g.name, g.mapcode, g.time_start FROM game_players gp   INNER"
                    + " JOIN game_players gp2 ON gp.game_id = gp2.game_id AND gp.team <> gp2.team  "
                    + " INNER JOIN games g ON g.id = gp.game_id   INNER JOIN two_player_games tpg"
                    + " ON tpg.game_id = g.id WHERE g.winner IS NOT NULL   AND gp.nick = ?   AND"
                    + " gp2.nick = ?   AND gp.team <> gp2.team ORDER BY gp.game_id DESC;";
        try {
            PreparedStatement stmt = DBUtils.createStatement(query);
            stmt.setString(1, player1);
            stmt.setString(2, player2);
            ResultSet result = stmt.executeQuery();
            int p1Wins = 0;
            int p2Wins = 0;
            int neitherWins = 0;
            ArrayList<VersusMatchupModel> recentMatchups = new ArrayList<>();
            while (result.next()) {
                String vsResult = result.getString("vsResult").trim();
                if (vsResult.equals("Player1")) {
                    p1Wins++;
                } else if (vsResult.equals("Player2")) {
                    p2Wins++;
                } else {
                    neitherWins++;
                }
                if (!vsResult.equals("Neither") && recentMatchups.size() < 10) {
                    int gameId = result.getInt("game_id");
                    String player1Name = result.getString("player1_name");
                    String player2Name = result.getString("player2_name");
                    String winnerName = null;
                    if (vsResult.equals("Player1")) {
                        winnerName = player1Name;
                    } else if (vsResult.equals("Player2")) {
                        winnerName = player2Name;
                    }
                    String gameName = result.getString("name");
                    String mapSeed = result.getString("mapcode");
                    java.sql.Timestamp startTime = result.getTimestamp("time_start");
                    recentMatchups.add(
                            new com.oddlabs.matchserver.models.VersusMatchupModel(
                                    player1Name,
                                    player2Name,
                                    winnerName,
                                    gameId,
                                    gameName,
                                    mapSeed,
                                    startTime));
                }
            }
            System.out.println("p1 wins: " + p1Wins);
            System.out.println("p2 wins: " + p2Wins);
            System.out.println("neither wins: " + neitherWins);
            return new VersusMatchupResultModel(
                    player1, player2, p1Wins, p2Wins, neitherWins, recentMatchups);
        } catch (Exception e) {
            System.out.println("err" + e.getMessage());
        }
        return null;
    }

    public static final Profile[] getProfilesByNick(String[] nicks) {
        if (nicks == null || nicks.length == 0) {
            return new Profile[0];
        }

        // Filter out null nicks
        java.util.List<String> nonNullNicksList = new java.util.ArrayList<>();
        for (String nick : nicks) {
            if (nick != null) {
                nonNullNicksList.add(nick);
            }
        }
        if (nonNullNicksList.isEmpty()) {
            return new Profile[0];
        }
        String[] filteredNicks = nonNullNicksList.toArray(new String[0]);

        try {
            String inClause = createInClause(filteredNicks.length);
            String sql =
                    "SELECT nick, rating, wins, losses, invalid FROM profiles WHERE nick IN "
                            + inClause;
            PreparedStatement stmt = DBUtils.createStatement(sql);
            try {
                // Set the nicks as parameters
                for (int i = 0; i < filteredNicks.length; i++) {
                    stmt.setString(i + 1, filteredNicks[i]);
                }

                ResultSet result = stmt.executeQuery();
                try {
                    List profiles = new ArrayList();
                    while (result.next()) {
                        String nick = result.getString("nick").trim();
                        int rating = result.getInt("rating");
                        int wins = result.getInt("wins");
                        int losses = result.getInt("losses");
                        int invalid = result.getInt("invalid");
                        profiles.add(new Profile(nick, rating, wins, losses, invalid, -1));
                    }

                    Profile[] profile_array = new Profile[profiles.size()];
                    for (int i = 0; i < profile_array.length; i++) {
                        profile_array[i] = (Profile) profiles.get(i);
                    }

                    return profile_array;
                } finally {
                    result.close();
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getProfilesByNick", e);
        }
        return null;
    }

    /**
     * Sets status of created games to dropped after a server restart (aka when the matchmaker
     * initializes)
     */
    public static final void initDropGames() {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement("UPDATE games G SET status = ? WHERE G.status = ?");
            try {
                stmt.setString(1, "dropped");
                stmt.setString(2, "created");
                int result = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "initDropGames", e);
        }
    }

    public static final void dropGame(String nick) {
        try {

            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE games G inner join game_players GP on G.id = GP.id SET G.status"
                                    + " = ? WHERE GP.nick = ? AND G.status = ?");
            try {
                stmt.setString(1, "dropped");
                stmt.setString(2, nick);
                stmt.setString(3, "created");
                int result = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "dropGame", e);
        }
    }

    public static final void startGame(TimestampedGameSession tgs, MatchmakingServer server) {
        GameSession session = tgs.getSession();
        GamePlayer[] playerInfo = session.getPlayerInfo();

        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE games G SET "
                                    + "G.time_start = ?, G.status = ? WHERE G.id = ?");
            try {
                stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                stmt.setString(2, "started");
                stmt.setInt(3, tgs.getDatabaseID());
                int result = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception (startGame): " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "startGame", e);
        }

        try {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < playerInfo.length; i++) {
                if (i != playerInfo.length - 1) {
                    builder.append("(?, ?, ?, ?), ");
                } else {
                    builder.append("(?, ?, ?, ?)");
                }
            }
            String sql =
                    "INSERT INTO game_players (game_id, nick, team, race) VALUES "
                            + builder.toString();
            PreparedStatement playerStmt = DBUtils.createStatement(sql);
            int parameter_index = 1;
            for (int i = 0; i < playerInfo.length; i++) {
                GamePlayer player = playerInfo[i];
                playerStmt.setInt(parameter_index++, tgs.getDatabaseID());
                playerStmt.setString(parameter_index++, player.getNick());
                playerStmt.setInt(parameter_index++, player.getTeam());
                playerStmt.setInt(parameter_index++, player.getRace());
            }
            try {
                int result = playerStmt.executeUpdate();
            } finally {
                playerStmt.getConnection().close();
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "startGame", e);
        }
    }

    /**
     * Ends a game session and updates the database.
     *
     * @param tgs The timestamped game session to end.
     * @param end_time The time at which the game ended.
     * @param winner The ID of the winning team. When -1 then the player lost against the AI or the
     *     game state was determined to be invalid and someone cheated. NULL if the game does not
     *     end naturally (dropped for example)
     */
    public static final void endGame(TimestampedGameSession tgs, long end_time, int winner) {
        GameSession session = tgs.getSession();
        Participant[] participants = session.getParticipants();

        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE games G SET G.time_stop = ?, G.status = ?, G.winner = ? WHERE"
                                    + " G.id = ?");
            try {
                stmt.setTimestamp(1, new Timestamp(end_time));
                stmt.setString(2, "completed");
                stmt.setInt(3, winner);
                stmt.setInt(4, tgs.getDatabaseID());
                int result = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "endGame", e);
        }
    }

    public static final boolean isProfileRegisteredToDiscord(String nick) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT COUNT(*) FROM discord_to_profiles WHERE nick = ?");
            try {
                stmt.setString(1, nick);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "isProfileRegistered", e);
        }
        return false;
    }

    public static final long getDiscordUserIdForProfile(String nick) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT discord_id FROM discord_to_profiles WHERE nick = ?");
            try {
                stmt.setString(1, nick);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getLong("discord_id");
                }
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getDiscordUserIdForProfile", e);
        }
        return -1L;
    }

    public static final String[] getProfilesRegisteredToDiscordUser(long discord_user_id) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "SELECT nick FROM discord_to_profiles WHERE discord_id = ?");
            try {
                stmt.setLong(1, discord_user_id);
                ResultSet result = stmt.executeQuery();
                List<String> nicks = new ArrayList<String>();
                while (result.next()) {
                    String nick = result.getString("nick").trim();
                    nicks.add(nick);
                }

                return nicks.toArray(new String[0]);
            } finally {
                stmt.getConnection().close();
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "registerProfileToDiscordUser", e);
        }
        return new String[0];
    }

    public static final void registerProfileToDiscordUser(String nick, long discord_user_id) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "INSERT INTO discord_to_profiles (nick, discord_id) VALUES (?, ?)");
            try {
                stmt.setString(1, nick);
                stmt.setLong(2, discord_user_id);
                int row_count = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "registerProfileToDiscordUser", e);
        }
    }

    public static final void profileOnline(String nick) {
        MatchmakingServer.getLogger().info("profileOnline '" + nick + "'");
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement("INSERT INTO online_profiles (nick) VALUES (?)");
            try {
                stmt.setString(1, nick);
                int row_count = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger().throwing(DBInterface.class.getName(), "profileOnline", e);
        }
    }

    public static final void profileOffline(String nick) {
        MatchmakingServer.getLogger().info("profileOffline '" + nick + "'");
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement("DELETE FROM online_profiles WHERE nick = ?");
            try {
                stmt.setString(1, nick);
                int row_count = stmt.executeUpdate();
                assert row_count == 1 : row_count;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "profileOffline", e);
        }
    }

    public static final void profileSetGame(String nick, int game_id) {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement(
                            "UPDATE online_profiles O SET O.game_id = ? WHERE O.nick = ?");
            try {
                stmt.setInt(1, game_id);
                stmt.setString(2, nick);
                int row_count = stmt.executeUpdate();
                assert row_count == 1 : row_count + " nick = '" + nick + "' | game_id = " + game_id;
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "profileOffline", e);
        }
    }

    public static final void clearOnlineProfiles() {
        try {
            PreparedStatement stmt = DBUtils.createStatement("TRUNCATE TABLE online_profiles");
            try {
                int row_count = stmt.executeUpdate();
            } finally {
                stmt.getConnection().close();
            }
        } catch (SQLException e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "clearOnlineProfiles", e);
        }
    }

    public static final String[] getOnlineProfiles() {
        try {
            PreparedStatement stmt =
                    DBUtils.createStatement("SELECT nick FROM online_profiles ORDER BY nick");
            try {
                ResultSet result = stmt.executeQuery();
                List<String> nicks = new ArrayList<String>();
                while (result.next()) {
                    String nick = result.getString("nick").trim();
                    nicks.add(nick);
                }

                return nicks.toArray(new String[0]);
            } finally {
                stmt.getConnection().close();
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            MatchmakingServer.getLogger()
                    .throwing(DBInterface.class.getName(), "getOnlineProfiles", e);
        }
        return new String[0];
    }
}
