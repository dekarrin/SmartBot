package com.dekarrin.bots;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class CatfactsModule extends Module {
	
	private double odds = 0.05;
	
	public CatfactsModule() {
		super("CATFACTS", "v1.1", "Gives cat facts every so often.");
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
							settings.setModuleSetting(getName(), "odds", ""+odds);
							bot.sendMessage(recipient, sender + ": changed cat fact odds to " + odds);
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
				return "Sets the odds of generating a cat fact";
			}

			@Override
			public String syntax() {
				return "%s [odds]";
			}
		});
		addCommand("GETODDS", new BotAction () {

			@Override
			public void execute(String[] params, String sender, String recipient) {
				bot.sendMessage(recipient, sender + ": odds of a cat fact are " + odds);
			}

			@Override
			public String help() {
				return "Gets the odds of a cat fact";
			}

			@Override
			public String syntax() {
				return "%s";
			}
			
		});
	}
	
	@Override
	protected void onModuleAdded() {
		String val = settings.getModuleSetting(getName(), "odds");
		if (val != null) {
			try {
				odds = Double.parseDouble(val);
			} catch (NumberFormatException e) {
				// do nothing
			}
		}
	}
	
	@Override
	public boolean onMessage(String channel, String sender, String login, String hostname, String message) {
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
				fact = cleanFact(input.substring(0, input.length() - 1));
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			bot.sendMessage(bot.getChannel(), "Catfact: " + fact);
			return true;
		} else {
			return false;
		}
	}
	
	private String cleanFact(String catfact) {
		StringBuilder sb = new StringBuilder();
		boolean escaping = false;
		boolean unicodeSeq = false;
		char[] unicodeDigits = new char[4];
		int unicodeSeqCount = 0;
		for (int i = 0; i < catfact.length(); i++) {
			char c = catfact.charAt(i);
			if (!escaping) {
				if (c == '\\') {
					escaping = true;
				} else {
					sb.append(c);
				}
			} else {
				if (unicodeSeq) {
					if (unicodeSeqCount < 4) {
						unicodeDigits[unicodeSeqCount] = c;
						unicodeSeqCount++;
					} else {
						char uni = translateUnicodeSequence(unicodeDigits);
						sb.append(uni);
						unicodeSeq = false;
						escaping = false;
					}
				} else {
					escaping = false;
					if (c == 'u') {
						escaping = true;
						unicodeSeq = true;
						unicodeSeqCount = 0;
					} else if (c == '\\') {
						sb.append(c);
					} else if (c == '"') {
						sb.append(c);
					}
				}
			}
		}
		return sb.toString();
	}
	
	private char translateUnicodeSequence(char[] sequence) {
		String hex = new String(sequence);
		int value = Integer.parseInt(hex, 16);
		return (char)value;
	}
	
}
