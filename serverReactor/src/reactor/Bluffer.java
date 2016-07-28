package reactor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.google.gson.Gson;

import tokenizer.StringMessage;

public class Bluffer extends Game {
	private String _correctAnswer; 
	private Map<String, ArrayList<String>> _answersGiven; //name of player - value, answerGiven - key
	private ArrayList<String>  _currRoundAnswers; //answers of current round
	private Question[] questions = new Question[3]; //questions for this game
	private int _counter=0; //counter for answers added to know when to print given answers to the players
	private int _roundsCounter=0; //counter for the current round number
	private int _playersCounter=0; //counter for the number of players returned their selected answer in the round
	private int _finishCounter=0; //counter signals all players finished the game
	
	/**
	 * Bluffer game constructor
	 * @param gameName
	 * @param roomName
	 * @param players
	 * @param fileName
	 */

	public Bluffer(String gameName, String roomName, LinkedList<String> players, String fileName) {
		super(gameName, roomName, players, fileName);	
		//_answersGiven = new HashMap<String,ArrayList<String>>();	
	}
	
	/**
	 * start game - get 3 questions and update score table by number of players
	 * start the first round 
	 */
			
	public void startGame () {	
		questions=nextThreeQuestion();
		for (int i=0; i<_players.size(); i++) {
			String nextPlayer = _players.get(i);
			_scoreTable.put(nextPlayer, 0);
		}
		nextRound();
	}	
	
	/**
	 * @return number of answers
	 */	
	
	public int getAnswersSize() {
		return _currRoundAnswers.size();
	}
	
	/**
	 * play three rounds. when last player enter insert new correct answer to list 
	 * and send the next question to players
	 * if finished 3 rounds next time will be sent to finishgame
	 */
	
	public synchronized void nextRound() {
		if (_roundsCounter==3) {
			finishGame();
		}
		else {
			_playersCounter++;
			if (_roundsCounter==0 || _playersCounter==_players.size()) {
				_answersGiven =  new HashMap<String,ArrayList<String>>();	
				_currRoundAnswers = new ArrayList<String>();
				_correctAnswer=questions[_roundsCounter].getAnswer();
				_currRoundAnswers.add(_correctAnswer);
				Iterator<String> iter = _players.iterator();
				while (iter.hasNext()) {
					try {
						_nickToCallback.get(iter.next()).sendMessage(new StringMessage("ASKTXT " + questions[_roundsCounter].getQuestionText()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}	
				_roundsCounter++;
				_playersCounter=0;
			}
		}
	}
	
	/**
	 * insert answer by the user to the list, if already was inserted previously add player to list
	 * will not show same answer twice
	 */
	
	public synchronized void addAnswer(String playername, String answer) {
		if (_answersGiven.containsKey(answer)) {
			_answersGiven.get(answer).add(playername);
		}
		else {
			ArrayList<String> tempPlayers = new ArrayList<String>();
			tempPlayers.add(playername);
			_answersGiven.put(answer,tempPlayers);
			_currRoundAnswers.add(answer);
		}
		_counter++;
	}
	
	/**
	 *@return list of suffeled answers given by the players with the correct answer
	 */	
	
	public ArrayList<String> getAnswers() {
		if (_counter==_players.size()) {			
			Collections.shuffle(_currRoundAnswers);	
			_counter=0;
			return _currRoundAnswers;
		}
		return null;		
	}
	
	/**
	 * add points to the players 
	 * @param nick
	 * @param points
	 */
	
	public void addPoints (String nick, int points) {
		int temp = _scoreTable.get(nick);
		_scoreTable.put(nick, temp+points);
	}
	
	/**
	 * last player enter will finish the game - print score table to the players 
	 */
	
	public synchronized void finishGame () { //we need to close the room and delete things!
		_finishCounter++;
		if(_finishCounter==_players.size()) {
			Iterator<String> iter = _players.iterator();
			String print = print();
			while (iter.hasNext()) {
				try {
					_nickToCallback.get(iter.next()).sendMessage(new StringMessage(print));
				//	_nickToCallback.get(iter,next()).sendMessage("GAMEMSG Winner is: " + winner());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			_finishCounter=0;
			_roundsCounter=0;
			_gamerooms.finish(_roomName);	
		}
	}
		
	private String print() {
		String ans="GAMEMSG Summary: ";
		Iterator <String> iter = _players.iterator();
		while (iter.hasNext()){
			String name = iter.next();
			String temp = "\n\t" + name + " " + _scoreTable.get(name) + "pts";
			ans=ans.concat(temp);
		}		
		return ans;
	}
	
	/**
	 * check the chosen answer for grade
	 * 5pts if someOne chose the player answer
	 * 10pts for correct answer
	 */

	public void chosenAnswer(String nick, int ans) {		
		try {
			if (_currRoundAnswers.get(ans).equals(_correctAnswer)) {
				_nickToCallback.get(nick).sendMessage(new StringMessage("GAMEMSG Correct! + 10pts"));		
				addPoints(nick,10);
			}
			else {
				_nickToCallback.get(nick).sendMessage(new StringMessage("GAMEMSG Wrong answer! :( no worries"));
				String answeredPicked = _currRoundAnswers.get(ans);
				ArrayList<String> bluffNick = _answersGiven.get(answeredPicked); 
				for (int i=0; i<bluffNick.size(); i++) {
					addPoints (bluffNick.get(i),5);
					_nickToCallback.get(bluffNick.get(i)).sendMessage(new StringMessage("GAMEMSG Wow! someOne chose your bluffed answer. +5pts"));	
				}			
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}	{
		
	}
	private Question[] nextThreeQuestion() {
		Question[] questions = new Question[3];
		Gson gson = new Gson();
		BufferedReader br=null;
		try {		
			br = new BufferedReader (new FileReader(System.getProperty("user.dir")+"/" + _fileName + ".json"));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Insert appData = gson.fromJson(br, Insert.class); //getting text from file
		Question[] tempForRandomize = appData.getQuestions();
		int[] numbers = new int[3];
		for (int i=0; i<3; i++) {
			int size = tempForRandomize.length;
			numbers[i] = (int)(Math.random()*size);
			if (i==1 && numbers[1] ==numbers[0]) 
				i--;
			if (i==2 && (numbers[2] == numbers[0] || numbers[2] == numbers[1]))
				i--;
		}
		for (int j=0; j<3; j++) {
			questions[j]=tempForRandomize[numbers[j]];			
		}
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return questions;
	}
	
	/**
	 * return the correct answer
	 */
	
	@Override
	public String getCorrectAns() {
		return _correctAnswer; 
	}
}
