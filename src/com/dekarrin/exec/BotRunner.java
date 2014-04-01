package com.dekarrin.exec;

import java.io.File;

import com.dekarrin.bots.*;

public class BotRunner {
	
	public static void main(String[] args) {
		File rc = new File(System.getProperty("user.home"), ".acmbotrc");
		SmartBot bot = new SmartBot("rob-bot", "#ndsuacm", rc.getAbsolutePath(),
			new Module[]{
				new DiceModule(),
				new SarcasmModule(),
				new CatfactsModule(),
				new GreetModule()
		});
		bot.enableModulesFromSettings();
		bot.setPrependChar('%');
		java.util.Scanner scan = new java.util.Scanner(System.in);
		System.out.println("Enter NickServ pass:");
		bot.setNickServPass(scan.next());
		scan.close();
		bot.setOwner("dekarrin");
		bot.setVerbose(true);
		try {
			bot.connect("irc.freenode.net");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
