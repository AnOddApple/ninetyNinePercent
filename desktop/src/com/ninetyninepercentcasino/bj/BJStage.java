package com.ninetyninepercentcasino.bj;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.ninetyninepercentcasino.bj.bjbuttons.*;
import com.ninetyninepercentcasino.game.Text;
import com.ninetyninepercentcasino.game.SFXManager;
import com.ninetyninepercentcasino.game.gameparts.Card;
import com.ninetyninepercentcasino.gameparts.*;
import com.ninetyninepercentcasino.net.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * this class contains all the actors in a BJGame stage
 * also includes methods for changing the game state and handles DTOs coming in from the server
 * @author Grant Liang
 */
public class BJStage extends Stage {
	private CardGroup playerHand;
	private CardGroup dealerHand;
	private CardGroup splits;
	private DeckActor deckActor;
	private ChipGroup chips;
	private Table betDisplays;

	private BetButton betButton;
	private HitButton hitButton;
	private SplitButton splitButton;
	private StandButton standButton;
	private DDButton doubleDownButton;
	private Label betDisplay;
	private BitmapFont font;

	private BJClient client;

	/**
	 * initializes a new BJStage
	 * @param viewport the viewport to be used
	 */
	public BJStage(Viewport viewport){
		super(viewport);
	}

	/**
	 * called by a BJScreen to update the game state
	 * @param update the DTO containing information about the game update
	 */
	public void handleDTO(DTO update){
		if(update instanceof BJBetRequest){
			startBetPhase();
		}
		else if(update instanceof BJInsuranceRequest){
			startInsurePhase();
		}
		else if(update instanceof BJCardUpdate){
			if(((BJCardUpdate)update).isPlayerCard()) addPlayerCard(((BJCardUpdate)update).getCard());
			else {
				addDealerCard(((BJCardUpdate)update).getCard());
				if(((BJCardUpdate)update).isVisible()) revealDealerHand(); //only the first card for the dealer will be sent as visible, so revealing the hand will just reveal that first card
			}
		}
		else if(update instanceof BJAvailActionUpdate){
			updateButtons(((BJAvailActionUpdate)update).getActions());
		}
		else if(update instanceof BJSplit){
			handleSplit((BJSplit)update);
		}
		else if(update instanceof BJHandEnd){
			revealDealerHand();
			endHand();
		}
	}

	/**
	 * begins the betting phase before cards are drawn
	 */
	private void startBetPhase(){
		final float WORLD_WIDTH = getViewport().getWorldWidth();
		final float WORLD_HEIGHT = getViewport().getWorldHeight();

		chips = new ChipGroup(1000, 5, 5, 5, 5, 400);
		addActor(chips);

		betButton = new BetButton();
		betButton.enable();
		betButton.setPosition(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);

		Text text = new Text();

		betDisplay = new Label("", text.getLeagueGothicLabelStyle(260));

		betDisplays = new Table();
		betDisplays.setFillParent(true);
		betDisplays.add(betButton).bottom();
		betDisplays.add(betDisplay).bottom().spaceLeft(WORLD_WIDTH/6);
		betDisplays.setZIndex(0);
		addActor(betDisplays);
	}

	/**
	 * updates the number displayed by the chip value calculator
	 * called by the BJScreen every render
	 */
	public void updateBetDisplay(){
		if(betDisplay != null) betDisplay.setText(chips.calculate());
	}

