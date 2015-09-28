/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Joseph Roque, Matthew L'Arrivee
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package roquelarrivee.dots;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;

import ocsf.server.AbstractServer;
import ocsf.server.ConnectionToClient;

/**
 * This class overrides some of the methods in the abstract superclass in order
 * to give more functionality to the server.
 *
 * @author Joseph Roque
 * @author Matthew L'Arrivee
 */
public class GameServer extends AbstractServer {

	/**Final string which represents login info in the ConnectionToClient hashmap*/
	final public static String LOGIN_ID = "loginID";

	/**List of clients who are currently playing or waiting to play*/
	private ArrayList <ConnectionToClient> playersWaitingToPlay = new ArrayList <ConnectionToClient>();
	/**List of ip addresses which have been kicked from the server and blocked*/
	private ArrayList <InetAddress> kickedClients = new ArrayList<InetAddress>();
	/**Username of the server host*/
	private String serverLoginID = null;

	@Override
	public void handleMessageFromClient(Object msg, ConnectionToClient client)
	{
		//Sends a control message to be displayed by the server host
		DotsApplication.getInstance().display("#CTMessage received: \"" + msg + "\" from " + client.getInfo(LOGIN_ID) + " - " + client);

		if (msg.toString().startsWith("#Login")) //New user logging in
		{
			String newLogin = msg.toString().substring(7);	//Gets username from the message

			/*
			 * Checks to see if the username has already been taken on the server
			 * and informs the new client if is has been, then closes
			 * their connection
			 */
			Thread[] connections = getClientConnections();
			for (int i = 0; i < connections.length; i++)
			{
				if (newLogin.equalsIgnoreCase((String)((ConnectionToClient)connections[i]).getInfo(LOGIN_ID)))
				{
					try
					{
						client.sendToClient("#BadLogin " + newLogin + " NameTaken");
					}
					catch(IOException ex) {}

					try
					{
						client.close();
					} catch (IOException ex) {}
					return;
				}
			}

			/*
			 * Checks to see if the client's ip address has been blocked. If so,
			 * tells the user they cannot connect to the server and then
			 * closes their connection
			 */
			Iterator<InetAddress> iteratorKickedClients = kickedClients.iterator();
			while (iteratorKickedClients.hasNext())
			{
				if (iteratorKickedClients.next().equals(client.getInetAddress()))
				{
					try
					{
						client.sendToClient("#BadLogin Kicked");
					}
					catch (IOException ex) {}

					try
					{
						client.close();
					}
					catch (IOException ex) {}
					return;
				}
			}

			//The first user to log on is saved as the server host
			if (connections.length == 1 && serverLoginID == null)
			{
				serverLoginID = newLogin;
			}

			try
			{
				//Sends the client the current state of the game
				client.sendToClient("#Settings " + getGameState());
			}
			catch (IOException ex)
			{
				this.clientException(client, ex);
			}

			//Sets the clients game info values to 0
			client.setInfo(LOGIN_ID, newLogin);
			client.setInfo("WIN", new Integer(0));
			client.setInfo("LOS", new Integer(0));
			client.setInfo("GPL", new Integer(0));
			client.setInfo("SQM", new Integer(0));
			sendToAllClients("#SV" + newLogin + " has connected!");
			return;
		}

		//Other messages are parsed for information
		try
		{
			parseClientMessage(msg, client);
		}
		catch (IOException ex)
		{
			this.clientException(client, ex);
		}
	}

