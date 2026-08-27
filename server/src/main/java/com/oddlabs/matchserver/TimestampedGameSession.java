package com.oddlabs.matchserver;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.oddlabs.matchmaking.GamePlayer;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.matchmaking.Participant;
import com.oddlabs.matchmaking.PlayerTypes;
import com.oddlabs.matchserver.discord.DiscordEmbedCreator;

public final class TimestampedGameSession {
    private static final long JOIN_MAX_TIME = 3 * 60 * 1000;
    private static final long END_GAME_MIN_TIME = 1 * 60 * 1000; // replace with clients reporting back there end time

    private static final int PARTICIPANT_UNKNOWN = 0;
    private static final int PARTICIPANT_JOINED = 1;
    private static final int PARTICIPANT_FREE_QUIT = 2;
    private static final int PARTICIPANT_QUIT = 3;
    private static final int PARTICIPANT_LOST = 4;
    private static final int PARTICIPANT_WON = 5;

    private static final String[] PARTICIPANT_DEBUG_CHARS = {"U", "J", "F", "Q", "L", "W"};

    private static final int TEAM_UNKNOWN = 0;
    private static final int TEAM_QUIT = 1;
    private static final int TEAM_LOST = 2;
    private static final int TEAM_WON = 3;

    private static final int GAME_STARTING = 1;
    private static final int GAME_ALL_JOINED = 2;
    private static final int GAME_INVALID = 3;

    private static final float STATUS_WINNING_FACTOR = 2f;
    private static final int STATUS_WINNING_TICK = 10000;

    private final long create_timestamp;
    private final int[] participant_state;
    private final GameSession session;
    private final int database_id;
    private int game_state = GAME_STARTING;
    private long start_timestamp;
    private boolean free_quit = true;
    private int[] last_status;
    private int last_tick = -1;
    private boolean game_ended;

    private boolean all_5_wins;
    private int[] player_ratings;

    private File commandEventFile;
    private DataOutputStream commandEventStream;
    private byte[] worldParamsData;

    private FileWriter spectatorFileWriter;
    private final Set<Integer> spectatorTicksWritten = new HashSet<>();
    private boolean spectatorFileChecked;

    public TimestampedGameSession(GameSession session, int database_id) {
        this.session = session;
        this.database_id = database_id;
        int num_participants = session.getParticipants().length;
        participant_state = new int[num_participants];
        last_status = new int[num_participants];
        this.create_timestamp = System.currentTimeMillis();
        String nicks = " ";
        for (int i = 0; i < num_participants; i++)
            nicks += session.getParticipants()[i].getNick() + " ";
        MatchmakingServer.getLogger().info(
                "Game " + database_id + " created. [" + nicks + "] " + getParticipantStates());

        try {
            String dirPath = ServerConfiguration.getInstance().get(ServerConfiguration.SPECTATOR_DATA_DIR);
            if (dirPath == null || dirPath.isEmpty()) dirPath = "/var/games";
            File spectatorDir = new File(dirPath);
            if (!spectatorDir.exists()) spectatorDir.mkdirs();
            spectatorFileWriter = new FileWriter(new File(spectatorDir, String.valueOf(database_id)));
            commandEventFile = new File(spectatorDir, database_id + ".events");
            commandEventStream = new DataOutputStream(new FileOutputStream(commandEventFile));
        } catch (IOException e) {
            MatchmakingServer.getLogger().warning(
                    "Failed to create spectator files for game " + database_id + ": " + e.getMessage());
        }
    }

    private String getParticipantStates() {
        String result = "";
        for (int i = 0; i < participant_state.length; i++)
            result += PARTICIPANT_DEBUG_CHARS[participant_state[i]];
        return result;

    }

    public GameSession getSession() {
        return session;
    }

    public int getDatabaseID() {
        return database_id;
    }

