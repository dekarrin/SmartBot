package com.dekarrin.bots;

/**
 * Holds a command action for a bot to execute.
 * @author Rebecca 'TF' Nelson
 */
public interface BotAction {
	
	/**
	 * Executes the action.
	 * 
	 * @param params The parameters to the action.
	 * @param data Extra data to the command. May be null.
	 * @return Whether the command executed successfully.
	 */
	public void execute(String[] params, String sender, String recipient);
	
	/**
	 * Gets the help for this action.
	 * 
	 * @return The help.
	 */
	public String help();
	
	/**
	 * Gets the syntax listing for this action. This does not include the name
	 * of the command; only additional parameters. The command name may be
	 * inserted by using %s.
	 */
	public String syntax();
	
}
