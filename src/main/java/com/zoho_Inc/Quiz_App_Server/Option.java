package com.zoho_Inc.Quiz_App_Server;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Option {
	private int optionId;
	private String optionText;

	@JsonCreator
	public Option(@JsonProperty("optionId") int optionId, @JsonProperty("optionText") String optionText) {
		this.optionId = optionId;
		this.optionText = optionText;
	}

	@JsonCreator
	public Option(@JsonProperty("optionText") String optionText) {
		this.optionText = optionText;
	}

	@JsonProperty("optionText")
	public String getOptionText() {
		return optionText;
	}

	@JsonProperty("optionId")
	public int getOptionId() {
		return optionId;
	}

	public String toString() {
		return optionId + ": " + optionText;
	}
}
