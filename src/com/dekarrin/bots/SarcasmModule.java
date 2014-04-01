package com.dekarrin.bots;

import java.util.ArrayList;
import java.util.List;


public class SarcasmModule extends Module {
	
	private double odds = 0.05;
	
	public SarcasmModule() {
		super("SARCASM", "v1.0", "Makes sarcastic remarks and replies.");
		addCommand("SETODDS", new BotAction () {

			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (params.length > 0) {
					try {
						double odds = Double.parseDouble(params[0]);
						if (odds < 0 || odds > 1.0) {
							bot.sendMessage(recipient, sender + ": odds must be between 0 and 1");
						} else {
							SarcasmModule.this.odds = odds;
							settings.setModuleSetting(getName(), "odds", ""+odds);
							bot.sendMessage(recipient, sender + ": changed sarcasm odds to " + odds);
						}
					} catch (NumberFormatException e) {
						bot.sendBadSyntax(recipient, sender);
					}
				} else {
					bot.sendBadSyntax(recipient, sender);
				}
			}

			@Override
			public String help() {
				return "Sets the odds of a sarcastic remark";
			}

			@Override
			public String syntax() {
				return "%s [odds]";
			}
			
		});
		addCommand("GETODDS", new BotAction () {

			@Override
			public void execute(String[] params, String sender, String recipient) {
				bot.sendMessage(recipient, sender + ": odds of a sarcastic remark are " + odds);
			}

			@Override
			public String help() {
				return "Gets the odds of a sarcastic remark";
			}

			@Override
			public String syntax() {
				return "%s";
			}
			
		});
	}
	
	@Override
	protected void onModuleAdded() {
		String val = settings.getModuleSetting(getName(), "odds");
		if (val != null) {
			try {
				odds = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				// do nothing
			}
		}
	}
	
	@Override
	public boolean onMessage(String channel, String sender, String login, String hostname, String message) {
		boolean addressingMe = message.toUpperCase().contains(bot.getNick().toUpperCase());
		boolean swearing = message.toUpperCase().contains("fuck you".toUpperCase());
		if (addressingMe && swearing) {
			String msg = "No, " + sender + ", fuck YOU";
			bot.sendMessage(bot.getIntendedChannel(), msg);
			return true;
		} else {
			List<String> remarks = new ArrayList<String>();
			remarks.add("Yeah right, %s, you WOULD say that.");
			remarks.add("As IF, %s");
			remarks.add("Sure, %s, that's DEFINATLY true. Mmhmm. idiot.");
			if (Math.random() < odds) {
				int msgIndex = (new java.util.Random()).nextInt(remarks.size());
				String msg = String.format(remarks.get(msgIndex), sender);
				bot.sendMessage(bot.getIntendedChannel(), msg);
				return true;
			} else {
				return false;
			}
		}
	}
	
}
