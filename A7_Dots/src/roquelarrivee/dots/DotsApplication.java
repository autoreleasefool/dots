/*
 * Joseph Roque 	7284039
 * Matt L'Arrivee 	6657183
 * 
 */

package roquelarrivee.dots;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.MenuElement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Singleton class which wraps the GameServer and GameClient
 * in a GUI.
 * 
 * @author Joseph Roque
 * @author Matthew L'Arrivee
 */
public class DotsApplication implements ChatIF
{
	/**Singleton instance of DotsApplication class*/
	private static DotsApplication instance = null;
	
	/**Represents the introduction screen in the CardLayout*/
	private static final String PANEL_INTRO = "Panel_ServerOrClient";
	/**Represents the join server screen in the CardLayout*/
	private static final String PANEL_JOIN_SERVER = "Panel_Login";
	/**Represents the create server screen in the CardLayout*/
	private static final String PANEL_CREATE_SERVER = "Panel_NewServer";
	/**Represents the game screen in the CardLayout*/
	private static final String PANEL_GAME = "Panel_Game";
	
	/**Indicates whether errors should be printed to the console - for debugging*/
	private static final boolean printErrorsToConsole = false;
	/**Indicates whether server status messages should be displayed to the server host*/
	private static boolean controlMessagesEnabled = true;
	
	/**Instance of the game server*/
	private GameServer dotsGameServer = null;
	/**Instance of the game client*/
	private GameClient dotsGameClient = null;
	
	/**Frame which contains the application*/
	private JFrame frame = null;
	/**Dialog to display information about the application*/
	private JDialog aboutDialog = null;
	/**Dialog to display how-to-play information to the user*/
	private JDialog howToDialog = null;
	/**Dialog to display the commands available to the user*/
	private JDialog commandsDialog = null;
	/**Dialog to display the current list of users on the server*/
	private JDialog usersDialog = null;
	/**JList to list the current users on the server*/
	private JList<String> usersList = null;
	/**CardLayout to organize the different screens in the application*/
	private CardLayout cardLayout = null;
	/**Custom styled text pane to display messages to the user*/
	private JTextPane chatMessageArea = null;
	/**Input are for the user*/
	private JTextArea chatInputArea = null;
	/**Label to display the ip address of the server*/
	private JTextArea ipAddressArea = null;
	/**Panel where the game is displayed and played*/
	private GamePanel gamePanel = null;
	
	/**JSplitPane to divide the input field and the custom text pane for messages*/
	private JSplitPane splitMessagesAndInput = null;
	/**JSplitPane to divide the game and the message area*/
	private JSplitPane splitChatAndGame = null;
	
	/**Indicates whether the user is player one*/
	private boolean playerOne = false;
	/**Indicates whether the user is player two*/
	private boolean playerTwo = false;
	/**Indicates whether it is the player's turn or not*/
	private boolean playerTurn = false;
	/**Indicates whether the user has opted to play or watch when joining a server*/
	private boolean playOrWatch = false;
	/**Username of the user who won the last game*/
	private String winnerOfLastGame = "";
	/**Username of player one*/
	private String playerOneName = "";
	/**Username of player two*/
	private String playerTwoName = "";
	/**Indicates whether a game is currently in progress*/
	private boolean gameInProgress;
	/**Count of the number of control messages received by the server*/
	private int controlMessageCount = 0;
	
	/**List of JMenuItems which should not be enabled when the client is not connected*/
	private ArrayList<JMenuItem> connectedClientMenuItems = new ArrayList<JMenuItem>();
	
	/**
	 * Checks to make sure the message is not a reserved command and
	 * begins with the character '#', then sends it to the game client
	 * to be sent to the server.
	 * 
	 * @param message the string to check
	 */
	public void formatMessageForClient(String message)
	{
		//Empty messages are not sent
		if (message.length() == 0)
		{
			return;
		}
		
		if (message.charAt(0) == '#') //If the message is a command from the user
		{
			//Filters certain commands which the user should not have access to
			String messageCheck = message.toUpperCase();
			if (messageCheck.startsWith("#GAME") || messageCheck.startsWith("#MOVE") || messageCheck.startsWith("#SETTINGS") || messageCheck.startsWith("#SET") || messageCheck.startsWith("#INC") || messageCheck.equalsIgnoreCase("#Users"))
			{
				display("#CLThat is not a valid message.");
				return;
			}
			
			sendMessageToClient(message);
		}
		else //any other messages are treated as chat messages
		{
			sendMessageToClient("#MSG" + getLoginID() + ":" + chatInputArea.getText());
		}
	}
	
	/**
	 * Sends a string to the game client to be sent to the server.
	 * @param message
	 */
	public void sendMessageToClient(String message)
	{
		dotsGameClient.handleMessageFromClientUI(message);
	}

