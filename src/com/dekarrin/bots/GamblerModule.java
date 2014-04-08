package com.dekarrin.bots;

import java.util.HashMap;
import java.util.Map;

public class GamblerModule extends Module {
	
	public static final long MAXIMUM_WINNINGS = 100000000L;
	
	public static final long STARTING_MONEY = 100L;
	
	public static final int TOP_COUNT = 3;
	
	private final Map<String, Long> money = new HashMap<String, Long>();
	
	public GamblerModule() {
		super("GAMBLER", "v0.1", "Let's you gamble money!");
		addCommand("GAMBLE", new BotAction() {
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				String nick = bot.getRegisteredNick(sender);
				if (nick != null) {
					checkGambler(recipient, nick);
					long current = getMoney(nick);
					if (current > 0) {
						long amount = calcGamble(current);
						if (amount > 0) {
							bot.sendMessage(recipient, sender + ": You won $"
									+ amount + "!");
						} else if (amount < 0) {
							bot.sendMessage(recipient, sender + ": You lost $"
									+ Math.abs(amount) + "!");
						} else {
							bot.sendMessage(recipient, sender
									+ ": You broke even.");
						}
						long total = current + amount;
						if (total < 0) {
							total = 0;
						}
						setMoney(nick, total);
						bot.sendMessage(recipient, sender + ": You now have $"
								+ total);
					} else {
						bot.sendMessage(
								recipient,
								sender
										+ ": You can't gamble; you're broke! Beg for money from someone!");
					}
				} else {
					bot.sendMessage(
							recipient,
							sender
									+ ": You must be logged in with NickServ to play the gambling game!");
				}
			}
			
			@Override
			public String help() {
				return "gambles (fake) money";
			}
			
			@Override
			public String syntax() {
				return "%s";
			}
		});
		addCommand("TOTAL", new BotAction() {
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				String nickToCheck = null;
				if (params.length > 0) {
					nickToCheck = params[0];
				} else {
					String logged = bot.getRegisteredNick(sender);
					if (logged == null) {
						nickToCheck = sender;
					} else {
						nickToCheck = logged;
					}
				}
				if (money.containsKey(nickToCheck.toUpperCase())) {
					long amount = getMoney(nickToCheck);
					bot.sendMessage(recipient, sender + ": " + nickToCheck
							+ " has $" + amount);
				} else {
					bot.sendMessage(recipient, sender + ": " + nickToCheck
							+ " doesn't gamble");
				}
			}
			
			@Override
			public String help() {
				return "shows amount of money that a user has has.";
			}
			
			@Override
			public String syntax() {
				return "%s <nick>";
			}
		});
		addCommand("TOP", new BotAction() {
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				long[] tops = new long[GamblerModule.TOP_COUNT];
				String[] topNames = new String[GamblerModule.TOP_COUNT];
				for (int i = 0; i < GamblerModule.TOP_COUNT; i++) {
					tops[i] = 0L;
					topNames[i] = null;
				}
				int notNulls = Math.min(GamblerModule.TOP_COUNT, money.size());
				for (String name : money.keySet()) {
					long value = getMoney(name);
					for (int i = 0; i < GamblerModule.TOP_COUNT; i++) {
						if (value >= tops[i]) {
							for (int j = GamblerModule.TOP_COUNT - 1; j > i; j--) {
								tops[j] = tops[j - 1];
								topNames[j] = topNames[j - 1];
							}
							tops[i] = value;
							topNames[i] = name;
							break;
						}
					}
				}
				String p = (notNulls == 1) ? " is" : "s are";
				bot.sendMessage(recipient, sender + ": top "
						+ GamblerModule.TOP_COUNT + " score" + p + "...");
				for (int i = 0; i < notNulls; i++) {
					bot.sendMessage(recipient, sender + ": #" + (i + 1) + " - "
							+ tops[i] + " - " + topNames[i]);
				}
			}
			
			@Override
			public String help() {
				return "Shows the top " + GamblerModule.TOP_COUNT
						+ " records for gambling money";
			}
			
			@Override
			public String syntax() {
				return "%s";
			}
		});
		addCommand("SETTOTAL", new BotAction() {
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (bot.isAuthorized(sender)) {
					if (params.length < 2) {
						bot.sendBadSyntax(recipient, sender);
					} else {
						try {
							long amt = Long.parseLong(params[1]);
							checkGambler(recipient, params[0]);
							setMoney(params[0], amt);
							bot.sendMessage(recipient, sender + ": "
									+ params[0] + "'s money was set to $" + amt);
						} catch (NumberFormatException e) {
							bot.sendMessage(recipient, sender + ": Bad amount!");
						}
					}
				} else {
					bot.sendNotAuthorized(recipient, sender);
				}
			}
			
			@Override
			public String help() {
				return "Sets the total money for a user";
			}
			
			@Override
			public String syntax() {
				return "%s [nick] [amount]";
			}
		});
		addCommand("GIFT", new BotAction() {
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				String donner = bot.getRegisteredNick(sender);
				if (donner == null) {
					bot.sendMessage(
							recipient,
							sender
									+ ": You must be logged in with NickServ to donate gambling money!");
				} else {
					if (params.length < 2) {
						bot.sendBadSyntax(recipient, sender);
					} else {
						try {
							long donation = Long.parseLong(params[1]);
							if (getMoney(donner) >= donation) {
								checkGambler(recipient, params[0]);
								setMoney(donner, getMoney(donner) - donation);
								setMoney(params[0], getMoney(params[0])
										+ donation);
								bot.sendMessage(recipient, sender + " gifted $"
										+ donation + " to " + params[0]);
							} else {
								bot.sendMessage(recipient, sender
										+ ": you don't have enough money!");
							}
						} catch (NumberFormatException e) {
							bot.sendMessage(recipient, sender + ": bad amount!");
						}
					}
				}
			}
			
			@Override
			public String help() {
				return "gives money to another player";
			}
			
			@Override
			public String syntax() {
				return "%s [user] [amount]";
			}
		});
		
	}
	
	private long calcGamble(long current) {
		double out = 2 * Math.random() - 1;
		long winnings = Math.round(Math.min(current, GamblerModule.MAXIMUM_WINNINGS) * out);
		return winnings;
	}
	
	private void checkGambler(String recipient, String nick) {
		if (!money.containsKey(nick.toUpperCase())) {
			setMoney(nick, GamblerModule.STARTING_MONEY);
			bot.sendMessage(recipient, "Welcome to the gambling ring, " + nick
					+ "! You start with $" + GamblerModule.STARTING_MONEY);
		}
	}
	
	@Override
	protected void onModuleAdded() {
		Map<String, String> settingsMap = settings.getAllModuleSettings(getName());
		if (settingsMap != null) {
			for (String s : settingsMap.keySet()) {
				if (s.startsWith("MONEY-")) {
					String user = s.substring(6);
					try {
						long amt = Long.parseLong(settingsMap.get(s));
						money.put(user.toUpperCase(), amt);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	private long getMoney(String user) {
		return money.get(user.toUpperCase());
	}
	
	private void setMoney(String user, long money) {
		this.money.put(user.toUpperCase(), money);
		settings.setModuleSetting(getName(), "MONEY-" + user.toUpperCase(),
				money + "");
	}
}
