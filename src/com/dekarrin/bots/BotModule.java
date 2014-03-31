package com.dekarrin.bots;

import java.util.HashMap;
import java.util.Map;

import org.jibble.pircbot.DccChat;
import org.jibble.pircbot.DccFileTransfer;
import org.jibble.pircbot.User;

/**
 * Holds BotActions.
 * 
 * @author Rebecca 'TF' Nelson
 */
public class BotModule {
	
	private final Map<String, BotAction> actions;
	
	private final String name;
	
	protected SmartBot bot;
	
	/**
	 * Creates a new BotModule of the given name.
	 * 
	 * @param name The name.
	 */
	public BotModule(final String name) {
		this.name = name;
		actions = new HashMap<String, BotAction>();
	}
	
	/**
	 * Adds an action to this BotModule.
	 * 
	 * @param name The command name.
	 * @param action The actual action.
	 * @param help The help string for this command.
	 * @return This BotModule, for chaining.
	 */
	public BotModule addCommand(final String name, final BotAction action) {
		actions.put(name.toUpperCase(), action);
		return this;
	}
	
	/**
	 * Adds a trigger to this BotModule.
	 */
	public BotModule addTrigger() {
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
	 * Gets the help for a command.
	 */
	public String getHelp(final String name) {
		final BotAction ba = actions.get(name.toUpperCase());
		if (ba != null) {
			return ba.help();
		} else {
			return null;
		}
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
	 * Gets the syntax for a command.
	 */
	public String getSyntax(final String name) {
		final BotAction ba = actions.get(name.toUpperCase());
		if (ba != null) {
			return String.format(ba.syntax(), name);
		} else {
			return null;
		}
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
	
	public void onAction(final String sender, final String login,
			final String hostname, final String target, final String action) {}
	
	public void onChannelInfo(final String channel, final int userCount,
			final String topic) {}
	
	public void onConnect() {}
	
	public void onDeop(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {}
	
	public void onDeVoice(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {}
	
	public void onDisconnect() {}
	
	public void onFileTransferFinished(final DccFileTransfer transfer,
			final Exception e) {}
	
	public void onFinger(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {}
	
	public void onIncomingChatRequest(final DccChat chat) {}
	
	public void onIncomingFileTransfer(final DccFileTransfer transfer) {}
	
	public void onInvite(final String targetNick, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String channel) {}
	
	public void onJoin(final String channel, final String sender,
			final String login, final String hostname) {}
	
	public void onKick(final String channel, final String kickerNick,
			final String kickerLogin, final String kickerHostname,
			final String recipientNick, final String reason) {}
	
	public void onMessage(final String channel, final String sender,
			final String login, final String hostname, final String message) {}
	
	public void onMode(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String mode) {}
	
	public void onNickChange(final String oldNick, final String login,
			final String hostname, final String newNick) {}
	
	public void onNotice(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target,
			final String notice) {}
	
	public void onOp(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {}
	
	public void onPart(final String channel, final String sender,
			final String login, final String hostname) {}
	
	public void onPing(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target,
			final String pingValue) {}
	
	public void onPrivateMessage(final String sender, final String login,
			final String hostname, final String message) {}
	
	public void onQuit(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String reason) {}
	
	public void onRemoveChannelBan(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String hostmask) {}
	
	public void onRemoveChannelKey(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String key) {}
	
	public void onRemoveChannelLimit(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onRemoveInviteOnly(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onRemoveModerated(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onRemoveNoExternalMessages(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onRemovePrivate(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {}
	
	public void onRemoveSecret(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {}
	
	public void onRemoveTopicProtection(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onServerPing(final String response) {}
	
	public void onServerResponse(final int code, final String response) {}
	
	public void onSetChannelBan(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String hostmask) {}
	
	public void onSetChannelKey(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String key) {}
	
	public void onSetChannelLimit(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname, final int limit) {}
	
	public void onSetInviteOnly(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {}
	
	public void onSetModerated(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {}
	
	public void onSetNoExternalMessages(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onSetPrivate(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {}
	
	public void onSetSecret(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname) {}
	
	public void onSetTopicProtection(final String channel,
			final String sourceNick, final String sourceLogin,
			final String sourceHostname) {}
	
	public void onTime(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {}
	
	public void onTopic(final String channel, final String topic,
			final String setBy, final long date, final boolean changed) {}
	
	public void onUnknown(final String line) {}
	
	public void onUserList(final String channel, final User[] users) {}
	
	public void onUserMode(final String targetNick, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String mode) {}
	
	public void onVersion(final String sourceNick, final String sourceLogin,
			final String sourceHostname, final String target) {}
	
	public void onVoice(final String channel, final String sourceNick,
			final String sourceLogin, final String sourceHostname,
			final String recipient) {}
	
	/**
	 * Sets the bot associated with this module.
	 */
	public void setBot(final SmartBot bot) {
		this.bot = bot;
	}
}
