package com.oddlabs.matchserver.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamAuthResponse {
    public SteamAuthResponseBody response;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamAuthResponseBody {
        public SteamAuthParams params;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamAuthParams {
        public String result;
        public String steamid;
    }
}
