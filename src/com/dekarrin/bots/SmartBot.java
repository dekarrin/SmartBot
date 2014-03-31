package com.dekarrin.bots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jibble.pircbot.DccChat;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.PircBot;

/**
 * Performs actions on IRC channels. SmartBot adds several features to the
 * PircBot model that it is built on. Notably, intelligent handling of kicks and
 * disconnects as well as dynamic command module loading and unloading. One
 * major limitation is that SmartBot is intended to run on one channel only;
 * attempting to get around this will result in undefined behavior.
 * 
 * @author Rebecca 'TF' Nelson
 */
public class SmartBot extends PircBot {
	
	private volatile int awaitingResponseEnd = -1;
	
	private volatile int awaitingResponseStart = -1;
	
	private volatile boolean awaitingResponseXfer = false;
	
	private volatile boolean awaitingServerResponse = false;
	
	private final String chan;
	
	private boolean cleanDc = false;
	
	private final Map<String, BotModule> loadedModules;
	
	private int maxReconnects = 3;
	
	private final Map<String, BotModule> modules;
	
	private String nickPass = null;
	
	private final Set<String> ops;
	
	private String owner = null;
	
	private char prependChar;
	
	private volatile List<Integer> serverResponseCodes = null;
	
	private volatile List<String> serverResponseData = null;
	
	private int timeBetweenReconnects = 15;
	
	private boolean trustNickServ = false;
	
	private boolean usePrependChar = false;
	
