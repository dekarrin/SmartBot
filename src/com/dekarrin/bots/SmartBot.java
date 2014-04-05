package com.dekarrin.bots;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jibble.pircbot.DccChat;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;

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
	
	/**
	 * The sender when the message is from the console.
	 */
	public static final String CONSOLE_USER = "-CONSOLE";
	
	/**
	 * Name of the core module. For ambiguity resolution, no command in any
	 * module may have this name.
	 */
	public static final String CORE_MODULE_NAME = "_CORE_";
	
	/**
	 * The base version string for SmartBot. It is formatted by passing the
	 * version number of the core module as the first format argument.
	 */
	public static final String VERSION = "NDSU ACM SmartBot %s - PircBot v1.5.0";
	
	private volatile boolean awaitingNickChange = false;
	
	private volatile boolean awaitingNickServGhost = false;
	
	private volatile int awaitingResponseEnd = -1;
	
	private volatile int awaitingResponseStart = -1;
	
	private volatile boolean awaitingResponseXfer = false;
	
	private volatile boolean awaitingServerResponse = false;
	
	private final String chan;
	
	private volatile boolean cleanDc = false;
	
	private Map<String, Module> enabledModules;
	
	private volatile boolean loggedIn = false;
	
	private volatile boolean loggingIn = false;
	
	private String loginNick = null;
	
	private int maxReconnects = 3;
	
	private String moduleClassPath;
	
	private Map<String, Module> modules;
	
	// TODO: Highly insecure. We should not keep the nickserv password in
	// memory.
	private String nickPass = null;
	
	private volatile String oldNick = null;
	
	private final Set<String> ops;
	
	private String owner = null;
	
	private char prependChar;
	
	private volatile List<Integer> serverResponseCodes = null;
	
	private volatile List<String> serverResponseData = null;
	
	private final Settings settings;
	
	private int timeBetweenReconnects = 15;
	
	private boolean trustNickServ = false;
	
	private boolean usePrependChar = false;
	
	/**
	 * Creates a new ACM Bot.
	 * 
	 * @param nick The nickname that the bot should have.
	 * @param chan The channel that this bot is intended for.
	 * @param rcfilepath The file containing settings for this bot. If the file
	 * doesn't exist, it will be created whenever the settings are changed.
	 * @param modules The modules to initialize this bot with. May be null for
	 * none.
	 */
	public SmartBot(final String nick, final String chan,
			final String RCFilePath, final Module[] modules) {
		setName(nick);
		loginNick = nick;
		setFinger("acm-bot");
		setLogin("acmbotsrv");
		setAutoNickChange(true);
		settings = new Settings(RCFilePath, true);
		ops = new HashSet<String>();
		this.chan = chan;
		initialize(modules);
		startConsoleInputThread();
	}
	
	/**
	 * Adds a module to this bot. The core module cannot be overwritten by using
	 * this method; attempts to assign a module with the same name or with a
	 * command that has the same name will cause an Exception to be thrown.
	 * 
	 * @param module The module to add.
	 */
	public void addModule(final Module module) {
		if (module.getName().equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
			throw new IllegalArgumentException("Module name '"
					+ SmartBot.CORE_MODULE_NAME
					+ "' is reserved for the core module");
		}
		if (module.hasCommand(SmartBot.CORE_MODULE_NAME)) {
			throw new IllegalArgumentException("Command '"
					+ SmartBot.CORE_MODULE_NAME + "' in module '"
					+ module.getName()
					+ "' conflicts with the core module name");
		}
		module.setBot(new SmartBotInterface() {
			
			@Override
			public void disconnect(String reason) {
				SmartBot.this.cleanDisconnect(reason);
			}
			
			@Override
			public String getChannel() {
				return SmartBot.this.getIntendedChannel();
			}
			
			@Override
			public String getNick() {
				return SmartBot.this.getNick();
			}
			
			@Override
			public String getVersion() {
				return SmartBot.this.getVersion();
			}
			
			@Override
			public boolean isAuthorized(String user) {
				return SmartBot.this.isAuthorized(user);
			}
			
			@Override
			public void sendBadSyntax(String recipient, String user) {
				SmartBot.this.sendBadSyntax(recipient, user);
			}
			
			@Override
			public void sendMessage(String recipient, String message) {
				SmartBot.this.sendResponse(recipient, message);
			}
			
			@Override
			public void sendNotAuthorized(String recipient, String user) {
				SmartBot.this.sendNotAuthorized(recipient, user);
			}
		}, settings);
		modules.put(module.getName().toUpperCase(), module);
	}
	
	/**
	 * Adds a bot operator.
	 * 
	 * @param op
	 */
	public void addOperator(final String op) {
		ops.add(op.toUpperCase());
		settings.setNickOp(op.toUpperCase(), true);
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
		sendMessage(getIntendedChannel(), "Disconnecting - " + reason);
		partChannel(getIntendedChannel(), reason);
		quitServer(reason);
		try {
			System.in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Enables all modules as specified by persistence settings. Any modules
	 * that are listed but are not currently added to this bot are skipped.
	 */
	public void enableModulesFromSettings() {
		enabledModules = new LinkedHashMap<String, Module>();
		for (final String module : settings.getEnabledModules()) {
			if (hasModule(module)) {
				enabledModules.put(module, getModule(module));
			}
		}
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
	 * Gets an unmodifiable view of the operators set.
	 * 
	 * @return The operators.
	 */
	public Set<String> getOperators() {
		return Collections.unmodifiableSet(ops);
	}
	
	/**
	 * Gets the owner of this bot.
	 * 
	 * @return The owner.
	 */
	public String getOwner() {
		return owner;
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
	 * Checks whether a username is an operator.
	 * 
	 * @param op The nick to check.
	 */
	public boolean hasOperator(final String op) {
		return ops.contains(op.toUpperCase());
	}
	
	/**
	 * Resets all modules and settings and loads them from disk.
	 * 
	 * @param modulesToAdd The modules to be loaded.
	 */
	public void initialize(final Module[] modulesToAdd) {
		loadSettings();
		loadModules(modulesToAdd);
	}
	
	/**
	 * Removes a module from this bot. The core module cannot be removed in this
	 * manner.
	 * 
	 * @param name The name of the module to remove.
	 */
	public void removeModule(final String name) {
		if (!name.equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
			setModuleEnabled(name, false);
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
		settings.setNickOp(op.toUpperCase(), false);
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
	 * Sets whether a module is enabled.
	 * 
	 * @param module The name of the module to disable/enable.
	 * @param enable Whether to enable the module.
	 */
	public void setModuleEnabled(final String module, final boolean enable) {
		if (enable) {
			final Module mod = getModule(module);
			if (mod != null) {
				enabledModules.put(module.toUpperCase(), mod);
				mod.onModuleEnabled();
			}
		} else {
			if (!module.equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
				final Module m = enabledModules.remove(module.toUpperCase());
				if (m != null) {
					m.onModuleDisabled();
				}
			}
		}
		settings.setModuleEnabled(module, enable);
	}
	
	public void setModulePath(final String path) {
		moduleClassPath = path;
		settings.setModuleSetting(SmartBot.CORE_MODULE_NAME, "modulePath", path);
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
		settings.setModuleSetting(SmartBot.CORE_MODULE_NAME, "owner", owner);
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
	 * Executes a command.
	 * 
	 * @param user User that sent the command.
	 * @param argv The words that make up the command.
	 */
	private void execute(final String[] argv, final String user,
			final boolean isPm) {
		String recipient = null;
		if (!user.equals(SmartBot.CONSOLE_USER)) {
			recipient = (isPm) ? user : getIntendedChannel();
		}
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
					sendResponse(recipient, user + ": Unknown command/module '"
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
		final Module core = getCoreModule();
		if (core.hasCommand(argv[0])) {
			// first priority is assume core module.
			final String[] params = Arrays.copyOfRange(argv, 1, argv.length);
			return core.execute(argv[0], params, sender, recipient);
		} else if (getModuleEnabled(argv[0])) {
			// next priority is assume named module
			if (argv.length > 1) {
				final String[] params = Arrays
						.copyOfRange(argv, 2, argv.length);
				return getModule(argv[0]).execute(argv[1], params, sender,
						recipient);
			} else {
				sendResponse(recipient, sender + ": '" + argv[0]
						+ "' - is a module");
				return true;
			}
		} else {
			// finally, search all modules for the command
			final List<Module> candidates = new ArrayList<Module>();
			for (final Module mod : enabledModules.values()) {
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
				sendResponse(recipient, sender + ": ambiguous command.");
				sendResponse(recipient, sender + ": '" + argv[0]
						+ "' exists in the following modules:");
				String msg = "";
				for (int i = 0; i < candidates.size(); i++) {
					msg += candidates.get(i).getName();
					if ((i + 1) < candidates.size()) {
						msg += ", ";
					}
				}
				sendResponse(recipient, msg);
				return true;
			} else {
				return false;
			}
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
	private Module getCoreModule() {
		return modules.get(SmartBot.CORE_MODULE_NAME);
	}
	
	/**
	 * Gets the named module. The core module CANNOT be retrieved in this
	 * manner.
	 * 
	 * @param name The name of the module to get.
	 * @return The named module, or null if the module does not exist.
	 */
	private Module getModule(final String name) {
		if (!name.equalsIgnoreCase(SmartBot.CORE_MODULE_NAME)) {
			return modules.get(name.toUpperCase());
		} else {
			return null;
		}
	}
	
	/**
	 * Checks if a module is loaded.
	 * 
	 * @param module The name of the module to check.
	 */
	private boolean getModuleEnabled(final String module) {
		return enabledModules.containsKey(module.toUpperCase());
	}
	
	/**
	 * Gets the names of the modules in this Bot.
	 * 
	 * @return An unmodifiable set of modules. The order is guaranteed to be the
	 * same as the order that they were loaded into this bot.
	 */
	private Set<String> getModuleNames() {
		return Collections.unmodifiableSet(modules.keySet());
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
	 * Checks if a module exists.
	 * 
	 * @param name The name to check for.
	 */
	private boolean hasModule(final String name) {
		return modules.containsKey(name.toUpperCase());
	}
	
	/**
	 * Checks if a user is authorized to perform operator-level actions on this
	 * bot.
	 * 
	 * @param user The user to check.
	 */
	private boolean isAuthorized(String user) {
		if (user.equals(SmartBot.CONSOLE_USER)) {
			return true;
		} else {
			user = getRegisteredNick(user);
			return ((user != null) && hasOperator(user));
		}
	}
	
	private void loadModules(final Module[] modulesToAdd) {
		enabledModules = new LinkedHashMap<String, Module>();
		modules = new HashMap<String, Module>();
		if (modules != null) {
			for (final Module m : modulesToAdd) {
				addModule(m);
			}
		}
		Module core = new CoreModule();
		core.setBot(new SmartBotCoreInterface() {
			
			@Override
			public void addOperator(String name) {
				SmartBot.this.addOperator(name);
			}
			
			@Override
			public void disconnect(String reason) {
				SmartBot.this.cleanDisconnect(reason);
			}
			
			@Override
			public String getChannel() {
				return SmartBot.this.getIntendedChannel();
			}
			
			@Override
			public Module getModule(String name) {
				return SmartBot.this.getModule(name);
			}
			
			@Override
			public boolean getModuleEnabled(String name) {
				return SmartBot.this.getModuleEnabled(name);
			}
			
			@Override
			public Set<String> getModuleNames() {
				return SmartBot.this.getModuleNames();
			}
			
			@Override
			public String getNick() {
				return SmartBot.this.getNick();
			}
			
			@Override
			public Set<String> getOperators() {
				return SmartBot.this.getOperators();
			}
			
			@Override
			public String getOwner() {
				return SmartBot.this.getOwner();
			}
			
			@Override
			public String getVersion() {
				return SmartBot.this.getVersion();
			}
			
			@Override
			public boolean hasModule(String name) {
				return SmartBot.this.hasModule(name);
			}
			
			@Override
			public boolean hasOperator(String name) {
				return SmartBot.this.hasOperator(name);
			}
			
			@Override
			public boolean isAuthorized(String user) {
				return SmartBot.this.isAuthorized(user);
			}
			
			@Override
			public void removeOperator(String name) {
				SmartBot.this.removeModule(name);
			}
			
			@Override
			public void sendBadSyntax(String recipient, String user) {
				SmartBot.this.sendBadSyntax(recipient, user);
			}
			
			@Override
			public void sendMessage(String recipient, String message) {
				SmartBot.this.sendResponse(recipient, message);
			}
			
			@Override
			public void sendNoSuchModule(String recipient, String user,
					String module) {
				SmartBot.this.sendNoSuchModule(recipient, user, module);
			}
			
			@Override
			public void sendNotAuthorized(String recipient, String user) {
				SmartBot.this.sendNotAuthorized(recipient, user);
			}
			
			@Override
			public void setModuleEnabled(String name, boolean enabled) {
				SmartBot.this.setModuleEnabled(name, enabled);
			}
		}, settings);
		modules.put(SmartBot.CORE_MODULE_NAME, core);
		enabledModules.put(SmartBot.CORE_MODULE_NAME,
				modules.get(SmartBot.CORE_MODULE_NAME));
		setVersion(String
				.format(SmartBot.VERSION, getCoreModule().getVersion()));
	}
	
	/**
	 * Loads the settings and immediately sets core properties.
	 */
	private void loadSettings() {
		try {
			settings.read();
		} catch (final FileNotFoundException e) {
			// this is okay
		} catch (final IOException e) {
			e.printStackTrace();
		}
		// assuming it didn't fail, set state from settings.
		// DO NOT SET MODULE ENABLING; we don't know if all the modules have
		// yet been added.
		if (settings.successfullyLoaded()) {
			for (final String op : settings.getOperators()) {
				ops.add(op.toUpperCase());
			}
			owner = settings.getModuleSetting(SmartBot.CORE_MODULE_NAME,
					"owner").toUpperCase();
			ops.add(owner.toUpperCase());
			moduleClassPath = settings.getModuleSetting(
					SmartBot.CORE_MODULE_NAME, "modulePath");
			if (moduleClassPath == null) {}
		}
	}
	
	/**
	 * Informs the user that they made a syntax error.
	 * 
	 * @param recipient
	 * @param user
	 */
	private void sendBadSyntax(final String recipient, final String user) {
		sendResponse(recipient, user + ": bad syntax");
	}
	
	/**
	 * Informs the user that the module that they specified does not exist.
	 */
	private void sendNoSuchModule(final String recipient, final String sender,
			final String mod) {
		sendResponse(recipient, sender + ": module '" + mod
				+ "' does not exist");
	}
	
	/**
	 * Informs a user that they have attempted to perform an unauthorized
	 * operation.
	 * 
	 * @param user The user to send it to.
	 * @param recipient The place to send it to.
	 */
	private void sendNotAuthorized(final String recipient, final String user) {
		String msg = user + ": You don't have permission to do that.";
		if (trustNickServ) {
			msg += " If you believe that you should have permission, ";
			msg += "identify with NickServ and try again.";
		}
		sendResponse(recipient, msg);
	}
	
	/**
	 * Sends a response to the appropriate location. DO NOT USE sendMessage()
	 * FROM MODULES! Use this method only.
	 */
	private void sendResponse(String recipient, String msg) {
		if (recipient == null) {
			System.out.println(msg);
		} else {
			sendMessage(recipient, msg);
		}
	}
	
	private void startConsoleInputThread() {
		(new Thread(new Runnable() {
			
			@Override
			public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				try {
					while (true) {
						String line = br.readLine();
						execute(line.split(" "), SmartBot.CONSOLE_USER, false);
					}
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}, "ConsoleInput")).start();
	}
	
	/**
	 * Logs in with the nick given at construction. If the nick is not
	 * available, this method attempts to reclaim it.
	 */
	protected void attemptIdentify() {
		if (!getNick().equals(loginNick)) {
			awaitingNickServGhost = true;
			loggingIn = true;
			sendMessage("NickServ",
					String.format("GHOST %s %s", loginNick, nickPass));
		} else {
			identify(nickPass);
			loggedIn = true;
		}
	}
	
	@Override
	protected void onAction(final String sender, final String login,
			final String hostname, final String target, final String action) {
		for (final Module m : enabledModules.values()) {
			if (m.onAction(sender, login, hostname, target, action)) {
				break;
			}
		}
	}
	
	@Override
	protected void onChannelInfo(final String channel, final int userCount,
			final String topic) {
		for (final Module m : enabledModules.values()) {
			if (m.onChannelInfo(channel, userCount, topic)) {
				break;
			}
		}
	}
	
	/**
	 * Performs NickServ identification and joins the primary channel.
	 */
	@Override
	protected void onConnect() {
		loggedIn = false;
		joinChannel(getIntendedChannel());
		for (final Module m : enabledModules.values()) {
			if (m.onConnect()) {
				break;
			}
		}
	}
	
	@Override
	protected void onDeop(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		for (final Module m : enabledModules.values()) {
			if (m.onDeop(channel, sourceNick, sourceLogin, sourceHostname,
					recipient)) {
				break;
			}
		}
	}
	
	@Override
	protected void onDeVoice(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		for (final Module m : enabledModules.values()) {
			if (m.onDeVoice(channel, sourceNick, sourceLogin, sourceHostname,
					recipient)) {
				break;
			}
		}
	}
	
	/**
	 * Attempts to reconnect if the disconnection was not caused by a clean
	 * disconnect.
	 */
	@Override
	protected void onDisconnect() {
		for (final Module m : enabledModules.values()) {
			if (m.onDisconnect()) {
				break;
			}
		}
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
	}
	
	@Override
	protected void onFileTransferFinished(final DccFileTransfer transfer,
			final Exception e) {
		for (final Module m : enabledModules.values()) {
			if (m.onFileTransferFinished(transfer, e)) {
				break;
			}
		}
	}
	
	@Override
	protected void onFinger(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {
		for (final Module m : enabledModules.values()) {
			if (m.onFinger(sourceNick, sourceLogin, sourceHostname, target)) {
				break;
			}
		}
	}
	
	@Override
	protected void onIncomingChatRequest(final DccChat chat) {
		for (final Module m : enabledModules.values()) {
			if (m.onIncomingChatRequest(chat)) {
				break;
			}
		}
	}
	
	@Override
	protected void onIncomingFileTransfer(final DccFileTransfer transfer) {
		for (final Module m : enabledModules.values()) {
			if (m.onIncomingFileTransfer(transfer)) {
				break;
			}
		}
	}
	
	@Override
	protected void onInvite(final String targetNick, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String channel) {
		for (final Module m : enabledModules.values()) {
			if (m.onInvite(targetNick, sourceNick, sourceLogin, sourceHostname,
					channel)) {
				break;
			}
		}
	}
	
	@Override
	protected void onJoin(final String channel, final String sender,
			final String login, final String hostname) {
		if (trustNickServ && (nickPass != null) && !loggedIn) {
			attemptIdentify();
		}
		for (final Module m : enabledModules.values()) {
			if (m.onJoin(channel, sender, login, hostname)) {
				break;
			}
		}
	}
	
	/**
	 * If bot was kicked, performs a clean disconnect.
	 */
	@Override
	protected void onKick(final String channel, final String kickerNick,
			final String kickerLogin, final String kickerHost,
			final String recipient, final String reason) {
		for (final Module m : enabledModules.values()) {
			if (m.onKick(channel, kickerNick, kickerLogin, kickerHost,
					recipient, reason)) {
				break;
			}
		}
		if (channel.equals(getIntendedChannel()) && recipient.equals(getNick())) {
			// we've been kicked from the chan; be well-behaved and shutdown.
			cleanDisconnect("kicked from active channel by '" + kickerNick
					+ "'");
		}
	}
	
	/**
	 * Called when a user (usually us) is killed from the connection. We will
	 * not usually have this information unless we are the one being killed.
	 * 
	 * @param nick The nickname of the user being killed.
	 * @param reason The reason for the killing. May be blank.
	 */
	protected void onKill(String nick, String reason) {
		for (final Module m : enabledModules.values()) {
			if (m.onKill(nick, reason)) {
				break;
			}
		}
		// we have been directly dc'd; be nice and stay dc'd.
		cleanDc = true;
	}
	
	@Override
	protected void onMessage(final String channel, final String sender,
			final String login, final String hostname, String message) {
		boolean consume = false;
		if (channel.equals(getIntendedChannel())) {
			int cmdPre = 0;
			if (message.toUpperCase().startsWith(getNick().toUpperCase() + ":")) {
				cmdPre = (getNick() + ":").length();
			} else if (usePrependChar && message.startsWith(prependChar + "")) {
				cmdPre = 1;
			}
			if (cmdPre > 0) {
				message = message.substring(cmdPre).trim()
						.replaceAll(" +", " ");
				execute(message.split(" "), sender, false);
				consume = true;
			}
		}
		if (!consume) {
			for (final Module m : enabledModules.values()) {
				if (m.onMessage(channel, sender, login, hostname, message)) {
					break;
				}
			}
		}
	}
	
	@Override
	protected void onMode(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String mode) {
		for (final Module m : enabledModules.values()) {
			if (m.onMode(channel, sourceNick, sourceLogin, sourceHostname, mode)) {
				break;
			}
		}
	}
	
	@Override
	protected void onNickChange(final String oldNick, final String login,
			final String hostname, final String newNick) {
		if (awaitingNickChange && loggingIn
				&& oldNick.equalsIgnoreCase(this.oldNick)
				&& newNick.equalsIgnoreCase(loginNick)) {
			awaitingNickChange = loggingIn = false;
			identify(nickPass);
			loggedIn = true;
		}
		for (final Module m : enabledModules.values()) {
			if (m.onNickChange(oldNick, login, hostname, newNick)) {
				break;
			}
		}
	}
	
	@Override
	protected void onNotice(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target,
			final String notice) {
		for (final Module m : enabledModules.values()) {
			if (m.onNotice(sourceNick, sourceLogin, sourceHostname, target,
					notice)) {
				break;
			}
		}
	}
	
	@Override
	protected void onOp(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		for (final Module m : enabledModules.values()) {
			if (m.onOp(channel, sourceNick, sourceLogin, sourceHostname,
					recipient)) {
				break;
			}
		}
	}
	
	@Override
	protected void onPart(final String channel, final String sender,
			final String login, final String hostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onPart(channel, sender, login, hostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onPing(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target,
			final String pingValue) {
		for (final Module m : enabledModules.values()) {
			if (m.onPing(sourceNick, sourceLogin, sourceHostname, target,
					pingValue)) {
				break;
			}
		}
		super.onPing(sourceNick, sourceLogin, sourceHostname, target, pingValue);
	}
	
	@Override
	protected void onPrivateMessage(final String sender, final String login,
			final String host, String message) {
		message = message.trim().replaceAll(" +", " ");
		execute(message.split(" "), sender, true);
		for (final Module m : enabledModules.values()) {
			if (m.onPrivateMessage(sender, login, host, message)) {
				break;
			}
		}
	}
	
	@Override
	protected void onQuit(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String reason) {
		if (awaitingNickServGhost && sourceNick.equalsIgnoreCase(loginNick)) {
			awaitingNickServGhost = false;
			awaitingNickChange = true;
			oldNick = getNick();
			changeNick(loginNick);
		}
		for (final Module m : enabledModules.values()) {
			if (m.onQuit(sourceNick, sourceLogin, sourceHostname, reason)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveChannelBan(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String hostmask) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveChannelBan(channel, sourceNick, sourceLogin,
					sourceHostname, hostmask)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveChannelKey(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String key) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveChannelKey(channel, sourceNick, sourceLogin,
					sourceHostname, key)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveChannelLimit(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveChannelLimit(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveInviteOnly(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveInviteOnly(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveModerated(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveModerated(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveNoExternalMessages(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveNoExternalMessages(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemovePrivate(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemovePrivate(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveSecret(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveSecret(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onRemoveTopicProtection(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onRemoveTopicProtection(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onServerPing(final String response) {
		for (final Module m : enabledModules.values()) {
			if (m.onServerPing(response)) {
				break;
			}
		}
		super.onServerPing(response);
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
		for (final Module m : enabledModules.values()) {
			if (m.onServerResponse(code, resp)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetChannelBan(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String hostmask) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetChannelBan(channel, sourceNick, sourceLogin,
					sourceHostname, hostmask)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetChannelKey(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String key) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetChannelKey(channel, sourceNick, sourceLogin,
					sourceHostname, key)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetChannelLimit(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final int limit) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetChannelLimit(channel, sourceNick, sourceLogin,
					sourceHostname, limit)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetInviteOnly(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetInviteOnly(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetModerated(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetModerated(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetNoExternalMessages(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetNoExternalMessages(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetPrivate(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetSecret(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetSecret(channel, sourceNick, sourceLogin, sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onSetTopicProtection(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		for (final Module m : enabledModules.values()) {
			if (m.onSetTopicProtection(channel, sourceNick, sourceLogin,
					sourceHostname)) {
				break;
			}
		}
	}
	
	@Override
	protected void onTime(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {
		for (final Module m : enabledModules.values()) {
			if (m.onTime(sourceNick, sourceLogin, sourceHostname, target)) {
				break;
			}
		}
	}
	
	@Override
	protected void onTopic(final String channel, final String topic,
			final String setBy, final long date, final boolean changed) {
		for (final Module m : enabledModules.values()) {
			if (m.onTopic(channel, topic, setBy, date, changed)) {
				break;
			}
		}
	}
	
	@Override
	protected void onUnknown(final String line) {
		// try to detect additional types of commands before passing it off as
		// unknown
		IRCMessage msg = new IRCMessage(line);
		if (msg.getCommand().equalsIgnoreCase("KILL")) {
			String userkilled = msg.getParams().get(0);
			String reason = "";
			if (msg.getParams().size() > 1) {
				reason = msg.getParams().get(1);
			}
			onKill(userkilled, reason);
		} else {
			for (final Module m : enabledModules.values()) {
				if (m.onUnknown(msg)) {
					break;
				}
			}
		}
	}
	
	@Override
	protected void onUserList(final String channel, final User[] users) {
		for (final Module m : enabledModules.values()) {
			if (m.onUserList(channel, users)) {
				break;
			}
		}
	}
	
	@Override
	protected void onUserMode(final String targetNick, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String mode) {
		for (final Module m : enabledModules.values()) {
			if (m.onUserMode(targetNick, sourceNick, sourceLogin,
					sourceHostname, mode)) {
				break;
			}
		}
	}
	
	@Override
	protected void onVersion(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {
		for (final Module m : enabledModules.values()) {
			if (m.onVersion(sourceNick, sourceLogin, sourceHostname, target)) {
				break;
			}
		}
	}
	
	@Override
	protected void onVoice(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		for (final Module m : enabledModules.values()) {
			if (m.onVoice(channel, sourceNick, sourceLogin, sourceHostname,
					recipient)) {
				break;
			}
		}
	}
}