	@Override
	public void display(String message)
	{
		boolean shouldPrintMessageToChat = false; //indicates whether a message will be displayed to the user
		
		if (message.startsWith("#BadLogin")) //User has been rejected from the server
		{
			final String finalMessage = message;
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					if (finalMessage.endsWith("NameTaken")) //the username the user chose is already taken
					{
						JOptionPane.showMessageDialog(frame, "Sorry, there is already a user with the name '" + finalMessage.substring(finalMessage.indexOf(" ") + 1, finalMessage.lastIndexOf(" ")) + "' on the server. Please enter another.", "Invalid username", JOptionPane.ERROR_MESSAGE);
					}
					else if (finalMessage.endsWith("Kicked")) //the user has been previously kicked from the server
					{
						JOptionPane.showMessageDialog(frame, "Sorry, you have been kicked from this server. You cannot join.", "You've been kicked!", JOptionPane.ERROR_MESSAGE);
					}
				}
			});
			
			dotsGameClient = null;
		}
		else if (message.startsWith("#Move")) //A move was made in the game
		{
			//Updates the GUI from the event dispatch thread.
			final String finalMessage = message;
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					String[] moveInfo = finalMessage.split(" ");
					int userWhoPlayed = 0, selectedSquare = 0, selectedSide = 0;
					int wasSquareCompleted = 0;
					try {
						selectedSquare = Integer.parseInt(moveInfo[1]);
						selectedSide = Integer.parseInt(moveInfo[2]);
						userWhoPlayed = Integer.parseInt(moveInfo[3]);
					} 
					catch (Exception nfe) {}
					
					switch(selectedSide)
					{
					case GameSquare.TOP_POTENTIAL:
						wasSquareCompleted = gamePanel.getSquares()[selectedSquare].pressed(gamePanel.getSquares(), userWhoPlayed, GameSquare.TOP_POTENTIAL);
						break;
					case GameSquare.RIGHT_POTENTIAL:
						wasSquareCompleted = gamePanel.getSquares()[selectedSquare].pressed(gamePanel.getSquares(), userWhoPlayed, GameSquare.RIGHT_POTENTIAL);
						break;
					case GameSquare.BOTTOM_POTENTIAL:
						wasSquareCompleted = gamePanel.getSquares()[selectedSquare].pressed(gamePanel.getSquares(), userWhoPlayed, GameSquare.BOTTOM_POTENTIAL);
						break;
					case GameSquare.LEFT_POTENTIAL:
						wasSquareCompleted = gamePanel.getSquares()[selectedSquare].pressed(gamePanel.getSquares(), userWhoPlayed, GameSquare.LEFT_POTENTIAL);
						break;
					}
					
					//Checks if it is now the user's turn
					if (isPlayer())
					{
						if (userWhoPlayed == 1)
						{
							playerTurn = (wasSquareCompleted > 0) ? playerOne:playerTwo;
						}
						else
						{
							playerTurn = (wasSquareCompleted > 0) ? playerTwo:playerOne;
						}
					}
					
					//Checks if the game has ended
					if (isServerHost())
					{
						
						
						int loserOfGame = gamePanel.getLoserOfGame();
						if (loserOfGame != 0)
						{
							sendMessageToClient("#GameOver " + loserOfGame);
						}
					}
					
					//Updates the game panel
					gamePanel.repaint();
				}
			});
			
			return;
		}
		else if (message.startsWith("#Settings")) //Server message about the current state of the game
		{
			String[] settingsInfo = message.split(" ");
			int gridSize = Integer.parseInt(settingsInfo[1]);
			gameInProgress = Boolean.parseBoolean(settingsInfo[2]);
			int[] gameSquareValues = null;
			if (gameInProgress)
			{
				playerOneName = settingsInfo[3];
				playerTwoName = settingsInfo[4];
				gameSquareValues = new int[settingsInfo.length - 5];
				for (int ii = 0; ii < gameSquareValues.length; ii++)
				{
					gameSquareValues[ii] = Integer.parseInt(settingsInfo[ii + 5]);
				}
			}
			
			if (playOrWatch)
			{
				sendMessageToClient("#Play");
			}
			showGamePanel(gridSize, gameSquareValues);
			
			return;
		}
		else if (message.startsWith("#GameOver")) //Server message about the game ending
		{
			winnerOfLastGame = message.substring(10);
			message = "#GMThe game has ended! The winner is: " + winnerOfLastGame;
			
			//Increases the player's # of games played, and wins or losses 
			if (isPlayer())
			{
				sendMessageToClient("#IncGPL");
				if (winnerOfLastGame.equalsIgnoreCase(getLoginID()))
				{
					sendMessageToClient("#IncWIN");
				}
				else
				{
					sendMessageToClient("#IncLOS");
				}
			}
			
			playerTurn = false;
			playerOne = false;
			playerTwo = false;
			playerOneName = "";
			playerTwoName = "";
			gameInProgress = false;
			shouldPrintMessageToChat = true;
			
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					gamePanel.repaint();
				}
			});
		}
		else if (message.startsWith("#NewGame")) //Server message about a new game beginning
		{
			//Checks if the user is one of the two players
			String[] newGameInfo = message.split(" ");
			playerOneName = newGameInfo[1];
			playerTwoName = newGameInfo[2];
			if (getLoginID().equals(newGameInfo[1]))
			{
				playerTurn = true;
				playerOne = true;
				playerTwo = false;
			}
			else if (getLoginID().equals(newGameInfo[2]))
			{
				playerTurn = false;
				playerOne = false;
				playerTwo = true;
			}
			else
			{
				playerTurn = false;
				playerOne = false;
				playerTwo = false;
			}
			message = "#GMA new game has begun. The players are:\n"
						+ "Player one: " + newGameInfo[1] + "\n"
						+ "Player two: " + newGameInfo[2];
			startNewGame();
			shouldPrintMessageToChat = true;
		}
		else if (message.startsWith("#Users")) //Server message containing all of the users connected to the server
		{
			final String[] usersNames = message.substring(7).split(" ");
			Arrays.sort(usersNames);
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					DefaultListModel<String> listModel = (DefaultListModel<String>)usersList.getModel();
					listModel.clear();
					for (int ii = 0; ii < usersNames.length; ii++)
					{
						listModel.addElement(usersNames[ii]);
					}
				}
			});
		}
		else if (message.equalsIgnoreCase("#Disconnect")) //Server is disconnecting the client
		{
			if (!isServerHost())
			{
				closeClientAndServer(false);
			}
		}
		else if (message.equalsIgnoreCase("#Kicked")) //Server has kicked the client
		{
			kickedFromServer();
		}
		else if (message.startsWith("#SV") || message.startsWith("#CL") || message.startsWith("#GM") || message.startsWith("#MSG") || message.startsWith("#CT"))
		{
			/*
			 * General message from the server, client, game or another user.
			 * These are simply displayed.
			 */
			shouldPrintMessageToChat = true;
		}
		
		//If the remaining message is not to be displayed, the method exists
		if (!shouldPrintMessageToChat)
		{
			return;
		}
		
		final String messageToPrint = message;
		
		/*
		 * Formats and displays the string to the message area
		 * in the event dispatch thread.
		 */
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				StyledDocument document = chatMessageArea.getStyledDocument();
				try
				{
					/*
					 *	Available styles:
					 *	"serverMessage"	To display messages from the server
					 *	"clientMessage" To display messages from the client
					 *	"gameMessage"	To display messages related to the game
					 *	"chatMessage"	To display chat messages
					 *	"userName"		To display other users' names
					 *	"thisUser"		To display this user's name 
					 */
					if (messageToPrint.startsWith("#SV"))
					{
						if (!messageToPrint.substring(3).startsWith("null"))
						{
							document.insertString(document.getLength(), "SERVER MSG> " + messageToPrint.substring(3) + "\n", chatMessageArea.getStyle("serverMessage"));
						}
					}
					else if (messageToPrint.startsWith("#CL"))
					{
						document.insertString(document.getLength(), "INFO> " + messageToPrint.substring(3) + "\n", chatMessageArea.getStyle("clientMessage"));
					}
					else if (messageToPrint.startsWith("#GM"))
					{
						document.insertString(document.getLength(), "GAME> " + messageToPrint.substring(3) + "\n", chatMessageArea.getStyle("gameMessage"));
					}
					else if (messageToPrint.startsWith("#CT"))
					{
						if (controlMessagesEnabled)
						{
							document.insertString(document.getLength(), "SERVER DATA> " + messageToPrint.substring(3) + "\n", chatMessageArea.getStyle("controlMessage"));
							if (controlMessageCount++ % 20 == 0)
							{
								document.insertString(document.getLength(), "SERVER DATA> To disable these messages, use the 'Server Commands' menu\n", chatMessageArea.getStyle("controlMessage"));
							}
						}
					}
					else if (messageToPrint.startsWith("#MSG" + getLoginID()))
					{
						document.insertString(document.getLength(), messageToPrint.substring(4, messageToPrint.indexOf(":")) + "> ", chatMessageArea.getStyle("thisUser"));
						document.insertString(document.getLength(), messageToPrint.substring(messageToPrint.indexOf(":") + 1) + "\n", chatMessageArea.getStyle("chatMessage"));
					}
					else if (messageToPrint.startsWith("#MSG"))
					{
						document.insertString(document.getLength(), messageToPrint.substring(4, messageToPrint.indexOf(":")) + "> ", chatMessageArea.getStyle("userName"));
						document.insertString(document.getLength(), messageToPrint.substring(messageToPrint.indexOf(":") + 1) + "\n", chatMessageArea.getStyle("chatMessage"));
					}
				} catch (BadLocationException ex){}
			}
		});
	}
	
	/**
	 * Creates the layout for the intro panel and then returns it.
	 * 
	 * @return the completed intro panel.
	 */
	private JPanel createIntroPanel()
	{
		JPanel serverOrClientPanel = new JPanel();
		serverOrClientPanel.setPreferredSize(new Dimension(800, 600));
		serverOrClientPanel.setLayout(new BoxLayout(serverOrClientPanel, BoxLayout.Y_AXIS));
		
		JButton buttonCreateServer = new JButton("Create a Server");
		buttonCreateServer.setFocusPainted(false);
		buttonCreateServer.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				cardLayout.show(frame.getContentPane(), PANEL_CREATE_SERVER);
			}
		});
		
		JButton buttonJoinServer = new JButton("Join a Server");
		buttonJoinServer.setFocusPainted(false);
		buttonJoinServer.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				cardLayout.show(frame.getContentPane(), PANEL_JOIN_SERVER);
			}
		});
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(2, 1));
		buttonPanel.setMaximumSize(new Dimension(200, 150));
		buttonPanel.add(buttonCreateServer);
		buttonPanel.add(buttonJoinServer);
		serverOrClientPanel.add(Box.createVerticalGlue());
		serverOrClientPanel.add(buttonPanel);
		serverOrClientPanel.add(Box.createVerticalGlue());
		
		return serverOrClientPanel;
	}
	
	/**
	 * Creates the layout for the join server panel and then returns it.
	 * 
	 * @return the completed join server panel.
	 */
	private JPanel createJoinServerPanel() {
		JPanel loginPanel = new JPanel();
		loginPanel.setLayout(new BoxLayout(loginPanel, BoxLayout.Y_AXIS));
		
		final JTextField userNameTextField = new JTextField(20);
		final JTextField hostNameTextField = new JTextField("localhost");
		hostNameTextField.setForeground(Color.gray);
		
		/*
		 * Automatically sets the text field's text if it loses focus
		 * and is empty. When focus is regained, if the default text
		 * is set, it clears it.
		 * Default text is "localhost"
		 */
		hostNameTextField.addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(FocusEvent event)
			{
				if (hostNameTextField.getText().equals("localhost"))
				{
					hostNameTextField.setText("");
					hostNameTextField.setForeground(Color.black);
				}
			}
			
			@Override
			public void focusLost(FocusEvent event)
			{
				if (hostNameTextField.getText().length() == 0)
				{
					hostNameTextField.setText("localhost");
					hostNameTextField.setForeground(Color.gray);
				}
			}
		});
		
		/*
		 * Automatically sets the text field's text if it loses focus
		 * and is empty. When focus is regained, if the default text
		 * is set, it clears it.
		 * Default text is "5555"
		 */
		final JTextField portNumberTextField = new JTextField("5555");
		portNumberTextField.setForeground(Color.gray);
		portNumberTextField.addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(FocusEvent event)
			{
				if (portNumberTextField.getText().equals("5555"))
				{
					portNumberTextField.setText("");
					portNumberTextField.setForeground(Color.black);
				}
			}
			
			@Override
			public void focusLost(FocusEvent event)
			{
				if (portNumberTextField.getText().length() == 0)
				{
					portNumberTextField.setText("5555");
					portNumberTextField.setForeground(Color.gray);
				}
			}
		});
		
		JLabel userNameLabel = new JLabel("Username:");
		userNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel hostNameLabel = new JLabel("Host:");
		hostNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel portNumberLabel = new JLabel("Port:");
		portNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		final JRadioButton playRadioButton = new JRadioButton("Play");
		final JRadioButton watchRadioButton = new JRadioButton("Watch");
		
		ButtonGroup radioButtonGroup = new ButtonGroup();
		radioButtonGroup.add(playRadioButton);
		radioButtonGroup.add(watchRadioButton);
		playRadioButton.setSelected(true);
		
		JButton joinServerButton = new JButton("Join Server");
		joinServerButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				joinServer(userNameTextField.getText(), hostNameTextField.getText(), portNumberTextField.getText(), playRadioButton.isSelected());
				userNameTextField.setText("");
				hostNameTextField.setText("localhost");
				portNumberTextField.setText("5555");
				playRadioButton.setSelected(true);
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				cardLayout.show(frame.getContentPane(), PANEL_INTRO);
				userNameTextField.setText("");
				hostNameTextField.setText("localhost");
				portNumberTextField.setText("5555");
				playRadioButton.setSelected(true);
			}
		});
		
		JPanel inputPanel = new JPanel();
		inputPanel.setMaximumSize(new Dimension(350, 200));
		inputPanel.setMinimumSize(new Dimension(350, 200));
		inputPanel.setPreferredSize(new Dimension(350, 200));
		inputPanel.setLayout(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(userNameLabel, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(userNameTextField, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(hostNameLabel, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(hostNameTextField, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(portNumberLabel, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(portNumberTextField, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(playRadioButton, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(watchRadioButton, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(joinServerButton, constraints);
		constraints.weightx = 0.5;
		constraints.weighty = 1.0;
		constraints.gridx = 0;
		constraints.gridy = 5;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(cancelButton, constraints);
		
		JPanel contentPanel = new JPanel();
		contentPanel.add(inputPanel, BorderLayout.CENTER);
		
		loginPanel.add(Box.createVerticalGlue());
		loginPanel.add(contentPanel);
		loginPanel.add(Box.createVerticalGlue());
		
		return loginPanel;
	}
	
	/**
	 * Creates the layout for the new server panel and then returns it.
	 * 
	 * @return the completed new server panel.
	 */
	private JPanel createNewServerPanel()
	{
		JPanel serverPanel = new JPanel();
		
		JLabel userNameLabel = new JLabel("Username:");
		userNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel portNumberLabel = new JLabel("Port");
		portNumberLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		JLabel gridSizeLabel = new JLabel("Grid Size:");
		gridSizeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		
		final JTextField userNameTextField = new JTextField(20);
		final JTextField portNumberTextField = new JTextField("5555");
		portNumberTextField.setForeground(Color.gray);
		
		/*
		 * Automatically sets the text field's text if it loses focus
		 * and is empty. When focus is regained, if the default text
		 * is set, it clears it.
		 * Default text is "5555"
		 */
		portNumberTextField.addFocusListener(new FocusListener()
		{
			@Override
			public void focusGained(FocusEvent event)
			{
				if (portNumberTextField.getText().equals("5555"))
				{
					portNumberTextField.setText("");
					portNumberTextField.setForeground(Color.black);
				}
			}
			
			@Override
			public void focusLost(FocusEvent event)
			{
				if (portNumberTextField.getText().length() == 0)
				{
					portNumberTextField.setText("5555");
					portNumberTextField.setForeground(Color.gray);
				}
			}
		});
		
		final JSlider gridSizeSlider = new JSlider();
		gridSizeSlider.setMinimum(3);
		gridSizeSlider.setMaximum(9);
		gridSizeSlider.setMajorTickSpacing(1);
		gridSizeSlider.setPaintTicks(true);
		gridSizeSlider.setPaintLabels(true);
		gridSizeSlider.setSnapToTicks(true);
		gridSizeSlider.setValue(5);
		
		final JRadioButton playRadioButton = new JRadioButton("Play");
		final JRadioButton watchRadioButton = new JRadioButton("Watch");
		
		ButtonGroup radioButtonGroup = new ButtonGroup();
		radioButtonGroup.add(playRadioButton);
		radioButtonGroup.add(watchRadioButton);
		playRadioButton.setSelected(true);
		
		JButton createServerButton = new JButton("Create Server");
		createServerButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				createServer(userNameTextField.getText(), portNumberTextField.getText(), gridSizeSlider.getValue(), playRadioButton.isSelected());
				userNameTextField.setText("");
				portNumberTextField.setText("5555");
				gridSizeSlider.setValue(5);
				playRadioButton.setSelected(true);
			}
		});
		
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				cardLayout.show(frame.getContentPane(), PANEL_INTRO);
				userNameTextField.setText("");
				portNumberTextField.setText("5555");
				gridSizeSlider.setValue(5);
				playRadioButton.setSelected(true);
			}
		});
		
		JPanel inputPanel = new JPanel();
		inputPanel.setMaximumSize(new Dimension(350, 200));
		inputPanel.setMinimumSize(new Dimension(350, 200));
		inputPanel.setPreferredSize(new Dimension(350, 200));
		inputPanel.setLayout(new GridBagLayout());
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(userNameLabel, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(userNameTextField, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(portNumberLabel, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(portNumberTextField, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(gridSizeLabel, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(gridSizeSlider, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(playRadioButton, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 1;
		constraints.gridy = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(watchRadioButton, constraints);
		constraints.weightx = 0.5;
		constraints.gridx = 0;
		constraints.gridy = 4;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(createServerButton, constraints);
		constraints.weightx = 0.5;
		constraints.weighty = 1.0;
		constraints.gridx = 0;
		constraints.gridy = 5;
		constraints.gridwidth = 2;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		inputPanel.add(cancelButton, constraints);
		
		JPanel contentPanel = new JPanel();
		contentPanel.add(inputPanel, BorderLayout.CENTER);
		
		serverPanel.setLayout(new BoxLayout(serverPanel, BoxLayout.Y_AXIS));
		serverPanel.add(Box.createVerticalGlue());
		serverPanel.add(contentPanel);
		serverPanel.add(Box.createVerticalGlue());
		
		return serverPanel;
	}
	
	/**
	 * Creates the layout for the game panel and then returns it.
	 * 
	 * @return the completed game panel.
	 */
	private JPanel createGamePanel()
	{
		gamePanel = new GamePanel();
		
		JPanel dotsPanel = new JPanel();
		dotsPanel.setLayout(new BorderLayout());
		dotsPanel.setPreferredSize(new Dimension(550, 600));
		dotsPanel.add(gamePanel, BorderLayout.CENTER);
		
		JPanel chatPanel = new JPanel();
		chatPanel.setLayout(new BorderLayout());
		chatPanel.setPreferredSize(new Dimension(250, 600));
		
		chatMessageArea = new JTextPane();
		chatMessageArea.setEditable(false);
		DefaultCaret caret = (DefaultCaret)chatMessageArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		StyleConstants.setForeground(chatMessageArea.addStyle("serverMessage", null), Color.red);
		StyleConstants.setForeground(chatMessageArea.addStyle("chatMessage", null), Color.black);
		StyleConstants.setForeground(chatMessageArea.addStyle("userName", null), Color.green);
		StyleConstants.setForeground(chatMessageArea.addStyle("thisUser", null), Color.blue);
		StyleConstants.setForeground(chatMessageArea.addStyle("clientMessage", null), Color.pink);
		StyleConstants.setForeground(chatMessageArea.addStyle("gameMessage", null), Color.orange);
		StyleConstants.setForeground(chatMessageArea.addStyle("controlMessage", null), Color.darkGray);
		
		final JButton sendMessageButton = new JButton("Send");
		sendMessageButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent event)
			{
				formatMessageForClient(chatInputArea.getText().trim());
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						chatInputArea.setText("");
						chatInputArea.requestFocusInWindow();
					}
				});
		}
		});
		
		chatInputArea = new JTextArea();
		chatInputArea.setLineWrap(true);
		chatInputArea.setWrapStyleWord(true);
		chatInputArea.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent event)
			{
				if (event.getKeyCode() == KeyEvent.VK_ENTER)
				{
					if (!event.isShiftDown())
					{
						sendMessageButton.doClick();
					}
					else
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								chatInputArea.append("\n");
							}
						});
					}
					event.consume();
				}
			}
		});
		
		JScrollPane messageScrollPane = new JScrollPane(chatMessageArea);
		messageScrollPane.setMinimumSize(new Dimension(140, 150));
		JScrollPane inputScrollPane = new JScrollPane(chatInputArea);
		inputScrollPane.setMinimumSize(new Dimension(140, 50));
		
		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new BorderLayout());
		inputPanel.add(inputScrollPane, BorderLayout.CENTER);
		inputPanel.add(sendMessageButton, BorderLayout.SOUTH);
		
		ipAddressArea = new JTextArea("IP Address of Server: \n\n");
		ipAddressArea.setEnabled(false);
		ipAddressArea.setWrapStyleWord(true);
		ipAddressArea.setLineWrap(true);
		
		splitMessagesAndInput = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
		splitMessagesAndInput.setTopComponent(messageScrollPane);
		splitMessagesAndInput.setBottomComponent(inputPanel);
		splitMessagesAndInput.setResizeWeight(1.0);
		chatPanel.add(ipAddressArea, BorderLayout.NORTH);
		chatPanel.add(splitMessagesAndInput, BorderLayout.CENTER);
		
		splitChatAndGame = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
		splitChatAndGame.setLeftComponent(gamePanel);
		splitChatAndGame.setRightComponent(chatPanel);
		splitChatAndGame.setResizeWeight(1.0);
		
		JPanel gamePanel = new JPanel();
		gamePanel.setLayout(new BorderLayout());
		gamePanel.add(splitChatAndGame, BorderLayout.CENTER);

		return gamePanel;
	}
	
	/**
	 * Creates the JMenuBar for the frame and all of its submenus and items
	 * then returns it.
	 * 
	 * @return the completed JMenuBar
	 */
	private JMenuBar createJMenuBar()
	{
		MenuBarActionListener menuBarActionListener = new MenuBarActionListener();
		
		//"File" Menu
		JMenuItem aboutMenuItem = new JMenuItem("About Dots");
		aboutMenuItem.setActionCommand("#FILE About");
		aboutMenuItem.addActionListener(menuBarActionListener);
		JMenuItem logOffMenuItem = new JMenuItem("Log off");
		logOffMenuItem.setActionCommand("#FILE Logoff");
		logOffMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(logOffMenuItem);
		JMenuItem quitMenuItem = new JMenuItem("Quit");
		quitMenuItem.setActionCommand("#FILE Quit");
		quitMenuItem.addActionListener(menuBarActionListener);
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(aboutMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(logOffMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(quitMenuItem);
		
		//"Game" Menu
		JMenuItem joinQueueMenuItem = new JMenuItem("Join Queue");
		joinQueueMenuItem.setActionCommand("#GAME #Play");
		joinQueueMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(joinQueueMenuItem);
		JMenuItem leaveQueueMenuItem = new JMenuItem("Leave Queue");
		leaveQueueMenuItem.setActionCommand("#GAME #Leave");
		leaveQueueMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(leaveQueueMenuItem);
		JMenuItem forfeitMenuItem = new JMenuItem("Forfeit Game");
		forfeitMenuItem.setActionCommand("#GAME #Forfeit");
		forfeitMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(forfeitMenuItem);
		
		JMenu gameMenu = new JMenu("Game");
		gameMenu.add(joinQueueMenuItem);
		gameMenu.add(leaveQueueMenuItem);
		gameMenu.addSeparator();
		gameMenu.add(forfeitMenuItem);
		
		//"Get Info" Menu
		JMenuItem positionMenuItem = new JMenuItem("My position in queue?");
		positionMenuItem.setActionCommand("#GET #Position");
		positionMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(positionMenuItem);
		JMenuItem winsMenuItem = new JMenuItem("How many wins?");
		winsMenuItem.setActionCommand("#GET #GetWIN");
		winsMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(winsMenuItem);
		JMenuItem lossesMenuItem = new JMenuItem("How many losses?");
		lossesMenuItem.setActionCommand("#GET #GetLOS");
		lossesMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(lossesMenuItem);
		JMenuItem tiesMenuItem = new JMenuItem("How many ties?");
		tiesMenuItem.setActionCommand("#GET #GetTIE");
		tiesMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(tiesMenuItem);
		JMenuItem gamesPlayedMenuItem = new JMenuItem("How many games played?");
		gamesPlayedMenuItem.setActionCommand("#GET #GetGPL");
		gamesPlayedMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(gamesPlayedMenuItem);
		JMenuItem statsMenuItem = new JMenuItem("Retrieve all stats");
		statsMenuItem.setActionCommand("#GET #GetSTA");
		statsMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(statsMenuItem);
		JMenuItem playersMenuItem = new JMenuItem("Who's playing?");
		playersMenuItem.setActionCommand("#GET #CurrentPlayers");
		playersMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(playersMenuItem);
		JMenuItem getHostNameMenuItem = new JMenuItem("Get Host Name");
		getHostNameMenuItem.setActionCommand("#GET #GetHost");
		getHostNameMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(getHostNameMenuItem);
		JMenuItem getPortNumberMenuItem = new JMenuItem("Get Port Number");
		getPortNumberMenuItem.setActionCommand("#GET #GetPort");
		getPortNumberMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(getPortNumberMenuItem);
		
		JMenu getMenu = new JMenu("Get Info");
		getMenu.add(positionMenuItem);
		getMenu.add(playersMenuItem);
		getMenu.addSeparator();
		getMenu.add(winsMenuItem);
		getMenu.add(lossesMenuItem);
		getMenu.add(tiesMenuItem);
		getMenu.add(gamesPlayedMenuItem);
		getMenu.add(statsMenuItem);
		getMenu.addSeparator();
		getMenu.add(getHostNameMenuItem);
		getMenu.add(getPortNumberMenuItem);
		
		//"Server Commands" Menu
		JMenuItem controlMessagesMenuItem = new JMenuItem("Disable Server Info Messages");
		controlMessagesMenuItem.setActionCommand("#SV Ctrl");
		controlMessagesMenuItem.addActionListener(menuBarActionListener);
		JMenuItem stopListeningMenuItem = new JMenuItem("Stop Listening");
		stopListeningMenuItem.setActionCommand("#SV #StopListening");
		stopListeningMenuItem.addActionListener(menuBarActionListener);
		JMenuItem startListeningMenuItem = new JMenuItem("Start Listening");
		startListeningMenuItem.setActionCommand("#SV #StartListening");
		startListeningMenuItem.addActionListener(menuBarActionListener);
		JMenuItem kickPlayerMenuItem = new JMenuItem("Kick Player");
		kickPlayerMenuItem.setActionCommand("#SV #Kick");
		kickPlayerMenuItem.addActionListener(menuBarActionListener);
		
		JMenu serverMenu = new JMenu("Server Commands");
		serverMenu.add(controlMessagesMenuItem);
		serverMenu.addSeparator();
		serverMenu.add(stopListeningMenuItem);
		serverMenu.add(startListeningMenuItem);
		serverMenu.addSeparator();
		serverMenu.add(kickPlayerMenuItem);
		
		//"Help" Menu
		JMenuItem howToPlayMenuItem = new JMenuItem("Getting Started");
		howToPlayMenuItem.setActionCommand("#HELP How");
		howToPlayMenuItem.addActionListener(menuBarActionListener);
		JMenuItem commandsMenuItem = new JMenuItem("Available Commands");
		commandsMenuItem.setActionCommand("#HELP Commands");
		commandsMenuItem.addActionListener(menuBarActionListener);
		JMenuItem usersMenuItem = new JMenuItem("List of users");
		usersMenuItem.setActionCommand("#HELP #Users");
		usersMenuItem.addActionListener(menuBarActionListener);
		connectedClientMenuItems.add(usersMenuItem);
		
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(howToPlayMenuItem);
		helpMenu.add(commandsMenuItem);
		helpMenu.addSeparator();
		helpMenu.add(usersMenuItem);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add(gameMenu);
		menuBar.add(getMenu);
		menuBar.add(serverMenu);
		menuBar.add(helpMenu);
		
		return menuBar;
	}
	
	/**
	 * Confirms that all input is valid (if not, displays a message to the user)
	 * then creates a new instance of GameServer and begins listening
	 * for clients. Then, joins the server as a client on a local connection.
	 * 
	 * @param userName the user-provided input for a username
	 * @param portNumber the user-provided number for a port to create the server on
	 * @param gridSize the size of the game field
	 * @param playOrWatch whether the user will be added to list of players waiting to play
	 */
	private void createServer(final String userName, final String portNumber, int gridSize, boolean playOrWatch)
	{
		int portNumberIntValue = 0;
		if (userName.equalsIgnoreCase("Server"))
		{
			JOptionPane.showMessageDialog(frame, "Sorry, '" + userName + "' is not a valid name. Please enter another.", "Invalid username", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (!userName.matches("^[-_a-zA-Z0-9]{1,16}"))
		{
			JOptionPane.showMessageDialog(frame, "Sorry, your username must consist of only numbers, letters and dashes, and be between 1-16 characters. Please enter another.", "Invalid username", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try {
			portNumberIntValue = Integer.parseInt(portNumber);
		}
		catch (NumberFormatException nfe)
		{
			int response = JOptionPane.showConfirmDialog(frame, "Sorry, but '" + portNumber + "' is not a valid port number. The default value '5555' will be used, is that okay?", "Invalid port number", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.YES_OPTION)
			{
				portNumberIntValue = 5555;
			}
			else
			{
				return;
			}
		}
		
		dotsGameServer = new GameServer(portNumberIntValue);
		try
		{
			dotsGameServer.listen();
		}
		catch (IOException ex)
		{
			displayErrorMessage("Error initializing server", "Could not listen for clients! Possible cause: port may already be in use.", ex);
			dotsGameServer = null;
			return;
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				Document document = ipAddressArea.getDocument();
				try
				{
					document.remove(0, document.getLength());
					document.insertString(0, "IP Address of Server: \n" + InetAddress.getLocalHost().getHostAddress() + "\nPort: " + dotsGameServer.getPort(), null);
				}
				catch (Exception ex){}
			}
		});
		
		setServerCommandsEnabled(true);
		gamePanel.setGridSize(gridSize, null);
		joinServer(userName, "localhost", portNumber, playOrWatch);
	}
	
	/**
	 * Confirms that all input is valid (if not, displays a message to the user)
	 * then creates a new instance of GameClient and connects to the 
	 * server specified by <code>hostName</code>.
	 * 
	 * @param userName the user-provided input for username
	 * @param hostName the user-provided input for host name
	 * @param portNumber the user-provided input for a port number
	 * @param playOrWatch whether the user will be added to list of players waiting to play
	 */
	private void joinServer(String userName, String hostName, String portNumber, boolean playOrWatch)
	{
		int portNumberIntValue = 0;
		
		if (hostName.equals(""))
		{
			JOptionPane.showMessageDialog(frame, "You must enter a host name.", "Invalid host name", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		if (userName.equalsIgnoreCase("Server"))
		{
			JOptionPane.showMessageDialog(frame, "Sorry, '" + userName + "' is not a valid name. Please enter another.", "Invalid username", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else if (!userName.matches("^[-_a-zA-Z0-9]{1,16}"))
		{
			JOptionPane.showMessageDialog(frame, "Sorry, your username must consist of only numbers, letters and dashes, and be between 1-16 characters. Please enter another.", "Invalid username", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try {
			portNumberIntValue = Integer.parseInt(portNumber);
		}
		catch (NumberFormatException nfe)
		{
			int response = JOptionPane.showConfirmDialog(frame, "Sorry, but '" + portNumber + "' is not a valid port number. The default value '5555' will be used, is that okay?", "Invalid port number", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (response == JOptionPane.YES_OPTION)
			{
				portNumberIntValue = 5555;
			}
			else
			{
				return;
			}
		}
		
		this.playOrWatch = playOrWatch;
		dotsGameClient = new GameClient(userName, hostName, portNumberIntValue, this);
		
		if (dotsGameServer == null)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					Document document = ipAddressArea.getDocument();
					try
					{
						document.remove(0, document.getLength());
						document.insertString(0, "IP Address of Server: \n" + dotsGameClient.getHost() + "\nPort: " + dotsGameClient.getPort(), null);
					}
					catch (BadLocationException ex) {}
				}
			});
		}
		
		setConnectedClientCommandsEnabled(true);
	}
	
	/**
	 * Calls the <code>closeConnection()</code> method on <code>dotsGameClient</code>
	 * if it is not null, then calls <code>close()</code> on <code>dotsGameServer</code>
	 * if it is not null. Finally, either exits the application or returns to the
	 * intro panel depending on <code>shouldExit</code>.
	 * 
	 * @param shouldExit Method will call System.exit(0) if true
	 */
	public void closeClientAndServer(boolean shouldExit)
	{
		boolean closed = false;
		try
		{
			if (dotsGameClient != null)
			{
				dotsGameClient.closeConnection();
				closed = true;
			}
		}
		catch (IOException ex) {}
		dotsGameClient = null;
		
		try
		{
			if (dotsGameServer != null)
			{
				dotsGameServer.close();
				closed = true;
			}
		}
		catch (IOException ex) {}
		
		dotsGameServer = null;
		
		if (shouldExit)
		{
			System.exit(0);
		}
		else
		{
			controlMessageCount = 0;
			controlMessagesEnabled = true;
			playerOne = playerTwo = playerTurn = gameInProgress = false;
			playerOneName = playerTwoName = winnerOfLastGame = "";
			
			setConnectedClientCommandsEnabled(false);
			setServerCommandsEnabled(false);
			
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					cardLayout.show(frame.getContentPane(), PANEL_INTRO);
					chatInputArea.setText("");
					StyledDocument document = chatMessageArea.getStyledDocument();
					Document document2 = ipAddressArea.getDocument();
					try
					{
						document.remove(0, document.getLength());
						document2.remove(0, document2.getLength());
					}
					catch (BadLocationException e){}
				}
			});
			if (closed)
			{
				displayErrorMessage("Connection lost!", "You have lost connection to the server and have been returned to the title screen.");
			}
		}
	}
	
	/**
	 * Calls the <code>closeConnection()</code> method on <code>dotsGameClient</code>
	 * if it is not null, then returns to the intro panel.
	 */
	private void kickedFromServer()
	{
		try
		{
			if (dotsGameClient != null)
			{
				dotsGameClient.closeConnection();
			}
		}
		catch (IOException ex) {}
		dotsGameClient = null;
		
		controlMessageCount = 0;
		controlMessagesEnabled = true;
		playerOne = playerTwo = playerTurn = gameInProgress = false;
		playerOneName = playerTwoName = winnerOfLastGame = "";
		
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				cardLayout.show(frame.getContentPane(), PANEL_INTRO);
				chatInputArea.setText("");
				StyledDocument document = chatMessageArea.getStyledDocument();
				try
				{
					document.remove(0, document.getLength());
				}
				catch (BadLocationException e){}
			}
		});
		displayErrorMessage("Connection lost!", "The server host kicked you and you have been returned to the title screen");
	}
	
	/**
	 * Clears the gamePanel and its objects then repaints it to be empty.
	 */
	private void startNewGame()
	{
		gameInProgress = true;
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				gamePanel.setGridSize(gamePanel.getGridSize(), null);
				gamePanel.repaint();
			}
		});
	}
	
	/**
	 * Sets the state of <code>gamePanel</code> to match the parameters provided
	 * then displays the game panel in the event dispatch thread.
	 * 
	 * @param gridSize the size of game grid
	 * @param gameSquareValues state values for the game squares
	 */
	private void showGamePanel(int gridSize, int[] gameSquareValues)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{	
				if (!isServerHost())
				{
					gamePanel.setGridSize(gridSize, gameSquareValues);
				}
				
				cardLayout.show(frame.getContentPane(), PANEL_GAME);
				splitMessagesAndInput.setDividerLocation(0.7);
				splitChatAndGame.setDividerLocation(0.7);
			}
		});
	}
	
	/**
	 * Displays a message box to the user using <code>title</code> and <code>message</code>.
	 * If an exception was included, its stack trace is output to the console
	 * 
	 * @param title title for the message box representing the error
	 * @param message text for the message box describing the error
	 * @param ex optional. Exception stack trace to print to the screen
	 */
	public static void displayErrorMessage(String title, String message, Exception... ex)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				JOptionPane.showMessageDialog(DotsApplication.getInstance().getFrame(), message, title, JOptionPane.ERROR_MESSAGE);
				if (DotsApplication.shouldPrintToConsole()) {
					System.out.println(message);
					for (int ii = 0; ii < ex.length; ii++)
					{
						ex[ii].printStackTrace();
					}
				}
			}
		});
		
	}
	
	/**
	 * Enables or disables commands which should not be available
	 * when the client is not connected.
	 * 
	 * @param enable true to enable the commands
	 */
	private void setConnectedClientCommandsEnabled(boolean enable)
	{
		for (int ii = 0; ii<connectedClientMenuItems.size(); ii++)
		{
			connectedClientMenuItems.get(ii).setEnabled(enable);
		}
	}
	
	/**
	 * Enables or disables the server commands menu.
	 * 
	 * @param enable true to enable the menu
	 */
	private void setServerCommandsEnabled(boolean enable)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				MenuElement[] topLevelElements = frame.getJMenuBar().getSubElements();
				for (MenuElement menuElement:topLevelElements)
				{
					if (menuElement instanceof JMenu)
					{
						JMenu menu = (JMenu) menuElement;
						if (menu.getText().equals("Server Commands"))
						{
							menu.setEnabled(enable);
							return;
						}
					}
				}
			}
		});
	}
	
	/**
	 * Constructor. Creates the application frame, adds the panels
	 * and displays the intro panel to the user.
	 */
	private DotsApplication()
	{
		frame = new JFrame();
		frame.setTitle("Dots");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(cardLayout = new CardLayout());
		frame.getContentPane().add(createIntroPanel(), PANEL_INTRO);
		frame.getContentPane().add(createJoinServerPanel(), PANEL_JOIN_SERVER);
		frame.getContentPane().add(createNewServerPanel(), PANEL_CREATE_SERVER);
		frame.getContentPane().add(createGamePanel(), PANEL_GAME);
		frame.setJMenuBar(createJMenuBar());
		frame.pack();
		frame.setMinimumSize(new Dimension(500, 400));
		frame.setLocationRelativeTo(null);
		
		setServerCommandsEnabled(false);
		setConnectedClientCommandsEnabled(false);
		
		cardLayout.show(frame.getContentPane(), PANEL_INTRO);
		frame.setVisible(true);
	}
	
	/**
	 * Sets the visibility of <code>howToDialog</code> to true. If <code>howToDialog</code>
	 * is null, it is created. Takes place in the event dispatch thread.
	 */
	private void showHowToDialog()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (howToDialog == null)
				{
					howToDialog = new JDialog();
					howToDialog.setTitle("How to Play");
					howToDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
					
					JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
					
					JPanel howToJoinPanel = new JPanel();
					howToJoinPanel.setLayout(new BoxLayout(howToJoinPanel, BoxLayout.Y_AXIS));
					JLabel titleLabel = new JLabel("<html><center>Logging In<br></center></html>");
					titleLabel.setFont(titleLabel.getFont().deriveFont(24f));
					titleLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
					titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
					howToJoinPanel.add(titleLabel);
					JLabel contentLabel = new JLabel("<html><body style='width:300px'><br>When you first open the application, you have two choices:"
							+ "<br><br>1. Create a new Server"
							+ "<br>2. Join a Server"
							+ "<br><br>To create a server, you must specify a username, which must be "
							+ "between 1 and 16 characters (inclusive) and be composed of "
							+ "alphanumeric characters, dashes and underscores. "
							+ "<br>Next, you must choose a port which the server will be run through. "
							+ "If you aren't sure what this does, the default value of \"5555\" "
							+ "will be used."
							+ "<br>Next, you can choose a size for the board. Once you create the "
							+ "server, this value cannot be changed. We recommend an odd value "
							+ "so that ties cannot occur in the game. "
							+ "<br>Finally, you can select to play the game or simply wait and watch "
							+ "when you first join the server. "
							+ "<br><br>To join a server, the process is similar, but you must now "
							+ "specify the host name and port number which the server is "
							+ "using. To get these values, the server can see them in the "
							+ "top right of their window.</body></html>");
					contentLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
					contentLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
					howToJoinPanel.add(contentLabel);
					JPanel tabbedPanel = new JPanel(new BorderLayout());
					tabbedPanel.setPreferredSize(new Dimension(500, 300));
					tabbedPanel.add(new JScrollPane(howToJoinPanel), BorderLayout.CENTER);
					tabbedPane.add("Logging In", tabbedPanel);
					
					JPanel howToPlayPanel = new JPanel();
					howToPlayPanel.setLayout(new BoxLayout(howToPlayPanel, BoxLayout.Y_AXIS));
					titleLabel = new JLabel("<html><center>Playing the Game</center></html>");
					titleLabel.setFont(titleLabel.getFont().deriveFont(24f));
					titleLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
					titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
					howToPlayPanel.add(titleLabel);
					contentLabel = new JLabel("<html><body style='width:300px'>"
							+ "<br>To play the game, first you must wait enter the queue "
							+ "and wait for your turn. Do this by typing \"#Play\" in the "
							+ "bottom right text box and pressing send. When you reach the "
							+ "front of the queue, a new game will automatically be started. "
							+ "<br><br>Playing the game is simple. When it is your turn, choose "
							+ "one of the lines on the screen to fill. If you fill in the last "
							+ "remaining side of a square, it is considered yours and will "
							+ "be filled in with your color. "
							+ "<br><br><b>The object of the game</b> is to fill more squares than "
							+ "your opponent. Each turn you *must* select a line to fill "
							+ "and need to be strategic so you end up with more squares than "
							+ "your opponent. "
							+ "<br><br>At the end of a game, the winner and loser will be decided "
							+ "and announced. Then, the winner will be placed back at the top of the "
							+ "queue and will get to play another game. The loser will be removed from "
							+ "the queue and must opt in again. In the event of a tie, both players "
							+ "will be removed from the queue. "
							+ "<br><br>If at any time you decide you no longer wish to play, "
							+ "you can type \"#Leave\" into the bottom right text box if you "
							+ "aren't currently playing, or \"#Forfeit\" if you wish to "
							+ "quit the current game.</body></html>");
					contentLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
					contentLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
					howToPlayPanel.add(contentLabel);
					tabbedPanel = new JPanel(new BorderLayout());
					tabbedPanel.setPreferredSize(new Dimension(500, 300));
					tabbedPanel.add(new JScrollPane(howToPlayPanel), BorderLayout.CENTER);
					tabbedPane.add("Playing the Game", tabbedPanel);
					
					JPanel otherPanel = new JPanel();
					otherPanel.setLayout(new BoxLayout(otherPanel, BoxLayout.Y_AXIS));
					titleLabel = new JLabel("<html><center>Other</center></html>");
					titleLabel.setFont(titleLabel.getFont().deriveFont(24f));
					titleLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
					titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
					otherPanel.add(titleLabel);
					contentLabel = new JLabel("<html><body style='width:300px'>"
							+ "<br>Besides playing Dots, there are a number of other "
							+ "things you can perform from within this application. "
							+ "<br><br>First and foremost, there is a chat system. You can "
							+ "chat with the other users on the server by simply typing a message "
							+ "in the bottom right text box and either pressing the send button "
							+ "or enter. "
							+ "<br><br>There is also a number of commands available to any user "
							+ "at all times. To view these commands, select \"Help\" from the top "
							+ "menu and choosing \"Available Commands\". Here, you will find "
							+ "a list of the commands available and what each of them does. There "
							+ "are also commands which are available to the server host only. These "
							+ "are depicted by the tag \"(Server)\". "
							+ "<br><br>Finally, you can log off the server at any time. This will "
							+ "bring you back to intro screen and you can proceed to create your own "
							+ "server, join a new server, or rejoin the server you just quit."
							+ "<br>If you are the server host and you leave a server, all the clients "
							+ "currently connected to that server will be returned to the intro screen.</body></html>");
					contentLabel.setAlignmentY(JLabel.CENTER_ALIGNMENT);
					contentLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
					otherPanel.add(contentLabel);
					tabbedPanel = new JPanel(new BorderLayout());
					tabbedPanel.setPreferredSize(new Dimension(500, 300));
					tabbedPanel.add(new JScrollPane(otherPanel), BorderLayout.CENTER);
					tabbedPane.add("Other", tabbedPanel);
					
					howToDialog.add(tabbedPane);
					howToDialog.pack();
				}
				
				if (!howToDialog.isVisible())
				{
					howToDialog.setLocationRelativeTo(frame);
					howToDialog.setVisible(true);
				}
			}
		});
	}
	
	/**
	 * Sets the visibility of <code>commandsDialog</code> to true. If <code>commandsDialog</code>
	 * is null, it is created. Takes place in the event dispatch thread.
	 */
	private void showCommandsDialog()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (commandsDialog == null)
				{
					commandsDialog = new JDialog();
					commandsDialog.setTitle("Available Commands");
					commandsDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
					commandsDialog.setMinimumSize(new Dimension(100, 150));
					
					TableModel dataModel = new AbstractTableModel()
					{
						private static final long serialVersionUID = 1L;
						public int getColumnCount() {return 2;}
						public int getRowCount() {return 17;}
						public Object getValueAt(int row, int col)
						{
							if (col == 0)
							{
								switch(row)
								{
								case 0: return "#Play";
								case 1: return "#Leave";
								case 2: return "#Position";
								case 3: return "#CurrentPlayers";
								case 4: return "#GetWIN <loginID>";
								case 5: return "#GetLOS <loginID>";
								case 6: return "#GetTIE <loginID>";
								case 7: return "#GetGPL <loginID>";
								case 8: return "#GetSTA <loginID>";
								case 9: return "#GetHost";
								case 10: return "#GetPort";
								case 11: return "#Logoff";
								case 12: return "#Quit";
								case 13: return "#Forfeit";
								case 14: return "#StopListening (Server)";
								case 15: return "#StartListening (Server)";
								case 16: return "#Kick  <loginID> (Server)";
								}
							}
							else
							{
								switch(row)
								{
								case 0: return "Add yourself to the queue";
								case 1: return "Remove yourself from the queue";
								case 2: return "Get your position in the queue";
								case 3: return "Gets the names of the players currently playing";
								case 4: return "Get the numbers of wins of the player with the ID <loginID>";
								case 5: return "Get the numbers of losses of the player with the ID <loginID>";
								case 6: return "Get the numbers of ties of the player with the ID <loginID>";
								case 7: return "Get the numbers of games played of the player with the ID <loginID>";
								case 8: return "Get the all stats of the player with the ID <loginID>";
								case 9: return "Get the current host name";
								case 10: return "Get the current port number";
								case 11: return "Quit the server and return to the main menu";
								case 12: return "Quit the server and the program";
								case 13: return "If you are playing, forfeits the game";
								case 14: return "Server command. Stop accepting new clients";
								case 15: return "Server command. Start accepting new clients";
								case 16: return "Kicks the player with the ID <loginID> and bans them from returning";
								}
							}
							
							return "";
						}
					};
					
					JTable table = new JTable(dataModel);
					table.getColumnModel().getColumn(0).setMaxWidth(150);
					table.getColumnModel().getColumn(0).setMinWidth(150);
					table.getColumnModel().getColumn(0).setHeaderValue("Command");
					table.getColumnModel().getColumn(1).setHeaderValue("Result");
					commandsDialog.add(new JScrollPane(table));
					commandsDialog.pack();
				}
				
				if (!commandsDialog.isVisible())
				{
					commandsDialog.setLocationRelativeTo(frame);
					commandsDialog.setVisible(true);
				}
			}
		});
	}
	
	/**
	 * Sets the visibility of <code>aboutDialog</code> to true. If <code>aboutDialog</code>
	 * is null, it is created. Takes place in the event dispatch thread.
	 */
	private void showAboutDialog()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (aboutDialog == null)
				{
					aboutDialog = new JDialog();
					aboutDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
					aboutDialog.setTitle("About");
					aboutDialog.setLayout(new BorderLayout());
					aboutDialog.setResizable(false);
					
					JLabel aboutLabel = new JLabel("<html><center>About Dots<br><br>"
													+ "Dots provides users with a fun gaming<br>"
													+ "experience. Users can play a game of<br>"
													+ "'Dots' with their friends, while chatting<br>"
													+ "and competing to see who's the best!<br><br>"
													+ "Created by<br>"
													+ "Matt L'Arrivee and Joseph Roque<br><br>"
													+ "&copy; 2014</center></html>");
					aboutLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
					aboutDialog.add(aboutLabel, BorderLayout.CENTER);
					aboutDialog.pack();
				}
				
				if (!aboutDialog.isVisible())
				{
					aboutDialog.setLocationRelativeTo(frame);
					aboutDialog.setVisible(true);
				}
			}
		});
	}
	
	/**
	 * Sets the visibility of <code>usersDialog</code> to true. If <code>usersDialog</code>
	 * is null, it is created. Takes place in the event dispatch thread.
	 */
	private void showUsersDialog()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (usersDialog == null)
				{
					usersDialog = new JDialog();
					usersDialog.setTitle("Current Users");
					usersDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
					
					DefaultListModel<String> usersModel = new DefaultListModel<String>();
					usersModel.addElement("Waiting for list to populate...");
					
					usersList = new JList<String>(usersModel);
					usersList.setCellRenderer(new DefaultListCellRenderer()
					{
						private static final long serialVersionUID = 1L;

						@Override
						public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
						{
							JLabel component = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
							component.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
							return component;
						}
					});
					JPanel contentPanel = new JPanel();
					contentPanel.setLayout(new BorderLayout());
					contentPanel.setPreferredSize(new Dimension(100,200));
					contentPanel.add(new JScrollPane(usersList), BorderLayout.CENTER);
					
					usersDialog.add(contentPanel);
					usersDialog.pack();
					
					usersDialog.addComponentListener(new ComponentAdapter(){
						@Override
						public void componentHidden(ComponentEvent e)
						{
							DefaultListModel<String> listModel = (DefaultListModel<String>)usersList.getModel();
							listModel.clear();
							listModel.addElement("Waiting for list to populate...");
						}
					});
				}
				
				if (!usersDialog.isVisible())
				{
					usersDialog.setLocationRelativeTo(frame);
					usersDialog.setVisible(true);
				}
			}
		});
	}
	
	/**
	 * Returns the JFrame containing the application
	 * 
	 * @return the value of <code>frame</code>
	 */
	public JFrame getFrame() {return frame;}
	/**
	 * Returns the GamePanel containing the current game state
	 * 
	 * @return the value of <code>gamePanel</code>
	 */
	public GamePanel getGamePanel() {return gamePanel;}
	/**
	 * Returns true if strings should be printed to the console
	 * for debugging, false otherwise.
	 * 
	 * @return the current value of <code>printErrorsToConsole</code>
	 */
	public static boolean shouldPrintToConsole() {return printErrorsToConsole;}
	
	/**
	 * Gets the login id of the user from the game client
	 * 
	 * @return The login id of the user
	 */
	public String getLoginID()
	{
		if (dotsGameClient != null)
		{
			return dotsGameClient.getLoginID();
		}
		return null;
	}
	
	/**
	 * Returns true if the user is hosting a server, false otherwise.
	 * 
	 * @return true if <code>dotsGameServer</code> is not null (there is an instance 
	 * of the server), false otherwise.
	 */
	public boolean isServerHost()
	{
		return dotsGameServer != null;
	}
	
	/**
	 * Returns the username of the user who won the last game
	 * 
	 * @return the value of <code>winnerOfLastGame</code>
	 */
	public String getWinnerOfLastGame() {return winnerOfLastGame;}
	
	/**
	 * Returns true if the user is a player in the current game, false otherwise.
	 * 
	 * @return true if <code>playerOne</code> or <code>playerTwo</code> is true, false otherwise.
	 */
	public boolean isPlayer() {return playerOne || playerTwo;}
	/**
	 * Indicates whether the user is player one
	 * 
	 * @return the value of <code>playerOne</code>
	 */
	public boolean isPlayerOne() {return playerOne;}
	/**
	 * Indicates whether the user is player two.
	 * 
	 * @return the value of <code>playerTwo</code>
	 */
	public boolean isPlayerTwo() {return playerTwo;}
	
	/**
	 * Indicates whether it is the user's turn in the game.
	 * @return the value of <code>playerTurn</code>
	 */
	public boolean isPlayerTurn() {return playerTurn;}
	
	/**
	 * Sets <code>playerTurn</code> to false
	 */
	public void disablePlayerTurn() {playerTurn = false;}
	
	/**
	 * Indicates whether a game is currently in progress
	 * 
	 * @return the value of <code>gameInProgress</code>
	 */
	public boolean isGameInProgress() {return gameInProgress;}
	
	/**
	 * Returns the username of player one
	 * 
	 * @return the value of <code>playerOneName</code>
	 */
	public String getPlayerOneName() {return playerOneName;}
	
	/**
	 * Returns the username of player two
	 * 
	 * @return the value of <code>playerTwoName</code>
	 */
	public String getPlayerTwoName() {return playerTwoName;}
	
	/**
	 * Singleton method. Returns an instance of DotsApplication.
	 * If one does not exist, it is created.
	 * 
	 * @return the value of <code>instance</code>
	 */
	public static DotsApplication getInstance()
	{
		if (instance == null)
		{
			instance = new DotsApplication();
		}
		return instance;
	}
	
	/**
	 * When the program closes, a short method will be run
	 * which will disconnect all the clients if the user
	 * is running a server.
	 */
	private static void addShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				if (DotsApplication.getInstance().dotsGameServer != null)
				{
					DotsApplication.getInstance().dotsGameServer.sendToAllClients("#Disconnect");
				}
			}
		});
	}

	/**
	 * Main method. Creates an instance of DotsApplication in the AWT
	 * event dispatch thread.
	 * 
	 * @param args not used
	 */
	public static void main(String[] args)
	{
		/*
		 * Setting VM properties
		 * We were having some issues with the GUI
		 * appearing wrong on some Windows machines.
		 * We found changing these properties as a
		 * suggested solution.
		 * 
		 * Source:
		 * http://stackoverflow.com/questions/8081559/is-this-a-swing-java-7-rendering-bug
		 * http://stackoverflow.com/questions/870472/corrupted-java-swing-window
		 */
		System.setProperty("sun.java2d.d3d", "false");
		System.setProperty("sun.java2d.ddoffscreen", "false");
		System.setProperty("sun.java2d.noddraw", "false");
		
		
		addShutdownHook();
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				DotsApplication.getInstance();
			}
		});
	}
	
	/**
	 * ActionListener to respond to events from the menu bar.
	 * 
	 * @author Joseph Roque
	 * @author Matthew Larrivee
	 *
	 */
	private static class MenuBarActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			if (event.getActionCommand().startsWith("#FILE"))
			{
				fileCommand(event.getActionCommand().substring(6));
			}
			else if (event.getActionCommand().startsWith("#GAME"))
			{
				gameCommand(event.getActionCommand().substring(6));
			}
			else if (event.getActionCommand().startsWith("#GET"))
			{
				getCommand(event.getActionCommand().substring(5));
			}
			else if (event.getActionCommand().startsWith("#SV"))
			{
				if (event.getActionCommand().endsWith("Ctrl"))
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							String messageToSet = "";
							JMenuItem menuItem = (JMenuItem) event.getSource();
							if (menuItem.getText().startsWith("En"))
							{
								messageToSet = "Disable Server Info Messages";
								controlMessagesEnabled = true;
							}
							else
							{
								messageToSet = "Enable Server Info Messages";
								controlMessagesEnabled = false;
							}
							menuItem.setText(messageToSet);
						}
					});
					return;
				}
				serverCommand(event.getActionCommand().substring(4));
			}
			else if (event.getActionCommand().startsWith("#HELP"))
			{
				helpCommand(event.getActionCommand().substring(6));
			}
		}
		
		/**
		 * Parses  "File" menu events
		 * @param command the action command from the menu which was activated
		 */
		private void fileCommand(String command)
		{
			if (command.equals("About"))
			{
				DotsApplication.getInstance().showAboutDialog();
			}
			else if (command.equals("Logoff"))
			{
				DotsApplication.getInstance().sendMessageToClient("#Logoff");
			}
			else if (command.equals("Quit"))
			{
				if (DotsApplication.getInstance().dotsGameClient != null
						&& DotsApplication.getInstance().dotsGameClient.isConnected())
				{
					DotsApplication.getInstance().sendMessageToClient("#Quit");
				}
				else
				{
					System.exit(0);
				}
			}
		}
		
		/**
		 * Parses  "Game" menu events
		 * @param command the action command from the menu which was activated
		 */
		private void gameCommand(String command)
		{
			DotsApplication.getInstance().sendMessageToClient(command);
		}
		
		/**
		 * Parses  "Get Info" menu events
		 * @param command the action command from the menu which was activated
		 */
		private void getCommand(String command)
		{
			if (command.equals("#GetHost") || command.equals("#GetPort"))
			{
				DotsApplication.getInstance().sendMessageToClient(command);
			}
			else if (command.startsWith("#Get"))
			{
				String title = "";
				if (command.endsWith("WIN"))
					title = "Number of Wins";
				else if (command.endsWith("LOS"))
					title = "Number of Losses";
				else if (command.endsWith("TIE"))
					title = "Number of Ties";
				else if (command.endsWith("GPL"))
					title = "Number of Games Played";
				else if (command.endsWith("STA"))
					title = "All Stats";
				else
					return;
				
				String userID = JOptionPane.showInputDialog(DotsApplication.getInstance().getFrame(), "Please enter the name of the user who you wish you get this information on.", title, JOptionPane.QUESTION_MESSAGE);
				if (userID == null || userID.length() == 0)
				{
					return;
				}
				
				DotsApplication.getInstance().sendMessageToClient(command + " " + userID);
			}
			else
			{
				DotsApplication.getInstance().sendMessageToClient(command);
			}
		}
		
		/**
		 * Parses  "Server Commands" menu events
		 * @param command the action command from the menu which was activated
		 */
		private void serverCommand(String command)
		{
			if (command.startsWith("#K"))
			{
				String userID = JOptionPane.showInputDialog(DotsApplication.getInstance().getFrame(), "Please enter the name of the user you wish to kick.", "Kick Player?", JOptionPane.QUESTION_MESSAGE);
				if (userID == null || userID.length() == 0)
				{
					return;
				}
				
				int response = JOptionPane.showConfirmDialog(DotsApplication.getInstance().getFrame(), "Are you SURE you wish to do this? The player will not be able to rejoin the server.", "Kick Player?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
				if (response == JOptionPane.OK_OPTION)
				{
					DotsApplication.getInstance().sendMessageToClient(command + " " + userID);
				}
			}
			else
			{
				DotsApplication.getInstance().sendMessageToClient(command);
			}
		}
		
		/**
		 * Parses  "Help" menu events
		 * @param command the action command from the menu which was activated
		 */
		private void helpCommand(String command)
		{
			if (command.equals("How"))
			{
				DotsApplication.getInstance().showHowToDialog();
			}
			else if (command.equals("Commands"))
			{
				DotsApplication.getInstance().showCommandsDialog();
			}
			else
			{
				DotsApplication.getInstance().showUsersDialog();
				DotsApplication.getInstance().sendMessageToClient(command);
			}
		}
	}
}
