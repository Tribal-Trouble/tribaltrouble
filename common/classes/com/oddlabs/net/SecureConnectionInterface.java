package com.oddlabs.net;

import javax.crypto.SealedObject;

public interface SecureConnectionInterface {
	void initAgreement(byte[] encoded_public_key);
	void tunnelEvent(SealedObject event);
}
