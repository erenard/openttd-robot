package com.openttd.robot.rule;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.event.DateEventListener;
import com.openttd.admin.event.GameScriptEvent;
import com.openttd.admin.event.GameScriptEventListener;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * Test the gamescript
 */
public class GameScriptTest extends AbstractRule implements GameScriptEventListener, DateEventListener {
	
	public GameScriptTest(OpenttdAdmin openttdAdmin) {
		super(openttdAdmin);
	}
	
	@Override
	public Collection<Class> listEventTypes() {
		Collection<Class> listEventTypes = new ArrayList<Class>(2);
		listEventTypes.add(GameScriptEvent.class);
		listEventTypes.add(DateEvent.class);
		return listEventTypes;
	}

	@Override
	public void onGameScriptEvent(GameScriptEvent gameScriptEvent) {
		System.out.println(gameScriptEvent);
	}

	@Override
	public void onDateEvent(DateEvent dateEvent) {
		Calendar now = dateEvent.getOpenttd().getDate();
		if(isNewWeek(now)) {
			super.getSend().gameScript("{\"script\": \"GSAdmin.Send({a:0});\"}");
		}
	}

	private int currentWeek;
	private boolean isNewWeek(Calendar now) {
		int nowWeek = now.get(Calendar.WEEK_OF_YEAR);
		if(nowWeek != currentWeek) {
			currentWeek = nowWeek;
			return true;
		}
		return false;
	}
}
