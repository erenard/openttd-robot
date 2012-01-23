package com.openttd.robot.rule;

import java.util.Calendar;

import com.openttd.admin.OpenttdAdmin;
import com.openttd.admin.event.DateEvent;
import com.openttd.admin.event.DateEventListener;
import com.openttd.admin.model.Company;
import com.openttd.admin.model.Game;
import com.openttd.network.admin.NetworkClient.Send;

/**
 * Warn non-passworded companies, once a month.
 */
public class CompanyPasswordRemainder implements DateEventListener {
	
	private final OpenttdAdmin client;
	public CompanyPasswordRemainder(OpenttdAdmin client, ExternalUsers externalUsers) {
		this.client = client;
	}

	private int currentMonth;
	
	private boolean isNewMonth(Calendar now) {
		int nowMonth = now.get(Calendar.MONTH) + 12 * now.get(Calendar.YEAR);
		if(nowMonth > currentMonth) {
			currentMonth = nowMonth;
			return true;
		}
		return false;
	}
	
	@Override
	public void onDateEvent(DateEvent dateEvent) {
		Game openttd = dateEvent.getOpenttd();
		Calendar now = openttd.getDate();
		if(isNewMonth(now)) {
			//Send message to non-passworded companies
			for(Company company : openttd.getCompanies()) {
				if(!company.isUsePassword()) {
					warningNoPassword(company.getId());
				}
			}
		}
	}

	private void warningNoPassword(int companyId) {
		Send send = client.getSend();
		send.chatCompany((short) companyId, "Warning: your company has NO PASSWORD.");
	}
}
