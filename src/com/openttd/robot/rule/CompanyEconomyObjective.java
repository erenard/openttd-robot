package com.openttd.robot.rule;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.ChatEvent;
import com.openttd.admin.event.ChatEventListener;
import com.openttd.admin.event.CompanyEvent;
import com.openttd.admin.event.CompanyEventListener;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.event.DateEventListener;
import com.openttd.admin.model.Client;
import com.openttd.admin.model.Company;
import com.openttd.admin.model.Game;
import com.openttd.network.admin.NetworkAdminSender;
import com.openttd.robot.ExternalServices;
import com.openttd.robot.ExternalServices.ExternalGameService;
import com.openttd.robot.model.ExternalUser;
import com.openttd.robot.model.GamePlayer;

/**
 * 
 * Rule #1: Handle !goal, !g
 * Rule #2: Handle !score, !cv, !cp
 */
public class CompanyEconomyObjective extends AbstractRule implements CompanyEventListener, DateEventListener, ChatEventListener {
	
	//TODO Add cargo delivered
	public enum ObjectiveType {
		VALUE, PERFORMANCE;
	}
	
	private final ExternalUsers externalUsers;
	private final ExternalGameService externalGameService = ExternalServices.getInstance().getExternalGameService();
	private final ObjectiveType objectiveType;
	private final double objectiveValue;
	private boolean objectiveReached;
	private NumberFormat companyValueFormat;
	private int currentWeek;
	private int currentTrimester;
	private String winnerName;
	private int victoryDisplayWeek;
	
	private Map<Integer, Integer> scoreByCompanyId = new HashMap<Integer, Integer>();
	private Map<Integer, Double> progressByCompanyId = new HashMap<Integer, Double>();
	private Map<Integer, Double> progressHistoryByCompanyId = new HashMap<Integer, Double>();
	
	public CompanyEconomyObjective(OpenttdAdmin openttdAdmin, ExternalUsers externalUsers, ObjectiveType objectiveType, double objectiveValue) {
		super(openttdAdmin);
		this.objectiveType = objectiveType;
		this.objectiveValue = objectiveValue;
		this.externalUsers = externalUsers;
		this.objectiveReached = false;
		this.companyValueFormat = new DecimalFormat("#,##0 EUR");
		this.currentWeek = 0;
		this.currentTrimester = 0;
	}
	
	private boolean isNewTrimester(Calendar now) {
		int nowTrimester = now.get(Calendar.MONTH) / 3 + 4 * now.get(Calendar.YEAR);
		if(nowTrimester > currentTrimester) {
			currentTrimester = nowTrimester;
			return true;
		}
		return false;
	}
	
	private boolean isNewWeek(Calendar now) {
		int nowWeek = now.get(Calendar.WEEK_OF_YEAR);
		if(nowWeek != currentWeek) {
			currentWeek = nowWeek;
			return true;
		}
		return false;
	}

	@Override
	public Collection<Class> listEventTypes() {
		Collection<Class> listEventTypes = new ArrayList<Class>(3);
		listEventTypes.add(CompanyEvent.class);
		listEventTypes.add(DateEvent.class);
		listEventTypes.add(ChatEvent.class);
		return listEventTypes;
	}

	@Override
	public void onDateEvent(DateEvent dateEvent) {
		Calendar now = dateEvent.getOpenttd().getDate();
		if(isNewWeek(now)) {
			Game openttd = dateEvent.getOpenttd();
			if(!objectiveReached) {
				// Test victory
				List<Entry<Integer, Double>> entries = getSortedProgressionByCompanyId();
				for(Entry<Integer, Double> entry : entries) {
					if(entry.getValue() >= 1) {
						Company company = openttd.getCompany(entry.getKey());
						winnerName = company.getName();
						victoryDisplayWeek = 4;
						saveGame(dateEvent.getOpenttd());
						objectiveReached = true;
					}
					
				}
			}
			if(objectiveReached) {
				victoryDisplayWeek--;
				if(victoryDisplayWeek > 0) {
					// Display game over and victory
					broadcastVictory(dateEvent.getOpenttd());
				} else {
					endGame();
				}
			}
		}
		//Show game progression
		if(!objectiveReached && isNewTrimester(now)) {
			Game openttd = dateEvent.getOpenttd();
			showScore(-1, openttd);
			progressHistoryByCompanyId.clear();
			for(Entry<Integer, Double> entry : progressByCompanyId.entrySet()) {
				progressHistoryByCompanyId.put(entry.getKey(), entry.getValue());
			}
		}
	}

	private void saveGame(Game openttd) {
		Collection<GamePlayer> gamePlayers = calculateScores(openttd);
		externalGameService.saveGame(gamePlayers);
	}

	@Override
	public void onCompanyEvent(CompanyEvent companyEvent) {
		if(objectiveReached) return;
		int companyId = companyEvent.getCompanyId();
		switch(companyEvent.getAction()) {
		case UPDATE: {
			// Update scores
			Game game = companyEvent.getOpenttd();
			Company company = game.getCompany(companyId);
			double progression;
			if(objectiveType.equals(ObjectiveType.PERFORMANCE)) {
				progression = company.getPerformance() / objectiveValue;
			} else {
				progression = company.getValue() / objectiveValue;
			}
			progressByCompanyId.put(companyId, progression);
			break;
		}
		case DELETE: {
			scoreByCompanyId.remove(companyId);
			progressByCompanyId.remove(companyId);
			progressHistoryByCompanyId.remove(companyId);
		}
		}
	}

