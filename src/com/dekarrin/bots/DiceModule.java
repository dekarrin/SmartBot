package com.dekarrin.bots;


public class DiceModule extends BotModule {

	public DiceModule() {
		super("DICE");
		addCommand("ROLL", new BotAction() {

			@Override
			public void execute(String[] params, String sender, String recipient) {
				java.util.Random rng = new java.util.Random();
				int sides = 6;
				int count = 1;
				if (params.length > 0 && !params[0].equals("")) {
					String[] parts = params[0].split("d", 2);
					if (parts.length != 2) {
						bot.sendMessage(recipient, sender + ": assuming 1d6");
					} else {
						try {
							count = Integer.parseInt(parts[0]);
							sides = Integer.parseInt(parts[1]);
						} catch (NumberFormatException e) {
							sides = 6;
							count = 1;
							bot.sendMessage(recipient, sender + ": assuming 1d6");
						}
					}
					if (count <= 0 || sides <= 0 || count > 99) {
						sides = 6;
						count = 1;
						bot.sendMessage(recipient, sender + ": assuming 1d6");
					}
				} else {
					bot.sendMessage(recipient, sender + ": assuming 1d6");
				}
				int total = 0;
				String msg = sender + ": rolled ";
				String seq = "";
				for (int i = 0; i < count; i++) {
					int v = rng.nextInt(sides) + 1;
					total += v;
					seq += v;
					if (i + 1 < count) {
						seq += ", ";
					}
				}
				msg += total + " (" + seq + ")";
				bot.sendMessage(recipient, msg);
			}

			@Override
			public String help() {
				return "Rolls dice (default 1d6)";
			}
			
			public String syntax() {
				return "%s <[count]d[sides]>";
			}
			
		});
	}
	
	
}
