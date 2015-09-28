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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;

/**
 * Stores information on which sides of a square in a game
 * of 'Dots' have been connected, and provides methods
 * for connecting them
 *
 * @author Joseph Roque
 * @author Matthew L'Arrivee
 *
 */
public class GameSquare
{
	/**Value representing the top side of a square*/
	public static final int TOP_POTENTIAL = 1;
	/**Value representing the right side of a square*/
	public static final int RIGHT_POTENTIAL = 2;
	/**Value representing the bottom side of a square*/
	public static final int BOTTOM_POTENTIAL = 4;
	/**Value representing the left side of a square*/
	public static final int LEFT_POTENTIAL = 8;

	/**The current size of the grid*/
	private static int gridSize;
	/**The current width/height of the board*/
	private static int boardSize;
	/**How far to offset drawing from the left of the frame*/
	private static int offsetFromLeft;
	/**The width and height of a single square on the board*/
	private static int boardSizeFraction;

	/**Indicates whether the top of the square has been filled or not*/
	private boolean topFilled;
	/**Indicates whether the right of the square has been filled or not*/
	private boolean rightFilled;
	/**Indicates whether the bottom of the square has been filled or not*/
	private boolean bottomFilled;
	/**Indicates whether the left of the square has been filled or not*/
	private boolean leftFilled;
	/**Indicates whether player '1' or player '2' filled the last side of the square*/
	private int userWhoCompleted;

	/**The x position of the square in the grid*/
	private final int gridX;
	/**The y position of the square in the grid*/
	private final int gridY;
	/**Location of the left side of the square when it is drawn*/
	private int xx;
	/**Location of the top side of the square when it is drawn*/
	private int yy;

	/**The side which the user is currently holding their mouse over*/
	private int potentialLineDrawn;

	/**
	 * Constructor which sets the position of the square in the grid
	 * and fills the relevant sides depending on the value of
	 * <code>sidesFilled</code>
	 *
	 * @param aGridX new value for <code>gridX</code>
	 * @param aGridY new value for <code>gridY</code>
	 * @param sidesFilled indicates which sides should be filled
	 */
	public GameSquare(int aGridX, int aGridY, int sidesFilled)
	{
		this.gridX = aGridX;
		this.gridY = aGridY;

		if (sidesFilled / LEFT_POTENTIAL > 0)
		{
			leftFilled = true;
			sidesFilled = sidesFilled % 8;
		}
		if (sidesFilled / BOTTOM_POTENTIAL > 0)
		{
			bottomFilled = true;
			sidesFilled = sidesFilled % 4;
		}
		if (sidesFilled / RIGHT_POTENTIAL > 0)
		{
			rightFilled = true;
			sidesFilled = sidesFilled % 2;
		}
		if (sidesFilled / TOP_POTENTIAL > 0)
		{
			topFilled = true;
		}
	}

	/**
	 * Called when a user clicks on a square in the GUI.
	 *
	 * @param squares the array of GameSquare objects being used to display the game
	 * @param aUserWhoCompleted player '1' or player '2'
	 * @param aPotentialLineDrawn new value for <code>potentialLineDrawn</code>, or <code>0</code> if it should not be used
	 *
	 * @return the number of squares which had all sides filled after calling this method
	 */
	public int pressed(GameSquare[] squares, int aUserWhoCompleted, int aPotentialLineDrawn)
	{
		if (aPotentialLineDrawn == TOP_POTENTIAL || aPotentialLineDrawn == RIGHT_POTENTIAL || aPotentialLineDrawn == BOTTOM_POTENTIAL || aPotentialLineDrawn == LEFT_POTENTIAL)
		{
			potentialLineDrawn = aPotentialLineDrawn;
		}

		int wasSquareCompleted = 0;

		switch(potentialLineDrawn)
		{
		case TOP_POTENTIAL:
			if (setTopFilled(true, aUserWhoCompleted))
			{
				wasSquareCompleted++;
			}
			potentialLineDrawn = 0;
			if(gridY > 0)
			{
				if (squares[(gridY - 1) * gridSize + gridX].setBottomFilled(true, aUserWhoCompleted))
				{
					wasSquareCompleted++;
				}
			}
			break;
		case RIGHT_POTENTIAL:
			if (setRightFilled(true, aUserWhoCompleted))
			{
				wasSquareCompleted++;
			}
			potentialLineDrawn = 0;
			if (gridX < gridSize - 1)
			{
				if (squares[gridY * gridSize + gridX + 1].setLeftFilled(true, aUserWhoCompleted))
				{
					wasSquareCompleted++;
				}
			}
			break;
		case BOTTOM_POTENTIAL:
			if (setBottomFilled(true, aUserWhoCompleted))
			{
				wasSquareCompleted++;
			}
			potentialLineDrawn = 0;
			if (gridY < gridSize - 1)
			{
				if (squares[(gridY + 1) * gridSize + gridX].setTopFilled(true, aUserWhoCompleted))
				{
					wasSquareCompleted++;
				}
			}
			break;
		case LEFT_POTENTIAL:
			if (setLeftFilled(true, aUserWhoCompleted))
			{
				wasSquareCompleted++;
			}
			potentialLineDrawn = 0;
			if (gridX > 0)
			{
				if (squares[gridY * gridSize + gridX - 1].setRightFilled(true, aUserWhoCompleted))
				{
					wasSquareCompleted++;
				}
			}
			break;
		default:
		}

		return wasSquareCompleted;
	}

