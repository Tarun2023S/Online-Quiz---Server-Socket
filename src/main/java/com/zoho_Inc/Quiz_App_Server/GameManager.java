package com.zoho_Inc.Quiz_App_Server;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoho_Inc.Quiz_App_Server.DBManager.Category;

public class GameManager {
	private static DBManager dbm;
	private static String databaseName;
	private static String optionTableName;
	private static String questionTableName;
	private static String questionOptionsTableName;

	static {
		dbm = new DBManager();
		databaseName = dbm.getDatabaseName();
		optionTableName = dbm.getOptionTableName();
		questionTableName = dbm.getQuestionTableName();
		questionOptionsTableName = dbm.getQuestionOptionTableName();
	}

	List<Person> personList;
	Map<Integer, Person> personQuizManager;

	public GameManager() {
		personQuizManager = new HashMap<>();
		personList = new LinkedList<>();
	}

	List<String> players = new ArrayList();

	static void playGameWithPlayer(ObjectOutputStream oos, ObjectInputStream ois, Socket clientSocket) {

	}

	static void attemptAgain(Connection connection, ObjectOutputStream oos, ObjectInputStream ois,
			Socket clientSocket) {
		// get the player id
		try {
			int userId = ois.readInt();
			List<QuizLog> quizLogList = DBManager.getLogs(connection, userId);
			// Send the corresponding quizzes attended by the user
			sendListOfObjectsToClient(quizLogList, clientSocket, oos, ois);
			// Get the quizzLogId from the user
			int qLogId = ois.readInt();
			QuizLog quizLog = null;
			for (QuizLog qzLog : quizLogList) {
				if (qzLog.getQuizId() == qLogId) {
					quizLog = qzLog;
					break;
				}
			}

			// Now finally, we need the list of questions matching our quiz id
			List<Question> questionList = new ArrayList();
			List<Integer> quizQuestionMappingIds = new ArrayList();
			DBManager.displayQuestionsWithSelectedOptions(connection, quizLog.getQuizId(), questionList,
					new ArrayList<Option>(), quizQuestionMappingIds);
			// Send the retrieved qnList to the user
			sendListOfObjectsToClient(questionList, clientSocket, oos, ois);

			boolean isTimed = (quizLog.getIsTimed() == 1);
			playGame(connection, -1, oos, ois, clientSocket, isTimed, false, questionList, quizQuestionMappingIds,
					quizLog.getQuizId());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	static void playGame(Connection connection, int categoryId, ObjectOutputStream oos, ObjectInputStream ois,
			Socket clientSocket, boolean isTimed, boolean isfirstAttempt, List<Question> questionList2,
			List<Integer> quizQuestionMappingIds, int quizId2) {
		try {
			Scanner sc = new Scanner(System.in);
			int playerId = ois.readInt();
			int randomFetchNumber = -1;
			int quizId = quizId2;
			System.out.println("S: Received from the client, userId: " + playerId);
			List<Question> questionList = new ArrayList();
			if (isfirstAttempt) {
				// Fetch All Categories
				getCategoryList(connection, clientSocket, oos, ois);
				categoryId = ois.readInt();
//                while(categoryId<-1 || categoryId>)
				System.out.println("C: Recieved from server, 'cId': " + categoryId);
				randomFetchNumber = ois.readInt();
				System.out.println("C: Recieved from server, 'FetchNo': " + randomFetchNumber);
				questionList = fetchRandomQuestions(connection, databaseName, questionTableName, randomFetchNumber,
						categoryId);

				// In case questions are not available
				if (questionList.size() == 0) {
					System.out.println("There are no questions available..");
					oos.writeInt(-1);
					oos.flush();
					return;
				}
				if (categoryId != -1 && randomFetchNumber > questionList.size()) {
					System.out.println(
							"There are only " + questionList.size() + " questions available for this category..");
				}

				// To indicate Questions are available
				oos.writeInt(1);
				oos.flush();

				quizId = DBManager.startQuiz(connection, playerId, categoryId, 0, 0, isTimed, 0, randomFetchNumber, 0);
				int currentIndex = 0;
				sendQuestionToClient(oos, questionList, currentIndex + 1, clientSocket);
			} else {
				categoryId = ois.readInt();
				randomFetchNumber = ois.readInt();
				questionList.addAll(questionList2);
			}

			// Receive the qId from the client
			for (Question q : questionList) {
				int qId = ois.readInt();
				List<Option> optionsList = displayOptionsForQuestion(qId, connection);
				sendOptionsToClient(oos, optionsList, clientSocket);
			}

			int correctAnswers = 0;
			int j = 0;
			for (Question q : questionList) {
				int questionId = ois.readInt();
				int selectedOptionId = ois.readInt();
				boolean isCorrect = ois.readBoolean();
				if (isCorrect == true)
					correctAnswers++;
				System.out.println("qId: " + questionId + "\nsOId: " + selectedOptionId + "\nisCorrect: " + isCorrect);
				if (isfirstAttempt) {
					DBManager.populateQuizQuestionMapping(connection, quizId, questionId, selectedOptionId, isCorrect);
				} else {
					// Update the questionMapping
					DBManager.updateQuizQuestionMapping(connection, quizQuestionMappingIds.get(j), questionId,
							selectedOptionId, isCorrect);
				}
				j++;
			}

			int percent = calculatPercentage(correctAnswers, randomFetchNumber);
//            String percentage = percent+"%";
			System.out.println("S: Percent: " + percent);
			DBManager.updateQuizTable(connection, quizId, correctAnswers, percent, isfirstAttempt);
			// Updating the user experience count
//            if(isfirstAttempt) {
			DBManager.updateExperience(connection, "totalQuizzes", playerId);
//            }
//            else {
//            	
//            }
			// If the user gets above 60%, its a win, else its a loss
			if (percent >= 60) {
				DBManager.updateExperience(connection, "winCount", playerId);
			} else {
				DBManager.updateExperience(connection, "lossCount", playerId);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error occurred: " + e.getMessage());
		}
	}

	private static int calculatPercentage(int a, int b) {
		float fractionScore = 100.0f / b;
		float percentageScore = fractionScore * a;
		return (int) percentageScore;
	}

	private static void sendQuestionToClient(ObjectOutputStream oos, List<?> questions, int questionNumber,
			Socket clientSocket) {
		ObjectMapper objectMapper = new ObjectMapper();
		String json;
		try {
			System.out.println("S: Sending qns..");
			json = objectMapper.writeValueAsString(questions);

			// Check if the connection is still open before writing to the output stream
			if (!clientSocket.isClosed()) {
				oos.writeObject(json);
				oos.flush();
				System.out.println("S: QnJsonObj: " + json);
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("S: error occurred: " + e.getMessage());
		}
	}

	private static void sendOptionsToClient(ObjectOutputStream oos, List<Option> options, Socket clientSocket) {
		ObjectMapper objectMapper = new ObjectMapper();
		String json;

		try {
			json = objectMapper.writeValueAsString(options);
			System.out.println("OPtions JSON: " + json);
			// Check if the connection is still open before writing to the output stream
			if (!clientSocket.isClosed()) {
//            	System.out.println("S: Options sent success..");
				oos.writeObject(json);
				oos.flush();
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("S: error occurred: " + e.getMessage());
		}
	}

	static void getQuizLogOfAPlayer(Connection connection, ObjectOutputStream oos, ObjectInputStream ois,
			Socket clientSocket) {
		int playerId = -1;
		try {
			playerId = ois.readInt();
			System.out.println("S: Received from the client, userId: " + playerId);
			List<QuizLog> quizLogList = DBManager.getLogs(connection, playerId);
			for (QuizLog ql : quizLogList) {
				System.out.println("S: QL: " + ql);
			}

			ObjectMapper objectMapper = new ObjectMapper();
			String json;
			json = objectMapper.writeValueAsString(quizLogList);
			System.out.println("S: QZLog JSON: " + json);
			// Check if the connection is still open before writing to the output stream
			if (!clientSocket.isClosed()) {
				oos.writeObject(json);
				oos.flush();
			}
			for (QuizLog ql : quizLogList) {
				int quizId = ql.getQuizId();
				List<Question> questionList = new ArrayList();
				List<Option> optionList = new ArrayList();
				DBManager.displayQuestionsWithSelectedOptions(connection, quizId, questionList, optionList,
						new ArrayList<Integer>());
				System.out.println("S: QnSz: " + questionList.size());
				System.out.println("S: OpSz: " + optionList.size());
				sendListOfObjectsToClient(questionList, clientSocket, oos, ois);
				sendListOfObjectsToClient(optionList, clientSocket, oos, ois);
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("S: error occurred: " + e.getMessage());
		}
	}

	// Method to four random questions
	static List<Question> fetchRandomQuestions(Connection connection, String databaseName, String questionTableName,
			int count, int category_id) throws SQLException {
		List<Question> questions = new ArrayList<>();
		String fetchDataQuery = "SELECT * FROM " + databaseName + "." + questionTableName;
		if (category_id != -1) {
			fetchDataQuery += " WHERE category_id = ?";
		}
		fetchDataQuery += " ORDER BY RAND() LIMIT ?";
		try (PreparedStatement ps1 = connection.prepareStatement(fetchDataQuery)) {
			if (category_id != -1) {
				ps1.setInt(1, category_id);
				ps1.setInt(2, count);
			} else {
				ps1.setInt(1, count);
			}

			ResultSet resultSet = ps1.executeQuery();

			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				String questionText = resultSet.getString("question");
				int categoryId = resultSet.getInt("category_id");
				int answerId = resultSet.getInt("answer_id");
				Question question = new Question(id, questionText, categoryId, answerId);
				System.out.println("S: Test.." + question);
				questions.add(question);
			}
		}
		return questions;
	}

	public static List<Option> displayOptionsForQuestion(int qId, Connection connection) {
		String fetchDataQuery = "SELECT * FROM " + databaseName + "." + optionTableName + " INNER JOIN " + databaseName
				+ "." + questionTableName + " ON options.question_id = question.id WHERE question.id = ? ";
		fetchDataQuery += "ORDER BY RAND();";

		List<Option> optionsList = new ArrayList<>();
//        System.out.println("S: Fetch Query for options: " + fetchDataQuery + ", Question ID: " + qId);

		try (PreparedStatement ps1 = connection.prepareStatement(fetchDataQuery)) {
			ps1.setInt(1, qId);
			ResultSet resultSet = ps1.executeQuery();

			Map<Character, Integer> optionChoiceMap = new HashMap<>();
			char optionChoice = 'a';

			while (resultSet.next()) {
				int optionId = resultSet.getInt("options.id");
				String optionText = resultSet.getString("options.options");
				System.out.print(optionChoice + ". " + optionText + "\t");

				// Create Option object and add to the list
				Option option = new Option(optionId, optionText);
				optionsList.add(option);

				optionChoiceMap.put(optionChoice, optionId);
				optionChoice++;
			}
		} catch (SQLException e) {
			System.out.println("Exception occurred: " + e.getMessage());
		}

		return optionsList;
	}

	public static List<?> getTopPlayers(Connection connection, Socket clientSocket, ObjectOutputStream oos,
			ObjectInputStream ois) {
		int limitNumber = 0;
		try {
			limitNumber = ois.readInt();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		List<Person> topPlayers = DBManager.topScoreOfPlayers(connection, limitNumber);
		for (Person p : topPlayers) {
			System.out.println("S: pName: " + p.getName());
			System.out.println("S: pPercent: " + p.getWinPercentage());
		}

		ObjectMapper objectMapper = new ObjectMapper();
		String json;
		try {
			json = objectMapper.writeValueAsString(topPlayers);
			System.out.println("S: topPlayers JSON: " + json);
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
		return topPlayers;
	}

	public static void sendListOfObjectsToClient(List<?> list, Socket clientSocket, ObjectOutputStream oos,
			ObjectInputStream ois) {
		ObjectMapper objectMapper = new ObjectMapper();
		String json;
		try {
			json = objectMapper.writeValueAsString(list);
			System.out.println("S: Objects JSON: " + json);
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

	public static void getCategoryList(Connection connection, Socket clientSocket, ObjectOutputStream oos,
			ObjectInputStream ois) {
		List<Category> categoryList = new ArrayList();
		DBManager.chooseCategory(connection, categoryList);
		System.out.println("S: CategrListSiz: " + categoryList.size());
		ObjectMapper objectMapper = new ObjectMapper();
		String json;

		try {
			json = objectMapper.writeValueAsString(categoryList);
			System.out.println("S: Category JSON: " + json);
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

	// Method to validate the user's answer
	private static boolean validateAnswer(Question question, char userChoice, Connection connection,
			Map<Character, Integer> optionChoiceMap) throws SQLException {
		int actualChoice = optionChoiceMap.get(userChoice);
		String fetchDataQuery = "SELECT * FROM " + databaseName + "." + questionTableName
				+ " WHERE id = ? AND answer_id = ?;";
		try (PreparedStatement ps1 = connection.prepareStatement(fetchDataQuery)) {
			ps1.setInt(1, question.getId());
			ps1.setInt(2, actualChoice);
			ResultSet resultSet = ps1.executeQuery();
			return resultSet.next();
		}
	}

}