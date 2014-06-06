package com.dekarrin.bots;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CardgamesModule extends Module {
	
	public static class Challenge {
		public final String challenger;
		public final String game;
		public final long startTime;
		public Challenge(String challenger, String game) {
			this.startTime = System.currentTimeMillis();
			this.challenger = challenger;
			this.game = game;
		}
	}
	
	private Map<String, List<CardGame>> activeGames = new HashMap<String, List<CardGame>>();
	
	private Map<String, List<Challenge>> challenges = new ConcurrentHashMap<String, Challenge>();
	
	public CardgamesModule() {
		super("CARDGAMES", "v0.1", "Challenge friends to card games");
		(new Thread(new Runnable() {
			
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
						synchronized (challenges) {
							List<String>
							for (String chr : challenges.keySet()) {
								
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
			
		}, "Challenge-Clean")).start();
		addCommand("GOFISH-CHALLENGE", new BotAction() {
			
			@Override
			public String syntax() {
				return "%s [oppenent]";
			}
			
			@Override
			public String help() {
				return "Challenge someone to Go Fish";
			}
			
			@Override
			public void execute(String[] params, String sender, String recipient) {
				if (params.length >= 1) {
					String challengerNick = bot.getRegisteredNick(sender);
					String challengeeNick = bot.getRegisteredNick(params[0]);
					if (challengerNick != null) {
						if (challengeeNick != null) {
							String registration = challengerNick + "_vs_" + challengeeNick;
							GoFishGame g = getGoFish(registration);
							if (g == null) {
								bot.sendMessage(sender, "You've sent out your challenge!");
								bot.sendMessage(params[0], "You've been challenged to Go Fish by " + sender + ". GOFISH-ACCEPT or GOFISH-REJECT to respond.");
								bot.sendMessage(bot.getChannel(), sender + " has challenged " + params[0] + " to a game of Go Fish!");
								synchronized (challenges) {
									if (!challenges.containsKey(challengeeNick)) {
										challenges.put(challengeeNick, new ArrayList<Challenge>());
									}
									List<Challenge> list = challenges.get(challengeeNick);
									if (, new Challenge(challengerNick, GoFishGame.NAME));
								}
							} else {
								bot.sendMessage(recipient, "You're already in a game with " + params[0] + ".");
							}
						} else {
							bot.sendMessage(recipient, "Your opponent must be logged in with NickServ to be challenged.");
						}
					} else {
						bot.sendMessage(recipient, "You must be logged in with NickServ to challenge oppenents.");
					}
				} else {
					bot.sendBadSyntax(recipient, sender);
				}
			}
		});
	}
	
	private GoFishGame getGoFish(String registration) {
		List<CardGame> games = activeGames.get(registration);
		if (games != null) {
			GoFishGame theGame = null;
			for (CardGame game : games) {
				if (game.getName().equals(GoFishGame.NAME)) {
					theGame = (GoFishGame)game;
					break;
				}
			}
			return theGame;
		} else {
			return null;
		}
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

abstract class CardGame {
	
	private final long startTime;
	
	private final String name;
	
	public CardGame(String name) {
		this.name = name;
		this.startTime = System.currentTimeMillis();
	}
	
	public String getName() {
		return name;
	}
	
	public long getStartTime() {
		return startTime;
	}
}

class GoFishGame extends CardGame {
	
	public static final String NAME = "Go Fish";
	
	public static class Player {
		
		public String name;
		
		public List<FrenchCard> hand = new ArrayList<FrenchCard>();
		
		public List<FrenchCard> lastGot = null;
		
		public Player(String name) {
			this.name = name;
		}
		
		public boolean hasCard(FrenchCard c) {
			return hand.contains(c);
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
		
		public List<FrenchCard> takeRank(FrenchCard c) {
			List<FrenchCard> cardsOfRank = new ArrayList<FrenchCard>();
			for (FrenchCard card : hand) {
				if (card.rank == c.rank) {
					cardsOfRank.add(card);
				}
			}
			for (FrenchCard card : cardsOfRank) {
				hand.remove(card);
			}
			return cardsOfRank;
		}
		
		public void give(List<FrenchCard> cards) {
			for (FrenchCard c : cards) {
				hand.add(c);
			}
			lastGot = cards;
		}
	}
	
	private FrenchDeck deck = new FrenchDeck(52);
	
	private List<Player> players = new LinkedList<Player>();
	
	private int turn;
	
	public GoFishGame(String player1, String player2) {
		super(NAME);
		players.add(new Player(player1));
		players.add(new Player(player2));
		deck.shuffle();
		List<List<FrenchCard>> hands = deck.deal(7, 2);
		players.get(0).hand = hands.get(0);
		players.get(1).hand = hands.get(1);
	}
	
	public FrenchDeck getDeck() {
		return deck;
	}
	
	public Player getPlayer(int num) {
		return players.get(num);
	}
	
	public int getActive() {
		return turn;
	}
	
	public Player getActivePlayer() {
		return players.get(turn);
	}
	
	public Player getInactivePlayer() {
		return players.get((turn + 1) % 2);
	}
	
	/**
	 * Returns true if the other player had it, false if go-fish.
	 */
	public boolean ask(FrenchCard card) {
		Player active = getActivePlayer();
		Player inactive = getInactivePlayer();
		if (active.hasCard(card) && inactive.hasRank(card)) {
			List<FrenchCard> cards = inactive.takeRank(card);
			active.give(cards);
			return true;
		} else {
			return false;
		}
	}
}
