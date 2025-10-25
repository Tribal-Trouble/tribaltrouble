package com.oddlabs.registration;

import java.security.SignedObject;

public interface RegClientInterface {
	short REGCLIENT_INTERFACE = 32;

	void registrationCompleted(SignedObject reg_info);
}
