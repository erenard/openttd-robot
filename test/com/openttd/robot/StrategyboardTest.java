/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.openttd.robot;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.ChatEvent;
import com.openttd.admin.event.ChatEventListener;
import com.openttd.network.core.Configuration;
import com.openttd.robot.rule.AbstractRule;
import com.openttd.robot.rule.Administration;
import com.openttd.robot.rule.CompanyLifeCycle;
import com.openttd.robot.rule.CompanyPasswordRemainder;
import com.openttd.robot.rule.ExternalUsers;
import com.openttd.robot.rule.ServerAnnouncer;
import com.openttd.robot.rule.TimerObjective;
import java.util.ArrayList;
import java.util.Collection;

public class StrategyboardTest {
	private final OpenttdAdmin robot;
	private final ExternalUsers externalUsers;
	private final CompanyLifeCycle companyLifeCycle;
	private final CompanyPasswordRemainder companyPasswordRemainder;
	private final ServerAnnouncer serverAnnouncer;
	private final TimerObjective timerObjective;
	private final Administration administration;
	private final PleaseKillMe pleaseKillMe;

	public StrategyboardTest(Configuration configuration) {
		robot = new OpenttdAdmin(configuration);
		externalUsers = new ExternalUsers(robot);
		companyLifeCycle = new CompanyLifeCycle(robot, externalUsers);
		companyLifeCycle.checkLogin = false;
		companyPasswordRemainder = new CompanyPasswordRemainder(robot);
		serverAnnouncer = new ServerAnnouncer(robot, externalUsers);
		timerObjective = new TimerObjective(robot, externalUsers, 10);
		administration = new Administration(robot, externalUsers);
		pleaseKillMe = new PleaseKillMe(robot);
		robot.startup();
	}

	private class PleaseKillMe extends AbstractRule implements ChatEventListener {
		public PleaseKillMe(OpenttdAdmin robot) {
			super(robot);
		}

		@Override
		protected Collection<Class> listEventTypes() {
			Collection<Class> eventTypes = new ArrayList<Class>();
			eventTypes.add(ChatEvent.class);
			return eventTypes;
		}

		@Override
		public void onChatEvent(ChatEvent chatEvent) {
			if(chatEvent.getMessage().equals("!killme")) {
				robot.shutdown();
			}
		}
	}
	
	public static void main(String [] args) {
		CLITestUtil.fakeExternalUserService();
		Configuration configuration = new Configuration();
		CLITestUtil.parseArguments(args, configuration);
		StrategyboardTest test = new StrategyboardTest(configuration);
	}
}
