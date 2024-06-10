package com.ninetyninepercentcasino.bj;

import com.ninetyninepercentcasino.net.*;
import com.ninetyninepercentcasino.screens.BJScreen;

import java.io.EOFException;
import java.io.IOException;
import java.io.OptionalDataException;
import java.net.Socket;
import java.net.SocketException;

/**
 * client of a blackjack game that receives messages from the server and handles them
 */
public class BJClient extends Connection {
	private BJScreen screen; //the BJScreen that this BJClient will update

	/**
	 * initializes a BJClient
	 * @param clientSocket the socket of the client
	 * @param screen the screen that the BJClient will update
	 * @throws IOException when there is a problem with the socket?
	 */
	public BJClient(Socket clientSocket, BJScreen screen) throws IOException {
		super(clientSocket);
		this.screen = screen;
	}

	/**
	 * the method called by the connection when it is started
	 * this will receive messages from the server on a separate thread from the main game
	 */
	public void run(){
		try {
			while (alive) {
				if(!clientSocket.isConnected()) {
					finish();
				}
				try {
					NetMessage message = (NetMessage) in.readObject(); //read the object coming from server
					message.setOrigin(clientSocket.getRemoteSocketAddress());
					if (message.getContent() != null) {
						System.out.printf("[%s] %s: %s\n",  message.getType(), clientSocket.getRemoteSocketAddress().toString(), message.getContent());
						switch(message.getType()) {
							case ACK:
								aliveMessage = (String) message.getContent();
								break;
							case PING:
								message.setType(NetMessage.MessageType.ACK);
								synchronized (out) {
									out.writeObject(message);
								}
								break;
							case INFO: //the message contains information about the game state
								Object content = message.getContent();
								if(content instanceof BJAvailActionUpdate){
									BJAvailActionUpdate thing = (BJAvailActionUpdate)content;
									for(BJAction action : thing.getActions().keySet()){
										System.out.println(action + " " + thing.getActions().get(action));
									}
								}
								screen.requestUpdate((DTO)content); //pass the update content to the BJScreen and request an update with it
							default:
						}
					}
				} catch (OptionalDataException e) {

				} catch (SocketException e) {
					System.err.println(e.getMessage());
					finish();
				} catch (EOFException e) {
					finish();
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dispose() {
	}
}
