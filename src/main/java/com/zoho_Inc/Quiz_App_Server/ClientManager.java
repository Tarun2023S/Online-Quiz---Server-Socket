package com.zoho_Inc.Quiz_App_Server;

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.net.*;

public class ClientManager implements Runnable {

	public static ArrayList<ClientManager> clientHandlers = new ArrayList();
	private Socket socket;
	private static Connection connection;
	private String playerName;
	private String password;

	public ClientManager() {

	}

	public ClientManager(Socket socket, Connection connection) {
		ClientManager.connection = connection;
		this.socket = socket;
	}

	private void processClientChoice(int clientChoice, ObjectOutputStream oos, ObjectInputStream ois,
			Socket clientSocket) {
		// Implement the logic to process the client's choice on the server side
		System.out.println("S: Recieved choice from client: " + clientChoice);
		switch (clientChoice) {
		case 1:
			// Handle playGame on the server
			GameManager.playGame(connection, -1, oos, ois, clientSocket, false, true, new ArrayList<Question>(),
					new ArrayList<Integer>(), -1);
			break;
		case 2:
			// Handle fetchQuestionsCategoryWise on the server
			GameManager.playGame(connection, -1, oos, ois, clientSocket, true, true, new ArrayList<Question>(),
					new ArrayList<Integer>(), -1);
			break;
		case 3:
			// Get ALL Categories
			GameManager.getCategoryList(connection, clientSocket, oos, ois);
			break;
		case 4:
			// Handle displayAllQuestions on the server
			displayAllQns(oos, clientSocket);
			break;
		case 5:
			// Get Complete Quiz Log Of the Player
			GameManager.getQuizLogOfAPlayer(connection, oos, ois, clientSocket);
			break;
		case 6:
			// Get Top P
			GameManager.getTopPlayers(connection, clientSocket, oos, ois);
			break;
		case 7:
			// Attempt an Quiz Again
			GameManager.attemptAgain(connection, oos, ois, clientSocket);
			break;
		case 8:
			System.out.println("Client requested to exit. Closing connection.");
			closeConnection(clientSocket);
			break;
		default:
			System.out.println("Invalid client choice received.");
		}
	}

	public void closeConnection(Socket clientSocket) {
		if (clientSocket != null && !clientSocket.isClosed()) {
			clientHandlers.remove(this);
		}
	}

	public static void displayAllQns(ObjectOutputStream oos, Socket clientSocket) {
		List<Question> questions = null;
		questions = DBManager.fetchDataFromTheTables("question", connection);
		ObjectMapper objectMapper = new ObjectMapper();
		String json;
		try {
			json = objectMapper.writeValueAsString(questions);
			// Check if the connection is still open before writing to the output stream
			if (!clientSocket.isClosed()) {
				oos.writeObject(json);
				oos.flush();
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("S: error occurred: " + e.getMessage());
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("Connected");

		try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
			this.playerName = (String) ois.readObject();
			this.password = (String) ois.readObject();
			Person p = new Person();
			p = DBManager.checkIfAlreadyExists(playerName, password, connection);
			if (p.getName() == "" || p.getName() == null) {
				System.out.println("S: Creating a new player with that username and password");
				p.setName(playerName);
				p.setPassword(password);
				DBManager.populateUserData("user", connection, playerName, password);
				System.out.println("A new user has been created");
			}

			// Sending the player object to the client
			try {
				ObjectMapper objectMapper = new ObjectMapper();
				String json = objectMapper.writeValueAsString(p);

				// Send the JSON string to the client using your preferred method (e.g.,
				// ObjectOutputStream)
				oos.writeObject(json);
				oos.flush();
				System.out.println("S: json: " + json);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("S: error occurred: " + e.getMessage());
			}

			clientHandlers.add(this);
			System.out.println("S: "+this.playerName+" has joined the SERVER..");
			System.out.println("S: Total Clients: " + clientHandlers.size());
			
			while (true) {
				int clientChoice = ois.readInt();
				if (clientChoice == -1) {
					// Client sent a disconnection signal
					closeConnection(socket);
					System.out.println("Client disconnected.");
					break;
				}

				processClientChoice(clientChoice, oos, ois, socket);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public static int validChoiceForInteger() {
		int choice = -1;
		boolean validInput = false;

		while (!validInput) {
			try (Scanner sc = new Scanner(System.in)) {
				System.out.print("Enter a valid integer: ");
				choice = Integer.parseInt(sc.nextLine());
				validInput = true; // If parsing succeeds, mark the input as valid
			} catch (NumberFormatException e) {
				System.out.println("Invalid input. Please enter a valid integer.");
			}
		}
		return choice;
	}

}
