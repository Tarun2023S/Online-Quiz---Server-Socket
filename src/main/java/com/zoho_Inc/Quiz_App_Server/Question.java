package com.zoho_Inc.Quiz_App_Server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Question {
	private int id;
	private String questionText;
	private int categoryId;
	private int answerId;

	@JsonCreator
	public Question(@JsonProperty("id") int id, @JsonProperty("questionText") String questionText,
			@JsonProperty("categoryId") int categoryId, @JsonProperty("answerId") int answerId) {
		this.id = id;
		this.questionText = questionText;
		this.categoryId = categoryId;
		this.answerId = answerId;
	}

	@JsonProperty("id")
	public void setId(int id) {
		this.id = id;
	}

	@JsonProperty("questionText")
	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	@JsonProperty("categoryId")
	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	@JsonProperty("answerId")
	public void setAnswerId(int answerId) {
		this.answerId = answerId;
	}

	@JsonProperty("id")
	public int getId() {
		return id;
	}

	@JsonProperty("questionText")
	public String getQuestionText() {
		return questionText;
	}

	@JsonProperty("categoryId")
	public int getCategoryId() {
		return categoryId;
	}

	@JsonProperty("answerId")
	public int getAnswerId() {
		return answerId;
	}

	@Override
	public String toString() {
		return "id: " + id + "\nquestionText: " + questionText + "\ncategory: " + categoryId + "\nanswerId: "
				+ answerId;
	}
}
