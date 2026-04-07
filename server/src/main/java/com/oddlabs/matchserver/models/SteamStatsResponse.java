package com.oddlabs.matchserver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamStatsResponse {
    public SteamStatsResult result;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamStatsResult {
        public int result; // 1 = success
    }
}
