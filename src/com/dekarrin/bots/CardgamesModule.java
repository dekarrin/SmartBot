package com.dekarrin.bots;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CardgamesModule extends Module {
	
	// measured in seconds
	public static final int DEFAULT_CHALLENGE_TIMEOUT = 1800;
	
	private static class Challenge {
		
		public final String challenger;
		
		private final List<String> opponents = new ArrayList<String>();
		
		private final boolean[] answers;
		
		public final String gameName;
		
		public final long startTime;
		
		public Challenge(String challenger, String[] opponents, String gameName) {
			this.startTime = System.currentTimeMillis();
			this.challenger = challenger;
			this.gameName = gameName;
			this.answers = new boolean[opponents.length];
			for (int i = 0; i < opponents.length; i++) {
				this.opponents.add(opponents[i]);
				answers[i] = false;
			}
		}
		
		public void accept(String opponent) {
			for (int i = 0; i < opponents.size(); i++) {
				if (opponents.get(i).toUpperCase().equals(opponent.toUpperCase())) {
					answers[i] = true;
					break;
				}
			}
		}
		
		public boolean isAcceptedByAll() {
			for (int i = 0; i < answers.length; i++) {
				if (!answers[i]) {
					return false;
				}
			}
			return true;
		}
		
		public boolean containsOpponent(String opponent) {
			return opponents.contains(opponent);
		}
		
		public boolean isTo(String[] opponents) {
			boolean to = false;
			if (this.opponents.size() == opponents.length) {
				to = true;
				for (String op : opponents) {
					if (!containsOpponent(op)) {
						to = false;
						break;
					}
				}
			}
			return to;
		}
	}
	
	private Thread cleanerThread = null;
	
	private Map<String, List<CardGame>> activeGames = new HashMap<String, List<CardGame>>();
	
	private Map<String, List<Challenge>> challenges = new HashMap<String, List<Challenge>>();
	
	private Map<String, GameData> gameTypes = new HashMap<String, GameData>();
	
	// this is in seconds
	private volatile int challengeTimeout;
	
	public CardgamesModule() {
		super("CARDGAMES", "v0.1", "Challenge friends to card games");
		challengeTimeout = DEFAULT_CHALLENGE_TIMEOUT;
		cleanerThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
						List<Integer> toRemove = new ArrayList<Integer>();
						synchronized (challenges) {
							for (List<Challenge> game : challenges.values()) {
								for (int i = 0; i < game.size(); i++) {
									Challenge c = game.get(i);
									if (System.currentTimeMillis() - c.startTime >= (challengeTimeout * 1000)) {
										toRemove.add(i);
									}
								}
								for (int i : toRemove) {
									game.remove(i);
								}
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
			
		}, "Challenge-Clean");
		cleanerThread.start();
		addAllGameTypes();
		addCommand("CHALLENGE", new BotAction() {
			
			@Override
			public String syntax() {
				return "%s [game] [oppenent1] <...[opponentN]>";
			}
			
			@Override
			public String help() {
				return "Challenge someone to a card game.";
			}
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (params.length < 2) {
					bot.sendBadSyntax(recipient, sender);
				} else {
					String rawGame = params[0].toUpperCase();
					GameData game = gameTypes.get(rawGame);
					if (game == null) {
						bot.sendMessage(recipient, "'" + rawGame + "' is not a playable game. Use GAMES for a list.");
					} else {
						String[] players = new String[params.length - 1];
						for (int i = 1; i < params.length; i++) {
							players[i - 1] = params[i];
						}
						challenge(recipient, sender, players, game);
					}
				}
			}
		});
	}
	
	/**
	 * Adds all games to the games map.
	 */
	private void addAllGameTypes() {
		addGameType("GOFISH", GoFishGame.DATA);
	}
	
	private void addGameType(String key, GameData data) {
		data.key = key;
		gameTypes.put(key, data);
	}
	
	/**
	 * 
	 * @param context Where the message came from. Will be a nick if it came from
	 * a private message or a channel if it came from public chat.
	 * @param challengerNick The nickname that the challenger is using
	 * @param opponentNicks The nicknames of the opponents.
	 * @param game Data about the game that the challenge is for. 
	 */
	private void challenge(String context, String challengerNick, String[] opponentNicks, GameData game) {
		if (opponentNicks.length < game.minPlayers || opponentNicks.length > game.maxPlayers) {
			String s = (game.maxPlayers != 1) ? "s" : "";
			String playersStr = game.minPlayers + "";
			if (game.minPlayers != game.maxPlayers) {
				playersStr += "-" + game.maxPlayers;
			}
			playersStr += " player" + s;
			bot.sendMessage(context, game.name + " is for " + playersStr + ".");
			return;
		}
		String challengerReg = bot.getRegisteredNick(challengerNick);
		String[] opponentRegs = new String[opponentNicks.length];
		List<String> badOpps = new ArrayList<String>();
		for (int i = 0; i < opponentRegs.length; i++) {
			opponentRegs[i] = bot.getRegisteredNick(opponentNicks[i]);
			if (opponentRegs[i] == null) {
				badOpps.add(opponentNicks[i]);
			}
		}
		if (challengerReg == null) {
			bot.sendMessage(context, "You must be logged in with NickServ to challenge oppenents.");
			return;
		}
		if (badOpps.size() > 0) {
			bot.sendMessage(context, toSeries(badOpps) + " must log in with NickServ before being challenged.");
			return;
		}
		if (gameExists(challengerReg, opponentRegs, game.name)) {
			bot.sendMessage(context, "You're already in a game with " + toSeries(opponentNicks) + ".");
			return;
		}
		for (int i = 0; i < opponentRegs.length; i++) {
			if (inChallenge(opponentRegs[i], challengerReg, game.name)) {
				bot.sendMessage(context, opponentNicks[i] + " has already challenged you. " + gameCmd + "-ACCEPT or " + gameCmd + "-REJECT to respond.");
				return;
			}
		}
		if (challengeExists(challengerReg, opponentRegs, game.name)) {
			bot.sendMessage(opponentNick, challengerNick + " is waiting for your response. " + gameCmd + "-ACCEPT or " + gameCmd + "-REJECT to respond.");
			bot.sendMessage(challengerNick, opponentNick + " has been reminded of your challenge.");
			return;
		}
		addChallenge(challengerReg, opponentReg, gameName);
		bot.sendMessage(opponentNick, "You've been challenged to " + gameName + " by " + challengerNick + ". " + gameCmd + "-ACCEPT or " + gameCmd + "-REJECT to respond.");
		bot.sendMessage(challengerNick, "You've sent out your challenge. Now awaiting " + opponentNick + "'s response.");
		bot.sendMessage(bot.getChannel(), challengerNick + " has challenged " + opponentNick + " to a game of " + gameName + "!");
	}
	
	private String toSeries(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			String s = list.get(i);
			sb.append(s);
			if (i + 1 < list.size()) {
				sb.append(", ");
				if (i + 2 == list.size()) {
					sb.append("and ");
				}
			}
		}
		return sb.toString();
	}
	
	private String toSeries(String[] list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.length; i++) {
			String s = list[i];
			sb.append(s);
			if (i + 1 < list.length) {
				sb.append(", ");
				if (i + 2 == list.length) {
					sb.append("and ");
				}
			}
		}
		return sb.toString();
	}
	
	private void addChallenge(String challenger, String opponent, String gameName) {
		Challenge chal = new Challenge(challenger, opponent, gameName);
		synchronized (challenges) {
			challenges.put(chal.getPlayerKey(), chal);
		}
	}
	
	private boolean inChallenge(String challenger, String opponent, String gameName) {
		boolean inChal = false;
		synchronized (challenges) {
			List<Challenge> game = challenges.get(gameName);
			if (game != null) {
				for (Challenge c : game) {
					if (c.challenger.equalsIgnoreCase(challenger) && c.containsOpponent(opponent)) {
						inChal = true;
						break;
					}
				}
			}
		}
		return inChal;
	}
	
	private boolean challengeExists(String challenger, String[] opponents, String gameName) {
		boolean exists = false;
		synchronized (challenges) {
			List<Challenge> game = challenges.get(gameName);
			if (game != null) {
				for (Challenge c : game) {
					if (c.challenger.equalsIgnoreCase(challenger) && c.isTo(opponents)) {
						exists = true;
					}
				}
			}
		}
		return exists;
	}
	
	private boolean gameExists(String challenger, String opponent, String gameName) {
		String playerKey = createPlayerKey(challenger, opponent);
		List<CardGame> games = activeGames.get(playerKey);
		if (games != null) {
			boolean running = false;
			for (CardGame game : games) {
				if (game.getName().equals(gameName)) {
					running = true;
					break;
				}
			}
			return running;
		} else {
			return false;
		}
	}

}

