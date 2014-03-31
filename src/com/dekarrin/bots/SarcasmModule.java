package com.dekarrin.bots;


public class SarcasmModule extends BotModule {
	
	private double minReq = 0.05;
	
	public SarcasmModule() {
		super("SARCASM");
		addCommand("SETODDS", new BotAction () {

			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (params.length > 0) {
					try {
						double odds = Double.parseDouble(params[0]);
						if (odds < 0 || odds > 1.0) {
							bot.sendMessage(recipient, sender + ": odds must be between 0 and 1");
						} else {
							minReq = odds;
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
	}
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		if (Math.random() < minReq) {
			bot.sendMessage(bot.getIntendedChannel(), "Yeah, right, " + sender + ". You WOULD say that.");
		}
	}
	
}
