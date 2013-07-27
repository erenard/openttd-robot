package com.openttd.robot;

import java.util.Collection;

import com.openttd.network.core.Configuration;
import com.openttd.robot.ExternalServices.ExternalGameService;
import com.openttd.robot.ExternalServices.ExternalUserService;
import com.openttd.robot.model.ExternalUser;
import com.openttd.robot.model.GamePlayer;

/**
 * Dummy class, makes samples easiers to read.
 */
public class CLITestUtil {
	public static void fakeExternalUserService() {
		//Fake user identification
		ExternalServices.getInstance().setExternalUserService(new ExternalUserService() {
			@Override
			public ExternalUser identifyUser(String token) {
				ExternalUser externalUser = new ExternalUser();
				externalUser.setName(token);
				if(token.startsWith("admin")) {
					externalUser.setAdmin(true);
				}
				return externalUser;
			}
		});
		//Fake game service
		ExternalServices.getInstance().setExternalGameService(new ExternalGameService() {
			@Override
			public void saveGame(Collection<GamePlayer> gamePlayers) {
				for(GamePlayer gamePlayer : gamePlayers) {
					System.out.println(gamePlayer.toString());
				}
			}
			@Override
			public void endGame() {
				System.exit(0);
			}
		});
	}

	public static void parseArguments(String [] args, Configuration configuration) {
		try {
			String url = args[0];
			configuration.host = url.split(":")[0];
			configuration.adminPort = new Integer(url.split(":")[1]);
			configuration.password = args[1];
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Usage: java -jar openttd-robot.jar localhost:3977 admin_password");
			System.err.println("See openttd.cfg to set your server's admin_password first !");
			System.exit(0);
		}
	}
}
