/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.openttd.robot;

import com.openttd.demo.CLIUtil;
import java.util.ArrayList;
import java.util.Collection;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.ChatEvent;
import com.openttd.admin.event.ChatEventListener;
import com.openttd.demo.TestUtil;
import com.openttd.network.core.Configuration;
import com.openttd.robot.rule.AbstractRule;
import com.openttd.robot.rule.Administration;
import com.openttd.robot.rule.CompanyLifeCycle;
import com.openttd.robot.rule.CompanyPasswordRemainder;
import com.openttd.robot.rule.ExternalUsers;
import com.openttd.robot.rule.ServerAnnouncer;
import com.openttd.robot.rule.TimerObjective;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

public class StrategyboardTest {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(StrategyboardTest.class);

	private final OpenttdAdmin robot;
	private final ExternalUsers externalUsers;
	private final CompanyLifeCycle companyLifeCycle;
	private final CompanyPasswordRemainder companyPasswordRemainder;
	private final ServerAnnouncer serverAnnouncer;
	private final TimerObjective timerObjective;
	private final Administration administration;
	private final PleaseKillMe pleaseKillMe;

	public StrategyboardTest() throws IOException {
		Configuration configuration = new Configuration();
		configuration.password = CLIUtil.readTestProperties().getProperty("password");
		TestUtil.fakeExternalUserService();
		robot = new OpenttdAdmin(configuration);
		externalUsers = new ExternalUsers(robot);
		companyLifeCycle = new CompanyLifeCycle(robot, externalUsers);
		companyLifeCycle.checkLogin = false;
		companyPasswordRemainder = new CompanyPasswordRemainder(robot);
		serverAnnouncer = new ServerAnnouncer(robot);
		timerObjective = new TimerObjective(robot, externalUsers, 10);
		administration = new Administration(robot, externalUsers);
		pleaseKillMe = new PleaseKillMe(robot);
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
	
	@Test
	public void testStrategyboard() {
		CLIUtil.wait(60);
	}
	
	@Before
	public void setUp() {
		robot.startup();
		CLIUtil.wait(1);
	}
	
	@After
	public void tearDown() {
		robot.shutdown();
		CLIUtil.wait(1);
	}
}
