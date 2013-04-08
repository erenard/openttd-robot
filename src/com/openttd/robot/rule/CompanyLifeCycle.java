package com.openttd.robot.rule;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.ChatEvent;
import com.openttd.admin.event.ChatEventListener;
import com.openttd.admin.event.ClientEvent;
import com.openttd.admin.event.ClientEventListener;
import com.openttd.admin.event.CompanyEvent;
import com.openttd.admin.event.CompanyEventListener;
import com.openttd.admin.event.CompanyEvent.Action;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.model.Client;
import com.openttd.admin.model.Company;
import com.openttd.admin.model.Game;
import com.openttd.network.admin.NetworkClient.Send;
import com.openttd.robot.model.ExternalUser;
import com.openttd.util.Convert;
import java.util.ArrayList;

/**
 * Company life-cycle rule :
 * Rule #1: only logged player can create
 * Rule #2: only one company by ip address can exist
 * Rule #3: handle !resetme
 * Rule #4: handle $companies, $cps
 * Rule #5: handle !info
 * Rule #6: handle $reset
 */
public class CompanyLifeCycle extends AbstractRule implements CompanyEventListener, ChatEventListener, ClientEventListener {
	
	private final ExternalUsers externalUsers;

	public CompanyLifeCycle(OpenttdAdmin openttdAdmin, ExternalUsers externalUsers) {
		super(openttdAdmin);
		this.externalUsers = externalUsers;
	}

	//Company id stored by the ip address of the creator  
	private Map<String, Long> createDateByIpAddress = new HashMap<String, Long>();
	private Collection<Integer> clientsOnProbation = new HashSet<Integer>();
	private Collection<Integer> certifiedCompanies = new HashSet<Integer>();

	//Configuration
	public static int CREATE_COMPANY_DAY_TIMEOUT = 90;
	public boolean checkLogin = true;
	public boolean checkIpAddress = true;
	
	@Override
	public void onClientEvent(ClientEvent clientEvent) {
		switch (clientEvent.getAction()) {
		case CREATE: {
			int clientId = clientEvent.getClientId();
			Client client = clientEvent.getOpenttd().getClient(clientId);
			int companyId = client.getCompanyId();
			if(companyId != 255 && !certifiedCompanies.contains(companyId)) {
				clientsOnProbation.add(clientId);
			}
			break;
		}
		case UPDATE: {
			int clientId = clientEvent.getClientId();
			if(clientsOnProbation.contains(clientId)) {
				clientsOnProbation.remove(clientId);
				Game game = clientEvent.getOpenttd();
				Client client = game.getClient(clientId);
				int companyId = client.getCompanyId();
				testCompany(companyId, game);
			}
			break;
		}
		case DELETE: {
			clientsOnProbation.remove(clientEvent.getClientId());
			break;
		}
		}		
	}

	@Override
	public Collection<Class> listEventTypes() {
		Collection<Class> listEventTypes = new ArrayList<Class>(3);
		listEventTypes.add(CompanyEvent.class);
		listEventTypes.add(ClientEvent.class);
		listEventTypes.add(ChatEvent.class);
		return listEventTypes;
	}

