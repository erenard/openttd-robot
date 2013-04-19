package com.openttd.robot;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.network.core.Configuration;
import com.openttd.robot.rule.GameScriptTest;

public class SimpleGameScriptRobotClient {
	
	private final OpenttdAdmin openttdAdmin;
	private final GameScriptTest gameScriptTest;

	public SimpleGameScriptRobotClient(Configuration configuration) {
		openttdAdmin = new OpenttdAdmin(configuration);
		gameScriptTest = new GameScriptTest(openttdAdmin);
	}
	
	public void startup() {
		openttdAdmin.startup();
	}

	public void shutdown() {
		openttdAdmin.shutdown();
	}

	public static void main(String[] args) {
		Configuration configuration = new Configuration();
		
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
		
		SimpleGameScriptRobotClient robot = new SimpleGameScriptRobotClient(configuration);
		robot.startup();
		try {
			Thread.sleep(90000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		robot.shutdown();
	}
	
}
