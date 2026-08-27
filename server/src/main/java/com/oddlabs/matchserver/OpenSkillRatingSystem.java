package com.oddlabs.matchserver;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import org.jspecify.annotations.NullMarked;
import com.pocketcombats.openskill.Adjudicator;
import com.pocketcombats.openskill.RatingModelConfig;
import com.pocketcombats.openskill.aggregate.DefaultTeamRatingAggregator;
import com.pocketcombats.openskill.aggregate.TeamRatingAggregator;
import com.pocketcombats.openskill.data.RatingAdjustment;
import com.pocketcombats.openskill.data.SimplePlayerResult;
import com.pocketcombats.openskill.data.SimpleTeamResult;
import com.pocketcombats.openskill.model.PlackettLuce;

import com.oddlabs.matchmaking.OpenSkillRating;

/**
 * Computes OpenSkill rating updates for a completed multiplayer game.
 */
@NullMarked
public final class OpenSkillRatingSystem {
    public static final double SCALING_FACTOR = 60.0;
    public static final double INITIAL_MU = 25.0;
    public static final double INITIAL_SIGMA = INITIAL_MU / 3.0;
    public static final int INITIAL_DISPLAY_RATING = displayRating(INITIAL_MU, INITIAL_SIGMA);
    private static final double PROVISIONAL_SIGMA_THRESHOLD = INITIAL_SIGMA / 3.0;

    private static final RatingModelConfig CONFIG = RatingModelConfig.builder().build();
    private static final TeamRatingAggregator TEAM_RATING_AGGREGATOR = new DefaultTeamRatingAggregator(CONFIG);
    private static final Adjudicator<Integer> ADJUDICATOR = new Adjudicator<>(CONFIG, new PlackettLuce(CONFIG));

    private static final Logger LOGGER = MatchmakingServer.getLogger();

    /**
     * Computes the display rating as {@code SCALING_FACTOR*(mu - sigma)} rounded, so that the
     * default rating (mu=25, sigma=25/3) displays as 1000.
     */
    public static int displayRating(double mu, double sigma) {
        return (int) Math.round(SCALING_FACTOR * (mu - sigma));
    }

    public static boolean isProvisional(double sigma) {
        return sigma > PROVISIONAL_SIGMA_THRESHOLD;
    }

