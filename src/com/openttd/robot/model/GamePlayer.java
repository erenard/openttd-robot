package com.openttd.robot.model;

public class GamePlayer {
	private ExternalUser externalUser;
	private double accomplishment;
	private double duration;
	private boolean winner;
	private int companyId;
	private int score;
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(externalUser.getId() + ":" + externalUser.getName());
		sb.append(" accomplishment:" + accomplishment);
		sb.append(" duration:" + duration);
		sb.append(" score:" + score);
		sb.append(" winner:" + winner);
		return sb.toString();
	}
	public boolean isWinner() {
		return winner;
	}
	public void setWinner(boolean winner) {
		this.winner = winner;
	}
	public ExternalUser getExternalUser() {
		return externalUser;
	}
	public void setExternalUser(ExternalUser externalUser) {
		this.externalUser = externalUser;
	}
	public double getAccomplishment() {
		return accomplishment;
	}
	public void setAccomplishment(double accomplishment) {
		this.accomplishment = accomplishment;
	}
	public double getDuration() {
		return duration;
	}
	public void setDuration(double duration) {
		this.duration = duration;
	}
	public int getCompanyId() {
		return companyId;
	}
	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}
//	public int getScore() {
//		return score;
//	}
//	public void setScore(int score) {
//		this.score = score;
//	}
}
