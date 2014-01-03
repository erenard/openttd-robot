package com.openttd.robot.rule;

import java.util.Collection;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.EventListener;
import com.openttd.network.admin.NetworkAdminSender;

public abstract class AbstractRule implements EventListener {
	private final OpenttdAdmin openttdAdmin;
	private final Collection<Class> eventClasses;
	
	public AbstractRule(OpenttdAdmin openttdAdmin) {
		this.openttdAdmin = openttdAdmin;
		this.eventClasses = this.listEventTypes();
		this.register();
	}
	
	protected NetworkAdminSender getSend() {
		return openttdAdmin.getSend();
	}
	
	abstract protected Collection<Class> listEventTypes();

	public final void register() {
		for(Class eventClass : eventClasses) {
			this.openttdAdmin.addListener(eventClass, this);
		}
	}
	
	public final void unregister() {
		for(Class eventClass : eventClasses) {
			this.openttdAdmin.removeListener(eventClass, this);
		}
	}
}
