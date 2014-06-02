package com.dekarrin.exec;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.util.Scanner;

import com.dekarrin.bots.*;

public class BotRunner {
	
	public static void main(String[] args) {
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
		bot.startConsole();
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
	
	private static void firstTimeSetup(SmartBot bot) {
		System.out.println("It looks like this is the first time you've run SmartBot on this system.");
		System.out.println("Let's go ahead and set up your bot!");
		System.out.println();
		System.out.println("Bot Location");
		bot.setServer(readString("Server"));
		bot.setChannel('#' + readString("Channel").replaceAll("#", ""));
		System.out.println();
		System.out.println("Bot Identity");
		bot.setBotNick(readString("Nick"));
		bot.setBotFinger(readString("Finger"));
		bot.setBotLogin(readString("Login"));
		bot.setNickServPass(readString("NickServ pass (blank for untrusted NickServ)"));
		System.out.println();
		System.out.println("Misc.");
		bot.setPrependChar(readChar("Command prepend char"));
		bot.setUsePrepend(true);
		bot.setReconnectionLimit(readInt("Max reconnection attempts"));
		bot.setTimeBetweenReconnects(readInt("Time between reconnections"));
		bot.setOwner(readString("Nick of bot owner"));
		System.out.println();
		System.out.println("Setup complete!");
		System.out.println("Settings are saved in the file .acmbotrc in your home directory.");
		readString("(press enter to continue)");
	}
	
	private static char readChar(String prompt) {
		char val = '\0';
		boolean set = false;
		while (!set) {
			String str = readString(prompt);
			if (str.length() < 1) {
				System.err.println("You must enter a character!");
			} else {
				val = str.charAt(0);
				set = true;
			}
		}
		return val;
	}
	
	private static int readInt(String prompt) {
		int val = 0;
		boolean set = false;
		while (!set) {
			try {
				val = Integer.parseInt(readString(prompt));
				set = true;
			} catch (NumberFormatException e) {
				System.err.println("You must enter an integer!");
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
	
}
