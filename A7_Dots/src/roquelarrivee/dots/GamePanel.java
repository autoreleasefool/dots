/*
 * Joseph Roque 	7284039
 * Matt L'Arrivee 	6657183
 * 
 */

package roquelarrivee.dots;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

/**
 * Canvas to track and display the state of the game being
 * played on the server.
 * 
 * @author Joseph Roque
 * @author Matt L'Arrivee
 *
 */
public class GamePanel extends JPanel implements MouseListener, MouseMotionListener
{
	/**Default generated serialVersionUID*/
	private static final long serialVersionUID = 1L;
	
	/**Array of GameSquare objects representing the state of the game*/
	private GameSquare[] squares = null;
	/**Size of the board*/
	private int gridSize;
	/**Most recent values from getWidth() and getHeight()*/
	private int lastCanvasWidth, lastCanvasHeight;
	
	/**
	 * Constructor. Calls the super constructor then sets defaults
	 * for a new instance of the class
	 */
	public GamePanel()
	{
		super();
		
		this.setMinimumSize(new Dimension(340, 340));
		this.setBackground(Color.white);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (getWidth() != lastCanvasWidth || getHeight() != lastCanvasHeight)
		{
			lastCanvasWidth = getWidth();
			lastCanvasHeight = getHeight();
		}
		
		//Clearing canvas
		g.setColor(new Color(171,214,255));
		g.fillRect(0, 0, getWidth(), getHeight());
		
		int canvasWidth = lastCanvasWidth - 8;
		int canvasHeight = lastCanvasHeight - (DotsApplication.getInstance().isGameInProgress() ? 140:100);
		int boardSize = (canvasWidth > canvasHeight) ? canvasHeight:canvasWidth;

		//Drawing the squares to the screen
		if (squares != null)
		{
			
			int offset = (canvasWidth > canvasHeight) ? ((canvasWidth - canvasHeight) / 2):0;
			g.setColor(Color.white);
			g.fillRect(offset, 0, boardSize+8, boardSize+8);
			GameSquare.resetProperties(boardSize, offset);
			
			for (int ii = 0; ii < squares.length; ii++)
			{
				squares[ii].paint(g);
			}
		}
		
		//Drawing white rectangle for game info
		g.setColor(Color.white);
		g.fillRect(5, boardSize + 13, lastCanvasWidth - 10, lastCanvasHeight - (boardSize + 13) - 5);
		
		//Drawing information on current player's turn
		g.setFont(g.getFont().deriveFont(24f));
		FontMetrics fontMetrics = g.getFontMetrics();
		
		if (DotsApplication.getInstance().isPlayerTurn())
		{
			g.setColor(DotsApplication.getInstance().isPlayerOne() ? Color.red:Color.blue);
			g.drawString("It's your turn!", lastCanvasWidth / 2 - fontMetrics.stringWidth("It's your turn!") / 2, boardSize + 18 + fontMetrics.getHeight());
		}
		else if (DotsApplication.getInstance().isPlayer())
		{
			g.setColor(Color.black);
			g.drawString("It's not your turn...", lastCanvasWidth / 2 - fontMetrics.stringWidth("It's not your turn...") / 2, boardSize + 18 + fontMetrics.getHeight());
		}
		else
		{
			g.setColor(Color.black);
			g.drawString("You are not playing", lastCanvasWidth / 2 - fontMetrics.stringWidth("You are not playing") / 2, boardSize + 18 + fontMetrics.getHeight());
		}
		
		g.setFont(g.getFont().deriveFont(18f));
		fontMetrics = g.getFontMetrics();
		
		g.setColor(Color.black);
		g.drawString("Last winner: " + DotsApplication.getInstance().getWinnerOfLastGame(), 10, boardSize + 47 + fontMetrics.getHeight());
		
		if (DotsApplication.getInstance().isGameInProgress())
		{
			g.setColor(Color.red);
			g.drawString("Player 1: " + DotsApplication.getInstance().getPlayerOneName(), 10, boardSize + 57 + fontMetrics.getHeight() * 2);
			g.setColor(Color.blue);
			g.drawString("Player 2: " + DotsApplication.getInstance().getPlayerTwoName(), 10, boardSize + 60 + fontMetrics.getHeight() * 3);
		}
		
	}
	