class GameData {
	
	public final String name;
	
	public final String objective;
	
	public final int minPlayers;
	
	public final int maxPlayers;
	
	public String key;
	
	public GameData(String name, String objective, int minPlayers, int maxPlayers) {
		this.name = name;
		this.objective = objective;
		this.minPlayers = minPlayers;
		this.maxPlayers = maxPlayers;
	}
}

enum FrenchSuit {
	CLUBS,
	DIAMONDS,
	HEARTS,
	SPADES
}

interface Card {
	
}

class FrenchCard implements Card {
	
	public static final int RANK_ACE = 1;
	public static final int RANK_JACK = 11;
	public static final int RANK_QUEEN = 12;
	public static final int RANK_KING = 13;

	public final FrenchSuit suit;
	
	public final int rank;
	
	public FrenchCard(final int rank, final FrenchSuit suit) {
		this.suit = suit;
		this.rank = rank;
	}

}

abstract class Deck<T extends Card> {
	
	private Deque<T> cards = new ArrayDeque<T>();
	
	private int startSize;
	
	public Deck(int size) {
		startSize = size;
		while (cards.size() < fullSize()) {
			cards.push(generateCard());
		}
	}
	
	public int size() {
		return cards.size();
	}
	
	public List<List<T>> deal(int count, int numHands) {
		List<List<T>> hands = new ArrayList<List<T>>();
		for (int i = 0; i < numHands; i++) {
			hands.add(new ArrayList<T>());
		}
		for (int i = 0; i < count; i++) {
			for (int j = 0; j < numHands; j++) {
				hands.get(j).add(draw());
			}
		}
		return hands;
	}
	