	/**
	 * Draws a light gray line at the edge of the square on the graphics object
	 * if <code>point</code> is over a side which is not filled.
	 *
	 * @param g the graphics contex to draw to
	 * @param point position of the mouse relative to the frame, or any point
	 */
	public void drawPotentialLine(Graphics g, Point point)
	{
		g.setColor(Color.lightGray);
		Polygon topPoly = new Polygon(new int[]{xx,xx + boardSizeFraction / 2,xx + boardSizeFraction}, new int[]{yy, yy + boardSizeFraction / 2, yy}, 3);
		if (!topFilled && topPoly.contains(point))
		{
			g.fillRect(xx + 4, yy - 3, boardSizeFraction - 8, 6);
			potentialLineDrawn = TOP_POTENTIAL;
			return;
		}

		Polygon rightPoly = new Polygon(new int[]{xx + boardSizeFraction, xx + boardSizeFraction / 2, xx + boardSizeFraction}, new int[]{yy, yy + boardSizeFraction / 2, yy + boardSizeFraction}, 3);
		if (!rightFilled && rightPoly.contains(point))
		{
			g.fillRect(xx + boardSizeFraction - 3, yy + 4, 6, boardSizeFraction - 8);
			potentialLineDrawn = RIGHT_POTENTIAL;
			return;
		}

		Polygon bottomPoly = new Polygon(new int[]{xx, xx + boardSizeFraction / 2, xx + boardSizeFraction}, new int[]{yy + boardSizeFraction, yy + boardSizeFraction / 2, yy + boardSizeFraction}, 3);
		if (!bottomFilled && bottomPoly.contains(point))
		{
			g.fillRect(xx + 4, yy + boardSizeFraction - 3, boardSizeFraction - 8, 6);
			potentialLineDrawn = BOTTOM_POTENTIAL;
			return;
		}

		Polygon leftPoly = new Polygon(new int[]{xx, xx + boardSizeFraction / 2, xx}, new int[]{yy, yy + boardSizeFraction / 2, yy + boardSizeFraction}, 3);
		if (!leftFilled && leftPoly.contains(point))
		{
			g.fillRect(xx - 3, yy + 4, 6, boardSizeFraction - 8);
			potentialLineDrawn = LEFT_POTENTIAL;
			return;
		}

		potentialLineDrawn = 0;
	}

	/**
	 * Draws a white box over any gray lines which have been drawn
	 * between two squares
	 *
	 * @param g the graphics context to draw to
	 */
	public void erasePotentialLine(Graphics g)
	{
		g.setColor(Color.white);
		switch(potentialLineDrawn)
		{
		case TOP_POTENTIAL:
			g.fillRect(xx + 4, yy - 3, boardSizeFraction - 8, 6);
			break;
		case RIGHT_POTENTIAL:
			g.fillRect(xx + boardSizeFraction - 3, yy + 4, 6, boardSizeFraction - 8);
			break;
		case BOTTOM_POTENTIAL:
			g.fillRect(xx + 4, yy + boardSizeFraction - 3, boardSizeFraction - 8, 6);
			break;
		case LEFT_POTENTIAL:
			g.fillRect(xx - 3, yy + 4, 6, boardSizeFraction - 8);
			break;
		default:
		}
		potentialLineDrawn = 0;
	}

