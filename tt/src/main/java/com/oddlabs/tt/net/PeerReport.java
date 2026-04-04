package com.oddlabs.tt.net;

import java.io.Serial;
import java.io.Serializable;

public final class PeerReport implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private final int turn;
    private final int[] report;

    public PeerReport(int turn, int[] report) {
        this.turn = turn;
        this.report = report;
    }

    public int[] getReport() {
        return report;
    }

    public int getTurn() {
        return turn;
    }
}
