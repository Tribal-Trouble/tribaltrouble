package com.oddlabs.matchmaking;

import java.io.Serializable;
import java.net.InetAddress;

public final class TunnelAddress implements Serializable {
	private final static long serialVersionUID = -2854382209354714233L;
	private final int host_id;
	private final InetAddress address;
	private final InetAddress local_address;

	public TunnelAddress(int host_id, InetAddress address, InetAddress local_address) {
		this.host_id = host_id;
		this.address = address;
		this.local_address = local_address;
	}

	public int getHostID() {
		return host_id;
	}

	public InetAddress getAddress() {
		return address;
	}

	public InetAddress getLocalAddress() {
		return local_address;
	}

        @Override
	public String toString() {
		return "host id = " + host_id + " address = " + address + " local_address = " + local_address;
	}
}	
