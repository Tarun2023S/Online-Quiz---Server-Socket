package com.zoho_Inc.Quiz_App_Server;

import java.sql.*;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DBManager {

	private static String databaseName = "quiz_app";

	private static String categoryTableName = "category";
	private static String categoryTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, category VARCHAR(60)";

	private static String userTableName = "user";
	private static String userTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(60) unique, password varchar(100), totalQuizzes INT DEFAULT 0, winCount INT DEFAULT 0, lossCount INT DEFAULT 0";

	private static String optionTableName = "options";
	private static String optionTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, options VARCHAR(100)";

	private static String quizTableName = "quiz";
	private static String quizTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, userId INT, categoryId INT, score INT, attempts INT DEFAULT 0, FOREIGN KEY(userId) references user(id), isTimed BOOLEAN DEFAULT false, correctlyAnswered INT, totalQuestions INT, percentage int default 0";

	private static String quizQuestionTableName = "quiz_question_mapping";
	private static String quizQuestionTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, quizId INT, questionId INT, selectedOptionId INT, isCorrect BOOLEAN DEFAULT false, FOREIGN KEY(quizId) references quiz(id), FOREIGN KEY(questionId) references question(id), FOREIGN KEY(selectedOptionId) references options(id)";

	private static String questionTableName = "question";
	private static String questionTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, question VARCHAR(2000), category_id INT, answer_id INT, FOREIGN KEY(category_id) REFERENCES "
			+ categoryTableName + "(id), FOREIGN KEY(answer_id) REFERENCES " + optionTableName + "(id)";

	private static String questionOptionTableName = "question_options";
	private static String questionOptionTableValues = "id INT PRIMARY KEY AUTO_INCREMENT, question_id INT, options_id INT, FOREIGN KEY(question_id) REFERENCES "
			+ questionTableName + "(id), FOREIGN KEY(options_id) REFERENCES " + optionTableName + "(id)";

	static String userColumnName = "";

	public static String getDatabaseName() {
		return databaseName;
	}

	public static void setDatabaseName(String databaseName) {
		DBManager.databaseName = databaseName;
	}

	public static String getCategoryTableName() {
		return categoryTableName;
	}

	public static void setCategoryTableName(String categoryTableName) {
		DBManager.categoryTableName = categoryTableName;
	}

	public static String getOptionTableName() {
		return optionTableName;
	}

	public static void setOptionTableName(String optionTableName) {
		DBManager.optionTableName = optionTableName;
	}

	public static String getQuestionTableName() {
		return questionTableName;
	}

	public static void setQuestionTableName(String questionTableName) {
		DBManager.questionTableName = questionTableName;
	}

	public static String getQuestionOptionTableName() {
		return questionOptionTableName;
	}

	public static void setQuestionOptionTableName(String questionOptionTableName) {
		DBManager.questionOptionTableName = questionOptionTableName;
	}

	static boolean databaseExists(Connection connection) {
		try {
			DatabaseMetaData metaData = connection.getMetaData();
			ResultSet resultSet = metaData.getCatalogs();

			while (resultSet.next()) {
				String dbName = resultSet.getString("TABLE_CAT");
				if (dbName.equals(databaseName)) {
					return true; // Database exists
				}
			}

			return false; // Database does not exist
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

//	static boolean databaseExists(Connection connection) {
//		try {
//			Statement statement = connection.createStatement();
//			String checkDatabaseExistsQuery = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '"
//					+ databaseName + "'";
//			return statement.executeQuery(checkDatabaseExistsQuery).next();
//		} catch (SQLException e) {
//			e.printStackTrace();
//			return false;
//		}
//	}

	static void createDatabase(Connection connection) {
		try {
			Statement statement = connection.createStatement();
			String queryToCreateDB = "CREATE DATABASE " + databaseName;
			statement.executeUpdate(queryToCreateDB);
			System.out.println("Database created successfully");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void createTableQuery(String tableName, String tableValues, Connection connection) {
		String queryToCreateTable = "CREATE TABLE IF NOT EXISTS " + tableName + " (" + tableValues + ");";
		try (PreparedStatement preparedStatement = connection.prepareStatement(queryToCreateTable)) {
			int modifiedRows = preparedStatement.executeUpdate();
//	            System.out.println("Modified no. of rows: " + modifiedRows);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Person checkIfAlreadyExists(String name, String password, Connection connection) {
		Person person = new Person();
		try {
//	            String query = "SELECT * FROM user WHERE name = ? AND password = ?";
			String query = "SELECT * FROM user WHERE name = ?";
			try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
				preparedStatement.setString(1, name);
//	                preparedStatement.setString(2, password);

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						// User exists, populate the Person object
						person = new Person();
						person.setPersonId(resultSet.getInt("id"));
						person.setName(resultSet.getString("name"));
						person.setPassword(resultSet.getString("password"));
						person.setTotalQuizTaken(resultSet.getInt("totalQuizzes"));
						person.setTotalWins(resultSet.getInt("winCount"));
						person.setTotalLosses(resultSet.getInt("lossCount"));
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace(); // Handle exceptions appropriately in a real application
		}

		return person;
	}

	public static List<QuizLog> getLogs(Connection connection, int userId) {
		String fetchQuery = "SELECT * FROM " + quizTableName + " WHERE userId = ?";
		List<QuizLog> quizLogList = new ArrayList();
		try (PreparedStatement ps = connection.prepareStatement(fetchQuery)) {
			ps.setInt(1, userId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				int quizId = rs.getInt("id");
				int isTimed = rs.getInt("isTimed");
				int categoryId = rs.getInt("categoryId");
				int correctlyAnswered = rs.getInt("correctlyAnswered");
				int totalQuestions = rs.getInt("totalQuestions");
				int attempts = rs.getInt("attempts");
				double percent = rs.getDouble("percentage");

				QuizLog quizLog = new QuizLog(quizId, isTimed, categoryId, correctlyAnswered, totalQuestions, attempts,
						percent);
				quizLogList.add(quizLog);
//	                displayQuizLogDetails(connection, quizLog);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return quizLogList;
	}

	static void displayQuestionsWithSelectedOptions(Connection connection, int quizId, List<Question> questList,
			List<Option> optionList, List<Integer> quizQuestionMappingIds) {
		String fetchQuestionsQuery = "SELECT qm.id AS question_Mapping_Id, q.id AS question_id, q.question, q.answer_id, op.id AS option_id, op.options "
				+ "FROM question q " + "JOIN quiz_question_mapping qm ON q.id = qm.questionId "
				+ "JOIN options op ON op.id = qm.selectedOptionId " + "WHERE qm.quizId = ?";

		try (PreparedStatement ps = connection.prepareStatement(fetchQuestionsQuery)) {
			ps.setInt(1, quizId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				int questionId = rs.getInt("question_id");
				String questionText = rs.getString("question");
				int answerId = rs.getInt("answer_id");

				// Add the question to the list
				Question question = new Question(questionId, questionText, -1, answerId);
				questList.add(question);

				int optionId = rs.getInt("option_id");
				String optionText = rs.getString("options");

				// Add the option to the list
				Option option = new Option(optionId, optionText);
				optionList.add(option);

				// Add the records to update them in the future
				quizQuestionMappingIds.add(rs.getInt("question_Mapping_Id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static int startQuiz(Connection connection, int userId, int categoryId, int score, int attempts,
			boolean isTimed, int correctlyAnswered, int totalQuestions, int percentage) {
		String insertQuery = "INSERT INTO " + databaseName + ".`" + quizTableName
				+ "`(userId, categoryId, score, attempts, isTimed, correctlyAnswered, totalQuestions, percentage) VALUES(?, ?, ?, ?, ?, ?, ?, ?);";
		try (PreparedStatement ps = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
			ps.setInt(1, userId);
			ps.setInt(2, categoryId);
			ps.setInt(3, score);
			ps.setInt(4, attempts);
			ps.setBoolean(5, isTimed);
			ps.setInt(6, correctlyAnswered);
			ps.setInt(7, totalQuestions);
			ps.setInt(8, percentage);

			// Execute the query
			int rowsModified = ps.executeUpdate();

			System.out.println("Rows modified: " + rowsModified);

			// Retrieve the auto-generated keys (should contain the ID)
			ResultSet generatedKeys = ps.getGeneratedKeys();
			if (generatedKeys.next()) {
				int generatedId = generatedKeys.getInt(1);
				System.out.println("Inserted record ID: " + generatedId);
				return generatedId;
			} else {
				throw new SQLException("Failed to get the inserted record ID.");
			}
		} catch (SQLException e) {
			System.out.println("S: An Exception occured");
			e.printStackTrace();
		}
		return -1;
	}

	public static void updateQuizTable(Connection connection, int quizId, int correctlyAnswered, int percent,
			boolean isFirstAttempt) {
		String updateQuery;

		if (!isFirstAttempt) {
			// If it's the first attempt, increment the attempts
			updateQuery = "UPDATE " + databaseName + ".`" + quizTableName
					+ "` SET correctlyAnswered = ?, percentage = ?, attempts = attempts + 1 WHERE id = ?;";
		} else {
			// If it's not the first attempt, don't modify the attempts
			updateQuery = "UPDATE " + databaseName + ".`" + quizTableName
					+ "` SET correctlyAnswered = ?, percentage = ? WHERE id = ?;";
		}

		try (PreparedStatement ps1 = connection.prepareStatement(updateQuery)) {
			System.out.println("S: updateQ: " + updateQuery);

			// Update the values for correctlyAnswered & percentage
			ps1.setInt(1, correctlyAnswered);
			ps1.setInt(2, percent);
			ps1.setInt(3, quizId);

			// Execute the query
			int rowsModified = ps1.executeUpdate();

			System.out.println("Rows modified: " + rowsModified);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void updateExperience(Connection connection, String columnName, int id) {
		String updateQuery = "UPDATE " + databaseName + ".`" + userTableName + "` SET " + columnName + " = "
				+ columnName + " + 1 " + "WHERE id = ?;";
		try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
			System.out.println("S: updateQ: " + updateQuery);
			// Update the Game Experience of the user
			ps.setInt(1, id);
			// Execute the query
			int rowsModified = ps.executeUpdate();
			System.out.println("Rows modified: " + rowsModified);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static List<Person> topScoreOfPlayers(Connection connection, int limitNumber) {
		String executeQuery = "SELECT name, winCount, totalQuizzes, (winCount / totalQuizzes) * 100 AS winPercentage FROM "
				+ databaseName + ".`" + userTableName + "` ORDER BY (winCount / totalQuizzes) DESC";

		if (limitNumber != 0) {
			executeQuery += " LIMIT ?";
		}

		List<Person> topPlayers = new ArrayList<>();

		// Define weights for experience and win count
		double weightExperience = 0.5;
		double weightWinCount = 0.5;

		try (PreparedStatement ps = connection.prepareStatement(executeQuery, ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY)) {
			// If a limit is specified, set the limit parameter in the prepared statement
			if (limitNumber != 0) {
				ps.setInt(1, limitNumber);
			}

			// Execute the SQL query and retrieve the result set
			ResultSet rs = ps.executeQuery();

			// Collect maxTotalQuizzes and maxWinCount while iterating through the result
			// set
			int totalNumberOfMatchesPlayedByAllPlayers = getMaxTotalQuizzes(rs);
			int totalWinCountByAllPlayers = getMaxWinCount(rs);
			System.out.println("S: TotMatc: " + totalNumberOfMatchesPlayedByAllPlayers);
			System.out.println("S: TotWinC: " + totalWinCountByAllPlayers);
			// Reset the cursor to the beginning of the result set
			rs.beforeFirst();

			// Iterate through the result set to process player data
			while (rs.next()) {
				// Retrieve player details from the result set
				String name = rs.getString("name");
				int numberOfWins = rs.getInt("winCount");
				int totalNumberOfMatchesPlayed = rs.getInt("totalQuizzes");
				double winPercentage = rs.getDouble("winPercentage");

				// Calculate the weighted score using defined weights
				double weightedScore = (weightExperience
						* (totalNumberOfMatchesPlayed / totalNumberOfMatchesPlayedByAllPlayers))
						+ (weightWinCount * (numberOfWins / totalWinCountByAllPlayers));
				System.out.println("S: wScore: " + weightedScore);
				// Create a Person object and set its attributes
				Person person = new Person();
				person.setName(name);
				person.setTotalWins(numberOfWins);
				person.setTotalQuizTaken(totalNumberOfMatchesPlayed);
				person.setWinPercentage(weightedScore);

				// Add the Person object to the list of top players
				topPlayers.add(person);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Sort the topPlayers list by weighted score in descending order
		topPlayers.sort((p1, p2) -> Double.compare(p2.getWinPercentage(), p1.getWinPercentage()));

		// Return the list of top players
		return topPlayers;
	}

	private static int getMaxTotalQuizzes(ResultSet rs) throws SQLException {
		int maxTotalQuizzes = 0;

		while (rs.next()) {
			int currentTotalQuizzes = rs.getInt("totalQuizzes");
			maxTotalQuizzes = Math.max(maxTotalQuizzes, currentTotalQuizzes);
		}

		// Reset the cursor to the beginning of the result set
		rs.beforeFirst();

		return maxTotalQuizzes == 0 ? 1 : maxTotalQuizzes; // Avoid division by zero
	}

	private static int getMaxWinCount(ResultSet rs) throws SQLException {
		int maxWinCount = 0;

		while (rs.next()) {
			int currentWinCount = rs.getInt("winCount");
			maxWinCount = Math.max(maxWinCount, currentWinCount);
		}

		// Reset the cursor to the beginning of the result set
		rs.beforeFirst();

		return maxWinCount == 0 ? 1 : maxWinCount; // Avoid division by zero
	}

	public static void populateQuizQuestionMapping(Connection connection, int quizId, int questionId,
			int selectedOptionId, boolean isCorrect) {
		String insertQueryForTables = "INSERT INTO " + databaseName + ".`" + quizQuestionTableName
				+ "`(quizId, questionId, selectedOptionId, isCorrect) VALUES(?, ?, ?, ?)";

		try (PreparedStatement ps1 = connection.prepareStatement(insertQueryForTables)) {
			System.out.println("S: isQ: " + insertQueryForTables);
			// Set values for name and password
			ps1.setInt(1, quizId);
			ps1.setInt(2, questionId);
			ps1.setInt(3, selectedOptionId);
			ps1.setBoolean(4, isCorrect);

			// Execute the query
			int rowsModified = ps1.executeUpdate();

			System.out.println("Rows modified: " + rowsModified);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void updateQuizQuestionMapping(Connection connection, int qmId, int questionId, int selectedOptionId,
			boolean isCorrect) {
		String insertQueryForTables = "UPDATE " + databaseName + ".`" + quizQuestionTableName
				+ "` SET questionId = ?, selectedOptionId = ?, isCorrect = ? WHERE id = ?;";

		try (PreparedStatement ps1 = connection.prepareStatement(insertQueryForTables)) {
			System.out.println("S: upQ: " + insertQueryForTables);
			// Set values for name and password
			ps1.setInt(1, questionId);
			ps1.setInt(2, selectedOptionId);
			ps1.setBoolean(3, isCorrect);
			ps1.setInt(4, qmId);

			// Execute the query
			int rowsModified = ps1.executeUpdate();

			System.out.println("Rows modified: " + rowsModified);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void populateUserData(String tableName, Connection connection, String playerName, String password) {
		String insertQueryForTables = "INSERT INTO " + databaseName + ".`" + tableName
				+ "`(name, password, totalQuizzes, winCount, lossCount) VALUES(?, ?, ?, ?, ?)";

		try (PreparedStatement ps1 = connection.prepareStatement(insertQueryForTables)) {
			// Set values for name and password
			ps1.setString(1, playerName);
			ps1.setString(2, password);
			System.out.println("S: isQ: " + insertQueryForTables);
			// Set default values for totalQuizzes, winCount, and lossCount
			ps1.setInt(3, 0);
			ps1.setInt(4, 0);
			ps1.setInt(5, 0);

			// Execute the query
			int rowsModified = ps1.executeUpdate();

			System.out.println("Rows modified: " + rowsModified);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void createNecessaryTables(Connection connection) {
		// CREATING ALL THE NECESSARY TABLES
		createTableQuery(categoryTableName, categoryTableValues, connection);
		createTableQuery(optionTableName, optionTableValues, connection);
		createTableQuery(questionTableName, questionTableValues, connection);
		createTableQuery(questionOptionTableName, questionOptionTableValues, connection);
		createTableQuery(userTableName, userTableValues, connection);
		createTableQuery(quizTableName, quizTableValues, connection);
		createTableQuery(quizQuestionTableName, quizQuestionTableValues, connection);
	}

	public static void populateData(String tableName, String columnNames, Connection connection) {
		String insertQueryForTables = "INSERT INTO " + databaseName + ".`" + tableName + "`(" + columnNames
				+ ") VALUES(";
		String[] columnNamesArray = columnNames.split(",");

		for (int i = 0; i < columnNamesArray.length; i++) {
			insertQueryForTables += "?,";
		}

		// Remove the trailing comma and close the VALUES parentheses
		insertQueryForTables = insertQueryForTables.substring(0, insertQueryForTables.length() - 1) + ")";

		System.out.println("Insert Query: " + insertQueryForTables);

		try (Scanner sc = new Scanner(System.in);
				PreparedStatement ps1 = connection.prepareStatement(insertQueryForTables)) {

			System.out.println("colArr: " + Arrays.toString(columnNamesArray));
			System.out.println("Enter the number of times you want to insert values: ");
			int n = sc.nextInt();
			sc.nextLine();

			while (n > 0) {
				for (int i = 0; i < columnNamesArray.length; i++) {
					System.out.println("Enter value for " + columnNamesArray[i] + ": ");
					String s = sc.nextLine();
					ps1.setObject(i + 1, s); // Use i + 1 as parameter index
				}
				ps1.addBatch();
				n--;
			}

			// Execute the batch and clear it
			int[] res = ps1.executeBatch();
			ps1.clearBatch();

			System.out.println("Rows modified: " + Arrays.toString(res));
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error occurred: " + e.getMessage());
		}
	}

	public static List<Question> fetchDataFromTheTables(String tableName, Connection connection) {
		List<Question> questions = new ArrayList<>();
		String fetchDataQuery = "SELECT * FROM " + databaseName + "." + tableName + ";";
//	         System.out.println("fetch Q: "+fetchDataQuery);
		try (PreparedStatement ps1 = connection.prepareStatement(fetchDataQuery)) {
			ResultSet resultSet = ps1.executeQuery();
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();

			while (resultSet.next()) {
				Question question = new Question(resultSet.getInt("id"), resultSet.getString("question"),
						resultSet.getInt("category_id"), resultSet.getInt("answer_Id"));
				questions.add(question);
			}
		} catch (SQLException e) {
			System.out.println("Exception occurred: " + e.getMessage());
		}
		return questions;
	}

	// Method to choose a category and return its ID
	public static void chooseCategory(Connection connection, List<Category> categoryList) {
		try {
			String fetchDataQuery = "SELECT * FROM " + databaseName + "." + categoryTableName + ";";
			try (PreparedStatement ps1 = connection.prepareStatement(fetchDataQuery)) {
				ResultSet resultSet = ps1.executeQuery();

				while (resultSet.next()) {
					int id = resultSet.getInt("id");
					String categoryName = resultSet.getString("category");
					Category c = new Category(id, categoryName);
					categoryList.add(c);
				}
				System.out.println("S: cList: " + categoryList.size());
			}
		} catch (SQLException e) {
			System.out.println("Exception occurred: " + e.getMessage());
			return; // Return -1 to indicate an error
		}
	}

	static class Category {

		public Category(@JsonProperty("id") int id, @JsonProperty("category") String category) {
			super();
			this.id = id;
			this.category = category;
		}

		private int id;
		private String category;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Category [id=" + id + ", category=" + category + "]";
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

	}
}