	/**
	 * Returns true if the point is within the bounds of the square, false otherwise.
	 *
	 * @param point position of the mouse relative to the screen, or any point
	 * @return true if the point lies within the bounds of the square, vertically and horizontally, false otherwise
	 */
	public boolean contains(Point point)
	{
		return (point.x >= xx && point.x <= xx + boardSizeFraction && point.y >= yy && point.y <= yy + boardSizeFraction);
	}

	/**
	 * Sets the values <code>boardSize</code> and <code>offsetFromLeft</code>
	 * and calculates <code>boardSizeFraction</code>
	 *
	 * @param aBoardSize new value for <code>boardSize</code>
	 * @param aOffsetFromLeft new value for <code>offsetFromLeft</code>
	 */
	public static void resetProperties(int aBoardSize, int aOffsetFromLeft)
	{
		boardSize = aBoardSize;
		offsetFromLeft = aOffsetFromLeft;

		boardSizeFraction = boardSize / gridSize;
	}

	/**
	 * Paints the component to the graphics context
	 *
	 * @param g graphics context to draw to
	 */
	public void paint(Graphics g)
	{
		xx = (offsetFromLeft + boardSizeFraction * gridX) + 4;
		yy = (boardSizeFraction * gridY) + 4;

		if (isSquareFilled())
		{
			g.setColor(userWhoCompleted == 1 ? Color.red:Color.blue);
			g.fillRect(xx, yy, boardSizeFraction, boardSizeFraction);
		}

		g.setColor(Color.gray);
		if (topFilled)
		{
			g.fillRect(xx + 4, yy - ((gridY == 0) ? 3:0), boardSizeFraction - 8, (gridY == 0 ? 6:3));
		}
		if (rightFilled)
		{
			g.fillRect(xx + boardSizeFraction - 3, yy + 4, (gridX == gridSize - 1 ? 6:3), boardSizeFraction - 8);
		}
		if (bottomFilled)
		{
			g.fillRect(xx + 4, yy + boardSizeFraction - 3, boardSizeFraction - 8, (gridY == gridSize - 1 ? 6:3));
		}
		if (leftFilled)
		{
			g.fillRect(xx - ((gridX == 0) ? 3:0), yy + 4, (gridX == 0 ? 6:3), boardSizeFraction - 8);
		}

		g.setColor(Color.black);
		g.fillRect(xx - ((gridX == 0) ? 4:0), yy - ((gridY == 0) ? 4:0), (gridX == 0 ? 8:4), (gridY == 0 ? 8:4));
		g.fillRect(xx + boardSizeFraction - 4, yy - ((gridY == 0) ? 4:0), (gridX == gridSize - 1 ? 8:4), (gridY == 0 ? 8:4));
		g.fillRect(xx - ((gridX == 0) ? 4:0), yy + boardSizeFraction - 4, (gridX == 0 ? 8:4), (gridY == gridSize - 1 ? 8:4));
		g.fillRect(xx + boardSizeFraction - 4, yy + boardSizeFraction - 4, (gridX == gridSize - 1 ? 8:4), (gridY == gridSize - 1 ? 8:4));
	}

	/**
	 * Returns true if all 4 sides are filled, false otherwise
	 *
	 * @return <code>topFilled && rightFilled && bottomFiled && leftFilled</code>
	 */
	public boolean isSquareFilled()
	{
		return topFilled && rightFilled && bottomFilled && leftFilled;
	}

	/**
	 * Sets <code>topFilled</code> to <code>aTopFilled</code> and checks
	 * if the square is now completed.
	 *
	 * @param aTopFilled new value for <code>topFilled</code>
	 * @param aUserWhoCompleted player '1' or player '2', '0' if neither
	 * @return true if all four sides of the square are now filled, false otherwise
	 */
	public boolean setTopFilled(boolean aTopFilled, int aUserWhoCompleted)
	{
		topFilled = aTopFilled;
		return checkIfCompleted(aUserWhoCompleted);
	}

