package reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import protocol.ProtocolCallback;
import tokenizer.StringMessage;

public abstract class Game { //abstract class of a game
	protected String _gameName;
	protected GameRooms _gamerooms;
	protected String _roomName;
	protected LinkedList <String> _players;
	protected Map <String, ProtocolCallback<StringMessage>> _nickToCallback;
	protected Map<String, Integer> _scoreTable;
	protected String _fileName;
	
	/**
	 * Game constructor.
	 * @param gameName - game type
	 * @param roomName - room name
	 * @param players - players list in the room
	 * @param fileName - json file to get questions from
	 */
	
	public Game (String gameName, String roomName, LinkedList<String> players, String fileName) {
		_gameName = gameName;
		_gamerooms = GameRooms.getInstance();
		_roomName=roomName;
		_players = players;
		_nickToCallback = new HashMap <String, ProtocolCallback<StringMessage>>();
		_scoreTable = new HashMap <String, Integer>();
		_fileName=fileName;
	}
	/**
	 * 
	 * @return roomname
	 */
	
	public String getRoomName () {
		return _roomName;
	}

	/**
	 * 
	 * @return list of players in the room
	 */
	
	public LinkedList <String> getPlayers () {
		return _players;
	}
	
	/**
	 * abstract startgame will be implemented for each game
	 */

	public abstract void startGame ();
	
	/**
	 * abstract getAnswers will be implemented for each game
	 * @return List of answers
	 */
	
	public abstract ArrayList<String> getAnswers();
	
	/**
	 * abstract addAnswer will be implemented for each game
	 * @param playername
	 * @param answer to insert
	 */
	
	public abstract void addAnswer(String playername, String answer);
	
	/**
	 * abstract chosenAnswer will be implemented for each game
	 * @param nick
	 * @param ans
	 */

	public abstract void chosenAnswer(String nick, int ans);
	
	/**
	 * add callback and nick to a map
	 * @param nick
	 * @param callback
	 */

	public void addCallback(String nick, ProtocolCallback<StringMessage> callback) {
		_nickToCallback.put(nick, callback);
	}
	
	/**
	 * abstract nextRound will be implemented for each game
	 */

	public abstract void nextRound();
	
	/**
	 * abstract getAnswersSize will be implemented for each game
	 * @return
	 */

	public abstract int getAnswersSize();
	
	/**
	 * abstract addCorrectAns will be implemented for each game
	 * @return
	 */

	public abstract String getCorrectAns();
}