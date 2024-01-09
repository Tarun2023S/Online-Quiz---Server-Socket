package com.zoho_Inc.Quiz_App_Server;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Server {
	private static String url = "jdbc:mysql://localhost:3306/";
	private static String userName = "root";
	private static String password = "root";
	private static String dbName = "quiz_app";
	private ServerSocket serverSocket;

	public Server(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public void startServer(Connection connection) {
		try {
			while (!serverSocket.isClosed()) {
				System.out.println("\n\tSERVER STARTED\n");
				Socket socket = serverSocket.accept();
				System.out.println("S: A new client has connected");

				ClientManager clientManager = new ClientManager(socket, connection);
				Thread thread = new Thread(clientManager);
				thread.start();
			}
		} catch (IOException e) {
			return;
		}
	}

	public static void main(String[] args) {
		int portNumber = 1230;
		try (ServerSocket serverSocket = new ServerSocket(portNumber); Connection connection = getConnection()) {
			Server server = new Server(serverSocket);
			server.startServer(connection);
		} catch (SocketException e) {
			// Handle the SocketException (Connection reset) gracefully
			System.out.println("Client disconnected unexpectedly."+e.getMessage());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
//			System.out.println("Close: ");
		}

	}

	public static Connection getConnection() {
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(url, userName, password);
			if (!DBManager.databaseExists(connection)) {
				DBManager.createDatabase(connection);
			}
			// Select the database
			connection.setCatalog(dbName);
			DBManager.createNecessaryTables(connection);

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return connection;
	}
}
