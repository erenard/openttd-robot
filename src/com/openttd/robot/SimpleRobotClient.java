package com.openttd.robot;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.network.admin.NetworkClient;
import com.openttd.network.core.Configuration;
import com.openttd.robot.ExternalServices.ExternalUserService;
import com.openttd.robot.model.ExternalUser;
import com.openttd.robot.rule.Administration;
import com.openttd.robot.rule.CompanyEconomyObjective;
import com.openttd.robot.rule.CompanyEconomyObjective.ObjectiveType;
import com.openttd.robot.rule.CompanyLifeCycle;
import com.openttd.robot.rule.CompanyPasswordRemainder;
import com.openttd.robot.rule.ExternalUsers;
import com.openttd.robot.rule.ServerAnnouncer;

public class SimpleRobotClient {
	
	private final OpenttdAdmin openttdAdmin;

	public SimpleRobotClient(Configuration configuration) {
		openttdAdmin = new OpenttdAdmin(configuration);
		ExternalUsers externalUsers = new ExternalUsers(openttdAdmin);
		CompanyLifeCycle companyLifeCycle = new CompanyLifeCycle(openttdAdmin, externalUsers);
		CompanyPasswordRemainder companyPasswordRemainder = new CompanyPasswordRemainder(openttdAdmin);
		ServerAnnouncer serverAnnouncer = new ServerAnnouncer(openttdAdmin, externalUsers);
		CompanyEconomyObjective companyEconomyObjective = new CompanyEconomyObjective(openttdAdmin, externalUsers, ObjectiveType.PERFORMANCE, 100);
		Administration administration = new Administration(openttdAdmin, externalUsers);
	}
	
	public void startup() {
		openttdAdmin.startup();
	}

	public void shutdown() {
		openttdAdmin.shutdown();
	}

	public static void main(String[] args) {
		ExternalServices.getInstance().setExternalUserService(new ExternalUserService() {
			@Override
			public ExternalUser identifyUser(String token) {
				ExternalUser externalUser = new ExternalUser();
				externalUser.setName(token);
				return externalUser;
			}}
		);
		SimpleRobotClient robot = new SimpleRobotClient(new Configuration());
		robot.startup();
		try {
			Thread.sleep(90000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		robot.shutdown();
	}
	
}
