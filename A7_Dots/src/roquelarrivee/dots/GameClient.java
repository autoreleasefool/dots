/*
 * Joseph Roque 	7284039
 * Matt L'Arrivee 	6657183
 * 
 */

package roquelarrivee.dots;

import java.io.IOException;
import java.net.InetAddress;

import ocsf.client.AbstractClient;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 *
 * @author Joseph Roque
 * @author Matthew L'Arrivee
 */
public class GameClient extends AbstractClient
{

	/**Instance of the user interface*/
	private ChatIF clientUI;
	/**The login id used by the client*/
	private String loginID;
	
	@Override
	public void sendToServer(Object message) throws IOException
	{
		super.sendToServer(message);
	}

	@Override
	public void handleMessageFromServer(Object msg)
	{
		clientUI.display(msg.toString());
	}
	
	/**
	 * Checks the message for proper formatting and invalid commands,
	 * then sends the message to the server.
	 * 
	 * @param message the message to check
	 * @throws IOException error sending message to the server
	 */
	private void performCommand(String message) throws IOException
	{
		String uppercaseMessage = message.toUpperCase();
		if (DotsApplication.getInstance().isServerHost()) //Commands only available to server host
		{
			if (uppercaseMessage.equalsIgnoreCase("#Quit") || uppercaseMessage.equalsIgnoreCase("StartListening") || uppercaseMessage.equalsIgnoreCase("StopListening") || uppercaseMessage.startsWith("#KICK"))
			{
				sendToServer(message);
				return;
			}
		}
		
		if (uppercaseMessage.equalsIgnoreCase("#Logoff"))
		{
			sendToServer(message);
			DotsApplication.getInstance().closeClientAndServer(false);
		}
		else if (uppercaseMessage.equalsIgnoreCase("#GetHost"))
		{
			if (DotsApplication.getInstance().isServerHost())
			{
				clientUI.display("#CLThe current hostname is: " + InetAddress.getLocalHost().getHostAddress());
			}
			else
			{
				clientUI.display("#CLThe current hostname is: " + getHost());
			}
		}
		else if (uppercaseMessage.equalsIgnoreCase("#GetPort"))
		{
			clientUI.display("#CLThe current port is: " + getPort());
		}
		else if (uppercaseMessage.equalsIgnoreCase("#Quit"))
		{
			sendToServer("#logoff");
			DotsApplication.getInstance().closeClientAndServer(true);
		}
		else
		{
			sendToServer(message);
		}
	}

	/**
	 * Receives input from the user interface and parses the message, then
	 * sends it to the server.
	 * 
	 * @param message the input to be parsed
	 * @see GameClient#performCommand(String)
	 */
	public void handleMessageFromClientUI(String message)
	{
		if (message.charAt(0) == '#')
		{
			try
			{
				performCommand(message);
			}
			catch (IOException ex)
			{
				DotsApplication.getInstance().closeClientAndServer(false);
				DotsApplication.displayErrorMessage("Error sending to server", "An error has occurred while sending a message to the server. You have been disconnected.", ex);
			}
		}
		else
		{
			clientUI.display("#CLThat is not a valid message.");
		}
	}
	
	/**
	 * Attempts to connect to the server
	 * 
	 * @throws IOException error connecting to the server
	 */
	private void connectToServer() throws IOException
	{
		openConnection();
		if (isConnected())
		{
			sendToServer("#Login " + loginID);
		}
	}
	
	public String getLoginID() {return loginID;}
	
	/**
	 * Constructor. Sends host and port to the super constructor, then
	 * tries to connect to the server.
	 * 
	 * @param loginID username the client wishes to use
	 * @param host hostname the client wishes to connect to
	 * @param port port number the client wishes to connect on
	 * @param clientUI user interface which instantiated this object
	 * @see GameClient#connectToServer()
	 */
	public GameClient(String loginID, String host, int port, ChatIF clientUI)
	{
		super(host, port);
		this.loginID = loginID;
		this.clientUI = clientUI;
		
		try
		{
			connectToServer();
		}
		catch (Exception ex)
		{
			DotsApplication.displayErrorMessage("Error connecting to server", "Can't setup connection to server. Host name or port number may be invalid.", ex);
		}
	}
}

