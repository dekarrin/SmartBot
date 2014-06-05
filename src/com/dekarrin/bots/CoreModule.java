package com.dekarrin.bots;


public class CoreModule extends Module {
	
	public CoreModule() {
		super(SmartBot.CORE_MODULE_NAME, "v0.6.1", "Built-in bot commands");
		addCommand("KILL", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				if (bot.isAuthorized(user)) {
					bot.disconnect("Terminated by user '" + user + "'");
				} else {
					bot.sendNotAuthorized(recipient, user);
				}
			}
			
			@Override
			public String help() {
				return "Terminates this bot";
			}
			
			@Override
			public String syntax() {
				return "%s";
			}
		});
		addCommand("MODULES", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				String msg = sender + ": Modules (* = enabled): *("
						+ SmartBot.CORE_MODULE_NAME + ")";
				for (final String name : ((SmartBotCoreInterface) bot).getModuleNames()) {
					if (!name.equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
						msg += ", ";
						if (((SmartBotCoreInterface) bot).getModuleEnabled(name)) {
							msg += "*";
						}
						msg += name.toUpperCase();
					}
				}
				bot.sendMessage(recipient, msg);
			}
			
			@Override
			public String help() {
				return "Lists the modules on this bot.";
			}
			
			@Override
			public String syntax() {
				return "%s";
			}
		});
		addCommand("ECHO", new BotAction() {
			
			@Override
			public String syntax() {
				return "%s [what to say]";
			}
			
			@Override
			public String help() {
				return "Has the bot output something";
			}
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				StringBuilder sb = new StringBuilder();
				for (String p : params) {
					sb.append(p + " ");
				}
				sb.replace(sb.length() - 1, sb.length() - 1, "");
				bot.sendMessage(bot.getChannel(), sb.toString());
			}
		});
		addCommand("HELP", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				String command = null;
				String moduleName = null;
				if (params.length > 1) {
					moduleName = params[0].toUpperCase();
					command = params[1].toUpperCase();
				} else if (params.length == 1) {
					// assume module first; this ambiguity can always be
					// resolved
					// by user putting in the name of the core module.
					if (((SmartBotCoreInterface) bot).hasModule(params[0])) {
						moduleName = params[0].toUpperCase();
					} else {
						command = params[0].toUpperCase();
					}
				}
				if ((command == null) && (moduleName == null)) {
					String msg1 = user + ": use LIST to see a list of core ";
					msg1 += "commands, ";
					msg1 += "or LIST [module] to list commands in a module, ";
					String msg2 = user + ": use HELP [command] for help with a";
					msg2 += " core command, HELP [module] for help with a ";
					msg2 += "module, or HELP [module] [command] for help with ";
					msg2 += "a command in a module.";
					String msg3 = user + ": anywhere a module is accepted, ";
					msg3 += SmartBot.CORE_MODULE_NAME
							+ " specifies the core module";
					bot.sendMessage(recipient, user + ": " + bot.getVersion());
					bot.sendMessage(recipient, msg1);
					bot.sendMessage(recipient, msg2);
					bot.sendMessage(recipient, msg3);
				} else if ((moduleName != null) && (command == null)) {
					// user did HELP [MODULE], and we assume module exists
					Module mod = null;
					if (moduleName.equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
						mod = CoreModule.this;
					} else {
						mod = ((SmartBotCoreInterface) bot).getModule(moduleName);
					}
					final String m1 = user + ": " + mod.getName() + " "
							+ mod.getVersion();
					final String m2 = user + ": " + mod.getHelp();
					final String m3 = user + ": " + "Type LIST '"
							+ mod.getName() + "' to see commands";
					bot.sendMessage(recipient, m1);
					bot.sendMessage(recipient, m2);
					bot.sendMessage(recipient, m3);
				} else {
					Module mod = null;
					if (moduleName == null) {
						mod = CoreModule.this;
					} else {
						mod = ((SmartBotCoreInterface) bot).getModule(moduleName);
					}
					if ((mod == null)) {
						((SmartBotCoreInterface) bot).sendNoSuchModule(recipient, user, moduleName);
					} else {
						if (mod.hasCommand(command)) {
							final String help = mod.getCommandHelp(command);
							final String syntax = mod.getCommandSyntax(command);
							bot.sendMessage(recipient, user + ": Syntax - "
									+ syntax);
							bot.sendMessage(recipient, user + ": " + help);
						} else {
							bot.sendMessage(recipient, user + ": command '"
									+ command + "' does not exist in module '"
									+ moduleName + "'");
						}
					}
				}
			}
			
			@Override
			public String help() {
				return "Lists out the help for a module";
			}
			
			@Override
			public String syntax() {
				return "%1$s <module>, %1$s <command>, %1$s <module> <command>";
			}
		});
		addCommand("OP", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				if (bot.isAuthorized(user)) {
					if (params.length > 0) {
						for (int i = 0; i < params.length; i++) {
							final String toOp = params[i];
							if (!((SmartBotCoreInterface) bot).hasOperator(toOp)) {
								((SmartBotCoreInterface) bot).addOperator(toOp);
								bot.sendMessage(recipient, user
										+ ": bot operator was granted to " + toOp);
							} else {
								bot.sendMessage(recipient, user + ": " + toOp
										+ " is already an operator.");
							}
						}
					} else {
						bot.sendBadSyntax(recipient, user);
					}
				} else {
					bot.sendNotAuthorized(recipient, user);
				}
			}
			
			@Override
			public String help() {
				return "Grants bot operator status to one or more users";
			}
			
			@Override
			public String syntax() {
				return "%s [nick] <nick...>";
			}
		});
		addCommand("DEOP", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				if (bot.isAuthorized(user)) {
					if (params.length > 0) {
						for (int i = 0; i < params.length; i++) {
							final String toDeop = params[i];
							if (((SmartBotCoreInterface) bot).hasOperator(toDeop)) {
								if (!((SmartBotCoreInterface) bot).getOwner().equals(toDeop.toUpperCase())) {
									((SmartBotCoreInterface) bot).removeOperator(toDeop);
									bot.sendMessage(recipient, user
											+ ": bot operator was revoked from "
											+ toDeop);
								} else {
									bot.sendMessage(recipient, user
											+ ": you cannot deop the bot owner!");
								}
							} else {
								bot.sendMessage(recipient, user + ": " + toDeop
										+ " is not an operator.");
							}
						}
					} else {
						bot.sendBadSyntax(recipient, user);
					}
				} else {
					bot.sendNotAuthorized(recipient, user);
				}
			}
			
			@Override
			public String help() {
				return "Revokes bot operator status from one or more users";
			}
			
			@Override
			public String syntax() {
				return "%s [nick] <nick...>";
			}
		});
		addCommand("SHOWOPS", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				bot.sendMessage(recipient, user + ": Registered bot operators:");
				int num = 0;
				String msg1 = user + ": ";
				for (String op : ((SmartBotCoreInterface) bot).getOperators()) {
					if (op.equalsIgnoreCase(((SmartBotCoreInterface) bot).getOwner())) {
						msg1 += "Owner:";
					}
					msg1 += op + " ";
					num++;
					if (num == 5) {
						bot.sendMessage(recipient, msg1);
						msg1 = user + ": ";
						num = 0;
					}
				}
				if (num != 0) {
					bot.sendMessage(recipient, msg1);
				}
			}
			
			@Override
			public String help() {
				return "Lists bot operators";
			}
			
			@Override
			public String syntax() {
				return "%s";
			}
		});
		addCommand("LIST", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				Module module = null;
				String opening = null;
				if (params.length > 0) {
					if (params[0].equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
						module = CoreModule.this;
						opening = sender + ": core commands:";
					} else {
						module = ((SmartBotCoreInterface) bot).getModule(params[0]);
						if (module != null) {
							opening = sender + ": " + module.getName()
									+ " commands:";
						}
					}
				} else {
					module = CoreModule.this;
					opening = sender + ": core commands:";
				}
				if (module == null) {
					bot.sendMessage(recipient,
							sender + ": module '" + params[0].toUpperCase()
									+ "' doesn't exist");
				} else {
					if (module.getCommandNames().length > 0) {
						bot.sendMessage(recipient, opening);
						for (final String n : module.getCommandNames()) {
							bot.sendMessage(recipient, sender + ": " + n + " - "
									+ module.getCommandHelp(n));
						}
						bot.sendMessage(recipient, sender + ": End command list");
					} else {
						bot.sendMessage(recipient,
								sender + ": module '" + module.getName()
										+ "' has no commands");
					}
				}
			}
			
			@Override
			public String help() {
				return "Shows the commands in a module";
			}
			
			@Override
			public String syntax() {
				return "%s <module>";
			}
		});
		addCommand("ENABLE", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				if (bot.isAuthorized(sender)) {
					if (params.length > 0) {
						for (int i = 0; i < params.length; i++) {
							final String mod = params[i].toUpperCase();
							if (((SmartBotCoreInterface) bot).hasModule(mod)) {
								if (!((SmartBotCoreInterface) bot).getModuleEnabled(mod)) {
									((SmartBotCoreInterface) bot).setModuleEnabled(mod, true);
									bot.sendMessage(recipient, sender + ": module '"
											+ mod + "' successfully enabled");
								} else {
									bot.sendMessage(recipient, sender + ": module '"
											+ mod + "' is already enabled");
								}
							} else {
								((SmartBotCoreInterface) bot).sendNoSuchModule(recipient, sender, mod);
							}
						}
					} else {
						bot.sendBadSyntax(recipient, sender);
					}
				} else {
					bot.sendNotAuthorized(recipient, sender);
				}
			}
			
			@Override
			public String help() {
				return "Enables one or more modules";
			}
			
			@Override
			public String syntax() {
				return "%s [module] <module...>";
			}
		});
		addCommand("DISABLE", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				if (bot.isAuthorized(sender)) {
					if (params.length > 0) {
						for (int i = 0; i < params.length; i++) {
							final String mod = params[i].toUpperCase();
							if (((SmartBotCoreInterface) bot).hasModule(mod)) {
								if (((SmartBotCoreInterface) bot).getModuleEnabled(mod)) {
									if (mod.equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
										bot.sendMessage(
												recipient,
												sender
														+ ": core module cannot be disabled");
									} else {
										((SmartBotCoreInterface) bot).setModuleEnabled(mod, false);
										bot.sendMessage(recipient, sender
												+ ": module '" + mod
												+ "' successfully disabled");
									}
								} else {
									bot.sendMessage(recipient, sender + ": module '"
											+ mod + "' is already disabled");
								}
							} else {
								((SmartBotCoreInterface) bot).sendNoSuchModule(recipient, sender, mod);
							}
						}
					} else {
						bot.sendBadSyntax(recipient, sender);
					}
				} else {
					bot.sendNotAuthorized(recipient, sender);
				}
			}
			
			@Override
			public String help() {
				return "Disables one or more modules";
			}
			
			@Override
			public String syntax() {
				return "%s [module] <module...>";
			}
		});
		addCommand("RELOAD", new BotAction() {
			
			@Override
			public String syntax() {
				return "%s";
			}
			
			@Override
			public String help() {
				return "Kills this bot, executes reload hooks, and starts back up.";
			}
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (bot.isAuthorized(sender)) {
					System.exit(100);
				}
			}
		});
	}
	
}