	/**
	 * Creates a new ACM Bot.
	 * 
	 * @param nick The nickname that the bot should have.
	 * @param chan The channel that this bot is intended for.
	 * @param modules The modules to initialize this bot with. May be null for
	 * none.
	 */
	public SmartBot(final String nick, final String chan,
			final BotModule[] modules) {
		setName(nick);
		setFinger("acm-bot");
		setLogin("acmbotsrv");
		setAutoNickChange(true);
		ops = new HashSet<String>();
		this.chan = chan;
		loadedModules = new HashMap<String, BotModule>();
		this.modules = new HashMap<String, BotModule>();
		this.modules.put(null, createCoreModule());
		if (modules != null) {
			for (final BotModule m : modules) {
				addModule(m.getName(), m);
			}
		}
		loadedModules.put(null, this.modules.get(null));
	}
	
	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		for (BotModule m : loadedModules.values()) {
			m.onAction(sender, login, hostname, target, action);
		}
	}
	
	@Override
	protected void onChannelInfo(String channel, int userCount, String topic) {
		for (BotModule m : loadedModules.values()) {
			m.onChannelInfo(channel, userCount, topic);
		}
	}
	
	/**
	 * Adds a module to this bot. The core module cannot be overwritten by using
	 * this method; attempts to assign a module to name 'null' (the core
	 * module's index) will be ignored.
	 * 
	 * @param name The name to use to address the module and to remove it later.
	 * @param module The module to add.
	 */
	public void addModule(final String name, final BotModule module) {
		if (name != null) {
			module.setBot(this);
			modules.put(name.toUpperCase(), module);
		}
	}
	
	/**
	 * Adds a bot operator.
	 * 
	 * @param op
	 */
	public void addOperator(final String op) {
		ops.add(op.toUpperCase());
	}
	
	/**
	 * Disconnects cleanly from channel and server. This method must be used to
	 * disconnect, or else reconnection will be attempted.
	 */
	public void cleanDisconnect() {
		cleanDisconnect("");
	}
	
	/**
	 * Disconnects cleanly from channel and server. This method must be used to
	 * disconnect, or else reconnection will be attempted.
	 * 
	 * @param reason The reason for disconnection.
	 */
	public void cleanDisconnect(final String reason) {
		cleanDc = true;
		log("Normal disconnect" + ((!reason.equals("")) ? ": " + reason : ""));
		partChannel(getIntendedChannel(), reason);
		quitServer(reason);
	}
	
	/**
	 * Gets the channel that this bot was designed for.
	 * 
	 * @return The channel name.
	 */
	public String getIntendedChannel() {
		return chan;
	}
	
	/**
	 * Gets the character used as the short name for this bot.
	 * 
	 * @return The character.
	 */
	public char getPrependChar() {
		return prependChar;
	}
	
	/**
	 * Gets number of reconnection attempts after an unexpected disconnect.
	 */
	public int getReconnectionLimit() {
		return maxReconnects;
	}
	
	/**
	 * Gets number of seconds to wait between attempts to reconnect after an
	 * unexpected disconnect.
	 */
	public int getTimeBetweenReconnects() {
		return timeBetweenReconnects;
	}
	
	/**
	 * Gets whether NickServ is trusted.
	 * 
	 * @return Whether it is.
	 */
	public boolean getTrustNickServ() {
		return trustNickServ;
	}
	
	/**
	 * Gets whether the prepend character is used in this bot.
	 */
	public boolean getUsePrepend() {
		return usePrependChar;
	}
	
	/**
	 * Checks if a module is loaded.
	 * 
	 * @param module The name of the module to check.
	 */
	public boolean hasLoadedModule(final String module) {
		return loadedModules.containsKey(module.toUpperCase());
	}
	
	/**
	 * Checks if a module exists.
	 * 
	 * @param name The name to check for.
	 */
	public boolean hasModule(final String name) {
		if (name != null) {
			return modules.containsKey(name.toUpperCase());
		} else {
			return true;
		}
	}
	
	/**
	 * Checks whether a username is an operator.
	 * 
	 * @param op The nick to check.
	 */
	public boolean hasOperator(final String op) {
		return ops.contains(op.toUpperCase());
	}
	
	/**
	 * Checks if a user is authorized to perform operator-level actions on this
	 * bot.
	 * 
	 * @param user The user to check.
	 */
	public boolean isAuthorized(String user) {
		user = getRegisteredNick(user);
		return ((user != null) && hasOperator(user));
	}
	
	/**
	 * Loads a module.
	 * 
	 * @param module The name of the module to load.
	 */
	public void loadModule(final String module) {
		final BotModule mod = getModule(module);
		if (mod != null) {
			loadedModules.put(module.toUpperCase(), mod);
		}
	}
	
	/**
	 * Removes a module from this bot. The core module cannot be removed in this
	 * manner.
	 * 
	 * @param name The name of the module to remove.
	 */
	public void removeModule(final String name) {
		if (name != null) {
			modules.remove(name.toUpperCase());
		}
	}
	
	/**
	 * Removes a bot operator.
	 * 
	 * @param op
	 */
	public void removeOperator(final String op) {
		ops.remove(op.toUpperCase());
	}
	
	/**
	 * Informs the user that they made a syntax error.
	 * 
	 * @param recipient
	 * @param user
	 */
	public void sendBadSyntax(final String recipient, final String user) {
		sendMessage(recipient, user + ": bad syntax");
	}
	
	/**
	 * Informs the user that the module that they specified does not exist.
	 */
	public void sendNoSuchModule(final String recipient, final String sender,
			final String mod) {
		sendMessage(recipient, sender + ": module '" + mod + "' does not exist");
	}
	
	/**
	 * Informs a user that they have attempted to perform an unauthorized
	 * operation.
	 * 
	 * @param user The user to send it to.
	 * @param recipient The place to send it to.
	 */
	public void sendNotAuthorized(final String recipient, final String user) {
		String msg = user + ": You don't have permission to do that.";
		if (trustNickServ) {
			msg += " If you believe that you should have permission, ";
			msg += "identify with NickServ and try again.";
		}
		sendMessage(recipient, msg);
	}
	
	/**
	 * Sends a command to the server and waits for the result. WARNING: Running
	 * on the InputThread will cause blocking. WARNING: Not very thread-safe.
	 * Near-consecutive calls to this method may cause race conditions.
	 * 
	 * @param cmd The command to execute.
	 * @param startCode The first expected response code. Responses are captured
	 * starting with the first one that begins with this code.
	 * @param endCode The last expected response code. Responses capture is
	 * complete after the first response with this code. This may be the same as
	 * startCode to capture only one response.
	 * @param codes A list to store the codes in.
	 * @param responses A list to store the responses in.
	 */
	public void sendServerCommand(final String cmd, final int startCode,
			final int endCode, final List<Integer> codes,
			final List<String> responses) {
		try {
			// EXTREMELY UNSAFE! we should really be setting up a queue if we
			// want to correctly handle this.
			// DO NOT RUN ON INPUT THREAD!
			while (awaitingServerResponse) {
				Thread.sleep(10);
			}
			while (awaitingResponseXfer) {
				Thread.sleep(10);
			}
			awaitingServerResponse = true;
			awaitingResponseXfer = false;
			awaitingResponseStart = startCode;
			awaitingResponseEnd = endCode;
			serverResponseData = new ArrayList<String>();
			serverResponseCodes = new ArrayList<Integer>();
			sendRawLineViaQueue(cmd);
			// sendRawLineViaQueue will set awaitingServerResponse to false
			// and awaitingResponseXfer to true when ready to continue
			while (awaitingServerResponse) {
				Thread.sleep(10);
			}
			for (final Integer i : serverResponseCodes) {
				codes.add(i);
			}
			for (final String s : serverResponseData) {
				responses.add(s);
			}
			awaitingResponseXfer = false;
		} catch (final InterruptedException e) {
			e.printStackTrace();
		} finally {
			awaitingResponseStart = -1;
			awaitingResponseEnd = -1;
			serverResponseData = null;
			serverResponseCodes = null;
			awaitingServerResponse = false;
			awaitingResponseXfer = false;
		}
	}
	
	/**
	 * Sets the NickServ identify password for this bot. Set to null if NickServ
	 * identification is not required. Setting to anything but null implicitly
	 * also calls setTrustNickServ(true).
	 * 
	 * @param pass What to use as the password.
	 */
	public void setNickServPass(final String pass) {
		nickPass = pass;
		if (pass != null) {
			setTrustNickServ(true);
		}
	}
	
	/**
	 * Sets the owner operator. This operator cannot be removed with the deop
	 * command. Also adds to the list of operators if the owner is not on it.
	 * 
	 * @param op The operator.
	 */
	public void setOwner(final String op) {
		addOperator(op);
		owner = op.toUpperCase();
	}
	
	/**
	 * Sets the character used as the short name for this bot. Also sets
	 * usePrepend to true.
	 * 
	 * @param c The character.
	 */
	public void setPrependChar(final char c) {
		prependChar = c;
		setUsePrepend(true);
	}
	
	/**
	 * Sets number of reconnection attempts after an unexpected disconnect. Set
	 * to 0 to disable reconnection.
	 */
	public void setReconnectionLimit(final int l) {
		maxReconnects = l;
	}
	
	/**
	 * Sets number of seconds to wait between attempts to reconnect after an
	 * unexpected disconnect.
	 */
	public void setTimeBetweenReconnects(final int t) {
		timeBetweenReconnects = t;
	}
	
	/**
	 * Sets whether NickServ is trusted.
	 * 
	 * @param t Whether it is.
	 */
	public void setTrustNickServ(final boolean t) {
		trustNickServ = t;
	}
	
	/**
	 * Sets whether the prepend character is used in this bot.
	 */
	public void setUsePrepend(final boolean u) {
		usePrependChar = u;
	}
	
	/**
	 * Unloads a module.
	 * 
	 * @param module The name of the module to unload.
	 */
	public void unloadModule(final String module) {
		if (module != null) {
			loadedModules.remove(module.toUpperCase());
		}
	}
	
	/**
	 * Creates the core module containing base commands for a SmartBot.
	 * 
	 * @return The core module.
	 */
	private BotModule createCoreModule() {
		final BotModule module = new BotModule(null);
		module.addCommand("KILL", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				if (isAuthorized(user)) {
					cleanDisconnect("Terminated by user '" + user + "'");
				} else {
					sendNotAuthorized(recipient, user);
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
		}).addCommand("MODULES", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				String msg = sender + ": Modules (* = loaded): *(CORE)";
				for (final String name : modules.keySet()) {
					if (name != null) {
						msg += ", ";
						if (hasLoadedModule(name)) {
							msg += "*";
						}
						msg += name.toUpperCase();
					}
				}
				sendMessage(recipient, msg);
			}
			
			@Override
			public String help() {
				return "Lists the modules on this bot.";
			}
			
			@Override
			public String syntax() {
				return "%s";
			}
		}).addCommand("HELP", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				String command = null;
				String module = null;
				if (params.length > 0) {
					command = params[0];
					if (params.length > 1) {
						module = params[1].toUpperCase();
					}
				}
				if ((command == null) && (module == null)) {
					getCoreModule().execute("LIST", params, user, recipient);
				} else {
					BotModule mod = null;
					if (module == null) {
						mod = getCoreModule();
					} else {
						mod = getModule(module);
					}
					if ((mod == null) && (module != null)) {
						sendNoSuchModule(recipient, user, module);
					} else {
						if (mod.hasCommand(command)) {
							final String help = mod.getHelp(command);
							final String syntax = mod.getSyntax(command);
							sendMessage(recipient, user + ": Syntax - "
									+ syntax);
							sendMessage(recipient, user + ": " + help);
						} else {
							sendMessage(recipient, user + ": command '"
									+ command + "' does not exist in module '"
									+ module + "'");
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
				return "%s <command> <module>";
			}
		}).addCommand("OP", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				if (isAuthorized(user)) {
					if (params.length > 0) {
						final String toOp = params[0];
						if (!hasOperator(toOp)) {
							addOperator(toOp);
							sendMessage(recipient, user
									+ ": bot operator was granted to " + toOp);
						} else {
							sendMessage(recipient, user + ": " + toOp
									+ " is already an operator.");
						}
					} else {
						sendBadSyntax(recipient, user);
					}
				} else {
					sendNotAuthorized(recipient, user);
				}
			}
			
			@Override
			public String help() {
				return "Grants bot operator status to a user";
			}
			
			@Override
			public String syntax() {
				return "%s [nick]";
			}
		}).addCommand("DEOP", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String user,
					final String recipient) {
				if (isAuthorized(user)) {
					if (params.length > 0) {
						final String toDeop = params[0];
						if (hasOperator(toDeop)) {
							if (!owner.equals(toDeop.toUpperCase())) {
								removeOperator(toDeop);
								sendMessage(recipient, user
										+ ": bot operator was revoked from "
										+ toDeop);
							} else {
								sendMessage(recipient, user
										+ ": you cannot deop the bot owner!");
							}
						} else {
							sendMessage(recipient, user + ": " + toDeop
									+ " is not an operator.");
						}
					} else {
						sendBadSyntax(recipient, user);
					}
				} else {
					sendNotAuthorized(recipient, user);
				}
			}
			
			@Override
			public String help() {
				return "Revokes bot operator status from a user";
			}
			
			@Override
			public String syntax() {
				return "%s [nick]";
			}
		}).addCommand("LIST", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				String mod = null;
				if (params.length > 0) {
					mod = params[1];
				}
				BotModule module = null;
				if (mod == null) {
					sendMessage(recipient, sender + ": core commands:");
					module = getCoreModule();
				} else {
					if (!hasModule(mod)) {
						sendMessage(recipient, sender + ": module '" + mod
								+ "' doesn't exist");
						return;
					}
					sendMessage(recipient, sender + ": commands in module '"
							+ mod.toUpperCase() + "':");
					module = getModule(mod);
				}
				for (final String n : module.getCommandNames()) {
					sendMessage(recipient, sender + ": " + n + " - "
							+ getCoreModule().getHelp(n));
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
		}).addCommand("LOAD", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				if (isAuthorized(sender)) {
					if (params.length > 0) {
						final String mod = params[0].toUpperCase();
						if (hasModule(mod)) {
							if (!hasLoadedModule(mod)) {
								loadModule(mod);
								sendMessage(recipient, sender + ": module '"
										+ mod + "' successfully loaded");
							} else {
								sendMessage(recipient, sender + ": module '"
										+ mod + "' is already loaded");
							}
						} else {
							sendNoSuchModule(recipient, sender, mod);
						}
					} else {
						sendBadSyntax(recipient, sender);
					}
				} else {
					sendNotAuthorized(recipient, sender);
				}
			}
			
			@Override
			public String help() {
				return "Enables a module";
			}
			
			@Override
			public String syntax() {
				return "%s [module]";
			}
		}).addCommand("UNLOAD", new BotAction() {
			
			@Override
			public void execute(final String[] params, final String sender,
					final String recipient) {
				if (isAuthorized(sender)) {
					if (params.length > 0) {
						final String mod = params[0].toUpperCase();
						if (hasModule(mod)) {
							if (hasLoadedModule(mod)) {
								unloadModule(mod);
								sendMessage(recipient, sender + ": module '"
										+ mod + "' successfully unloaded");
							} else {
								sendMessage(recipient, sender + ": module '"
										+ mod + "' is already unloaded");
							}
						} else {
							sendNoSuchModule(recipient, sender, mod);
						}
					} else {
						sendBadSyntax(recipient, sender);
					}
				} else {
					sendNotAuthorized(recipient, sender);
				}
			}
			
			@Override
			public String help() {
				return "Disables a module";
			}
			
			@Override
			public String syntax() {
				return "%s [module]";
			}
		});
		return module;
	}
	
	@Override
	protected void onIncomingChatRequest(DccChat chat) {
		for (BotModule m : loadedModules.values()) {
			m.onIncomingChatRequest(chat);
		}
	}
	
	@Override
	protected void onIncomingFileTransfer(DccFileTransfer transfer) {
		for (BotModule m : loadedModules.values()) {
			m.onIncomingFileTransfer(transfer);
		}
	}
	
	@Override
	protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
		for (BotModule m : loadedModules.values()) {
			m.onInvite(targetNick, sourceNick, sourceLogin, sourceHostname, channel);
		}
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname) {
		for (BotModule m : loadedModules.values()) {
			m.onJoin(channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
		for (BotModule m : loadedModules.values()) {
			m.onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
		}
	}
	
	@Override
	protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
		for (BotModule m : loadedModules.values()) {
			m.onNickChange(oldNick, login, hostname, newNick);
		}
	}
	
	@Override
	protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
		for (BotModule m : loadedModules.values()) {
			m.onNotice(sourceNick, sourceLogin, sourceHostname, target, notice);
		}
	}
	
	@Override
	protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
		for (BotModule m : loadedModules.values()) {
			m.onOp(channel, sourceNick, sourceLogin, sourceHostname, recipient);
		}
	}
	
	@Override
	protected void onPart(String channel, String sender, String login, String hostname) {
		for (BotModule m : loadedModules.values()) {
			m.onPart(channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
		for (BotModule m : loadedModules.values()) {
			m.onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
		}
		super.onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
	}
	
	@Override
	protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
		for (BotModule m : loadedModules.values()) {
			m.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
		}
	}
	
	@Override
	protected void onRemoveChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, hostmask);
		}
	}
	
	@Override
	protected void onRemoveChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, key);
		}
	}
	
	@Override
	protected void onRemoveChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onRemoveNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onRemovePrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onRemoveSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onRemoveTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
		for (BotModule m : loadedModules.values()) {
			m.onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
		}
	}
	
	@Override
	protected void onServerPing(String response) {
		for (BotModule m : loadedModules.values()) {
			m.onServerPing(response);
		}
		super.onServerPing(response);
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname)  {
		for (BotModule m : loadedModules.values()) {
			m.onJoin(channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname)  {
		for (BotModule m : loadedModules.values()) {
			m.onJoin(channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname)  {
		for (BotModule m : loadedModules.values()) {
			m.onJoin(channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname)  {
		for (BotModule m : loadedModules.values()) {
			m.onJoin(channel, sender, login, hostname);
		}
	}
	
	@Override
	protected void onJoin(String channel, String sender, String login, String hostname)  {
		for (BotModule m : loadedModules.values()) {
			m.onJoin(channel, sender, login, hostname);
		}
	}
	
	
	
	/**
	 * Executes a command.
	 * 
	 * @param user User that sent the command.
	 * @param argv The words that make up the command.
	 */
	private void execute(final String[] argv, final String user,
			final boolean isPm) {
		final String recipient = (isPm) ? user : getIntendedChannel();
		class Execution implements Runnable {
			
			private final String[] argv;
			
			private final String recipient;
			
			private final String user;
			
			public Execution(final String[] argv, final String user,
					final String recipient) {
				this.argv = argv;
				this.user = user;
				this.recipient = recipient;
			}
			
			@Override
			public void run() {
				if (!executeCommand(argv, user, recipient)) {
					sendMessage(recipient, user + ": Unknown command/module '"
							+ argv[0] + "'");
				}
			}
		};
		final Execution runme = new Execution(argv, user, recipient);
		(new Thread(runme, "WorkerThread")).start();
	}
	
	/**
	 * Executes a command with modules.
	 * 
	 * @param user The user that executed the command.
	 * @param argv The words in the command.
	 * @return Whether the command was handled.
	 */
	private boolean executeCommand(final String[] argv, final String sender,
			final String recipient) {
		final BotModule core = getCoreModule();
		if (core.hasCommand(argv[0])) {
			// first priority is assume core module.
			final String[] params = Arrays.copyOfRange(argv, 1, argv.length);
			return core.execute(argv[0], params, sender, recipient);
		} else if (hasLoadedModule(argv[0])) {
			// next priority is assume named module
			final String[] params = Arrays.copyOfRange(argv, 2, argv.length);
			return getModule(argv[0]).execute(argv[1], params, sender,
					recipient);
		} else {
			// finally, search all modules for the command
			final List<BotModule> candidates = new ArrayList<BotModule>();
			for (final BotModule mod : loadedModules.values()) {
				if (mod.hasCommand(argv[0])) {
					candidates.add(mod);
				}
			}
			if (candidates.size() == 1) {
				final String[] params = Arrays
						.copyOfRange(argv, 1, argv.length);
				return candidates.get(0).execute(argv[0], params, sender,
						recipient);
			} else if (candidates.size() > 1) {
				sendMessage(recipient, sender + ": ambiguous command.");
				sendMessage(recipient, sender + ": '" + argv[0]
						+ "' exists in the following modules:");
				String msg = "";
				for (int i = 0; i < candidates.size(); i++) {
					msg += candidates.get(i).getName();
					if ((i + 1) < candidates.size()) {
						msg += ", ";
					}
				}
				sendMessage(recipient, msg);
				return true;
			} else {
				return false;
			}
		}
	}
	
	/**
	 * Checks user registration against an authorization database. If NickServ
	 * is trusted, it is used.
	 * 
	 * @param user The user to check.
	 * @return The nick that the user is registered as, or null if the user is
	 * not currently registered.
	 */
	private String getRegisteredNick(final String user) {
		if (trustNickServ) {
			final List<Integer> codes = new ArrayList<Integer>();
			final List<String> data = new ArrayList<String>();
			sendServerCommand("WHOIS :" + user, 311, 318, codes, data);
			String line = null;
			for (int i = 0; i < codes.size(); i++) {
				if (codes.get(i) == 330) {
					line = data.get(i);
					break;
				}
			}
			if (line != null) {
				line = line.trim();
				final String[] parts = line.split(" ", 4);
				return parts[2];
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the default module containing base commands. The core module is
	 * never allowed to be unloaded and thus provides a guarantee that the
	 * commands it contains are always available. The core module may be
	 * modified to suit the needs of subclasses.
	 * 
	 * @return The default module.
	 */
	protected BotModule getCoreModule() {
		return modules.get(null);
	}
	
	/**
	 * Gets the named module. The core module CANNOT be retrieved in this
	 * manner.
	 * 
	 * @param name The name of the module to get.
	 * @return The named module, or null if the module does not exist.
	 */
	protected BotModule getModule(final String name) {
		return modules.get(name.toUpperCase());
	}
	
	/**
	 * Performs NickServ identification and joins the primary channel.
	 */
	@Override
	protected void onConnect() {
		if (trustNickServ && (nickPass != null)) {
			identify(nickPass);
		}
		joinChannel(getIntendedChannel());
		for (BotModule m : loadedModules.values()) {
			m.onConnect();
		}
	}
	
	@Override
	protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
		for (BotModule m : loadedModules.values()) {
			m.onDeop(channel, sourceNick, sourceLogin, sourceHostname, recipient);
		}
	}
	
	@Override
	protected void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
		for (BotModule m : loadedModules.values()) {
			m.onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, recipient);
		}
	}
	
	/**
	 * Attempts to reconnect if the disconnection was not caused by a clean
	 * disconnect.
	 */
	@Override
	protected void onDisconnect() {
		if (cleanDc) {
			dispose();
		} else {
			boolean connected = false;
			final int attempts = 0;
			try {
				while (!connected && (attempts < maxReconnects)) {
					Thread.sleep(1000 * timeBetweenReconnects);
					try {
						reconnect();
						connected = true;
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			} catch (final InterruptedException e) {
				// Should NEVER happen
				e.printStackTrace();
			}
			if (!connected) {
				log("Lost connection. Could not reconnect.");
				dispose();
			}
		}
		for (BotModule m : loadedModules.values()) {
			m.onDisconnect();
		}
	}
	
	@Override
	protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
		for (BotModule m : loadedModules.values()) {
			m.onFileTransferFinished(transfer, e);
		}
	}
	
	@Override
	protected void onFinger(String sourceNick, String sourceLogin, String sourceHostname, String target) {
		for (BotModule m : loadedModules.values()) {
			m.onFinger(sourceNick, sourceLogin, sourceHostname, target);
		}
	}
	
	/**
	 * If bot was kicked, performs a clean disconnect.
	 */
	@Override
	protected void onKick(final String channel, final String kickerNick,
			final String kickerLogin, final String kickerHost,
			final String recipient, final String reason) {
		for (BotModule m : loadedModules.values()) {
			m.onKick(channel, kickerNick, kickerLogin, kickerHost, recipient, reason);
		}
		if (channel.equals(getIntendedChannel()) && recipient.equals(getNick())) {
			// we've been kicked from the chan; be well-behaved and shutdown.
			cleanDisconnect("kicked from active channel by '" + kickerNick
					+ "'");
		}
	}
	
	@Override
	protected void onMessage(final String channel, final String sender,
			final String login, final String hostname, String message) {
		if (channel.equals(getIntendedChannel())) {
			int cmdPre = 0;
			if (message.toLowerCase().startsWith(getNick() + ":")) {
				cmdPre = (getNick() + ":").length();
			} else if (usePrependChar && message.startsWith(prependChar + "")) {
				cmdPre = 1;
			}
			if (cmdPre > 0) {
				message = message.substring(cmdPre).trim()
						.replaceAll(" +", " ");
				execute(message.split(" "), sender, false);
			}
		}
		for (BotModule m : loadedModules.values()) {
			m.onMessage(channel, sender, login, hostname, message);
		}
	}
	
	@Override
	protected void onPrivateMessage(final String sender, final String login,
			final String host, String message) {
		message = message.trim().replaceAll(" +", " ");
		execute(message.split(" "), sender, true);
		for (BotModule m : loadedModules.values()) {
			m.onPrivateMessage(sender, login, host, message);
		}
	}
	
	@Override
	protected void onServerResponse(final int code, final String resp) {
		if (awaitingServerResponse) {
			if ((code == awaitingResponseStart)
					|| (awaitingResponseStart == -1)) {
				awaitingResponseStart = -1;
				serverResponseCodes.add(code);
				serverResponseData.add(resp);
				if (code == awaitingResponseEnd) {
					awaitingResponseEnd = -1;
					awaitingResponseXfer = true;
					awaitingServerResponse = false;
				}
			}
		}
		for (BotModule m : loadedModules.values()) {
			m.onServerResponse(code, resp);
		}
	}
}