	/**
	 * Sets <code>rightFilled</code> to <code>aRightFilled</code> and checks
	 * if the square is now completed.
	 *
	 * @param aRightFilled new value for <code>rightFilled</code>
	 * @param aUserWhoCompleted player '1' or player '2', '0' if neither
	 * @return true if all four sides of the square are now filled, false otherwise
	 */
	public boolean setRightFilled(boolean aRightFilled, int aUserWhoCompleted)
	{
		rightFilled = aRightFilled;
		return checkIfCompleted(aUserWhoCompleted);
	}

	/**
	 * Sets <code>bottomFilled</code> to <code>aBottomFilled</code> and checks
	 * if the square is now completed.
	 *
	 * @param aBottomFilled new value for <code>bottomFilled</code>
	 * @param aUserWhoCompleted player '1' or player '2', '0' if neither
	 * @return true if all four sides of the square are now filled, false otherwise
	 */
	public boolean setBottomFilled(boolean aBottomFilled, int aUserWhoCompleted)
	{
		bottomFilled = aBottomFilled;
		return checkIfCompleted(aUserWhoCompleted);
	}

	/**
	 * Sets <code>leftFilled</code> to <code>aLeftFilled</code> and checks
	 * if the square is now completed.
	 *
	 * @param aLeftFilled new value for <code>leftFilled</code>
	 * @param aUserWhoCompleted player '1' or player '2', '0' if neither
	 * @return true if all four sides of the square are now filled, false otherwise
	 */
	public boolean setLeftFilled(boolean aLeftFilled, int aUserWhoCompleted) {
		leftFilled = aLeftFilled;
		return checkIfCompleted(aUserWhoCompleted);
	}

	/**
	 * Setter method for <code>gridSize</code>
	 *
	 * @param aGridSize the new value for <code>gridSize</code>
	 */
	public static void setGridSize(int aGridSize) {gridSize = aGridSize;}

	/**
	 * Getter method for <code>userWhoCompleted</code>
	 *
	 * @return the value of <code>userWhoCompleted</code>
	 */
	public int getUserWhoCompleted() {return userWhoCompleted;}

	/**
	 * Getter method for <code>potentialLineDrawn</code>
	 *
	 * @return the value of <code>potentialLineDrawn</code>
	 */
	public int getPotentialLineDrawn() {return potentialLineDrawn;}

	/**
	 * Returns a value from 0-15 (inclusive), depending on which sides of the square are filled.
	 *
	 * @return a value from 0-15
	 */
	public int getSideValue()
	{
		return ((topFilled) ? TOP_POTENTIAL:0) + ((rightFilled) ? RIGHT_POTENTIAL:0) + ((bottomFilled) ? BOTTOM_POTENTIAL:0) + ((leftFilled) ? LEFT_POTENTIAL:0);
	}

	/**
	 * Checks if all four sides of the square are filled. If so, then <code>userWhoCompleted</code>
	 * is set to <code>aUserWhoCompleted</code> and the method returns true. Returns false otherwise.
	 *
	 * @param aUserWhoCompleted new value for <code>userWhoCompleted</code>
	 * @return true if all four sides of the square are filled, false otherwise,
	 */
	private boolean checkIfCompleted(int aUserWhoCompleted)
	{
		if(isSquareFilled())
		{
			userWhoCompleted = aUserWhoCompleted;
			return true;
		}

		return false;
	}

	/**
	 * Returns the boolean corresponding to <code>lineValue</code>, which should be
	 * one of <code>TOP_POTENTIAL</code>, <code>RIGHT_POTENTIAL</code>, <code>BOTTOM_POTENTIAL</code>, <code>LEFT_POTENTIAL</code>
	 *
	 * @param lineValue which side to check
	 * @return the value of either <code>topFilled</code>, <code>rightFilled</code>, <code>bottomFilled</code> or <code>leftFilled</code>
	 */
	public boolean isLineFilled(int lineValue)
	{
		switch(lineValue)
		{
		case TOP_POTENTIAL: return topFilled;
		case RIGHT_POTENTIAL: return rightFilled;
		case BOTTOM_POTENTIAL: return bottomFilled;
		case LEFT_POTENTIAL: return leftFilled;
		}
		return false;
	}
}
