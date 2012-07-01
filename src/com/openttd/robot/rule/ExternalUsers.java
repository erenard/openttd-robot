package com.openttd.robot.rule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.ChatEvent;
import com.openttd.admin.event.ChatEventListener;
import com.openttd.admin.model.Company;
import com.openttd.admin.model.Game;
import com.openttd.network.admin.NetworkClient.Send;
import com.openttd.robot.ExternalServices;
import com.openttd.robot.ExternalServices.ExternalUserService;
import com.openttd.robot.model.ExternalUser;

/**
 * Store the users info
 * Rule #1: Handle !login
 * Rule #2: Handle !howto
 * Rule #3: Handle !rename
 */
public class ExternalUsers implements ChatEventListener {
	
	private static final Logger log = LoggerFactory.getLogger(ExternalUsers.class);
	
	private final OpenttdAdmin openttdAdmin;
	private final ExternalUserService externalUserService = ExternalServices.getInstance().getExternalUserService();

	public ExternalUsers(OpenttdAdmin openttdAdmin) {
		this.openttdAdmin = openttdAdmin;
	}

	private Map<Integer, ExternalUser> externalUserByClientId = new HashMap<Integer, ExternalUser>();
	private Map<Integer, ExternalUser> externalUserByCompanyId = new HashMap<Integer, ExternalUser>();

	@Override
	public void onChatEvent(ChatEvent chatEvent) {
		Integer clientId = chatEvent.getClientId();
		String message = chatEvent.getMessage();
		if(clientId != null && message != null) {
			message = message.trim();
			if(message.startsWith("!login ")) {
				//Rule #1
				String loginToken = message.substring("!login ".length());
				ExternalUser user = externalUserService.identifyUser(loginToken);
				if(user != null) {
					boolean logged = true;
					if(externalUserByClientId.containsKey(clientId)) {
						if(externalUserByClientId.get(clientId).equals(user)) {
							showClientAlreadyLogged(clientId);
						} else {
							logged = false;
						}
					} else {
						showLoginSucceed(clientId);
						externalUserByClientId.put(clientId, user);
						renameClient(clientId, user.getName());
					}
					Game openttd = chatEvent.getOpenttd();
					int companyId = openttd.getClient(clientId).getCompanyId();
					if(logged && companyId != 255) {
						ExternalUser companyOwner = externalUserByCompanyId.get(companyId);
						Company company = openttd.getCompany(companyId);
						String companyName = company.getName();
						if(companyOwner != null) {
							if(companyOwner.equals(user)) {
								showCompanyAlreadyOwned(clientId, companyName);
							} else {
								showCompanyAlreadyOwned(clientId, companyName, companyOwner);
							}
						} else {
							Set<Entry<Integer, ExternalUser>> copy = new HashSet<Entry<Integer, ExternalUser>>(externalUserByCompanyId.entrySet());
							for(Entry<Integer, ExternalUser> entry : copy) {
								log.info(entry.getKey() + " " + entry.getValue());
								if(user.equals(entry.getValue())) {
									externalUserByCompanyId.remove(entry.getKey());
								}
							}
							externalUserByCompanyId.put(companyId, user);
							showCompanyOwned(clientId, companyName, user);
						}
					}
				} else {
					showLoginFailed(clientId);
					showHowtoLogin(clientId);
				}
			} else if(message.equals("!howto")) {
				//Rule #2
				showHowtoLogin(clientId);
			} else if(message.startsWith("!rename ")) {
				//Rule #3
				String newName = message.substring("!rename ".length()).trim();
				renameClient(clientId, newName);
			}
		}
	}
	
	private void showCompanyOwned(Integer clientId, String companyName, ExternalUser user) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, "Congratulation ! You now own " + companyName + ".");
	}

	private void showCompanyAlreadyOwned(Integer clientId, String companyName) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, "You already own " + companyName + ".");
	}

	private void showCompanyAlreadyOwned(Integer clientId, String companyName, ExternalUser user) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, companyName + "is already owned by " + user.getName());
	}

	private void renameClient(Integer clientId, String name) {
		Send send = openttdAdmin.getSend();
		if(name.length() > 1) {
			if(name.startsWith("Player")) {
				send.chatClient(clientId, "Player... names are not allowed, try again...");
			} else {
				while(name.indexOf(' ') != -1) {
					name = name.replace(' ', '_');
				}
				send.rcon("client_name " + clientId + " " + name);
			}
		} else {
			send.chatClient(clientId, "Try : !rename NewName");
		}
	}

	public ExternalUser getExternalUser(int clientId) {
		return externalUserByClientId.get(clientId);
	}
	
	public ExternalUser getOwnerOf(int companyId) {
		return externalUserByCompanyId.get(companyId);
	}
	
	public void setOwnerOf(int companyId, ExternalUser owner) {
		externalUserByCompanyId.put(companyId, owner);
	}

	public void removeOwnerOf(int companyId) {
		externalUserByCompanyId.remove(companyId);
	}

	private void showLoginFailed(int clientId) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, "Login failed.");
	}
	
	private void showLoginSucceed(int clientId) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, "Login succeed.");
	}
	
	public void showHowtoLogin(int clientId) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, "How to login ***");
		send.chatClient(clientId, "1. Goto www.strategyboard.net and register there,");
		send.chatClient(clientId, "2. Login there and click 'In game login',");
		send.chatClient(clientId, "3. Copy and paste the whole !login command line into the chat window of the game.");
	}
		
	private void showClientAlreadyLogged(int clientId) {
		Send send = openttdAdmin.getSend();
		send.chatClient(clientId, "You are already logged.");
	}
	
}
