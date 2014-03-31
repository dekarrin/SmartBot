package com.dekarrin.bots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class CatfactsModule extends BotModule {
	
	private double odds = 0.05;
	
	public CatfactsModule() {
		super("FACTS");
		addCommand("SETODDS", new BotAction () {

			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (params.length > 0) {
					try {
						double odds = Double.parseDouble(params[0]);
						if (odds < 0 || odds > 1.0) {
							bot.sendMessage(recipient, sender + ": odds must be between 0 and 1");
						} else {
							CatfactsModule.this.odds = odds;
							bot.sendMessage(recipient, sender + ": changed funfact odds to " + odds);
						}
					} catch (NumberFormatException e) {
						bot.sendBadSyntax(recipient, sender);
					}
				} else {
					bot.sendBadSyntax(recipient, sender);
				}
			}

			@Override
			public String help() {
				return "Sets the odds of generating a funfact";
			}

			@Override
			public String syntax() {
				return "%s [odds]";
			}
		});
	}
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
		if (Math.random() < odds) {
			URL url = null;
			String fact = "";
			try {
				url = new URL("http://catfacts-api.appspot.com/api/facts?number=1");
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String input = "";
				String line;
				while ((line = in.readLine()) != null) {
					input += line;
				}
				in.close();
				input = input.replaceAll(".*\\[", "").replaceAll("].*", "").substring(1);
				fact = input.substring(0, input.length() - 1);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			bot.sendMessage(bot.getIntendedChannel(), "Funfact: " + fact);
		}
	}
	
}
