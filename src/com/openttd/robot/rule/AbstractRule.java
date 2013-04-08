package com.openttd.robot.rule;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.EventListener;
import com.openttd.network.admin.NetworkClient;
import java.util.Collection;

public abstract class AbstractRule implements EventListener {
	private final OpenttdAdmin openttdAdmin;
	private final Collection<Class> eventClasses;
	
	public AbstractRule(OpenttdAdmin openttdAdmin) {
		this.openttdAdmin = openttdAdmin;
		this.eventClasses = this.listEventTypes();
		this.register();
	}
	
	protected NetworkClient.Send getSend() {
		return openttdAdmin.getSend();
	}
	
	abstract protected Collection<Class> listEventTypes();

	public void register() {
		for(Class eventClass : eventClasses) {
			this.openttdAdmin.addListener(eventClass, this);
		}
	}
	
	public void unregister() {
		for(Class eventClass : eventClasses) {
			this.openttdAdmin.removeListener(eventClass, this);
		}
	}
}