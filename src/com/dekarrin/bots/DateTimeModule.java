package com.dekarrin.bots;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class DateTimeModule extends Module {
	
	public DateTimeModule() {
		super("DATETIME", "v1.0", "Retrieves the date and time");
		addCommand("DATE", new BotAction() {
			
			@Override
			public String syntax() {
				return "%s";
			}
			
			@Override
			public String help() {
				return "Gets the current date.";
			}
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				bot.sendMessage(recipient, sender + ": it is " + getTimeString("EEEE, MMMM dd, YYYY"));
			}
		});
		addCommand("TIME", new BotAction() {
			
			@Override
			public String syntax() {
				return "%s";
			}
			
			@Override
			public String help() {
				return "Gets the current time.";
			}
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				bot.sendMessage(recipient, sender + ": it is " + getTimeString("HH:mm:ss zzz"));
			}
		});
	}
	
	private String getTimeString(String format) {
		Calendar c = Calendar.getInstance();
		c.getTime();
		SimpleDateFormat fmt = new SimpleDateFormat(format);
		return fmt.format(c.getTime());
	}
	
}
