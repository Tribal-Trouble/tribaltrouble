package com.oddlabs.net;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;


public final class Connection extends AbstractConnection implements Handler, ConnectionPeerInterface {
	public static final int BUFFER_SIZE = 16382;
	private static final short HEADER_SIZE = 2;

	private final ByteBuffer read_buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	private final List<ARMIEvent> back_log_list = new LinkedList<>();
	private final ByteBuffer write_buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
	private final @NonNull ConnectionPeerInterface peer_interface;
	private final boolean ping_reply;
	private final @NonNull NetworkSelector network;
	private final ARMIInterfaceMethods interface_methods = new ARMIInterfaceMethods(ConnectionPeerInterface.class);
	private boolean writing = false;
	private boolean pinged = false;
	private @Nullable SelectionKey key;
	private InetAddress local_address;

	public static void configureChannel(@NonNull SocketChannel channel) throws IOException {
		channel.configureBlocking(false);
		channel.socket().setTcpNoDelay(true);
	}

	private Connection(@NonNull NetworkSelector network, boolean ping_reply) {
		this.network = network;
		network.registerForPingTimeout(this);
		this.ping_reply = ping_reply;
		ARMIEventWriter event_writer = (ARMIEvent event) -> {
                    if (!back_log_list.isEmpty() || !writeEvent(event))
                        back_log_list.add(event);
                };
		this.peer_interface = (ConnectionPeerInterface)ARMIEvent.createProxy(event_writer, ConnectionPeerInterface.class);
		peer_interface.ping();
	}
	
	public Connection(@NonNull NetworkSelector network, String dns_name, int port, ConnectionInterface connection_interface) {
		this(network, true);
		setConnectionInterface(connection_interface);
		network.asyncConnect(dns_name, port, this);
	}

	public Connection(@NonNull NetworkSelector network, SocketAddress address, ConnectionInterface connection_interface) {
		this(network, true);
		setConnectionInterface(connection_interface);
		connect(address);
	}

	Connection(@NonNull NetworkSelector network, SelectionKey key, ConnectionInterface connection_interface) {
		this(network, false);
		network.registerForPing(this);
		setConnectionInterface(connection_interface);
		notifyConnected();
		setKey(key);
	}

	public void doPing() {
		if (isConnected())
			peer_interface.ping();
	}

	@Override
	public void ping() {
		if (ping_reply)
			peer_interface.ping();
		pinged = true;
	}

	public void timeout() {
		if (!isConnected())
			return;
		if (!pinged) {
			handleError(new IOException("Connection timed out"));
		} else {
			pinged = false;
			network.registerForPingTimeout(this);
		}
	}

	public void connected(InetAddress local_address) {
		this.local_address = local_address;
		notifyConnected();
	}

	public InetAddress getLocalAddress() {
		return local_address;
	}

	public void error(IOException e) {
		notifyError(e);
	}

	public void connect(SocketAddress socket_address) {
		IOException exception = null;
		InetAddress local_address = null;
		SelectionKey key = null;
		if (!network.getDeterministic().isPlayback()) {
			Selector selector = network.getSelector();
			try {
				SocketChannel channel = SocketChannel.open();
				try {
					configureChannel(channel);
					boolean success = channel.connect(socket_address);
					if (success) {
						key = channel.register(selector, SelectionKey.OP_READ);
						local_address = channel.socket().getLocalAddress();
					} else {
						key = channel.register(selector, SelectionKey.OP_CONNECT);
					}
				} catch (IOException e) {
					channel.close();
					throw e;
				}
			} catch (IOException e) {
				exception = e;
			}
		}
		setKey(key);
		if (writing)
			doSetWriting();
		else
			doResetWriting();
		if (network.getDeterministic().log(exception != null))
			error(network.getDeterministic().log(exception));
		else if (network.getDeterministic().log(local_address != null))
			connected(network.getDeterministic().log(local_address));
	}

	public void dnsError(IOException e) {
		error(e);
	}
	
	private void setKey(@Nullable SelectionKey key) {
		assert key != null || network.getDeterministic().isPlayback();
		this.key = key;
		network.attachToKey(key, this);
	}

	private void doSetWriting() {
		if (!network.getDeterministic().isPlayback()) {
			int new_ops = key.interestOps() | SelectionKey.OP_WRITE;
			key.interestOps(new_ops);
		}
	}

	private void doResetWriting() {
		if (!network.getDeterministic().isPlayback()) {
			int new_ops = key.interestOps() & ~SelectionKey.OP_WRITE;
			key.interestOps(new_ops);
		}
	}

	private void resetWriting() {
		assert writing;
		if (isKeyValid())
			doResetWriting();
		writing = false;
	}

	private void setWriting() {
		if (writing)
		   return;
		if (isKeyValid())
			doSetWriting();
		writing = true;
	}

	@Override
	public void handle(ARMIEvent event) {
		peer_interface.receiveEvent(event);
	}

	private boolean writeNextEvent() {
		if (back_log_list.isEmpty())
			return false;
		ARMIEvent event = back_log_list.getFirst();
		boolean success = writeEvent(event);
		if (success)
			back_log_list.removeFirst();
		return success;
	}

	private void writeBackLog() {
		while (writeNextEvent())
			;
	}

	private void writeToChannel(@NonNull SocketChannel channel) throws IOException {
		int bytes_written;
		do {
			bytes_written = 0;
            if (write_buffer.position() > 0) {
			    write_buffer.flip();
                if (!network.getDeterministic().isPlayback())
                    bytes_written = channel.write(write_buffer);
                bytes_written = network.getDeterministic().log(bytes_written);
                int new_position = network.getDeterministic().log(write_buffer.position());
                write_buffer.position(new_position);
			    write_buffer.compact();
            }
			writeBackLog();
		} while (bytes_written > 0);
		if (write_buffer.position() == 0) {
			resetWriting();
			writeBufferDrained();
		}
	}

