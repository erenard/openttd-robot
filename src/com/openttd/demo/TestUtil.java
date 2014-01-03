/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.openttd.demo;

import com.openttd.robot.ExternalServices;
import com.openttd.robot.model.ExternalUser;
import com.openttd.robot.model.GamePlayer;
import java.util.Collection;

/**
 *
 * @author erenard
 */
public class TestUtil {

	public static void fakeExternalUserService() {
		//Fake user identification
		ExternalServices.getInstance().setExternalUserService(new ExternalServices.ExternalUserService() {
			@Override
			public ExternalUser identifyUser(String token) {
				ExternalUser externalUser = new ExternalUser();
				externalUser.setName(token);
				if (token.startsWith("admin")) {
					externalUser.setAdmin(true);
				}
				return externalUser;
			}
		});
		//Fake game service
		ExternalServices.getInstance().setExternalGameService(new ExternalServices.ExternalGameService() {
			@Override
			public void saveGame(Collection<GamePlayer> gamePlayers) {
				for (GamePlayer gamePlayer : gamePlayers) {
					System.out.println(gamePlayer.toString());
				}
			}

			@Override
			public void endGame() {
				System.exit(0);
			}
		});
	}

}