	/**
	 * Takes a message from a client and parses it for information
	 * which is relevant to the user, or sends it to the rest of
	 * the clients if it is not.
	 *
	 * @param msg the message to parse
	 * @param client the client who sent the message
	 * @throws IOException thrown if an error occurs while sending messages to clients
	 */
	private void parseClientMessage(Object msg, ConnectionToClient client) throws IOException
	{
		String uppercaseMessage = msg.toString().toUpperCase();
		if (uppercaseMessage.equalsIgnoreCase("#Play")) //Indicates user wishes to be added to the current list of players
		{
			/*
			 * Checks if the user is already on the list of players and informs
			 * them, or adds them to the list and tells them their position in line
			 */
			int index = playersWaitingToPlay.indexOf(client);
			if (index > 1)
			{
				client.sendToClient("#GMYou're on the list. Your spot is " + (playersWaitingToPlay.indexOf(client) - 1));
			}
			else if (index > -1 && playersWaitingToPlay.size() >= 2)
			{
				client.sendToClient("#GMYou're playing right now!");
			}
			else if (index > -1)
			{
				client.sendToClient("#GMYou're on the list. Waiting for another player to join.");
			}
			else
			{
				playersWaitingToPlay.add(client);
				index = playersWaitingToPlay.indexOf(client);
				if (index > 1)
				{
					client.sendToClient("#GMYou're on the list. Your spot is " + (playersWaitingToPlay.indexOf(client) - 1));
				}
				else if (index == 1)
				{
					startNewGame();
				}
				else
				{
					client.sendToClient("#GMYou're on the list. Waiting for another player to join.");
				}
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Leave")) //Indicates user wishes to be removed from the list of waiting players
		{
			/*
			 * Finds the user's position in the list. If they are playing, they are informed
			 * they cannot be removed. Otherwise, they are removed from the list of players
			 * waiting to play.
			 */
			int index = playersWaitingToPlay.indexOf(client);
			if (index > 1 || (index == 0 && playersWaitingToPlay.size() == 1))
			{
				playersWaitingToPlay.remove(client);
				client.sendToClient("#GMYou're no longer on the list.");
			}
			else if (index > -1)
			{
				client.sendToClient("#GMYou can't leave in the middle of a game. Try '#Forfeit' instead.");
			}
			else
			{
				client.sendToClient("#GMYou aren't currently waiting to play.");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#CurrentPlayers")) //Indicates user requesting usernames of current players
		{
			StringBuilder messageToSend = new StringBuilder("#GMThe ");
			if (playersWaitingToPlay.size() == 1)
			{
				messageToSend.append(" only player is: " + playersWaitingToPlay.get(0).getInfo(LOGIN_ID));
			}
			else
			{
				messageToSend.append(" current players are: " + playersWaitingToPlay.get(0).getInfo(LOGIN_ID) + " and " + playersWaitingToPlay.get(1).getInfo(LOGIN_ID));
			}
			client.sendToClient(messageToSend.toString());
		}
		else if (uppercaseMessage.startsWith("#SET")) //Sets game info of the user
		{
			/*
			 * #SetWIN <value>		# of Wins
			 * #SetLOS <value>		# of Losses
			 * #SetGPL <value>		# of Games Played
			 * #SetSQM <value>		# of Squares Made
			 */
			setClientInfo(msg.toString().substring(4), client);
		}
		else if (uppercaseMessage.startsWith("#INC")) //Increments game info of the user by 1
		{
			/*
			 * #IncWIN		Increment # of wins by 1
			 * #IncLOS		Increment # of losses by 1
			 * #IncGLP		Increment # of games played by 1
			 * #IncSQM		Increment # of squares made by 1
			 */
			incrementClientInfo(msg.toString().substring(4), client);
		}
		else if (uppercaseMessage.startsWith("#GET")) //Returns info relating to the specified user to the client requesting it
		{
			/*
			 * #GetWIN <loginid>	# of wins of <loginid>
			 * #GetLOS <loginid>	# of losses of <loginid>
			 * #GetTIE <loginid>	# of ties of <loginid>
			 * #GetGPL <loginid>	# of games played by <loginid>
			 * #GetISP <loginid>	Whether <loginid> is currently playing or not
			 * #GetSTA <loginid>	Get all above stats
			 */
			getClientInfo(msg.toString().substring(4), client);
		}
		else if (uppercaseMessage.startsWith("#KICK")) //Indicates user wishes to kick another from the server
		{
			String userID = msg.toString().substring(6);

			if (!isClientServerHost(client))
			{
				client.sendToClient("#SVOnly the server host can do that!");
				return;
			}

			if (userID.equalsIgnoreCase(serverLoginID))
			{
				client.sendToClient("#SVYou can't kick the server host!");
				return;
			}

			/*
			 * Finds the user to kick and removes them from the list of players. If they
			 * are playing, they are forced to forfeit. Then, their ip address
			 * is added to a blacklist
			 */
			ConnectionToClient clientToKick = null;
			Thread[] connections = getClientConnections();
			for (int ii = 0; ii < connections.length; ii++)
			{
				ConnectionToClient currentClient = (ConnectionToClient)connections[ii];
				if (userID.equalsIgnoreCase((String)currentClient.getInfo(LOGIN_ID)))
				{
					clientToKick = currentClient;
					break;
				}
			}

			if (clientToKick != null)
			{
				int indexInWaitingList = playersWaitingToPlay.indexOf(clientToKick);
				kickedClients.add(clientToKick.getInetAddress());
				clientToKick.sendToClient("#Kicked");
				clientToKick.close();
				sendToAllClients("#SV" + userID + " has been kicked from the game.");

				if (indexInWaitingList == 0)
				{
					parseClientMessage("#GameOver 1", client);
				}
				else if (indexInWaitingList == 1)
				{
					parseClientMessage("#GameOver 2", client);
				}
				else if (indexInWaitingList > 1)
				{
					playersWaitingToPlay.remove(indexInWaitingList);
				}
			}
			else
			{
				client.sendToClient("#SV" + userID + " is not in the server.");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Position")) //Indicates user wishes to know their position in line
		{
			int index = playersWaitingToPlay.indexOf(client);
			if (index > 1)
			{
				client.sendToClient("#GMYou're on the list. Your spot is " + (playersWaitingToPlay.indexOf(client) - 1));
			}
			else if (index > -1 && playersWaitingToPlay.size() >= 2)
			{
				client.sendToClient("#GMYou're playing right now!");
			}
			else if (index > -1)
			{
				client.sendToClient("#GMYou're on the list. Waiting for another player to join.");
			}
			else
			{
				client.sendToClient("#GMYou're not currently on the list of players waiting.");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Quit")) //Indicates user wishes to close the server
		{
			//Only the server host can close the server
			//Sends message to disconnect all clients, then quits the application
			if (isClientServerHost(client))
			{
				sendToAllClients("#Disconnect");
				DotsApplication.getInstance().closeClientAndServer(true);
			}
			else
			{
				client.sendToClient("#SVOnly the server host can do that!");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#StopListening")) //Indicates user wishes to stop server from listening for new clients
		{
			//Only the server host has access to this command
			if (isClientServerHost(client))
			{
				sendToAllClients("#SVThe server has stopped accepting new clients.");
				stopListening();
			}
			else
			{
				client.sendToClient("#SVOnly the server host can do that!");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#StartListening")) //Indicates user wishes to start server listening for new clients
		{
			//Only the server host has access to this command
			if (isClientServerHost(client))
			{
				try {
					listen();
					sendToAllClients("#SVThe server has started accepting new clients");
				} catch (IOException e) {
					client.sendToClient("#SVAn error occurred. The server cannot listen for new clients");
				}
			}
			else
			{
				client.sendToClient("#SVOnly the server host can do that!");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Logoff")) //Indicates user wishes to disconnect from the server
		{
			//If the server host disconnects, all clients are disconnected
			if (isClientServerHost(client))
			{
				sendToAllClients("#Disconnect");
			}
			else
			{
				//Removes client from their position in queue and forces them to forfeit if they are playing
				sendToAllClients("#SV" + client.getInfo(LOGIN_ID) + " has logged off.");
				int indexInWaitingList = playersWaitingToPlay.indexOf(client);
				if (indexInWaitingList == 0 || indexInWaitingList == 1)
				{
					if (playersWaitingToPlay.size() > 1)
					{
						sendToAllClients("#GM" + client.getInfo(LOGIN_ID) + " has forfeit.");
						parseClientMessage("#GameOver " + (indexInWaitingList + 1), client);
					}
					else
					{
						playersWaitingToPlay.remove(client);
					}
				}
				else if (indexInWaitingList > 1)
				{
					playersWaitingToPlay.remove(indexInWaitingList);
				}
			}
		}
		else if (uppercaseMessage.startsWith("#GAMEOVER")) //Indicates the game has ended
		{
			/*
			 * Finds the winner and loser of the game, informs them of the results
			 * then removes the loser from the list of players waiting to play.
			 * If there are still two players waiting to play, a new game is begun.
			 */
			int loserOfGame = Integer.parseInt(msg.toString().substring(10));
			ConnectionToClient winner = null;
			ConnectionToClient loser = null;
			if (loserOfGame == 3)
			{
				sendToAllClients("#GameOver It's a tie!");
				playersWaitingToPlay.remove(0).sendToClient("#You tied! You have been removed from the queue. To play again, type '#Play'");
				playersWaitingToPlay.remove(0).sendToClient("#You tied! You have been removed from the queue. To play again, type '#Play'");;
			}
			else
			{
				winner = playersWaitingToPlay.get(-(loserOfGame - 1) + 1);
				loser = playersWaitingToPlay.get(loserOfGame - 1);
				sendToAllClients("#GameOver " + winner.getInfo(LOGIN_ID));
				winner.sendToClient("#GMYou won! You will get to play again in the next game!");
				loser.sendToClient("#GMYou lost! You have been removed from the queue. To play again, type '#Play'");
				playersWaitingToPlay.remove(loser);
			}

			if (playersWaitingToPlay.size() >= 2)
			{
				startNewGame();
			}
			else if (playersWaitingToPlay.size() == 1)
			{
				playersWaitingToPlay.get(0).sendToClient("#GMThere are no players waiting to play. A new game will start when a second player joins.");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Forfeit")) //Indicates user wishes to forfeit the match
		{
			int indexInWaitingList = playersWaitingToPlay.indexOf(client);
			if (indexInWaitingList == 0)
			{
				sendToAllClients("#GM" + client.getInfo(LOGIN_ID) + " has forfeit.");
				parseClientMessage("#GameOver 1", client);
			}
			else if (indexInWaitingList == 1)
			{
				sendToAllClients("#GM" + client.getInfo(LOGIN_ID) + " has forfeit.");
				parseClientMessage("#GameOver 2", client);
			}
			else if (indexInWaitingList > 1)
			{
				client.sendToClient("#SVYou are not currently playing.");
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Users")) //Indicates client is requesting a list of all users in the server
		{
			//Creates a list of all the usernames in the server then returns it
			StringBuilder stringBuilder = new StringBuilder("#Users");
			Thread[] connections = getClientConnections();
			for (int ii = 0; ii < connections.length; ii++)
			{
				ConnectionToClient currentClient = (ConnectionToClient)connections[ii];
				stringBuilder.append(" ");
				stringBuilder.append(currentClient.getInfo(LOGIN_ID));
			}
			client.sendToClient(stringBuilder.toString());
		}
		else //Any other messages are sent to all the clients
		{
			sendToAllClients(msg);
		}
	}

	/**
	 * Builds a string containing the current state of any
	 * game which is taking place, including the size of the board,
	 * names of the players, and state of all of the squares and which
	 * sides have been selected and filled.
	 *
	 * @return a string which represents the current state of the game
	 */
	private String getGameState()
	{
		StringBuilder gameStateBuilder = new StringBuilder();
		GamePanel gamePanel = DotsApplication.getInstance().getGamePanel();
		GameSquare[] gameSquares = gamePanel.getSquares();

		gameStateBuilder.append(gamePanel.getGridSize());
		gameStateBuilder.append(" ");
		gameStateBuilder.append(DotsApplication.getInstance().isGameInProgress());
		if (DotsApplication.getInstance().isGameInProgress())
		{
			gameStateBuilder.append(" ");
			gameStateBuilder.append(DotsApplication.getInstance().getPlayerOneName());
			gameStateBuilder.append(" ");
			gameStateBuilder.append(DotsApplication.getInstance().getPlayerTwoName());
			for (int ii = 0; ii < gameSquares.length; ii++)
			{
				gameStateBuilder.append(" ");
				gameStateBuilder.append(gameSquares[ii].getSideValue());
			}
		}

		return gameStateBuilder.toString();
	}

	/**
	 * Returns true if the client is the server host, false otherwise
	 *
	 * @param client the client to compare to the server host login id
	 * @return true if <code>serverLoginID</code> is equal to
	 * 			<code>client.getInfo(LOGIN_ID)</code>, ignoring case.
	 */
	private boolean isClientServerHost(ConnectionToClient client)
	{
		return serverLoginID.equalsIgnoreCase((String)client.getInfo(LOGIN_ID));
	}

	/**
	 * Randomly selects one of the first to clients in the list
	 * <code>playersWaitingToPlay</code> to go first, sends a message
	 * to all clients with the usernames of the two players in the game.
	 */
	private void startNewGame()
	{
		boolean randomizeOrder = Math.random() < 0.5;
		if (randomizeOrder)
		{
			sendToAllClients("#NewGame " + playersWaitingToPlay.get(0).getInfo(LOGIN_ID) + " " + playersWaitingToPlay.get(1).getInfo(LOGIN_ID));
		}
		else
		{
			sendToAllClients("#NewGame " + playersWaitingToPlay.get(1).getInfo(LOGIN_ID) + " " + playersWaitingToPlay.get(0).getInfo(LOGIN_ID));
			playersWaitingToPlay.add(0, playersWaitingToPlay.remove(1));
		}
	}

	/**
	 * Sets a value with a key, specified in <code>info</code>, with the
	 * first 3 characters being the key and the remaining characters
	 * the value.
	 *
	 * @param info the key and value to store
	 * @param client the connection which will store the value
	 */
	private void setClientInfo(String info, ConnectionToClient client)
	{
		client.setInfo(info.substring(0, 3), Integer.parseInt(info.substring(4)));
	}

	/**
	 * Increments a value stored at a key specified in <code>info</code>
	 *
	 * @param info the key's which value will be incremented
	 * @param client the connection which will store the value
	 */
	private void incrementClientInfo(String info, ConnectionToClient client)
	{
		Integer clientInfoToIncrement = (Integer)client.getInfo(info);
		Integer infoIncrementedByOne = new Integer(clientInfoToIncrement.intValue() + 1);
		client.setInfo(info, infoIncrementedByOne);
	}

	/**
	 * Gets a value from a key, specified in <code>info</code>, from the client
	 * with the username specified by <code>info</code> and sends this value
	 * to <code>client</code>. The first 3 characters of <code>info</code> specify
	 * the key, and the remaining characters specify the username.
	 *
	 * @param info
	 * @param client
	 * @throws IOException
	 */
	private void getClientInfo(String info, ConnectionToClient client) throws IOException
	{
		StringBuilder messageToSend = new StringBuilder("#SV");
		ConnectionToClient clientInfoToGet = null;
		Thread[] connections = getClientConnections();
		for (int ii = 0; ii < connections.length; ii++)
		{
			ConnectionToClient currentClient = (ConnectionToClient)connections[ii];
			if (info.substring(4).equalsIgnoreCase((String)(currentClient.getInfo(LOGIN_ID))))
			{
				clientInfoToGet = currentClient;
				break;
			}
		}

		if (clientInfoToGet == null)
		{
			client.sendToClient("#SVThat user does not exist.");
			return;
		}
		info = info.toUpperCase();

		if (info.startsWith("STA"))
		{
			messageToSend.append("Stats of " + clientInfoToGet.getInfo(LOGIN_ID) + ":\n");
			messageToSend.append("Number of games played: " + clientInfoToGet.getInfo("GPL") + "\n");
			messageToSend.append("Number of wins: " + clientInfoToGet.getInfo("WIN") + "\n");
			messageToSend.append("Number of losses: " + clientInfoToGet.getInfo("LOS") + "\n");
			messageToSend.append("Number of ties: " + ((Integer)clientInfoToGet.getInfo("GPL") - (Integer)clientInfoToGet.getInfo("WIN") - (Integer)clientInfoToGet.getInfo("LOS")) + "\n");
			messageToSend.append("Is this user playing? ");
			if (playersWaitingToPlay.indexOf(clientInfoToGet) == 0 || playersWaitingToPlay.indexOf(clientInfoToGet) == 1)
			{
				messageToSend.append("Yes");
			}
			else
			{
				messageToSend.append("No");
			}
		}
		else if (info.startsWith("TIE"))
		{
			messageToSend.append(clientInfoToGet.getInfo(LOGIN_ID) + " has " + ((Integer)clientInfoToGet.getInfo("GPL") - (Integer)clientInfoToGet.getInfo("WIN") - (Integer)clientInfoToGet.getInfo("LOS")) + " ties.");
		}
		else if (info.startsWith("ISP"))
		{
			if (playersWaitingToPlay.indexOf(clientInfoToGet) == 0 || playersWaitingToPlay.indexOf(clientInfoToGet) == 1)
			{
				messageToSend.append("Yes, " + clientInfoToGet.getInfo(LOGIN_ID) + " is currently playing.");
 			}
			else
			{
				messageToSend.append("No, " + clientInfoToGet.getInfo(LOGIN_ID) + " is not currently playing.");
			}
		}
		else if (info.startsWith("WIN"))
		{
			messageToSend.append(clientInfoToGet.getInfo(LOGIN_ID) + " has " + clientInfoToGet.getInfo("WIN") + " wins.");
		}
		else if (info.startsWith("LOS"))
		{
			messageToSend.append(clientInfoToGet.getInfo(LOGIN_ID) + " has " + clientInfoToGet.getInfo("LOS") + " losses.");
		}
		else if (info.startsWith("GPL"))
		{
			messageToSend.append(clientInfoToGet.getInfo(LOGIN_ID) + " has played " + clientInfoToGet.getInfo("GPL") + " games.");
		}
		else
		{
			messageToSend.append("That is not a valid command");
		}

		client.sendToClient(messageToSend.toString());
	}

	/**
	 * Constructor which passes the parameter to the
	 * AbstractServer super constructor
	 *
	 * @param port the port to create the server through
	 */
	public GameServer(int port)
	{
		super(port);
	}

	@Override
	synchronized protected void clientException(ConnectionToClient client, Throwable exception)
	{
		DotsApplication.getInstance().display("#CTError with connection to client: " + client.getInfo(LOGIN_ID) + "\nThey will be disconnected.");
		try
		{
			client.sendToClient("#Disconnect");
		} catch (IOException ex) {}
		try
		{
			client.close();
		} catch (IOException ex) {}
	}

	@Override
	synchronized protected void clientDisconnected(ConnectionToClient client)
	{
		try
		{
			parseClientMessage("#Logoff", client);
		}
		catch (IOException ex) {}
	}
}
