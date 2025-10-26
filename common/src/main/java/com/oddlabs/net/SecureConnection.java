package com.oddlabs.net;

import com.oddlabs.event.Deterministic;
import com.oddlabs.util.KeyManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.SealedObject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.List;

public final class SecureConnection extends AbstractConnection implements SecureConnectionInterface {
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(SecureConnectionInterface.class);
	private final Deterministic deterministic;
	private final @NonNull AbstractConnection wrapped_connection;
	private final @NonNull SecureConnectionInterface secure_interface;
	private final @Nullable KeyAgreement key_agreement;
	private final List<ARMIEvent> event_backlog = new ArrayList<>();
	private Cipher decrypt_cipher;
	private Cipher encrypt_cipher;

	public SecureConnection(Deterministic deterministic, @NonNull AbstractConnection wrapped_conn, @Nullable AlgorithmParameterSpec param_spec) {
		this.deterministic = deterministic;
		setConnectionInterface(wrapped_conn.getConnectionInterface());
		this.wrapped_connection = wrapped_conn;
		wrapped_connection.setConnectionInterface(new ConnectionInterface() {
                        @Override
			public void error(AbstractConnection conn, IOException e) {
				notifyError(e);
			}
                        @Override
			public void connected(AbstractConnection conn) {
			}
                        @Override
			public void handle(Object sender, @NonNull ARMIEvent event) {
				processEvent(event);
			}
                        @Override
			public void writeBufferDrained(AbstractConnection conn) {
				SecureConnection.this.writeBufferDrained();
			}
		});
		this.secure_interface = (SecureConnectionInterface)ARMIEvent.createProxy(wrapped_connection, SecureConnectionInterface.class);
		if (param_spec != null) {
			KeyPair key_pair = KeyManager.generateInitialKeyPair(param_spec);
			this.key_agreement = KeyManager.generateAgreement(key_pair.getPrivate());
			secure_interface.initAgreement(key_pair.getPublic().getEncoded());
		} else
			this.key_agreement = null;
	}

	public AbstractConnection getWrappedConnection() {
		return wrapped_connection;
	}

	private @NonNull SealedObject encrypt(ARMIEvent event) {
		try {
			return new SealedObject(event, encrypt_cipher);
		} catch (IllegalBlockSizeException | IOException e) {
			throw new RuntimeException(e);
		}
	}

        @Override
	public void initAgreement(byte[] public_key_encoded) {
		if (isConnected())
			return;
		try {
			KeyAgreement key_agreement = this.key_agreement;
			PublicKey public_key = KeyManager.readPublicKey(public_key_encoded, KeyManager.AGREEMENT_ALGORITHM);
			if (key_agreement == null) {
				KeyPair key_pair = deterministic.log(KeyManager.generateKeyPairFromKey(public_key));
				key_agreement = KeyManager.generateAgreement(key_pair.getPrivate());
				secure_interface.initAgreement(key_pair.getPublic().getEncoded());
			}
			decrypt_cipher = KeyManager.createCipher(Cipher.DECRYPT_MODE, key_agreement, public_key);
			encrypt_cipher = KeyManager.createCipher(Cipher.ENCRYPT_MODE, key_agreement, public_key);
			notifyConnected();
            for (ARMIEvent event : event_backlog) {
                tunnel(event);
            }
		} catch (IOException e) {
			notifyError(e);
		} catch (GeneralSecurityException e) {
			notifyError(new IOException(e.getMessage()));
		}
	}

        @Override
	public void tunnelEvent(@NonNull SealedObject sealed_event) {
		try {
			if (decrypt_cipher == null)
				throw new IOException("Illegal stream state, event received before key agreement");
			ARMIEvent event = (ARMIEvent)sealed_event.getObject(decrypt_cipher);
			receiveEvent(event);
		} catch (BadPaddingException | IllegalBlockSizeException | ClassNotFoundException e) {
			notifyError(new IOException(e.getMessage()));
		} catch (IOException e) {
			notifyError(e);
		}
	}

	public @NonNull AbstractConnection getWrappedConnectionAndShutdown() {
		wrapped_connection.setConnectionInterface(getConnectionInterface());
		return wrapped_connection;
	}
	
        @Override
	protected void doClose() {
		wrapped_connection.close();
	}

	private void tunnel(ARMIEvent event) {
		secure_interface.tunnelEvent(encrypt(event));
	}

	private void processEvent(@NonNull ARMIEvent event) {
		try {
			event.execute(interface_methods, this);
		} catch (IllegalARMIEventException e) {
			notifyError(new IOException(e.getMessage()));
		}
	}
	
        @Override
	public void handle(ARMIEvent event) {
		if (encrypt_cipher == null)
			event_backlog.add(event);
		else {
			tunnel(event);
		}
	}
}
