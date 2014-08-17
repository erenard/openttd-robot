package com.openttd.robot;

import com.openttd.demo.CLIUtil;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.event.DateEventListener;
import com.openttd.gamescript.GSNewsPaper;
import com.openttd.network.core.Configuration;
import com.openttd.robot.rule.AbstractRule;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HelloWorldGameScriptTest {
	//Openttd admin port library
	private final OpenttdAdmin openttdAdmin;
	//Hello world dummy rule
	private final HelloWorld helloworld;

	public HelloWorldGameScriptTest() throws IOException {
		Configuration configuration = new Configuration();
		configuration.password = CLIUtil.readTestProperties().getProperty("password");
		openttdAdmin = new OpenttdAdmin(configuration);
		helloworld = new HelloWorld(openttdAdmin);
	}
	
	@Before
	public void startup() {
		openttdAdmin.startup();
		CLIUtil.wait(1);
	}

	@After
	public void shutdown() {
		openttdAdmin.shutdown();
		CLIUtil.wait(1);
	}

	@Test
	public void main() {
		//Wait 1 minute
		CLIUtil.wait(10);
	}
	
	/**
	 * This rule show a message on the game chat every monday.
	 * 2 things are important here:
	 * - extending AbstractRule
	 * - implementing DateEventListener
	 */
	public class HelloWorld extends AbstractRule implements DateEventListener {

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
				GSNewsPaper newsPaper = new GSNewsPaper(GSNewsPaper.NewsType.NT_GENERAL, "Admin\r\nHello world !!!");
				openttdAdmin.getGSExecutor().send(newsPaper);
			}
		}
	}
}
