package com.dekarrin.bots;

import java.util.ArrayList;
import java.util.List;


public class GreetModule extends BotModule {
	
	public GreetModule() {
		super("GREET");
	}
	
	@Override
	public void onJoin(String channel, String sender, String login, String hostname) {
		List<String> greets = new ArrayList<String>();
		greets.add("Ahoy there, %s!");
		greets.add("Hello, %s.");
		greets.add("Yo, 'sup, %s?");
		greets.add("Salutations, %s.");
		greets.add("Is that %s? How the heck are you?");
		greets.add("Greetings, %s.");
		greets.add("Hi %s");
		greets.add("Well, if it isn't my nemesis, Dr. %s.");
		greets.add("Ah, my good friend %s! It's good to see you've returned.");
		greets.add("Hiya~ %s");
		greets.add("Welcome to the channel, %s");
		greets.add("Well, hi-de-ho, neighbor %s!");
		greets.add("Ah, %s. It's been too long.");
		greets.add("%s, you're here! Welcome!");
		greets.add("Nice to see you, %s");
		greets.add("And here we have the most important person in this room: %s");
		greets.add("What's up, %s?");
		greets.add("Ah, it's the legendary %s");
		greets.add("The hero of time, %s, has come to save us from Ganon!");
		if (!sender.equals(bot.getNick())) {
			int msgIndex = (new java.util.Random()).nextInt(greets.size());
			String msg = String.format(greets.get(msgIndex), sender);
			bot.sendMessage(bot.getIntendedChannel(), msg);
		}
	}
	
}