	/**
	 * Counts the number of squares which are filled, then compares the count
	 * of squares player 1 finished vs. player 2. If all the squares are finished,
	 * returns either '1' or '2', corresponding to the loser, '3' if it was a tie,
	 * or '0' if the game is not over yet.
	 * 
	 * @return 0-3, 0 meaning the game is not over, 1-2 being the losing player,
	 * 3 meaning a tie.
	 */
	public int getLoserOfGame()
	{
		int playerOneSquareCount = 0;
		int playerTwoSquareCount = 0;
		for (int ii = 0; ii < squares.length; ii++)
		{
			if (squares[ii].isSquareFilled())
			{
				playerOneSquareCount += (squares[ii].getUserWhoCompleted() == 1) ? 1:0;
				playerTwoSquareCount += (squares[ii].getUserWhoCompleted() == 2) ? 1:0;
			}
			else
			{
				return 0;
			}
		}
		
		if (playerOneSquareCount > playerTwoSquareCount)
		{
			return 2;
		}
		else if (playerOneSquareCount < playerTwoSquareCount)
		{
			return 1;
		}
		else
		{
			return 3;
		}
	}
	
	@Override
	public void mousePressed(MouseEvent event)
	{
		//User cannot do anything if it is not their turn
		if (!DotsApplication.getInstance().isPlayerTurn())
		{
			return;
		}
		
		if (squares != null)
		{
			for (int ii = 0; ii < squares.length; ii++)
			{
				//Checks to see if the user is over square and processes the event if so
				if (squares[ii].contains(event.getPoint()))
				{
					
					int lineClicked = squares[ii].getPotentialLineDrawn();
					if (squares[ii].isLineFilled(lineClicked) || lineClicked == 0)
					{
						return;
					}
					DotsApplication.getInstance().disablePlayerTurn();
					DotsApplication.getInstance().sendMessageToClient("#Move " + ii + " " + lineClicked + " " + (DotsApplication.getInstance().isPlayerOne() ? 1:2));
					break;
				}
			}
			repaint();
		}
	}
	
	@Override
	public void mouseMoved(MouseEvent event) {
		//User cannot do anything if they are not playing
		if (!DotsApplication.getInstance().isPlayer())
		{
			return;
		}
		
		if (squares != null)
		{
			//Checks to see if the user is over square and processes the event if so
			Graphics g = this.getGraphics();
			for (int ii = 0; ii < squares.length; ii++)
			{
				squares[ii].erasePotentialLine(g);
				if (!squares[ii].isSquareFilled() && squares[ii].contains(event.getPoint()))
				{
					squares[ii].drawPotentialLine(g, event.getPoint());
				}
			}
			g.dispose();
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent event) {}
	@Override
	public void mouseDragged(MouseEvent event) {}
	@Override
	public void mouseClicked(MouseEvent event) {}
	@Override
	public void mouseEntered(MouseEvent event) {}
	@Override
	public void mouseExited(MouseEvent event) {}
	
	/**
	 * Sets the value of <code>gridSize</code> and instantiates a new
	 * array of <code>GameSquare</code> objects with the values
	 * in <code>gameSquareValues</code>. If <code>gameSquareValues</code>
	 * is null, a value of '0' is used.
	 * 
	 * @param aGridSize the new value for <code>gridSize</code>
	 * @param gameSquareValues the values to instantiate <code>squares</code> with
	 */
	public void setGridSize(int aGridSize, int[] gameSquareValues)
	{
		gridSize = aGridSize;
		GameSquare.setGridSize(gridSize);
		
		squares = new GameSquare[gridSize * gridSize];
		for (int ii = 0; ii < gridSize * gridSize; ii++)
		{
			squares[ii] = new GameSquare(ii % gridSize, ii / gridSize, (gameSquareValues != null ? (gameSquareValues.length >= ii ? gameSquareValues[ii]:0):0));
		}
	}
	
	/**
	 * Getter method for <code>gridSize</code>
	 * 
	 * @return the value of <code>gridSize</code>
	 */
	public int getGridSize() {return gridSize;}
	
	/**
	 * Getter method for <code>squares</code>
	 * 
	 * @return the array of <code>squares</code>
	 */
	public GameSquare[] getSquares()
	{
		return squares;
	}
}
