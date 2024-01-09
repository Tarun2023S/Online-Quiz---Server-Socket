package com.zoho_Inc.Quiz_App_Server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class QuizLog {
	@Override
	public String toString() {
		return "QuizLog [quizId=" + quizId + ", isTimed=" + isTimed + ", categoryId=" + categoryId
				+ " correctlyAnswered=" + correctlyAnswered + ", totalQuestions=" + totalQuestions + ", attempts="
				+ attempts + ", percent=" + percent + "]";
	}

	private int quizId;
	private int isTimed;
	private int categoryId;
	private int correctlyAnswered;
	private int totalQuestions;
	private int attempts;
	private double percent;

	@JsonCreator
	public QuizLog(@JsonProperty("quizId") int quizId, @JsonProperty("isTimed") int isTimed,
			@JsonProperty("categoryId") int categoryId, @JsonProperty("correctlyAnswered") int correctlyAnswered,
			@JsonProperty("totalQuestions") int totalQuestions, @JsonProperty("attempts") int attempts,
			@JsonProperty("percent") double percent) {
		this.quizId = quizId;
		this.isTimed = isTimed;
		this.categoryId = categoryId;
		this.correctlyAnswered = correctlyAnswered;
		this.totalQuestions = totalQuestions;
		this.attempts = attempts;
		this.percent = percent;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public int getQuizId() {
		return quizId;
	}

	public int getIsTimed() {
		return isTimed;
	}

	public void setIsTimed(int isTimed) {
		this.isTimed = isTimed;
	}

	public int getCorrectlyAnswered() {
		return correctlyAnswered;
	}

	public void setCorrectlyAnswered(int correctlyAnswered) {
		this.correctlyAnswered = correctlyAnswered;
	}

	public int getTotalQuestions() {
		return totalQuestions;
	}

	public void setTotalQuestions(int totalQuestions) {
		this.totalQuestions = totalQuestions;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public double getPercent() {
		return percent;
	}

	public void setPercent(double percent) {
		this.percent = percent;
	}

	public void setQuizId(int quizId) {
		this.quizId = quizId;
	}
}