package com.dekarrin.exec;

import com.dekarrin.bots.*;

public class BotRunner {
	
	public static void main(String[] args) {
		SmartBot bot = new SmartBot("rob-bot", "#ndsuacm", new BotModule[]{
				new DiceModule(),
				new SarcasmModule()
		});
		bot.setPrependChar('%');
		java.util.Scanner scan = new java.util.Scanner(System.in);
		System.out.println("Enter NickServ pass:");
		bot.setNickServPass(scan.next());
		bot.setOwner("dekarrin");
		bot.setVerbose(true);
		try {
			bot.connect("irc.freenode.net");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
