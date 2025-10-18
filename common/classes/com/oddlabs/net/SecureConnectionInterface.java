package com.oddlabs.net;

import javax.crypto.SealedObject;

public interface SecureConnectionInterface {
	public void initAgreement(byte[] encoded_public_key);
	public void tunnelEvent(SealedObject event);
}
