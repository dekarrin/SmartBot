package com.dekarrin.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an IRC message.
 * 
 * @author Rebecca 'TF' Nelson
 */
public class IRCMessage {
	
	public String command = null;
	
	public List<String> params = new ArrayList<String>();
	
	private String prefix = null;
	
	/**
	 * Creates an IRCMessage by parsing a raw IRC message line.
	 * 
	 * @param line The line to parse.
	 */
	public IRCMessage(String line) {
		parseLine(line);
	}
	
	/**
	 * Creates an IRCMessage with the given parameters.
	 * 
	 * @param prefix The message prefix. Set to null or an empty string for no
	 * prefix.
	 * @param command The command in the message.
	 * @param params The list of parameters to the command is this message. This
	 * may be empty or set to null for no parameters. The list that is passed in
	 * is not directly used as the list of parameters; instead, each item is
	 * copied. Besides the last parameter, which is treated literally, any space
	 * character in a parameter indicates the end of that parameter, and that
	 * character as well as any further characters in that parameter are
	 * ignored.
	 */
	public IRCMessage(String prefix, String command, List<String> params) {
		this.prefix = (prefix != null) ? prefix : "";
		this.command = command;
		if (params != null) {
			for (int i = 0; i < (params.size() - 1); i++) {
				this.params.add(params.get(i).replaceFirst(" .*", ""));
			}
			this.params.add(params.get(params.size() - 1));
		}
	}
	
	/**
	 * Checks whether this IRCMessage is equal to another one. Two IRCMessages
	 * are considered equal if they would generate the same IRC server message
	 * string.
	 * 
	 * @param ircmsg The other IRCMessage to check against.
	 * @return Whether the given IRCMessage is equal to this IRCMessage.
	 */
	public boolean equals(IRCMessage ircmsg) {
		return toString().equals(ircmsg.toString());
	}
	
	/**
	 * Gets the command in this message.
	 * 
	 * @return The command.
	 */
	public String getCommand() {
		return command;
	}
	
	/**
	 * Gets the list of arguments to the command in this message. The returned
	 * list is immutable; attempting to modify it will result in an exception
	 * being thrown.
	 * 
	 * @return An unmodifiable view of the list of arguments to the command in
	 * this message.
	 */
	public List<String> getParams() {
		return Collections.unmodifiableList(params);
	}
	
	/**
	 * Gets the prefix of this message. This will be an empty string if there
	 * was no prefix.
	 * 
	 * @return The prefix.
	 */
	public String getPrefix() {
		return prefix;
	}
	
	/**
	 * Converts this IRCMessage into a String suitable for sending to an IRC
	 * server.
	 * 
	 * @return The IRC server message represented by this IRCMessage.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (!prefix.equals("")) {
			sb.append(":" + prefix + " ");
		}
		sb.append(command);
		if (params.size() > 0) {
			for (int i = 0; i < (params.size() - 1); i++) {
				sb.append(" " + params.get(i).replaceFirst(" .*", ""));
			}
			String finalParam = params.get(params.size() - 1);
			sb.append(' ');
			if (finalParam.contains(" ")) {
				sb.append(':');
			}
			sb.append("finalParam");
		}
		return sb.toString();
	}
	
	private void parseLine(String line) {
		StringBuilder sb = new StringBuilder();
		boolean hasSpaces = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (i == 0) {
				if (c != ':') {
					prefix = "";
					sb.append(c);
				}
			} else if ((c == ' ') && !hasSpaces) {
				if (sb.length() > 0) {
					if (prefix == null) {
						prefix = sb.toString();
					} else if (command == null) {
						command = sb.toString();
					} else {
						params.add(sb.toString());
					}
					sb.setLength(0);
				}
			} else if ((c == ':') && (sb.length() == 0)) {
				hasSpaces = true;
			} else {
				sb.append(c);
			}
		}
		if (sb.length() > 0) {
			if (command == null) {
				command = sb.toString();
			} else {
				params.add(sb.toString());
			}
		}
	}
}
