package com.openttd.robot;

import com.openttd.admin.OpenttdAdmin;
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

public class SimpleRobotClient extends OpenttdAdmin {
	
	public SimpleRobotClient(Configuration configuration) {
		super(configuration);
	}

	public static void main(String[] args) {
		SimpleRobotClient simpleClient = new SimpleRobotClient(new Configuration());
		ExternalServices.getInstance().setExternalUserService(new ExternalUserService() {
			@Override
			public ExternalUser identifyUser(String token) {
				ExternalUser externalUser = new ExternalUser();
				externalUser.setName(token);
				return externalUser;
			}}
		);
		simpleClient.startup();
		ExternalUsers externalUsers = new ExternalUsers(simpleClient);
		simpleClient.addListener(externalUsers);
		CompanyLifeCycle companyLifeCycle = new CompanyLifeCycle(simpleClient, externalUsers);
		simpleClient.addListener(companyLifeCycle);
		CompanyPasswordRemainder companyPasswordRemainder = new CompanyPasswordRemainder(simpleClient, externalUsers);
		simpleClient.addListener(companyPasswordRemainder);
		ServerAnnouncer serverAnnouncer = new ServerAnnouncer(simpleClient, externalUsers);
		simpleClient.addListener(serverAnnouncer);
		CompanyEconomyObjective companyEconomyObjective = new CompanyEconomyObjective(simpleClient, externalUsers, ObjectiveType.PERFORMANCE, 100);
		simpleClient.addListener(companyEconomyObjective);
		Administration administration = new Administration(simpleClient, externalUsers);
		simpleClient.addListener(administration);
		try {
			Thread.sleep(90000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//simpleClient.shutdown();
	}
	
}
