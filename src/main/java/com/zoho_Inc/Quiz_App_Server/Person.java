package com.zoho_Inc.Quiz_App_Server;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Person implements Serializable {
	private static int id = 1;

	private int personId;
	private String name;
	private String password;
	private int totalQuizTaken = 0;
	private int totalWins = 0;
	private int totalLosses = 0;
	private double winPercentage = 0;

	public double getWinPercentage() {
		return winPercentage;
	}

	public void setWinPercentage(double winPercentage) {
		this.winPercentage = winPercentage;
	}

	public Person() {

	}

	@JsonCreator
	public Person(@JsonProperty("name") String name, @JsonProperty("password") String password) {
//        this.personId = id++;
		this.name = name;
		this.password = password;
	}

	public void setPersonId(int personId) {
		this.personId = personId;
	}

	public int getPersonId() {
		return personId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getTotalQuizTaken() {
		return totalQuizTaken;
	}

	public void setTotalQuizTaken(int totalQuizTaken) {
		this.totalQuizTaken = totalQuizTaken;
	}

	public int getTotalWins() {
		return totalWins;
	}

	public void setTotalWins(int totalWins) {
		this.totalWins = totalWins;
	}

	public int getTotalLosses() {
		return totalLosses;
	}

	public void setTotalLosses(int totalLosses) {
		this.totalLosses = totalLosses;
	}

	@Override
	public String toString() {
		return "PersonId: " + personId + "\nPerson name: " + name + "\nTotal Games Played: " + totalQuizTaken
				+ "\nTotal Wins: " + totalWins + "\nTotal Losses: " + totalLosses;
	}
}
