package com.dekarrin.exec;

import java.io.File;
import java.net.ConnectException;
import java.util.Scanner;

import com.dekarrin.bots.*;

public class BotRunner {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter NickServ pass:");
		String pass = scan.nextLine();
		File rc = new File(System.getProperty("user.home"), ".acmbotrc");
		SmartBot bot = new SmartBot("rob-bot", "#ndsuacm", rc.getAbsolutePath(),
			new Module[]{
				new DiceModule(),
				new SarcasmModule(),
				new CatfactsModule(),
				new GreetModule(),
				new DateTimeModule()
		});
		bot.enableModulesFromSettings();
		bot.setNickServPass(pass);
		bot.setPrependChar('%');
		bot.setOwner("dekarrin");
		bot.setVerbose(true);
		try {
			boolean connected = false;
			while (!connected) {
				try {
					bot.connect("irc.freenode.org");
					connected = true;
				} catch (ConnectException e) {
					
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
