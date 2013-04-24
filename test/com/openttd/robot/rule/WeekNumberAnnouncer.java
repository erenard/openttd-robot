package com.openttd.robot.rule;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.event.DateEventListener;
import com.openttd.admin.event.GameScriptEvent;
import com.openttd.admin.event.GameScriptEventListener;
import com.openttd.constant.OTTD;
import com.openttd.network.constant.GameScript;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * Show the week number in a newspaper every week
 */
public class WeekNumberAnnouncer extends AbstractRule implements GameScriptEventListener, DateEventListener {
	
	public WeekNumberAnnouncer(OpenttdAdmin openttdAdmin) {
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
			super.getSend().newsBroadcast(OTTD.NewsType.NT_GENERAL, "This is week number " + now.get(Calendar.WEEK_OF_YEAR));
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
