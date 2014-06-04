package com.dekarrin.exec;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;

import com.dekarrin.bots.*;

public class BotRunner {
	
	public static void main(String[] args) {
		if (System.console() == null) {
			System.err.println("Error: Not connected to compatible console");
			System.exit(1);
		}
		File rc = new File(System.getProperty("user.home"), ".acmbotrc");
		boolean firstTime = !rc.exists();
		SmartBot bot = new SmartBot(rc.getAbsolutePath(),
			new Module[]{
				new DiceModule(),
				new SarcasmModule(),
				new CatfactsModule(),
				new GreetModule(),
				new DateTimeModule(),
				new GamblerModule()
		});
		if (firstTime) {
			firstTimeSetup(bot);
		}
		String extraMsg = "";
		for (String s : args) {
			if (!parseOption(s, bot)) {
				extraMsg = s;
			}
		}
		bot.setStartupMessage(extraMsg);
		bot.enableModulesFromSettings();
		bot.setVerbose(true);
		try {
			boolean connected = false;
			while (!connected) {
				try {
					bot.connect();
					connected = true;
				} catch (ConnectException e) {
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static boolean parseOption(String opStr, SmartBot bot) {
		if (opStr.length() > 1 && opStr.charAt(0) == '-') {
			if (opStr.charAt(1) == '-' && opStr.length() > 2) {
				execLongOption(opStr.substring(2), bot);
			} else {
				for (int i = 1; i < opStr.length(); i++) {
					execOption(opStr.charAt(i), bot);
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	private static void execLongOption(String op, SmartBot bot) {
		if (op.equals("help")) {
			execOption('h', bot);
		} else if (op.equals("console")) {
			execOption('c', bot);
		}
	}
	
	private static void execOption(char op, SmartBot bot) {
		switch (op) {
		case 'h':
			printHelp();
			System.exit(0);
			break;
			
		case 'c':
			bot.startConsole();
			break;
		}
	}
	
	private static void printHelp() {
		System.out.println("SmartBot");
		System.out.println();
		System.out.println("Options:");
		System.out.println("-h, --help      Show this help");
		System.out.println("-c, --console   Start with interactive command console");
	}
	
	// returns sha-256 hash of password
	private static void firstTimeSetup(SmartBot bot) {
		Console console = System.console();
		System.out.println("It looks like this is the first time you've run SmartBot on this system.");
		System.out.println("Let's go ahead and set up your bot!");
		System.out.println();
		System.out.println("Location");
		System.out.println("========");
		String server = readString("Server");
		String channel = '#' + readString("Channel").replaceAll("#", "");
		System.out.println();
		System.out.println("Identity");
		System.out.println("========");
		System.out.println("The bot nick is the nickname that SmartBot will identify itself as on the");
		System.out.println("server.");
		String nick = readString("Nick");
		System.out.println("The real name is what SmartBot will use in response to a FINGER command.");
		String finger = readString("Real name");
		System.out.println("The local username is placed before the @ in the address shown in WHOIS");
		System.out.println("output.");
		String login = readString("Local username");
		System.out.println("Some networks allow nick authentication with NickServ. If you are joining");
		System.out.println("such a network, you may enter a password now.");
		char[] pass = console.readPassword("NickServ pass (blank for untrusted/unused NickServ): ");
		System.out.println();
		System.out.println("Owner");
		System.out.println("=====");
		System.out.println("The owner is a bot operator that can never be de-oped.");
		String owner = readString("Nick of bot owner");
		System.out.println();
		System.out.println("Prepend Character");
		System.out.println("=================");
		System.out.println("The prepend character is a special character that SmartBot can listen for.");
		System.out.println("If any message in the channel that SmartBot is in starts with that character,");
		System.out.println("the remainder of the message will be interpreted as a command.");
		System.out.println("Without using a prepend character, all commands will have to be sent to");
		System.out.println("SmartBot by private message.");
		boolean usePrepend = readBoolean("Use prepend character?", true);
		char prependChar = '%';
		if (usePrepend) {
			prependChar = readChar("Command prepend character", '%');
		}
		System.out.println();
		System.out.println("Anti-Flood");
		System.out.println("==========");
		int msgDelay = readInt("Time in milliseconds between bot messages sent to server", 1000);
		int reconLimit = readInt("Max reconnection attempts", 3);
		int reconTime = readInt("Time in seconds between reconnections", 15);
		System.out.println();
		bot.setServer(server);
		bot.setChannel(channel);
		bot.setBotNick(nick);
		bot.setBotFinger(finger);
		bot.setBotLogin(login);
		bot.setNickServPass(pass);
		bot.setBotMessageDelay(msgDelay);
		bot.setReconnectionLimit(reconLimit);
		bot.setTimeBetweenReconnects(reconTime);
		bot.setUsePrepend(usePrepend);
		bot.setPrependChar(prependChar);
		bot.setOwner(owner);
		System.out.println("Setup complete!");
		System.out.println("Settings are saved in the file .acmbotrc in your home directory.");
		readString("(press enter to continue)");
	}
	
	private static char readChar(String prompt, char defaultChar) {
		char val = '\0';
		boolean set = false;
		while (!set) {
			String str = readString(prompt + " (default '" + defaultChar + "')");
			if (str.length() < 1) {
				val = defaultChar;
			} else {
				val = str.charAt(0);
			}
			set = true;
		}
		return val;
	}
	
	private static int readInt(String prompt, int defaultInt) {
		int val = 0;
		boolean set = false;
		while (!set) {
			try {
				String s = readString(prompt + " (default " + defaultInt + ")");
				if (s.equals("")) {
					val = defaultInt;
				} else {
					val = Integer.parseInt(s);
				}
				set = true;
			} catch (NumberFormatException e) {
				System.err.println("Please enter an integer");
			}
		}
		return val;
	}
	
	private static String readString(String prompt) {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print(prompt + ": ");
		String read = null;
		try {
			read = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return read;
	}
	
	private static boolean readBoolean(String prompt, boolean defaultVal) {
		boolean val = false;
		boolean set = false;
		while (!set) {
			String s = readString(prompt + " (T/F) (default " + (defaultVal ? "T" : "F") + ")");
			if (s.length() > 0) {
				if (s.charAt(0) == 't' || s.charAt(0) == 'T') {
					val = true;
					set = true;
				} else if (s.charAt(0) == 'f' || s.charAt(0) == 'F') {
					val = false;
					set = true;
				} else {
					System.err.println("Please enter T or F");
				}
			} else {
				val = defaultVal;
				set = true;
			}
		}
		return val;
	}
	
}
