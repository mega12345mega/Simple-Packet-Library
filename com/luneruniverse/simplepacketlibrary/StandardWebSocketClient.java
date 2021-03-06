package com.luneruniverse.simplepacketlibrary;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

class StandardWebSocketClient extends WebSocketClient implements BasicSocket {
	
	private final Client client;
	private final Queue<PacketData> packets;
	
	public StandardWebSocketClient(Client client, String ip, int port) throws URISyntaxException, IOException {
		super(new URI(ip + ":" + port));
		this.client = client;
		this.packets = new ConcurrentLinkedQueue<>();
		try {
			if (!this.connectBlocking(5000, TimeUnit.MILLISECONDS))
				throw new IOException("Unable to connect to server");
		} catch (InterruptedException e) {
			throw new IOException("Unable to connect to server", e);
		}
	}
	
	@Override
	public void onOpen(ServerHandshake handshakedata) {
		
	}
	
	@Override
	public void onMessage(String message) {
		onMessage(ByteBuffer.wrap(message.getBytes()));
	}
	
	@Override
	public void onMessage(ByteBuffer buf) {
		byte[] data = new byte[buf.remaining()];
		buf.get(data);
		try {
			packets.add(readPacket(new DataInputStream(new ByteArrayInputStream(data))));
		} catch (IOException e) {
			client.onError(e, client, ErrorHandler.Error.READING_WEBSOCKET_PACKET);
		}
	}
	
	@Override
	public void onError(Exception e) {
		client.onError(e, client, ErrorHandler.Error.GENERIC_WEBSOCKET);
	}
	
	@Override
	public void onClose(int code, String reason, boolean remote) {
		packets.clear();
	}
	
	@Override
	public PacketData readPacket() throws IOException {
		while (packets.isEmpty()) {
			if (isClosed())
				throw new EOFException();
			Thread.yield();
		}
		return packets.remove();
	}
	
	@Override
	public void sendPacket(byte[] data) throws IOException {
		send(data);
	}
	
}