	private boolean writeEvent(@NonNull ARMIEvent event) {
		short event_size = event.getEventSize();
		int total_event_size = event_size + HEADER_SIZE;
		assert total_event_size <= write_buffer.capacity();
		boolean fits = total_event_size <= write_buffer.remaining();
		if (fits) {
			write_buffer.putShort(event_size);
			event.write(write_buffer);
		}
		// Avoid differently sized serializing (we don't care about the write_buffer contents at playback anyway)
		int new_position = network.getDeterministic().log(write_buffer.position());
		write_buffer.position(new_position);
		setWriting();
		return fits;
	}

	private int readDeterministic(@NonNull SocketChannel channel) throws IOException {
		int old_position = read_buffer.position();
		int num_bytes_read = -1;
		IOException exception;
		try {
			if (!network.getDeterministic().isPlayback())
				num_bytes_read = channel.read(read_buffer);
			exception = null;
		} catch (IOException e) {
			exception = e;
		}
		if (network.getDeterministic().log(exception != null))
			throw network.getDeterministic().log(exception);
		else
			num_bytes_read = network.getDeterministic().log(num_bytes_read);
		int new_position = network.getDeterministic().log(read_buffer.position());
		int old_limit = read_buffer.limit();
		read_buffer.limit(new_position);
		read_buffer.position(old_position);
		network.getDeterministic().log(read_buffer);
		assert read_buffer.position() == new_position && !read_buffer.hasRemaining() : read_buffer.position() + " " + new_position + " " + !read_buffer.hasRemaining();
		read_buffer.limit(old_limit);
		return num_bytes_read;
	}

	private void readFromChannel(@NonNull SocketChannel channel) throws IOException {
		boolean bytes_read;
		do {
			int num_bytes_read = readDeterministic(channel);
			if (num_bytes_read == -1)
				throw new IOException("Channel closed");
			bytes_read = num_bytes_read > 0;
            if (bytes_read) {
			    read_buffer.flip();
			    while (read_buffer.remaining() >= HEADER_SIZE) {
				    short event_size = read_buffer.getShort(read_buffer.position());
				    if (event_size > read_buffer.capacity() - HEADER_SIZE) {
					    handleError(new IOException("Message too large: " + event_size));
                        return; // Stop processing this corrupt buffer
                    }
				    if (read_buffer.remaining() >= event_size + HEADER_SIZE) {
					    read_buffer.position(read_buffer.position() + HEADER_SIZE);
					    ARMIEvent event = ARMIEvent.read(read_buffer, event_size);
					    network.getDeterministic().checkpoint();
					    try {
						    event.execute(interface_methods, this);
					    } catch (IllegalARMIEventException e) {
						    throw new IOException(e);
					    }
				    } else {
                        // Not enough data for the full event, break and wait for more
                        break;
                    }
			    }
			    read_buffer.compact();
            }
		} while (bytes_read && network.getDeterministic().log(network.getDeterministic().isPlayback() || channel.isOpen()));
	}

	@Override
	public void handle() throws IOException {
		SocketChannel channel;
		if (!network.getDeterministic().isPlayback())
			channel = (SocketChannel)key.channel();
		else
			channel = null;
		network.getDeterministic().checkpoint();
		if (network.getDeterministic().log(network.getDeterministic().isPlayback() || !channel.isConnected())) {
			boolean success = false;
			IOException exception;
			try {
				if (!network.getDeterministic().isPlayback())
					success = channel.finishConnect();
				exception = null;
			} catch (IOException e) {
				exception = e;
			}
			if (network.getDeterministic().log(exception != null))
				throw network.getDeterministic().log(exception);
			else
				success = network.getDeterministic().log(success);
			assert success; // finishConnect should always succeed (or throw), because we are called on OP_CONNECT
			int interest_ops;
			if (network.getDeterministic().isPlayback())
				interest_ops = network.getDeterministic().log(-1);
			else
				interest_ops = network.getDeterministic().log(key.interestOps());
			int new_ops = (interest_ops | SelectionKey.OP_READ) & ~SelectionKey.OP_CONNECT;
			if (!network.getDeterministic().isPlayback())
				key.interestOps(new_ops);
			connected(network.getDeterministic().log(network.getDeterministic().isPlayback() ? null : channel.socket().getLocalAddress()));
		} else {
			network.getDeterministic().checkpoint();
			if (writing)
				writeToChannel(channel);
			readFromChannel(channel);
		}
	}
	
	private boolean isKeyValid() {
		// double negation because we want to the common result to be false, the default logger value
		return !network.getDeterministic().log(network.getDeterministic().isPlayback() || !(key != null && key.isValid()));
	}

	@Override
	protected void doClose() {
		if (isKeyValid()) {
			network.cancelKey(key, this);
			if (!network.getDeterministic().isPlayback()) {
				SocketChannel channel = (SocketChannel)key.channel();
				try {
					channel.socket().shutdownInput();
				} catch (IOException _) {
					// ignore
				}
				try {
					channel.socket().shutdownOutput();
				} catch (IOException _) {
					// Ignore
				}
				try {
					channel.close();
				} catch (IOException _) {
					// ignore
				}
			}
		}
		network.unregisterForPinging(this);
	}
	
	@Override
	public void handleError(IOException e) {
		error(e);
	}
}