	private void testCompany(int companyId, Game game) {
		/*
		 * This case will always be called AFTER UPDATE,
		 * It's due to openttd.
		 */
		Send send = super.getSend();
		//Case : Company creation
		// Find the company owner
		ExternalUser companyOwner = null;
		Client creator = null;
		String ipAddress = null;
		{
			Collection<Client> clients = game.getClients(companyId);
			if(clients != null) {
				creator = clients.iterator().next();
				if(creator != null) {
					ipAddress = creator.getIp();
					//Client is logged
					companyOwner = externalUsers.getExternalUser(creator.getId());
				}
			}
		}
		// Verify Rule #1
		if(checkLogin && companyOwner == null) {
			send.chatCompany((short) companyId, "Company " + companyId + " creation rejected: Client non logged.");
			deleteCompany(game, companyId);
			Collection<Client> clients = game.getClients(companyId);
			for(Client client : clients) {
				externalUsers.showHowtoLogin(client.getId());
			}
			return;
		}
		// Verify Rule #2
		if(checkIpAddress && (companyOwner == null || !companyOwner.isAdmin())) {
			Long dayOfLastCreation = createDateByIpAddress.get(ipAddress);
			if(dayOfLastCreation != null) {
				Calendar dateOfAllowedCreation = Convert.dayToCalendar(dayOfLastCreation);
				dateOfAllowedCreation.add(Calendar.DAY_OF_YEAR, CREATE_COMPANY_DAY_TIMEOUT);
				if(dateOfAllowedCreation.after(game.getDate())) {
					DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.UK);
					StringBuffer sb = new StringBuffer();
					Formatter formatter = new Formatter(sb);
					formatter.format("Company %d creation rejected: Only one company every 90 days is allowed (%s).", companyId, dateFormat.format(dateOfAllowedCreation.getTime()));
					send.chatCompany((short) companyId, sb.toString());
					deleteCompany(game, companyId);
					return;
				}
			}
		}
		// Add the company to the allowed companies
		if(ipAddress != null) {
			certifiedCompanies.add(companyId);
			createDateByIpAddress.put(ipAddress, Convert.calendarToDay(game.getDate()));
		}
		if(companyOwner != null) {
			//First remove ownership of every other company
			Collection<Company> companies = game.getCompanies();
			for(Company company : companies) {
				ExternalUser externalUser = externalUsers.getOwnerOf(company.getId());
				if(companyOwner.equals(externalUser)) {
					externalUsers.removeOwnerOf(company.getId());
				}
			}
			//Then own a company
			externalUsers.setOwnerOf(companyId, companyOwner);
		}
	}
	
	@Override
	public void onCompanyEvent(CompanyEvent companyEvent) {
		int companyId = companyEvent.getCompanyId();
		Action action = companyEvent.getAction();
		Game game = companyEvent.getOpenttd();
		switch(action) {
		case CREATE: {
			testCompany(companyId, game);
			break;
		}
		case UPDATE: {
			break;
		}
		case DELETE: {
			//Case : Company deletion
			certifiedCompanies.remove(companyId);
			//Keep the local model up to date
			externalUsers.removeOwnerOf(companyId);
			break;
		}
		}
		
	}

	private void deleteCompany(Game game, int companyId) {
		Send send = super.getSend();
		Collection<Client> clients = game.getClients(companyId);
		for(Client client : clients) {
			send.rcon("move " + client.getId() + " 255");
		}
		send.rcon("reset_company " + (companyId + 1));
	}

	@Override
	public void onChatEvent(ChatEvent chatEvent) {
		Integer clientId = chatEvent.getClientId();
		String message = chatEvent.getMessage();
		Game openttd = chatEvent.getOpenttd();
		if(clientId != null && message != null) {
			message = message.trim();
			if(message.equals("!resetme")) {
				//Rule #3
				Client client = openttd.getClient(clientId);
				int companyId = client.getCompanyId();
				if(companyId >= 0 && companyId < 255) {
					deleteCompany(openttd, companyId);
				}
			} else if(message.equals("!info")) {
				//Rule #5
				showInfo(openttd, clientId);
			} else if(externalUsers.getExternalUser(clientId) != null
					&& externalUsers.getExternalUser(clientId).isAdmin()) {
				if(message.equals("$companies") || message.equals("$cps")) {
					//Rule #4
					showCompanies(openttd, clientId);
				} else if(message.startsWith("$reset") || message.startsWith("$r")) {
					//Rule #6
					try {
						String argument = message.split(" ")[1].trim();
						Integer companyId = new Integer(argument);
						deleteCompany(openttd, companyId);
					} catch(Exception e) {
						showMessage(clientId, "Usage: $reset companyId (try $companies to find companyIds)");
					}
				}
			}
		}
	}

	private void showMessage(int clientId, String message) {
		Send send = super.getSend();
		send.chatClient(clientId, message);
	}
	
	private void showCompanies(Game openttd, Integer clientId) {
		Send send = super.getSend();
		DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, Locale.UK);
		send.chatClient(clientId, "#Id, Name, CEO, Inauguration.");
		for(Company company : openttd.getCompanies()) {
			if(company != null) {
				StringBuilder sb = new StringBuilder("#");
				Formatter formatter = new Formatter(sb);
				formatter.format("# %3d, %255s, %255s, %s",
						company.getId(),
						company.getName(),
						externalUsers.getOwnerOf(company.getId()).getName(),
						dateFormat.format(company.getInauguration().getTime()));
				send.chatClient(clientId, sb.toString());
			}
		}
	}
	
	private void showInfo(Game openttd, Integer clientId) {
		Send send = super.getSend();
		Client client = openttd.getClient(clientId);
		if(client != null) {
			String userName = "Anonymous";
			{
				ExternalUser user = externalUsers.getExternalUser(clientId);
				if(user != null) {
					userName = user.getName();
				}
			}
			send.chatClient(clientId, "User: " + userName + ", play as (" + client.getName() + ").");
			int companyId = client.getCompanyId();
			if(companyId == 255) {
				send.chatClient(clientId, "Spectator.");
			} else {
				Company company = openttd.getCompany(companyId);
				ExternalUser owner = externalUsers.getOwnerOf(companyId);
				if(company != null && owner != null) {
					send.chatClient(clientId, "Company: " + company.getName() + ", owned by " + owner.getName() + ".");	
				}
			}
		}
	}
}