	public int fullSize() {
		return startSize;
	}
	
	protected abstract T generateCard();
	
	public T draw() {
		return cards.pop();
	}
	
	public List<T> draw(int count) {
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < count; i++) {
			list.add(cards.pop());
		}
		return list;
	}
	
	public T check() {
		return cards.peek();
	}
	
	public List<T> check(int num) {
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < num; i++) {
			list.add(cards.pop());
		}
		for (T card : list) {
			cards.push(card);
		}
		return list;
	}
	
	public void shuffle() {
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < cards.size(); i++) {
			list.add(cards.pop());
		}
		Collections.shuffle(list);
		for (int i = 0; i < list.size(); i++) {
			cards.push(list.get(i));
		}
	}
}

class FrenchDeck extends Deck<FrenchCard> {
	
	private int curRank = 1;
	
	private FrenchSuit curSuit = FrenchSuit.SPADES;
	
	public FrenchDeck(int size) {
		super(size);
	}
	
	protected FrenchCard generateCard() {
		FrenchCard card = new FrenchCard(curRank, curSuit);
		curRank++;
		if (curRank > FrenchCard.RANK_KING) {
			curRank = 1;
			switch (curSuit) {
			case SPADES:
				curSuit = FrenchSuit.HEARTS;
				break;
				
			case HEARTS:
				curSuit = FrenchSuit.DIAMONDS;
				break;
				
			case DIAMONDS:
				curSuit = FrenchSuit.CLUBS;
				break;
				
			case CLUBS:
				curSuit = FrenchSuit.SPADES;
				break;
			}
		}
		return card;
	}
	
}

abstract class CardGame<T extends Card> {
	
	private final GameData data;
	
	private final long startTime;
	
	private final Player<T> challenger;
	
	private final List<Player<T>> opponents;
	
	protected final List<Player<T>> players;
	
	private int turn;
	
	public CardGame(GameData data, Player<T> challenger, Player<T>[] opponents) {
		this.data = data;
		this.startTime = System.currentTimeMillis();
		this.challenger = challenger;
		this.opponents = new ArrayList<Player<T>>();
		players = new ArrayList<Player<T>>();
		players.add(challenger);
		for (int i = 0; i < opponents.length; i++) {
			players.add(opponents[i]);
		}
	}
	
