package com.dekarrin.bots;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jibble.pircbot.DccChat;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.User;

/**
 * Holds BotActions. All of the onX methods can be overridden to specify a
 * behavior that occurs when that action is called. They should return whether
 * they have consumed the event (and thus no more modules should receive it),
 * but should not consume it unless it is necessary.
 * 
 * @author Rebecca 'TF' Nelson
 */
public class Module {
	
	private final Map<String, BotAction> actions;
	
	private final String help;
	
	private final String name;
	
	private final String version;
	
	protected SmartBotInterface bot;
	
	protected Settings settings;
	
	/**
	 * Creates a new BotModule of the given name.
	 * 
	 * @param name The name of the module. Module names follow the same rules as
	 * a Java identifier, with the exception of not being case sensitive.
	 * @param version The version string.
	 * @param help The help String.
	 */
	public Module(final String name, final String version, final String help) {
		this.name = name.toUpperCase();
		this.help = help;
		this.version = version;
		actions = new LinkedHashMap<String, BotAction>();
	}
	
	/**
	 * Adds an action to this BotModule.
	 * 
	 * @param name The command name.
	 * @param action The actual action.
	 * @param help The help string for this command.
	 * @return This BotModule, for chaining.
	 */
	public Module addCommand(final String name, final BotAction action) {
		actions.put(name.toUpperCase(), action);
		return this;
	}
	
	/**
	 * Tries to run a command with this module.
	 * 
	 * @param command The name of the command.
	 * @param params Parameters to this command.
	 * @param data extra data to the command.
	 * @return Whether the command was executed;
	 */
	public boolean execute(final String command, final String[] params,
			final String user, final String recipient) {
		final BotAction action = actions.get(command.toUpperCase());
		if (action == null) {
			return false;
		} else {
			action.execute(params, user, recipient);
			return true;
		}
	}
	
	/**
	 * Gets the help for a command.
	 */
	public String getCommandHelp(final String name) {
		final BotAction ba = actions.get(name.toUpperCase());
		if (ba != null) {
			return ba.help();
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the names of all the commands.
	 */
	public String[] getCommandNames() {
		final String[] names = new String[actions.size()];
		int i = 0;
		for (final String s : actions.keySet()) {
			names[i++] = s;
		}
		return names;
	}
	
	/**
	 * Gets the syntax for a command.
	 */
	public String getCommandSyntax(final String name) {
		final BotAction ba = actions.get(name.toUpperCase());
		if (ba != null) {
			return String.format(ba.syntax(), name);
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the help for this Module.
	 */
	public String getHelp() {
		return help;
	}
	
	/**
	 * Gets the name of this BotModule.
	 * 
	 * @return The name.
	 */
	public String getName() {
		return name.toUpperCase();
	}
	
	/**
	 * Gets the version of this Module.
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Checks whether this BotModule contains a definition for an Action.
	 * 
	 * @param name The command to check.
	 * @return Whether it does.
	 */
	public boolean hasCommand(final String name) {
		return actions.containsKey(name.toUpperCase());
	}
	
	public boolean onAction(final String sender, final String login,
			final String hostname, final String target, final String action) {
		return false;
	}
	
	public boolean onChannelInfo(final String channel, final int userCount,
			final String topic) {
		return false;
	}
	
	public boolean onConnect() {
		return false;
	}
	
	public boolean onDeop(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		return false;
	}
	
	public boolean onDeVoice(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		return false;
	}
	
	public boolean onDisconnect() {
		return false;
	}
	
	public boolean onFileTransferFinished(final DccFileTransfer transfer,
			final Exception e) {
		return false;
	}
	
	public boolean onFinger(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {
		return false;
	}
	
	public boolean onIncomingChatRequest(final DccChat chat) {
		return false;
	}
	
	public boolean onIncomingFileTransfer(final DccFileTransfer transfer) {
		return false;
	}
	
	public boolean onInvite(final String targetNick, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String channel) {
		return false;
	}
	
	public boolean onJoin(final String channel, final String sender,
			final String login, final String hostname) {
		return false;
	}
	
	public boolean onKick(final String channel, final String kickerNick,
			final String kickerLogin, final String kickerHostname,
			final String recipientNick, final String reason) {
		return false;
	}
	
	public boolean onKill(String nick, String reason) {
		return false;
	}
	
	public boolean onMessage(final String channel, final String sender,
			final String login, final String hostname, final String message) {
		return false;
	}
	
	public boolean onMode(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String mode) {
		return false;
	}
	
	/**
	 * Called when this module is disabled.
	 */
	public void onModuleDisabled() {}
	
	/**
	 * Called when this module is enabled.
	 */
	public void onModuleEnabled() {}
	
	public boolean onNickChange(final String oldNick, final String login,
			final String hostname, final String newNick) {
		return false;
	}
	
	public boolean onNotice(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target,
			final String notice) {
		return false;
	}
	
	public boolean onOp(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		return false;
	}
	
	public boolean onPart(final String channel, final String sender,
			final String login, final String hostname) {
		return false;
	}
	
	public boolean onPing(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target,
			final String pingValue) {
		return false;
	}
	
	public boolean onPrivateMessage(final String sender, final String login,
			final String hostname, final String message) {
		return false;
	}
	
	public boolean onQuit(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String reason) {
		return false;
	}
	
	public boolean onRemoveChannelBan(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String hostmask) {
		return false;
	}
	
	public boolean onRemoveChannelKey(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String key) {
		return false;
	}
	
	public boolean onRemoveChannelLimit(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onRemoveInviteOnly(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onRemoveModerated(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onRemoveNoExternalMessages(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onRemovePrivate(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onRemoveSecret(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onRemoveTopicProtection(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onServerPing(final String response) {
		return false;
	}
	
	public boolean onServerResponse(final int code, final String response) {
		return false;
	}
	
	public boolean onSetChannelBan(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String hostmask) {
		return false;
	}
	
	public boolean onSetChannelKey(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String key) {
		return false;
	}
	
	public boolean onSetChannelLimit(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final int limit) {
		return false;
	}
	
	public boolean onSetInviteOnly(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onSetModerated(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onSetNoExternalMessages(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onSetPrivate(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {
		return false;
	}
	
	public boolean onSetSecret(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {
		return false;
	}
	
	public boolean onSetTopicProtection(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {
		return false;
	}
	
	public boolean onTime(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {
		return false;
	}
	
	public boolean onTopic(final String channel, final String topic,
			final String setBy, final long date, final boolean changed) {
		return false;
	}
	
	public boolean onUnknown(final IRCMessage msg) {
		return false;
	}
	
	public boolean onUserList(final String channel, final User[] users) {
		return false;
	}
	
	public boolean onUserMode(final String targetNick, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String mode) {
		return false;
	}
	
	public boolean onVersion(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {
		return false;
	}
	
	public boolean onVoice(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {
		return false;
	}
	
	/**
	 * Sets the bot associated with this module, as well as the persistence
	 * settings object.
	 */
	public void setBot(final SmartBotInterface bot, Settings settings) {
		this.bot = bot;
		this.settings = settings;
		onModuleAdded();
	}
	
	/**
	 * Called when this module has been added to a bot. This method is called
	 * once both the bot and settings variables have been set. Modules should
	 * use this method to read settings and set internal state.
	 */
	protected void onModuleAdded() {}
}