    public boolean join(MatchmakingServer server, Client client) {
        if (!free_quit || game_state != GAME_STARTING || System.currentTimeMillis() - create_timestamp > JOIN_MAX_TIME)
            return false;
        int index = findIndex(server, client);

        if (index != -1 && participant_state[index] == PARTICIPANT_UNKNOWN) {
            Participant[] participants = session.getParticipants();
            participant_state[index] = PARTICIPANT_JOINED;
            MatchmakingServer.getLogger().info("Game " + database_id + ": joined " + getParticipantStates());

            int num_joined = 0;
            for (int i = 0; i < participant_state.length; i++)
                if (participant_state[i] != PARTICIPANT_UNKNOWN)
                    num_joined++;
            if (num_joined == participant_state.length) {
                start_timestamp = System.currentTimeMillis();
                game_state = GAME_ALL_JOINED;
                MatchmakingServer.getLogger().info(
                        "Game " + database_id + ": all joined game " + getParticipantStates());

                DiscordEmbedCreator.SendGameStartedDiscordEmbed(session, database_id);

                //saving rated player info (someone could lose and delete a profile before the game ends)
                all_5_wins = true;
                player_ratings = new int[participants.length];
                for (int i = 0; i < participants.length; i++) {
                    String nick = participants[i].getNick();
                    try {
                        if (DBInterface.getWins(nick) < GameSession.MIN_WINS_FOR_RANKING)
                            all_5_wins = false;
                    } catch (SQLException e) {
                        all_5_wins = false; //participant must be a guest
                        return true;
                    }
                    try {
                        player_ratings[i] = DBInterface.getRating(participants[i].getNick());
                    } catch (SQLException e) {
                        throw new RuntimeException("Could you find rating for nick=" + nick + " e=" + e);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public long getStartTime() {
        return start_timestamp;
    }

    public void freeQuitStop() {
        free_quit = false;
    }

    public void updateGameStatus(int tick, int[] status) {
        if (status.length != session.getParticipants().length || game_state != GAME_ALL_JOINED)
            return;
        if (last_status != null) {
            if (tick > last_tick) {
                last_tick = tick;
                last_status = status;
                DBInterface.saveGameReport(database_id, tick, getTeamScores(status));
            } else if (tick == last_tick) {
                for (int i = 0; i < status.length; i++)
                    if (last_status[i] != status[i]) {
                        last_status = null;
                        return;
                    }
            }
        }
    }

    public void updateSpectatorInfo(int tick, String info) {
        if (spectatorFileWriter == null) {
            if (!spectatorFileChecked) {
                spectatorFileChecked = true;
                MatchmakingServer.getLogger().warning("Spectator file writer not initialized for game " + database_id);
            }
            return;
        }
        try {
            if (!spectatorTicksWritten.contains(tick)) {
                spectatorFileWriter.write(info);
                spectatorFileWriter.flush();
                spectatorTicksWritten.add(tick);
            }
        } catch (IOException e) {
            MatchmakingServer.getLogger().warning(
                    "Error writing spectator data for game " + database_id + ": " + e.getMessage());
        }
    }

    public void updateWorldParams(byte[] data) {
        this.worldParamsData = data;
        MatchmakingServer.getLogger().info("Game " + database_id + ": world params stored (" + data.length + " bytes)");
    }

    public byte[] getWorldParamsData() {
        return worldParamsData;
    }

    public int getLastTick() {
        return last_tick;
    }

    public void updateCommandEvent(int tick, int client_id, short event_size, byte[] event_data) {
        if (commandEventStream == null) return;
        if (event_size <= 0 || event_size > event_data.length) return;
        if (tick > last_tick) last_tick = tick;
        try {
            commandEventStream.writeInt(tick);
            commandEventStream.writeInt(client_id);
            commandEventStream.writeShort(event_size);
            commandEventStream.write(event_data, 0, event_size);
            commandEventStream.flush();
        } catch (IOException e) {
            MatchmakingServer.getLogger().warning(
                    "Error writing command event for game " + database_id + ": " + e.getMessage());
        }
    }

    public byte[] readEventLog() {
        if (commandEventFile == null || !commandEventFile.exists()) return new byte[0];
        try {
            if (commandEventStream != null) commandEventStream.flush();
            try (FileInputStream fis = new FileInputStream(commandEventFile)) {
                return fis.readAllBytes();
            }
        } catch (IOException e) {
            MatchmakingServer.getLogger().warning(
                    "Error reading event log for game " + database_id + ": " + e.getMessage());
            return new byte[0];
        }
    }

    private int getWinningTeamFromLastStatus() {
        if (last_status != null && last_tick > STATUS_WINNING_TICK) {
            int[] team_score = getTeamScores(last_status);
            int best_team = -1;
            int best_score = 1;
            int next_score = 1;
            for (int i = 0; i < team_score.length; i++) {
                if (team_score[i] > best_score) {
                    next_score = best_score;
                    best_score = team_score[i];
                    best_team = i;
                } else if (team_score[i] > next_score) {
                    next_score = team_score[i];
                }
            }
            float factor = best_score / (float) next_score;
            if (factor >= STATUS_WINNING_FACTOR) {
                return best_team;
            }
        }
        return -1;
    }

    private int[] getTeamScores(int[] status) {
        int[] team_score = new int[MatchmakingServerInterface.MAX_PLAYERS];
        Participant[] participants = session.getParticipants();
        for (int i = 0; i < participants.length; i++)
            team_score[participants[i].getTeam()] += status[i];
        return team_score;
    }

    public void participantQuit(MatchmakingServer server, Client client) {
        if (!free_quit) {
            return;
        }
        int index = findIndex(server, client);
        if (index == -1) {
            return;
        }
        if (participant_state[index] == PARTICIPANT_JOINED)
            participant_state[index] = PARTICIPANT_FREE_QUIT;
    }

    public void gameQuit(MatchmakingServer server, Client client) {
        int index = findIndex(server, client);

        if (!free_quit && !(participant_state[index] == PARTICIPANT_FREE_QUIT)) {
            game_state = GAME_INVALID;
            MatchmakingServer.getLogger().warning(
                    "Game " + database_id + " is now invalid. " + client.getUsername() + " tried to free_quit. " + getParticipantStates());
        }
        gameDone(server, client, PARTICIPANT_QUIT, "quit");
    }

    public void gameLost(MatchmakingServer server, Client client) {
        gameDone(server, client, PARTICIPANT_LOST, "lost");
    }

    public void gameWon(MatchmakingServer server, Client client) {
        int index = findIndex(server, client);
        if (participant_state[index] == PARTICIPANT_FREE_QUIT) {
            game_state = GAME_INVALID;
            MatchmakingServer.getLogger().warning(
                    "Game " + database_id + " is now invalid. " + client.getUsername() + " tried to win while having free_quit. " + getParticipantStates());
        }
        gameDone(server, client, PARTICIPANT_WON, "won");
    }

    private int findIndex(MatchmakingServer server, Client client) {
        Participant[] participants = session.getParticipants();
        for (int i = 0; i < participants.length; i++) {
            Client search_client = server.getClientFromID(participants[i].getMatchID());
            if (search_client == client)
                return i;
        }
        return -1;
    }

    private void gameDone(MatchmakingServer server, Client client, int result, String result_string) {
        participant_state[findIndex(server, client)] = result;
        MatchmakingServer.getLogger().info(
                "Game " + database_id + ": " + client.getUsername() + " finished. Result " + result_string + " " + getParticipantStates());
        //if (game_state != GAME_STARTING)
        evaluateGame(server);
    }

    private void evaluateGame(MatchmakingServer server) {
        Participant[] participants = session.getParticipants();
        int[] team_sizes = new int[MatchmakingServerInterface.MAX_PLAYERS];
        int[] team_done = new int[MatchmakingServerInterface.MAX_PLAYERS];
        int[] team_result = new int[MatchmakingServerInterface.MAX_PLAYERS];
        for (int i = 0; i < participants.length; i++) {
            int team = participants[i].getTeam();
            int state = participant_state[i];
            team_sizes[team]++;
            if (state == PARTICIPANT_QUIT || state == PARTICIPANT_LOST || state == PARTICIPANT_WON) {
                team_done[team]++;
                if (team_result[team] == TEAM_UNKNOWN && state == PARTICIPANT_QUIT)
                    team_result[team] = TEAM_QUIT;
                if ((team_result[team] == TEAM_UNKNOWN || team_result[team] == TEAM_QUIT) && state == PARTICIPANT_LOST)
                    team_result[team] = TEAM_LOST;
                if (state == PARTICIPANT_WON)
                    team_result[team] = TEAM_WON;
            }
        }
        int winning_teams = 0;
        int winning_team_index = -1;
        boolean teams_lost = false;
        for (int i = 0; i < team_sizes.length; i++) {
            if (team_sizes[i] == team_done[i]) {
                if (team_result[i] == TEAM_WON) {
                    winning_teams++;
                    winning_team_index = i;
                } else if (team_result[i] == TEAM_LOST)
                    teams_lost = true;
            } else
                return; // someone is still playing
        }
        long end_time = System.currentTimeMillis();
        if (winning_teams == 0) {
            int aiOnlyTeamsMask = findAiOnlyTeams();
            // No human team won. If there were some AI-only teams,
            // they all tie for first place.
            for (int team = 0; team < team_result.length; team++) {
                if ((aiOnlyTeamsMask & (1 << team)) != 0) {
                    team_result[team] = TEAM_WON;
                }
            }
            updateOpenSkillRatings(server, team_result);

            MatchmakingServer.getLogger().info(
                    "Game " + database_id + ". No human team won; AI teams tie for first place. " + getParticipantStates());
            DBInterface.endGame(this, end_time, -1);
            DiscordEmbedCreator.SendHumansLoseToBotsDiscordEmbed(session, database_id);
            game_ended = true;
            closeSpectatorStreams();
            return;
        }

        if (winning_teams > 1 || game_state == GAME_INVALID) {
            winning_team_index = getWinningTeamFromLastStatus();
            if (winning_team_index != -1) {
                MatchmakingServer.getLogger().info(
                        "Game " + database_id + ". Team " + (winning_team_index + 1) + " won from status reports. " + getParticipantStates());
                teams_lost = true;
                for (int i = 0; i < team_result.length; i++)
                    if (i == winning_team_index)
                        team_result[i] = TEAM_WON;
                    else
                        team_result[i] = TEAM_LOST;
            } else {
                // someone cheated - everyone gets an invalid_game
                for (int i = 0; i < participants.length; i++) {
                    String nick = participants[i].getNick();
                    MatchmakingServer.getLogger().warning(
                            "Game " + database_id + ". " + nick + " ended invalid game " + getParticipantStates());
                    DBInterface.increaseInvalidGames(nick);
                    Client client = server.getClientFromID(participants[i].getMatchID());
                    if (client != null)
                        client.updateProfile();
                }
                MatchmakingServer.getLogger().warning(
                        "Game " + database_id + " was invalid. " + winning_teams + " winning teams. " + getParticipantStates());
                DBInterface.endGame(this, end_time, -1);
                DiscordEmbedCreator.SendInvalidatedGameDiscordEmbed(session, database_id);
                game_ended = true;
                closeSpectatorStreams();
                return;
            }
        }

        updateOpenSkillRatings(server, team_result);
        if (teams_lost) {
            teamWon(server, team_result);
            MatchmakingServer.getLogger().info(
                    "Game " + database_id + ". Team " + (winning_team_index + 1) + " won (humans vs humans). " + getParticipantStates());
            DBInterface.endGame(this, end_time, winning_team_index);
            DiscordEmbedCreator.SendHumansWinAgainstOtherHumans(winning_team_index, session, database_id);
        } else {
            MatchmakingServer.getLogger().info(
                    "Game " + database_id + ". Team " + (winning_team_index + 1) + " won (humans vs bots). " + getParticipantStates());
            DBInterface.endGame(this, end_time, -1);
            DiscordEmbedCreator.SendHumansWinAgainstBotsDiscordEmbed(winning_team_index, session, database_id);
        }
        game_ended = true;
        closeSpectatorStreams();
    }

    /**
     * Finds the teams that consist solely of AI players (no human participants). Such teams
     * never report results to the server, so when no human team won, they are considered the
     * winners.
     *
     * @return a bitmask where bit {@code 1 << teamIndex} is set for each AI-only team
     */
    private int findAiOnlyTeams() {
        int humanTeamsMask = 0;
        int aiTeamsMask = 0;
        for (GamePlayer player : session.getPlayerInfo()) {
            int bit = 1 << player.getTeam();
            if (player.getPlayerType() == PlayerTypes.Human) {
                humanTeamsMask |= bit;
            } else {
                aiTeamsMask |= bit;
            }
        }
        return aiTeamsMask & ~humanTeamsMask;
    }

    @NullMarked
    private void updateOpenSkillRatings(MatchmakingServer server, int[] team_result) {
        var rateGameInput = transformTeamResultToRateGameFormat(team_result);
        if (rateGameInput == null) {
            return;
        }

        OpenSkillRatingSystem.rateGame(rateGameInput.teams(), rateGameInput.ranks());

        // Refresh the profiles of all human participants still connected.
        Participant[] participants = session.getParticipants();
        for (Participant participant : participants) {
            Client client = server.getClientFromID(participant.getMatchID());
            if (client != null) {
                client.updateProfile();
            }
        }
    }

    private record TransformTeamResultToRateGameFormatResult(
                                                             List<List<String>> teams,
                                                             int[] ranks
    ) {
    }

    @NullMarked
    private @Nullable TransformTeamResultToRateGameFormatResult transformTeamResultToRateGameFormat(int[] team_result) {
        Map<Integer, List<String>> teamsByIndex = new LinkedHashMap<>();
        GamePlayer[] playerInfo = session.getPlayerInfo();
        for (GamePlayer player : playerInfo) {
            teamsByIndex.computeIfAbsent(
                    player.getTeam(),
                    k -> new ArrayList<>()
            ).add(player.getNick());
        }

        List<List<String>> teams = new ArrayList<>();
        int[] ranks = new int[teamsByIndex.size()];
        Arrays.fill(ranks, 1);
        int i = 0;
        for (Map.Entry<Integer, List<String>> entry : teamsByIndex.entrySet()) {
            teams.add(entry.getValue());
            if (team_result[entry.getKey()] != TEAM_WON) {
                ranks[i] = 2;
            }
            i++;
        }

        if (teams.size() < 2) {
            // Would like to `throw` instead but apparently a rogue client could theoretically
            // cause this code path to activate.
            MatchmakingServer.getLogger().info(
                    "Game " + database_id + ": fewer than 2 non-empty teams, skipping OpenSkill rating");
            return null;
        }

        return new TransformTeamResultToRateGameFormatResult(teams, ranks);
    }

    private void closeSpectatorStreams() {
        try {
            if (commandEventStream != null) commandEventStream.close();
            if (spectatorFileWriter != null) spectatorFileWriter.close();
        } catch (IOException e) {
            MatchmakingServer.getLogger().warning(
                    "Error closing spectator streams for game " + database_id + ": " + e.getMessage());
        }
    }

    protected void finalize() {
        if (!game_ended)
            DBInterface.endGame(this, System.currentTimeMillis(), -1);
        closeSpectatorStreams();
    }

    private void teamWon(MatchmakingServer server, int[] team_result) {
        Participant[] participants = session.getParticipants();
        for (int i = 0; i < participants.length; i++) {
            String nick = participants[i].getNick();
            int team = participants[i].getTeam();
            if (team_result[team] == TEAM_WON) {
                MatchmakingServer.getLogger().info("Game " + database_id + ". " + nick + " won game");
                DBInterface.increaseWins(nick);
            } else if (team_result[team] == TEAM_LOST) {
                MatchmakingServer.getLogger().info("Game " + database_id + ". " + nick + " lost game");
                DBInterface.increaseLosses(nick);
            }
            Client client = server.getClientFromID(participants[i].getMatchID());
            if (client != null)
                client.updateProfile();
        }
        updatePlayerStreaks(team_result);
        updateSteamAchievements(team_result);
        if (session.isRated() && all_5_wins)
            rerateParticipants(server, team_result);
    }

    private void updatePlayerStreaks(int[] team_result) {
        Participant[] participants = session.getParticipants();
        for (int i = 0; i < participants.length; i++) {
            String nick = participants[i].getNick();
            int team = participants[i].getTeam();

            try {
                int[] streaks = DBInterface.getStreaks(nick);
                int currentStreak = streaks[0];
                int bestStreak = streaks[1];

                if (team_result[team] == TEAM_WON) {
                    currentStreak++;
                    if (currentStreak > bestStreak) {
                        bestStreak = currentStreak;
                    }
                } else if (team_result[team] == TEAM_LOST) {
                    currentStreak = 0;
                }

                DBInterface.updateStreaks(nick, currentStreak, bestStreak);
                MatchmakingServer.getLogger().info(
                        "Game " + database_id + ". Updated streaks for " + nick + " (currentStreak=" + currentStreak + ", bestStreak=" + bestStreak + ")");
            } catch (SQLException e) {
                MatchmakingServer.getLogger().warning(
                        "Game " + database_id + ". SQLException while updating streaks for " + nick + ": " + e.getMessage());
            }
        }
    }

    private void updateSteamAchievements(int[] team_result) {
        if (!ServerConfiguration.getInstance().isSteamStatsConfigured()) {
            return;
        }

        // Mirror the DB win/loss accounting. This runs only from teamWon, which
        // fires when a real human opponent lost, so AI fillers are irrelevant and
        // pure-vs-AI games never reach here (matching the wins/losses columns).
        Participant[] participants = session.getParticipants();
        for (int i = 0; i < participants.length; i++) {
            String nick = participants[i].getNick();

            Long accountId = DBInterface.getSteamIdByNick(nick);
            if (accountId == null) {
                MatchmakingServer.getLogger().warning(
                        "Game " + database_id + ". No Steam ID linked for " + nick + ", skipping Steam stats push");
                continue;
            }

            try {
                int totalWins = DBInterface.getWins(nick);
                int totalLosses = DBInterface.getIntField("losses", nick);
                int[] streaks = DBInterface.getStreaks(nick);

                // registrations.steam_id stores the 32-bit account ID, but the Steam Web API
                // requires the full 64-bit SteamID, so add the base offset before pushing.
                long steamId64 = accountId + SteamAuthValidator.STEAM_ID_BASE;
                boolean success = SteamAchievementService.updatePlayerStats(
                        steamId64, totalWins, totalLosses, streaks[0], streaks[1]);

                if (success) {
                    MatchmakingServer.getLogger().info(
                            "Game " + database_id + ". Updated Steam achievements for " + nick);
                } else {
                    MatchmakingServer.getLogger().warning(
                            "Game " + database_id + ". Failed to update Steam achievements for " + nick);
                }
            } catch (SQLException e) {
                MatchmakingServer.getLogger().warning(
                        "Game " + database_id + ". SQLException while reading stats for Steam achievements for " + nick + ": " + e.getMessage());
            }
        }
    }

    private void rerateParticipants(MatchmakingServer server, int[] team_result) {
        Participant[] participants = session.getParticipants();
        int[] player_teams = new int[participants.length];

        for (int i = 0; i < participants.length; i++) {
            int team = participants[i].getTeam();
            assert team < 2 : "Participant on team " + team;
            player_teams[i] = team;
        }
        int[][] points = GameSession.calculateMatchPoints(player_ratings, player_teams);
        for (int i = 0; i < participants.length; i++) {
            String nick = participants[i].getNick();
            int dpoints;
            if (team_result[player_teams[i]] == TEAM_WON)
                dpoints = points[i][GameSession.WIN];
            else
                dpoints = points[i][GameSession.LOSE];

            MatchmakingServer.getLogger().info("Game " + database_id + ". " + nick + " rating change was " + dpoints);
            DBInterface.updateRating(participants[i].getNick(), dpoints);

            Client client = server.getClientFromID(participants[i].getMatchID());
            if (client != null)
                client.updateProfile();
        }
    }
}
