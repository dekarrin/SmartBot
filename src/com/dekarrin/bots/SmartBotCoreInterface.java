package com.dekarrin.bots;

import java.util.Set;

/**
 * For the core module to interact directly with other module.
 * 
 * @author Rebecca 'TF' Nelson
 */
public interface SmartBotCoreInterface extends SmartBotInterface {
	
	/**
	 * Gets the names of the modules currently loaded in this bot. This set
	 * cannot be changed.
	 * 
	 * @return The set of names.
	 */
	public Set<String> getModuleNames();
	
	/**
	 * Checks whether a module is enabled.
	 * 
	 * @param name The module to check. Case-insensitive.
	 * @return Whether it is.
	 */
	public boolean getModuleEnabled(String name);
	
	/**
	 * Enables/disables a module.
	 * 
	 * @param name The module to set.
	 * @param enabled Whether it is enabled.
	 */
	public void setModuleEnabled(String name, boolean enabled);
	
	/**
	 * Checks whether a module is loaded.
	 * 
	 * @param name The module to check. Case-insensitive.
	 * @return Whether it is loaded.
	 */
	public boolean hasModule(String name);
	
	/**
	 * Gets a module directly. The core module may NOT be accessed this way.
	 * 
	 * @param name The name of the module to get.
	 * @return The module.
	 */
	public Module getModule(String name);
	
	/**
	 * Sends a message that the user referred to a module that does not exist.
	 * 
	 * @param recipient Where to send it.
	 * @param user Who to tell.
	 * @param module Module that is being spoken of.
	 */
	public void sendNoSuchModule(String recipient, String user, String module);
	
	/**
	 * Checks if an operator exists.
	 * 
	 * @param name The name of the operator to check.
	 */
	public boolean hasOperator(String name);
	
	/**
	 * Adds an operator.
	 * 
	 * @param name The name of the operator to add.
	 */
	public void addOperator(String name);
	
	/**
	 * Removes an operator.
	 * 
	 * @param name The name of the operator to remove.
	 */
	public void removeOperator(String name);
	
	/**
	 * Gets an immutable list of operators.
	 * 
	 * @return The names of the operators.
	 */
	public Set<String> getOperators();
	
	/**
	 * Gets the owner of this bot.
	 * 
	 * @return The bot owner.
	 */
	public String getOwner();
}
