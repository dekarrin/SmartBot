package com.dekarrin.bots;

import java.util.ArrayList;
import java.util.List;


public class GreetModule extends Module {
	
	private List<String> greets = new ArrayList<String>();
	
	public GreetModule() {
		super("GREET", "v1.0.5", "Gives a greeting when a user joins the channel.");
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
		greets.add("Ooh! The hero of time, %s, has come to save us from Ganon!");
		greets.add("Expert coder %s! Hello!");
		greets.add("Leet haxor %s in the house!");
		greets.add("We've been graced with the presence of %s. Excellent!");
		greets.add("Ah, good. You're finally here, %s.");
		greets.add("All right, %s is here! Now the party can get started!");
		greets.add("Well, well, well. If it isn't %s.");
		greets.add("And the room got a little brighter as %s stepped into it. Glad to see you :)");
		greets.add("Oh hey! It's %s!");
		greets.add("omg omg OMG OMG! %1$s is here! %1$s is so cool ^_^");
		greets.add("I'd like to offer a warm welcome to %s.");
		greets.add("Announcing: %s! Say hello everybody!");
		greets.add("Lookit dat %s, dropping science like Galileo dropped the orange.");
		greets.add("<plays fanfare on monocle for %s's entrance>");
		greets.add("Oh, %s. If I'd have known you were coming, I would have put on something a bit nicer.");
		greets.add("I've been waiting for you, %s.");
		greets.add("Boy, I remember when me and %s were sittin' around, playin' Super Smash Bros.");
		greets.add("Shalom, my friend %s.");
		greets.add("Revenge is a dish best served cold. Unless you're %s. Interpret that as you will.");
		greets.add("I'd like to see Cosmos hosted by %s. I'm just sayin'.");
	}
	
	@Override
	public boolean onJoin(String channel, String sender, String login, String hostname) {
		if (!sender.equals(bot.getNick())) {
			int msgIndex = (new java.util.Random()).nextInt(greets.size());
			String msg = String.format(greets.get(msgIndex), sender);
			bot.sendMessage(bot.getChannel(), msg);
			return true;
		} else {
			return false;
		}
	}
	
}
