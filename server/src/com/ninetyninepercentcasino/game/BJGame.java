package com.ninetyninepercentcasino.game;

import com.ninetyninepercentcasino.database.Account;
import com.ninetyninepercentcasino.game.BJMessageListener;
import com.ninetyninepercentcasino.game.bj.BJDealer;
import com.ninetyninepercentcasino.game.bj.BJPlayer;
import com.ninetyninepercentcasino.game.gameparts.Card;
import com.ninetyninepercentcasino.game.gameparts.Deck;
import com.ninetyninepercentcasino.net.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Stack;

/**
 * Runs logic for a bj game
 * @author Grant Liang
 */
public class BJGame {
    private static final int PLAYER_WON = 0;
    private static final int DEALER_WON = 1;
    private static final int TIE = 3;
    private static final int PLAYER_BLACKJACK = 4;

    private Deck deck;
    private Account player;
    private BJDealer dealer;
    private boolean waiting;
    private Stack<BJHand> hands;
    private Stack<BJHand> resolved;
    private Server server;
    private BJMessageListener bjMessageListener;



    private final BJSynchronizer bjSynchronizer;
    private double firstBet;

    public BJGame(Account player) throws IOException {
        this.player = player;
        hands = new Stack<>();
        resolved = new Stack<>();
        waiting = false;
        bjSynchronizer = new BJSynchronizer();
        bjMessageListener = new BJMessageListener(new Socket("127.0.0.1", 9925), this);
        try {
            server = new Server();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.start();

    }
    public void startRound() throws InterruptedException {
        getInitialBet();
        deck = new Deck();
        deck.shuffle();
        dealer = new BJDealer(deck);

        dealer.setup();

        BJHand firstHand = new BJHand(player);
        firstHand.setBet(firstBet);
        hands.push(firstHand);

        if(dealer.hasVisibleAce()) dealer.setInsuranceBet(firstHand.getInsurance());

        drawCardUpdate(firstHand.drawCard(deck), player, true);
        drawCardUpdate(firstHand.drawCard(deck), player, true);

        while(!hands.isEmpty()){
            BJHand currentHand = hands.peek();
            BJAction clientAction = currentHand.act();
            switch(clientAction){
                case HIT:
                    currentHand.drawCard(deck);
                    break;
                case STAND:
                    resolved.push(hands.pop());
                    break;
                case SPLIT:
                    Card card1 = deck.drawCard();
                    Card card2 = deck.drawCard();
                    BJHand hand1 = new BJHand(player, currentHand.getCard(0), card1);
                    BJHand hand2 = new BJHand(player, currentHand.getCard(1), card2);
                    hands.push(hand1);
                    hands.push(hand2);
                    break;
                case DOUBLE_DOWN:
                    currentHand.drawCard(deck);
                    currentHand.doubleBet();
                    resolved.push(hands.pop());
                    break;
            }
        }
        dealer.play();
        int dealerScore = dealer.getScore();
        while (!resolved.isEmpty()) {
            BJHand currentHand = resolved.pop();
            int outcome = findWinner(currentHand, dealer);
            switch(outcome){
                case PLAYER_BLACKJACK:
                    player.addBalance(currentHand.getAmountBet()*2.5);
                    break;
                case PLAYER_WON:
                    player.addBalance(currentHand.getAmountBet()*2);
                    break;
                case TIE:
                    player.addBalance(currentHand.getAmountBet());
                    break;
                case DEALER_WON:
                    if(dealer.getNumCards() == 2) player.addBalance(dealer.getInsuranceBet()*3);
                    break;
            }
        }

    }
    public BJDealer getDealer(){
        return dealer;
    }
    /**
     * simulates the action of the dealer
     */
    private void dealerAction(){

    }
    private void getInitialBet() throws InterruptedException {
        NetMessage getBet = new NetMessage(NetMessage.MessageType.INFO, new BJBetRequest());
        try {
            server.sendAll(getBet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        synchronized(bjSynchronizer) {
            bjSynchronizer.wait(); //waits until the client returns the amount bet
        }
    }

    /**
     * determines the winner of a blackjack game between the dealer and a player hand
     * @param playerHand the hand of the player to be compared with the dealer
     * @param dealer the dealer's hand
     * @return 0 if the player won, 1 if the dealer won, 2 for a tie
     */
    private int findWinner(BJHand playerHand, BJDealer dealer){
        int playerScore = playerHand.getScore();
        int dealerScore = dealer.getScore();
        if(playerScore == 21 && dealerScore == 21){
            if(playerHand.getCards().size() == 2 && dealer.getNumCards() == 2) return TIE;
            else if(playerHand.getCards().size() == 2) return PLAYER_BLACKJACK;
            else return DEALER_WON;
        }
        else if((playerScore > 21 && dealerScore > 21)) return DEALER_WON;
        else if(playerScore == dealerScore) return TIE;
        else if(playerScore > 21) return DEALER_WON;
        else if(dealerScore > 21) return PLAYER_WON;
        else if(playerScore > dealerScore) return PLAYER_WON;
        else return DEALER_WON;
    }

    /**
     * called when the client needs to be updated about a card that was drawn
     */
    private void drawCardUpdate(Card card, BJPlayer player, boolean visible){
        NetMessage cardUpdate = new NetMessage(NetMessage.MessageType.INFO, new BJCardUpdate(card, player, visible));
        try {
            server.sendAll(cardUpdate);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BJSynchronizer getBjSynchronizer() {
        return bjSynchronizer;
    }
    public void setFirstBet(double firstBet){
        this.firstBet = firstBet;
    }
}
