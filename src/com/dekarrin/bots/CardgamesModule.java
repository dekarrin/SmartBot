package com.dekarrin.bots;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class CardgamesModule extends Module {
	
	public CardgamesModule() {
		super("CARDGAMES", "v0.1", "Challenge friends to card games!");
		//addCommand("GOFISH", )
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

class GoFishGame {
	
	public static class Player {
		
		public String name;
		
		public List<FrenchCard> hand = new ArrayList<FrenchCard>();
		
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
	}
	
	private FrenchDeck deck = new FrenchDeck(52);
	
	private List<Player> players = new LinkedList<Player>();
	
	private int turn;
	
	public GoFishGame(String player1, String player2) {
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
			inactive.takeRank(c)
		}
	}
}
