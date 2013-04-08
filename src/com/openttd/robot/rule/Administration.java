package com.openttd.robot.rule;

import java.util.HashMap;
import java.util.Map;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.ChatEvent;
import com.openttd.admin.event.ChatEventListener;
import com.openttd.admin.model.Client;
import com.openttd.admin.model.Game;
import com.openttd.network.admin.NetworkClient.Send;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Take care of the admins commands
 * Rule #1: Handle $clients, $cls
 * Rule #2: Handle $kick, $k
 * Rule #3: Handle $ban, $b //TODO Test
 * Rule #4: Handle $warn, $w
 */
public class Administration extends AbstractRule implements ChatEventListener {
	
	private final ExternalUsers externalUsers;
	
	public Administration(OpenttdAdmin openttdAdmin, ExternalUsers externalUsers) {
		super(openttdAdmin);
		this.externalUsers = externalUsers;
	}
	

	@Override
	public void onChatEvent(ChatEvent chatEvent) {
		Integer clientId = chatEvent.getClientId();
		String message = chatEvent.getMessage();
		if(clientId != null && message != null) {
			message = message.trim();
			if(externalUsers.getExternalUser(clientId) != null
			&& externalUsers.getExternalUser(clientId).isAdmin()) {
				if(message.startsWith("$clients") || message.startsWith("$cls")) {
					//Rule #1
					showClients(chatEvent.getOpenttd(), clientId);
				} else if(message.startsWith("$kick") || message.startsWith("$k")) {
					//Rule #2
					try {
						String argument = message.split(" ")[1].trim();
						Integer kickedClientId = new Integer(argument);
						kick(kickedClientId);
					} catch(Exception e) {
						showMessage(clientId, "Usage: $kick clientId (try $clients to find clientIds)");
					}
				} else if(message.startsWith("$ban") || message.startsWith("$b")) {
					//Rule #3
					try {
						String argument = message.split(" ")[1].trim();
						Integer bannedClientId = new Integer(argument);
						ban(bannedClientId);
					} catch(Exception e) {
						showMessage(clientId, "Usage: $kick clientId (try $clients to find clientIds)");
					}
				} else if(message.startsWith("$warn") || message.startsWith("$w")) {
					//Rule #4
					try {
						String [] args = message.split(" ");
						String argument = args[1].trim();
						int _clientId = Integer.parseInt(argument);
						String _message = "";
						if(args.length > 1) _message = message.substring(message.indexOf(args[2].trim()));
						warn(_clientId, _message);
					} catch(Exception e) {
						showMessage(clientId, "Usage: $warn clientId [message] (try $clients to find clientIds)");
					}
				}
			}
		}
	}

	@Override
	public Collection<Class> listEventTypes() {
		Collection<Class> listEventTypes = new ArrayList<Class>(1);
		listEventTypes.add(ChatEvent.class);
		return listEventTypes;
	}


	private void showClients(Game openttd, int clientId) {
		Send send = super.getSend();
		send.chatClient(clientId, "#ClientId, Name, IpAddress, CompanyId");
		for(Client client : openttd.getClients()) {
			send.chatClient(clientId, "#" + client.getId() + ", " + client.getName() + ", " + client.getIp() + ", " + client.getCompanyId());
		}
	}

	private void showMessage(int clientId, String message) {
		Send send = super.getSend();
		send.chatClient(clientId, message);
	}
	
	private void kick(int clientId) {
		Send send = super.getSend();
		send.rcon("kick " + clientId);
	}

	private void ban(int clientId) {
		Send send = super.getSend();
		send.rcon("ban " + clientId);
	}

	private Map<Integer, Integer> warningCountByClientId = new HashMap<Integer, Integer>();
	
	private void warn(int clientId, String warning) {
		Send send = super.getSend();
		Integer warningCount = warningCountByClientId.get(clientId);
		if(warningCount == null) {
			warningCount = 0;
		}
		if(warningCount == 0) {
			send.chatClient(clientId, "Be warned! No rule breaking!" + (warning != null ? " (" + warning + ")" : ""));
		} else if(warningCount == 1) {
			send.chatClient(clientId, "Last warning! No rule breaking!" + (warning != null ? " (" + warning + ")" : ""));
		} else if(warningCount == 2) {
			kick(clientId);
		} else if(warningCount > 2) {
			ban(clientId);
		}
		warningCountByClientId.put(clientId, warningCount + 1);
	}
}