	public boolean hasExactOpponents(String[] opponents) {
		List<String> opsStr = new ArrayList<String>();
		for (Player p : players) {
			opsStr.add(p.name);
		}
		boolean hasExact = false;
		if (this.opponents.size() == opponents.length) {
			hasExact = true;
			for (String s : opponents) {
				if (!opsStr.contains(s)) {
					hasExact = false;
					break;
				}
			}
		}
		return hasExact;
	}
	
	public String getName() {
		return data.name;
	}
	
	public String getKey() {
		return data.key;
	}
	
	public String getObjective() {
		return data.objective;
	}
	
	public int getMinPlayers() {
		return data.minPlayers;
	}
	
	public int getMaxPlayers() {
		return data.maxPlayers;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
	public Player getPlayer(int num) {
		return players.get(num);
	}
	
	public Player getActivePlayer() {
		return players.get(turn);
	}
	
	protected int getTurn() {
		return turn;
	}
}

class Player<T extends Card> {
	
	public String name;
	
	public List<T> hand = new ArrayList<T>();
	
	public List<T> lastAdded = null;
	
	public List<T> lastRemoved = null;
	
	public Player(String name) {
		this.name = name;
	}
	
	public boolean hasCard(T c) {
		return hand.contains(c);
	}
	
	public void give(T card) {
		hand.add(card);
		lastAdded = new ArrayList<T>();
		lastAdded.add(card);
	}
	
	public void give(List<T> cards) {
		for (T c : cards) {
			hand.add(c);
		}
		lastAdded = cards;
	}
	
	public boolean isInHand(T card) {
		return hand.contains(card);
	}
	
	/**
	 * Checks if all of the given cards are in the player's hand.
	 * @param cards
	 * @return
	 */
	public boolean isInHand(List<T> cards) {
		for (T card : cards) {
			if (!isInHand(card)) {
				return false;
			}
		}
		return true;
	}
	
	public void lose(T card) {
		if (isInHand(card)) {
			hand.remove(card);
		}
		lastRemoved = new ArrayList<T>();
		lastRemoved.add(card);
	}
	
	public void lose(List<T> cards) {
		for (T card : cards) {
			if (isInHand(card)) {
				hand.remove(card);
			}
		}
		lastRemoved = cards;
	}
}

class FrenchPlayer extends Player<FrenchCard> {

	public FrenchPlayer(String name) {
		super(name);
	}
	
	public boolean hasRank(FrenchCard c) {
		boolean has = false;
		for (FrenchCard card : hand) {
			if (card.rank == c.rank) {
				has = true;
				break;
			}
		}
		return has;
	}
	
	public List<FrenchCard> loseRank(FrenchCard c) {
		List<FrenchCard> cardsOfRank = new ArrayList<FrenchCard>();
		for (FrenchCard card : hand) {
			if (card.rank == c.rank) {
				cardsOfRank.add(card);
			}
		}
		for (FrenchCard card : cardsOfRank) {
			hand.remove(card);
		}
		lastRemoved = cardsOfRank;
		return cardsOfRank;
	}
}

class GoFishGame extends CardGame {
	
	public static final GameData DATA = new GameData("Go Fish", "Get the most pairs", 2, 2);
	
	private FrenchDeck deck = new FrenchDeck(52);
	
	public GoFishGame(String player1, String player2) {
		super(DATA, new Player(player1), new Player[]{new Player(player2)});
		deck.shuffle();
		List<List<FrenchCard>> hands = deck.deal(7, 2);
		getPlayer(0).hand = hands.get(0);
		getPlayer(1).hand = hands.get(1);
	}
	
	public FrenchDeck getDeck() {
		return deck;
	}
	
	/**
	 * Returns true if the other player had it, false if go-fish.
	 */
	public boolean ask(FrenchCard card) {
		Player active = getActivePlayer();
		Player inactive = getInactivePlayer();
		if (active.hasCard(card) && inactive.hasRank(card)) {
			List<FrenchCard> cards = inactive.loseRank(card);
			active.give(cards);
			return true;
		} else {
			return false;
		}
	}
}
