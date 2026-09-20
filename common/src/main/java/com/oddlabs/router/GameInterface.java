package com.oddlabs.router;

import com.oddlabs.net.ARMIEvent;

public interface GameInterface {
    // client_id a spectator's relayed event arrives with; it belongs to no player slot
    int SPECTATOR_CLIENT_ID = -1;

    void relayEventTo(int client_id, ARMIEvent event);

    void relayEvent(ARMIEvent event);

    void relayGameStateEvent(ARMIEvent event);

    void checksum(int checksum);
}