    /**
     * Rates a completed game and persists each entity's updated mu/sigma.
     *
     * @param teams the participating teams
     * @param ranks each team's placement (parallel to {@code teams}); lower values indicate better
     *              placements, and equal values represent ties
     */
    public static void rateGame(List<List<String>> teams, int[] ranks) {
        if (teams.size() < 2) {
            throw new IllegalArgumentException("Need at least two teams to rate a game");
        }
        if (teams.size() != ranks.length) {
            throw new IllegalArgumentException("Need exactly one rank per team");
        }

        // Load the current ratings for each participant.
        Map<String, OpenSkillRating> ratings = new LinkedHashMap<>();
        for (List<String> team : teams) {
            for (String nick : team) {
                if (!ratings.containsKey(nick)) {
                    ratings.put(nick, loadRating(nick));
                }
            }
        }

        // Build a structure suitable for passing to the OpenSkill library. Each entry gets a
        // unique entry ID so that multiple clone entries on the same team can be temporarily
        // treated as distinct participants for match-expectation purposes.
        List<SimpleTeamResult<Integer>> gameResult = new ArrayList<>();
        List<String> entryIdToNick = new ArrayList<>();
        List<Integer> entryIdToTeamIdx = new ArrayList<>();
        int entrySeq = 0;
        for (int teamIdx = 0; teamIdx < teams.size(); teamIdx++) {
            List<String> team = teams.get(teamIdx);
            if (team.isEmpty()) {
                continue;
            }
            List<SimplePlayerResult<Integer>> teamResult = new ArrayList<>();
            for (String nick : team) {
                Integer entryId = entrySeq++;
                OpenSkillRating rating = ratings.get(nick);
                teamResult.add(new SimplePlayerResult<>(entryId, rating.mu(), rating.sigma()));
                entryIdToNick.add(nick);
                entryIdToTeamIdx.add(teamIdx);
            }
            var teamRating = TEAM_RATING_AGGREGATOR.computeTeamRating(teamResult);
            int rank = ranks[teamIdx];
            gameResult.add(new SimpleTeamResult<>(teamRating.mu(), teamRating.sigma(), rank, teamResult));
        }

        if (gameResult.size() < 2) {
            return;
        }

        List<RatingAdjustment<Integer>> adjustments = ADJUDICATOR.rate(gameResult);

        // Two-stage aggregation of per-entry adjustments into per-nick ratings so that every
        // persistent nick receives exactly one rating update regardless of how many entries it has.
        //
        // The OpenSkill library returns absolute post-match mu/sigma values per entry rather than
        // deltas. Those values are averaged, by arithmetic mean, in two stages: first within each
        // team, then across teams. The final result is persisted as the rating for that nick.
        //
        // Stage 1 (within-team): for each (nick, team) pair, average the mu/sigma of that nick's
        // clone entries on that team. A nick with multiple clones on the same team collapses to one
        // team-level result, so the number of clone entries does not inflate the nick's weight.
        //
        // Stage 2 (across-teams): for each nick, average its per-team results. Each team
        // appearance contributes equally regardless of how many clone entries it contained.
        //
        // Humans (and CPUs with a single entry on a single team) have one entry on one team,
        // so both stages for them are effectively no-ops and they receive their result directly.

        Map<String, Map<Integer, List<RatingAdjustment<Integer>>>> adjustmentsByNickThenTeam;
        adjustmentsByNickThenTeam = new LinkedHashMap<>();
        for (RatingAdjustment<Integer> adj : adjustments) {
            String nick = entryIdToNick.get(adj.playerId());
            int teamIdx = entryIdToTeamIdx.get(adj.playerId());
            adjustmentsByNickThenTeam.computeIfAbsent(nick, k -> new LinkedHashMap<>()).computeIfAbsent(teamIdx,
                    k -> new ArrayList<>()).add(adj);
        }

        for (var entry : adjustmentsByNickThenTeam.entrySet()) {
            String nick = entry.getKey();
            Map<Integer, List<RatingAdjustment<Integer>>> adjustmentsPerTeam = entry.getValue();

            double acrossMu = 0;
            double acrossSigma = 0;
            for (List<RatingAdjustment<Integer>> teamEntries : adjustmentsPerTeam.values()) {
                double withinMu = 0;
                double withinSigma = 0;
                for (RatingAdjustment<Integer> adj : teamEntries) {
                    withinMu += adj.mu();
                    withinSigma += adj.sigma();
                }
                int teamEntryCount = teamEntries.size();
                withinMu /= teamEntryCount;
                withinSigma /= teamEntryCount;

                acrossMu += withinMu;
                acrossSigma += withinSigma;
            }
            int teamCount = adjustmentsPerTeam.size();
            acrossMu /= teamCount;
            acrossSigma /= teamCount;

            saveRating(new OpenSkillRating(nick, acrossMu, acrossSigma));

            String suffix = teamCount > 1 ? " (averaged over %d team(s))".formatted(teamCount) : "";
            LOGGER.info(("OpenSkillRatingSystem: %s -> mu=%s sigma=%s rating=%d" + suffix).formatted(
                    nick, acrossMu, acrossSigma, displayRating(acrossMu, acrossSigma)));
        }
    }

    private static OpenSkillRating loadRating(String nick) {
        try {
            OpenSkillRating rating = DBInterface.getOpenSkillRating(nick);
            if (rating != null) {
                return rating;
            }
        } catch (SQLException e) {
            LOGGER.warning("OpenSkillRatingSystem: could not load rating for %s: %s".formatted(nick, e.getMessage()));
        }
        return new OpenSkillRating(nick, INITIAL_MU, INITIAL_SIGMA);
    }

    private static void saveRating(OpenSkillRating rating) {
        try {
            DBInterface.upsertOpenSkillRating(rating);
        } catch (SQLException e) {
            LOGGER.warning("OpenSkillRatingSystem: could not save rating for %s: %s".formatted(rating.nick(),
                    e.getMessage()));
        }
    }
}
