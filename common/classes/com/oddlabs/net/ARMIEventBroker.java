package com.oddlabs.net;

public interface ARMIEventBroker {
	void handle(Object sender, ARMIEvent event);
}
