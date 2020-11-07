package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner; 
/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    
    private int firstMove;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
        File handbook = new File("handbook.txt");
        if(!handbook.exists()) {
        	createFile(handbook);
        	ArrayList<String> startingStats = new ArrayList<String>();
        	//12 = 6 index for winning stats and  6 for loosing
        	for (int i=0; i<12; i++) {
        		startingStats.add("0");
        	}
        	writeToFile(startingStats);
        }
    }
    
    public void createFile(File file) {
    	try {
    		file.createNewFile();
    	}
	     catch (IOException e) {
	      System.out.println("An error occurred.");
	      e.printStackTrace();
	    }
    }
    

	public void writeToFile(ArrayList<String> stats) {
		try {
  	      FileWriter writer = new FileWriter("handbook.txt");
  	      for(int i = 0; i < stats.size(); i++) {
  	    	  writer.write(stats.get(i) + "\n");
  	      }
  	      writer.close();
  	    } catch (IOException e) {
  	      System.out.println("An error occurred.");
  	      e.printStackTrace();
  	    }
	}
	
	public ArrayList<String> readFromFile() {
		ArrayList<String> handbookData = new ArrayList<String>();
		try {
		      File handbook = new File("handbook.txt");
		      Scanner scanner = new Scanner(handbook);
		      while (scanner.hasNextLine()) {
		        String data = scanner.nextLine();
		        handbookData.add(data);
		      }
		      scanner.close();
		    } catch (FileNotFoundException e) {
		      System.out.println("An error occurred.");
		      e.printStackTrace();
		    }
		return handbookData;
	}

    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        int firstMove = -1;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                        if(player == 1) 
                        {
                        	//storing of the winning-statistics in the handbook, the position-1 equals the line in the txtf-file  
	                        ArrayList<String> handbookData = this.readFromFile();
	                        int stats = Integer.parseInt(handbookData.get(firstMove-1));
	                        stats++;
	                        handbookData.set(firstMove-1, Integer.toString(stats));
	                        this.writeToFile(handbookData);
                        }
                    }
                    else
                    {
                        addText("I lost...");
                        //storing for the losing-statistics
                        if(player == 1) 
                        {
	                        ArrayList<String> handbookData = this.readFromFile();
	                        int stats = Integer.parseInt(handbookData.get(firstMove+5));
	                        stats++;
	                        handbookData.set(firstMove+5, Integer.toString(stats));
	                        this.writeToFile(handbookData);
                        }
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                	if(player == 1) 
                    {
                        ArrayList<String> handbookData = this.readFromFile();
                        int stats = Integer.parseInt(handbookData.get(firstMove+5));
                        stats++;
                        handbookData.set(firstMove+5, Integer.toString(stats));
                        this.writeToFile(handbookData);
                    }
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            
                        	//check if it is the first move of the game and then save it globally and in the end 
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove;
                            if(firstMove==-1) 
                            {
                            	/*
                            	 * For the simulation, we forced both AIs with a given starting position for the first move, so that we've got
                            	 * every combination. Only the win/loss from the first AI is stored in the handbook. Tie is counted as loss
                            	 */
//                            	if(player == 1) 
//                            	{
//                            	 	firstMove = 1;
//                                	cMove = firstMove;
//                            	} else {
//                            		firstMove = 1;
//                            		cMove = firstMove;
//                            	}
//                            	
                            	/*
                            	 * For the first move, the program is reading the handbook data and compares the winning-statistics of the positions
                            	 * We are calculating the percentage of the total amount of games played, so that we get the correct statistics
                            	 */
                            	ArrayList<String> handbookData = this.readFromFile();
                            	int bestStartMove = 1;
                            	float bestValue = -1;
                            	for(int i = 0; i < 6; i++) {
                            		int win = Integer.parseInt(handbookData.get(i));
                            		int total = Integer.parseInt(handbookData.get(i)) + Integer.parseInt(handbookData.get(i+6));
                            		float posValue = win/total;
                            		if (posValue > bestValue) {
                            			bestValue = posValue;
                            			bestStartMove = i+1;
                            		}
                            	}
                            	firstMove = bestStartMove;
                            	cMove = bestStartMove;
                            } 
                            else 
                            {
                            	cMove = getMove(currentBoard);
                            }
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     * @author Viktor Andersson, Annie Berend, Bheemeswara Aravind Poolla 
     */
    public int getMove(GameState currentBoard)
    {
        int myMove = calculateMove(currentBoard);
        return myMove;
    }
    
    /**
     * The function handles the iterative deepening and calls the minimax-function on every depth
     * it returns the position for the best move
     */
    public int calculateMove(GameState currentBoard) 
    {
    	
    	int bestPosition = 0;
    	int depth = 0;
    	int maxValue = Integer.MIN_VALUE;
    	
    	// calculation of the time, when the searching with the minimax-algorithm should stop
		long t = System.currentTimeMillis();
		long endOfSearch = t+1000;

		while(System.currentTimeMillis() < endOfSearch) 
		{
//    		iterating over the 6 positions
	    	for(int i=1; i<7; i++) 
	    	{
//				check if move is possible
				if(currentBoard.moveIsPossible(i))
				{
				
					int utilityValue = minimax(currentBoard, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, player, i, endOfSearch);
					if(utilityValue > maxValue)
					{
						maxValue = utilityValue;
						bestPosition = i;
					}
				}
			}
	    	depth++;
		}
	
		return bestPosition;
    }
    
    //return value is the ambo that is best: the utility-value is calculated by comparing the amount of pebbles in the houses of the two players
    public int minimax(GameState currentBoard, int depth, int alpha, int beta, int activePlayer, int position, long endOfSearch) 
    {
    	//check if max depth-level is reached
    	if(depth!=0) 
    	{
			GameState clonedBoard = currentBoard.clone();
			clonedBoard.makeMove(position);
			
			//check if game ended
	    	if(clonedBoard.gameEnded()) 
	    	{
	    		int utilityValue;
				if(player == 1) 
				{
					utilityValue = currentBoard.getScore(1)-currentBoard.getScore(2);
				} 
				else 
				{
					utilityValue = currentBoard.getScore(2)-currentBoard.getScore(1);
				}
				return utilityValue;
	    	}
						    	
	    	int maxValue = Integer.MIN_VALUE;
	    	int minValue = Integer.MAX_VALUE;
	    	int posValue;
	    	/**
	    	 * Depending on which player is active, we are calculating the minValue or maxValue
	    	 * in every iteration and we are performing alpha-beta-pruning as well
	    	 */
	    	if(activePlayer==player) 
	    	{
	    	   	for(int i=1; i<7; i++)
	    	   	{
//	    	   		check if it is on the 5-sec-time-limit
	    			if(System.currentTimeMillis() < endOfSearch) 
	    			{
		    	   		if(clonedBoard.moveIsPossible(i)) 
		    	   		{
		    	   			if(player == 1) 
		    	   			{
		    	   				posValue = minimax(clonedBoard, depth-1, alpha, beta, 2, i,endOfSearch);
		    	   			} 
		    	   			else 
		    	   			{
		    	   				posValue = minimax(clonedBoard, depth-1, alpha, beta, 1, i, endOfSearch);
		    	   			}
			        		maxValue = Integer.max(maxValue, posValue);
			        		alpha = Integer.max(alpha, posValue);
//			        		check if pruning is possible
			        		if(beta <= alpha) 
			        		{
			        			break;
			        		}
		    	   		}
	    			}
	    			else 
	    			{
	    				int utilityValue;
	    				if(player == 1) 
	    				{
	    					utilityValue = currentBoard.getScore(1)-currentBoard.getScore(2);
	    				} 
	    				else 
	    				{
	    					utilityValue = currentBoard.getScore(2)-currentBoard.getScore(1);
	    				}
	    				return utilityValue;
	    			}
	        	}
	        	return maxValue;
	    	}
	    	else 
	    	{
	    		for(int i=1; i<7; i++) 
	    		{
	      			if(System.currentTimeMillis() < endOfSearch) 
	      			{
		    			if(clonedBoard.moveIsPossible(i))
		    			{
		    	   			posValue = minimax(clonedBoard, depth-1, alpha, beta, player, i, endOfSearch);
			        		minValue = Integer.min(minValue, posValue);
			        		beta = Integer.min(beta, posValue);
			        		if(beta<=alpha) 
			        		{
			        			break;
			        		}
		    			}
	      			}
	      			else
	      			{
	      				int utilityValue;
	      				if(player == 1) 
	      				{
	      					utilityValue = currentBoard.getScore(1)-currentBoard.getScore(2);
	      				} 
	      				else 
	      				{
	      					utilityValue = currentBoard.getScore(2)-currentBoard.getScore(1);
	      				}
	      				return utilityValue;
	      			}
	        	}
	    		return minValue;
	    	}
		}
    	else 
		{
			int utilityValue;
			if(player == 1) 
			{
				utilityValue = currentBoard.getScore(1)-currentBoard.getScore(2);
			} 
			else 
			{
				utilityValue = currentBoard.getScore(2)-currentBoard.getScore(1);
			}
			return utilityValue;
		}
	}
		
    
    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     * 
     * @return Random ambo number
     */
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }
}