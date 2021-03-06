package com.luneruniverse.simplepacketlibrary;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

class StandardWebSocketServer extends WebSocketServer implements BasicServer {
	
	private final Map<WebSocket, StandardWebSocketServerConnection> connections;
	private final Queue<StandardWebSocketServerConnection> connectionsQueue;
	
	public StandardWebSocketServer(int port) {
		super(new InetSocketAddress(port));
		this.connections = new WeakHashMap<>();
		this.connectionsQueue = new ConcurrentLinkedQueue<>();
		this.start();
	}
	
	@Override
	public void onStart() {
		
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		StandardWebSocketServerConnection connection = new StandardWebSocketServerConnection(conn);
		connections.put(conn, connection);
		connectionsQueue.add(connection);
	}
	
	@Override
	public void onMessage(WebSocket conn, String message) {
		onMessage(conn, ByteBuffer.wrap(message.getBytes()));
	}
	
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		try {
			connections.get(conn).packetReceived(message);
		} catch (IOException e) {
			ServerConnection connection = connections.get(conn).getConnection();
			if (connection != null)
				connection.onError(e, connection, ErrorHandler.Error.READING_WEBSOCKET_PACKET);
		}
	}
	
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		connectionsQueue.remove(connections.remove(conn));
	}
	
	@Override
	public void onError(WebSocket conn, Exception e) {
		ServerConnection connection = connections.get(conn).getConnection();
		if (connection != null)
			connection.onError(e, connection, ErrorHandler.Error.GENERIC_WEBSOCKET);
	}
	
	@Override
	public BasicSocket accept() throws IOException, InterruptedException {
		while (connectionsQueue.isEmpty()) {
			if (Thread.interrupted()) {
				close();
				throw new InterruptedException();
			}
			Thread.yield();
		}
		return connectionsQueue.remove();
	}
	
	@Override
	public void close() throws IOException {
		try {
			this.stop();
		} catch (InterruptedException e) {
			throw new IOException("Error stopping server", e);
		}
	}
	
}