	/**
	 * sends a bet to the server
	 */
	public void sendBet(){
		BJBetRequest betRequest = new BJBetRequest(chips.calculate());
		NetMessage message = new NetMessage(NetMessage.MessageType.INFO, betRequest);
		try {
			client.message(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		chips.disableChipsHeld();
		setupGame();
	}

	/**
	 * sets up the player hand, dealer hand, buttons, and everything else needed for the BJ gaem
	 */
	private void setupGame() {
		final float WORLD_WIDTH = getViewport().getWorldWidth();
		final float WORLD_HEIGHT = getViewport().getWorldHeight();

		betDisplays.setVisible(false);

		playerHand = new CardGroup(true, true);
		dealerHand = new CardGroup(false, false);
		splits = new CardGroup(true, false);
		deckActor = new DeckActor();

		Table bjButtons = new Table();
		hitButton = new HitButton();
		splitButton = new SplitButton();
		standButton = new StandButton();
		doubleDownButton = new DDButton();
		bjButtons.add(hitButton);
		bjButtons.add(standButton);
		bjButtons.add(splitButton);
		bjButtons.add(doubleDownButton);

		Table bottomUI = new Table();
		bottomUI.setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 2);
		bottomUI.add(bjButtons).bottom();

		Table upperTable = new Table();
		upperTable.add(deckActor).padRight(100);
		upperTable.add(dealerHand);
		upperTable.setPosition(WORLD_WIDTH / 2, WORLD_HEIGHT / 1.5f);

		Table root = new Table();
		root.setPosition(WORLD_WIDTH / 2, 0);
		root.add(playerHand).bottom();
		root.add(splits).bottom().padLeft(WORLD_WIDTH/16);

		addActor(upperTable);
		addActor(bottomUI);
		addActor(root);
	}

	/**
	 * begins the insurance phase of betting
	 */
	private void startInsurePhase(){
		final float WORLD_WIDTH = getViewport().getWorldWidth();
		final float WORLD_HEIGHT = getViewport().getWorldHeight();

		InsureButton insureButton = new InsureButton();
		chips.enableChipsHeld();
		insureButton.enable();
		insureButton.setPosition(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f);
		Label.LabelStyle labelStyle = new Label.LabelStyle();
		labelStyle.font = font;
		betDisplay = new Label("", labelStyle);

		betDisplays = new Table();
		betDisplays.setFillParent(true);
		betDisplays.add(insureButton).bottom();
		betDisplays.add(betDisplay).bottom().spaceLeft(WORLD_WIDTH/6);
		betDisplays.setZIndex(0);
		betDisplays.setVisible(true);
		addActor(betDisplays);

	}

	/**
	 * sends an insurance bet to the server
	 */
	public void sendInsure() {
		BJBetRequest betRequest = new BJBetRequest(chips.calculate());
		NetMessage message = new NetMessage(NetMessage.MessageType.INFO, betRequest);
		try {
			client.message(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		chips.disableChipsHeld();
	}

	/**
	 * adds a Card to the player's hand
	 * the card is immediately displayed
	 * @param card the card to be added
	 */
	private void addPlayerCard(Card card){
		SFXManager.playSlideSound();
		playerHand.addCard(card);
	}
	/**
	 * adds a Card to the dealer's hand
	 * the card is immediately displayed
	 * @param card the card to be added
	 */
	private void addDealerCard(Card card){
		SFXManager.playSlideSound();
		dealerHand.addCard(card);
	}

	/**
	 * reveals all cards in the dealer's hand
	 */
	private void revealDealerHand(){
		dealerHand.reveal();
	}

	/**
	 * getter for the stage's viewport
	 * @return this stage's viewport
	 */
	@Override
	public Viewport getViewport(){
		return super.getViewport();
	}

	/**
	 * updates the BJ buttons given the available actions
	 * @param actions describes the available and unavailable actions
	 */
	private void updateButtons(HashMap<BJAction, Boolean> actions){
		if(actions.get(BJAction.HIT)) hitButton.enable();
		else hitButton.disable();
		if(actions.get(BJAction.STAND)) standButton.enable();
		else standButton.disable();
		if(actions.get(BJAction.SPLIT)) splitButton.enable();
		else splitButton.disable();
		if(actions.get(BJAction.DOUBLE_DOWN)) doubleDownButton.enable();
		else doubleDownButton.disable();
	}

	/**
	 * performs the hit action in blackjack
	 * sends the action to the server
	 */
	public void hit() {
		BJActionUpdate actionUpdate = new BJActionUpdate(BJAction.HIT);
		NetMessage message = new NetMessage(NetMessage.MessageType.INFO, actionUpdate);
		try {
			client.message(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		disableAllButtons();
	}
	/**
	 * performs the stand action in blackjack
	 * sends the action to the server
	 */
	public void stand() {
		BJActionUpdate actionUpdate = new BJActionUpdate(BJAction.STAND);
		NetMessage message = new NetMessage(NetMessage.MessageType.INFO, actionUpdate);
		try {
			client.message(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		disableAllButtons();
	}
	/**
	 * performs the double down action in blackjack
	 * sends the action to the server
	 */
	public void doubleDown(){
		BJActionUpdate actionUpdate = new BJActionUpdate(BJAction.DOUBLE_DOWN);
		NetMessage message = new NetMessage(NetMessage.MessageType.INFO, actionUpdate);
		try {
			client.message(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		disableAllButtons();
	}
	/**
	 * performs the splitting action in blackjack
	 * sends the action to the server
	 */
	public void split(){
		BJActionUpdate actionUpdate = new BJActionUpdate(BJAction.SPLIT);
		NetMessage message = new NetMessage(NetMessage.MessageType.INFO, actionUpdate);
		try {
			client.message(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		disableAllButtons();
	}
	/**
	 * handles the server sending over a successful split
	 */
	private void handleSplit(BJSplit bjSplit){
		splits.addCard(bjSplit.getHand2().getCard(0));
		playerHand.removeCard(bjSplit.getHand1().getCard(0));
	}
	/**
	 * disables each button
	 */
	private void disableAllButtons(){
		hitButton.disable();
		standButton.disable();
		splitButton.disable();
		doubleDownButton.disable();
	}

	private void endHand(){
		playerHand.hide();
	}
	/**
	 * this method NEEDS TO BE CALLED to set the client of a BJStage if the stage is to communicate with server
	 * @param client the client of the stage that communicates with the server
	 */
	public void setClient(BJClient client){
		this.client = client;
	}
}
