package de.incentergy.test;

import java.util.Calendar;

public class TestBeanWithCustomClassConstructor {
	private Calendar calendar = Calendar.getInstance();

	public Calendar getCalendar() {
		return calendar;
	}

	public void setCalendar(Calendar calendar) {
		this.calendar = calendar;
	}

}
