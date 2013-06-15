package com.openttd.robot;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.event.DateEventListener;
import com.openttd.constant.OTTD;
import com.openttd.gamescript.GSNewsPaper;
import com.openttd.network.core.Configuration;
import com.openttd.robot.rule.AbstractRule;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

public class HelloWorldGameScriptTest {
	//Openttd admin port library
	private final OpenttdAdmin openttdAdmin;
	//Hello world dummy rule
	private final HelloWorld helloworld;

	public HelloWorldGameScriptTest(Configuration configuration) {
		openttdAdmin = new OpenttdAdmin(configuration);
		helloworld = new HelloWorld(openttdAdmin);
	}
	
	public void startup() {
		openttdAdmin.startup();
	}

	public void shutdown() {
		openttdAdmin.shutdown();
	}

	public static void main(String[] args) {
		Configuration configuration = new Configuration();
		CLITestUtil.parseArguments(args, configuration);
		//Create the robot
		HelloWorldGameScriptTest robot = new HelloWorldGameScriptTest(configuration);
		//Start the robot and connect it to OpenTTD
		robot.startup();
		//Wait 1 minute
		try {
			Thread.sleep(60000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//Disconnect the robot and shut it down
		robot.shutdown();
	}
	
	/**
	 * This rule show a message on the game chat every monday.
	 * 2 things are important here:
	 * - extending AbstractRule
	 * - implementing DateEventListener
	 */
	public static class HelloWorld extends AbstractRule implements DateEventListener {

		public HelloWorld(OpenttdAdmin openttdAdmin) {
			//AbstractRule constructor call
			super(openttdAdmin);
		}

		/**
		 * Registrer here, all the event types this rule is listenning.
		 * @return the event type this rule is interested in.
		 */
		@Override
		public Collection<Class> listEventTypes() {
			Collection<Class> listEventTypes = new ArrayList<Class>();
			// Register DateEvent here
			listEventTypes.add(DateEvent.class);
			return listEventTypes;
		}

		/**
		 * Implements here the behavior in case of a DateEvent.
		 * @param dateEvent 
		 */
		@Override
		public void onDateEvent(DateEvent dateEvent) {
			Calendar date = dateEvent.getOpenttd().getDate();
			if(date.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
				GSNewsPaper newsPaper = new GSNewsPaper(GSNewsPaper.NewsType.NT_GENERAL, "Admin\nHello world !!!");
				super.getSend().gameScript(newsPaper.toString());
			}
		}
	}
}