	@Override
	public void onChatEvent(ChatEvent chatEvent) {
		int clientId = chatEvent.getClientId();
		String message = chatEvent.getMessage();
		if(message != null) {
			message = message.trim().toLowerCase();
			if(message.equals("!goal") || message.equals("!g")) {
				//Rule #1
				showGoal(clientId);
			} else if(message.equals("!score") || message.equals("!cv") || message.equals("!cp")) {
				//Rule #2
				showScore(clientId, chatEvent.getOpenttd());
			} else if(message.equals("$score")) {
				Collection<GamePlayer> gamePlayers = calculateScores(chatEvent.getOpenttd());
				NetworkAdminSender send = super.getSend();
				for(GamePlayer gamePlayer : gamePlayers) {
					send.chatClient(clientId, gamePlayer.toString());
				}
			}
		}
	}
	
	private void showGoal(int clientId) {
		NetworkAdminSender send = super.getSend();
		send.chatClient(clientId, "Goal ***");
		if(this.objectiveType.equals(ObjectiveType.PERFORMANCE)) {
			send.chatClient(clientId, "The first company to reach a performance of " + objectiveValue + " win the game.");
		} else {
			send.chatClient(clientId, "The first company to reach a value of " + objectiveValue * 2 + " EUR win the game.");
		}
	}
	
	private void broadcastVictory(Game openttd) {
		NetworkAdminSender send = super.getSend();
		send.chatBroadcast("Game Over : " + winnerName + " win !!!");
		showScore(-1, openttd);
		send.chatBroadcast("Server will restart.");
	}
	
	private void showScore(long clientId, Game openttd) {
		NetworkAdminSender send = super.getSend();
		send.chatClient(clientId, "Score ***");
		List<Entry<Integer, Double>> entries = getSortedProgressionByCompanyId();
		int idx = 0;
		for(Entry<Integer, Double> entry : entries) {
			idx++;
			int companyId = entry.getKey();
			Company company = openttd.getCompany(companyId);
			
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("#" + idx);
			if(objectiveType.equals(ObjectiveType.PERFORMANCE)) {
				stringBuilder.append(" - " + company.getPerformance());
			} else {
				stringBuilder.append(" - " + companyValueFormat.format(company.getValue() * 2));
			}
			int progressPercent = (int) (entry.getValue() * 100);
			int lastProgressPercent = 0;
			if(progressHistoryByCompanyId.get(companyId) != null) {
				lastProgressPercent = (int) (progressHistoryByCompanyId.get(companyId) * 100);
			}
			stringBuilder.append(" - " + progressPercent + "%");
			stringBuilder.append(" (" + (progressPercent - lastProgressPercent >= 0 ? "+" : "") + (progressPercent - lastProgressPercent) + "%)");
			stringBuilder.append(" - " + company.getName());
			ExternalUser owner = externalUsers.getOwnerOf(companyId);
			if(objectiveReached) {
				Integer points = scoreByCompanyId.get(companyId);
				stringBuilder.append(" " + owner.getName() + " score " + (points != null ? points.intValue() : 0) + " points on the leaderboard");
			} else {
				stringBuilder.append("(" + owner.getName() + ")");
			}
			stringBuilder.append(".");
			if(clientId < 0) {
				send.chatBroadcast(stringBuilder.toString());
			} else {
				send.chatClient(clientId, stringBuilder.toString());
			}
		}
	}

	
	private List<Entry<Integer, Double>> getSortedProgressionByCompanyId() {
		List<Entry<Integer, Double>> sorted = new ArrayList<Map.Entry<Integer,Double>>(progressByCompanyId.entrySet());
		Collections.sort(sorted, new Comparator<Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		Collections.reverse(sorted);
		return sorted;
	}
	
	private void endGame() {
		externalGameService.endGame();
	}
	
	private Collection<GamePlayer> calculateScores(Game openttd) {
		Calendar openttdDate = openttd.getDate();
		Collection<GamePlayer> gamePlayers = new ArrayList<GamePlayer>();
		long maxDuration = 0;
		double maxAccomplishment = 0.0;
		for(Client client : openttd.getClients()) {
			int clientId = client.getId();
			ExternalUser externalUser = externalUsers.getExternalUser(clientId);
			Company company = openttd.getCompany(client.getCompanyId());
			if(externalUser != null && company != null) {
				GamePlayer gamePlayer = new GamePlayer();
				gamePlayer.setExternalUser(externalUser);
				gamePlayer.setCompanyId(company.getId());
				Calendar inauguration = company.getInauguration();
				long duration = openttdDate.getTimeInMillis() - inauguration.getTimeInMillis();
				gamePlayer.setDuration(duration);
				maxDuration = Math.max(maxDuration, duration);
				if(objectiveType.equals(ObjectiveType.PERFORMANCE)) {
					gamePlayer.setAccomplishment(company.getPerformance());
				} else {
					gamePlayer.setAccomplishment(company.getValue());
				}
				maxAccomplishment = Math.max(maxAccomplishment, gamePlayer.getAccomplishment());
				gamePlayers.add(gamePlayer);
			}
		}
		for(GamePlayer gamePlayer : gamePlayers) {
			//Duration scaling to 0.0 ... 1.0
			double duration = 0;
			if(maxDuration != 0) duration = (double) gamePlayer.getDuration() / (double) maxDuration;
			gamePlayer.setDuration(duration);
			
			//Accomplishment scaling to 0.0 ... 1.0
			double accomplishment = 0;
			if(maxAccomplishment != 0) accomplishment = (double) gamePlayer.getAccomplishment() / (double) maxAccomplishment;
			gamePlayer.setAccomplishment(accomplishment);
			if(accomplishment == 1.0) gamePlayer.setWinner(true);

			scoreByCompanyId.put(gamePlayer.getCompanyId(), (int) (100 * accomplishment / duration));
		}
		return gamePlayers;
	}
}
