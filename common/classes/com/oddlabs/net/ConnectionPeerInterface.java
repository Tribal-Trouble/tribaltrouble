package com.oddlabs.net;

interface ConnectionPeerInterface {
	void ping();
	void receiveEvent(ARMIEvent event);
}
