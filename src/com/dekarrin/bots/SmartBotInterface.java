package com.dekarrin.bots;

/**
 * Passed to modules for interacting with SmartBot.
 */
public interface SmartBotInterface {
	
	/**
	 * Disconnects cleanly from the server and shuts down bot.
	 * 
	 * @param reason The reason for disconnecting.
	 */
	public void disconnect(String reason);
	
	/**
	 * Gets the version of the bot.
	 */
	public String getVersion();
	
	/**
	 * Gets the channel that the bot was intended for.
	 * 
	 * @return The channel.
	 */
	public String getChannel();
	
	/**
	 * Checks whether a user is authorized to perform privileged operations on
	 * this bot.
	 * 
	 * @param user The user to check.
	 * @return Whether the user has authorization.
	 */
	public boolean isAuthorized(String user);
	
	/**
	 * Sends a message that the user had bad syntax.
	 * 
	 * @param recipient Where to send it to.
	 * @param user The user to tell had bad syntax.
	 */
	public void sendBadSyntax(String recipient, String user);
	
	/**
	 * Sends a message.
	 * 
	 * @param recipient Where to send it to.
	 * @param message The message to send.
	 */
	public void sendMessage(String recipient, String message);
	
	/**
	 * Sends a message that the user is not authorized.
	 * 
	 * @param recipient Where to send it to.
	 * @param user The user to tell had bad syntax.
	 */
	public void sendNotAuthorized(String recipient, String user);
	
	/**
	 * Gets the nick of this SmartBot.
	 * 
	 * @return The nickname.
	 */
	public String getNick();
}
