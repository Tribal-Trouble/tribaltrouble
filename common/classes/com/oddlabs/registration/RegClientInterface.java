package com.oddlabs.registration;

import java.security.SignedObject;

public interface RegClientInterface {
	public final static short REGCLIENT_INTERFACE = 32;

	public void registrationCompleted(SignedObject reg_info);
}
