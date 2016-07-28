package server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRooms {
	
	private Map <String, LinkedList<String>> _rooms; //name of room and a list of players
	private Map <String, Boolean> _isInGame; // room names and signal if in play
	private Map <String, String> _gamersCurrRoom; //gamer is the key room is the value
	private Map<ProtocolCallback<String>, String> _callbacksToNicks;
	private Map<String, ProtocolCallback<String>> _nicksToCallbacks;
	private LinkedList <String> _games; //name of games
	private Map <String, Game> _liveGame; //name of Room - key, Live Game - value
	private String _questionsFileName;
	
	
	//Create singelthon of the gameRooms
	private static class GameRoomsHolder {
		private static GameRooms instance = new GameRooms();		
	}
	/**
	 * Constructor of the game rooms
	 */
	private GameRooms () {
		_rooms = new ConcurrentHashMap <String, LinkedList<String>>();
		_isInGame = new ConcurrentHashMap <String, Boolean>();
		_gamersCurrRoom = new ConcurrentHashMap <String, String>();
		_callbacksToNicks = new ConcurrentHashMap <ProtocolCallback<String>,String>();
		_nicksToCallbacks = new ConcurrentHashMap <String, ProtocolCallback<String>>();
		_games= new LinkedList <String> (); 
		_liveGame = new ConcurrentHashMap <String, Game>();
	}
	
	/**
	 * Returns the only one Instance of the gameRoom
	 * @return gameRoomsInstance
	 */
	
	public static GameRooms getInstance() {
		return GameRoomsHolder.instance;
	}
	
	/**
	 * 
	 * @param game to add to the supported games in the server
	 */
	public void addGame (String game) {
		_games.add(game);
	}
	
	/**
	 * 
	 * @return list of games the server support
	 */
	
	public LinkedList<String> getGames () {
		return _games;
	}
	/**
	 * 
	 * @param game
	 * @return true if game exist in the server
	 */
	
	public boolean gameExist (String game) {
		return _games.contains(game);
	}
	
	/**
	 * 
	 * @param name
	 * @return name of room the player is in right now
	 */
	
	public String getCurrRoom (String name) {
		return _gamersCurrRoom.get(name);
	}
	
	/**
	 * 
	 * @param room
	 * @return list of players in a specific room
	 */
	
	public LinkedList<String> getGamersList (String room) {
		return _rooms.get(room);
	}
	
	/**
	 * 
	 * @param name
	 * @return callback of the specific player
	 */
	
	public ProtocolCallback<String> getCallback (String name) {
		return _nicksToCallbacks.get(name);
	}
	
	/**
	 * 
	 * @param name
	 * @return playername of the specific callback
	 */
	
	public String getName (ProtocolCallback<String> callback) {
		return _callbacksToNicks.get(callback);
	}
	
	/**
	 * add new player with a unique nick to the relevant maps 
	 * @param nick
	 * @param callback
	 */
	
	public void insertNick (String nick, ProtocolCallback<String> callback) {
		_callbacksToNicks.put(callback, nick);
		_nicksToCallbacks.put(nick, callback);
	}
	
	/**
	 * 
	 * @param callback
	 * @return true if user already exist
	 */
	
	public boolean userExist (ProtocolCallback<String> callback) {
		return (_callbacksToNicks.containsKey(callback));
	}
	
	/**
	 * 
	 * @param nick
	 * @return true if nick already exist
	 */
	
	public boolean nickExist (String nick) {
		return (_callbacksToNicks.containsValue(nick));
	}
	
	/**
	 * disconnect a player from the server
	 * @param gamerName
	 * @param callback
	 */
	
	public void disconnectGamer (String gamerName, ProtocolCallback<String> callback) {
		_callbacksToNicks.remove(callback);
		_nicksToCallbacks.remove(gamerName);
		_gamersCurrRoom.remove(gamerName);		
	}
	
	/**
	 * removes player from room if possible
	 * @param gamerName
	 * @return if player successfully removed from room
	 */
	
	public boolean removeGamerFromRoom (String gamerName) {
		if (_gamersCurrRoom.containsKey(gamerName)) {
			if (!_isInGame.get(_gamersCurrRoom.get(gamerName))) {			
				_rooms.get(_gamersCurrRoom.get(gamerName)).remove(gamerName);
				_gamersCurrRoom.remove(gamerName);
				return true; //deleted from the room
			}
			else {
				return false; //was in a game that in play
			}
		}
		return true; //wasnt in any room
	}
	
	/**
	 * try to add a gamer to a room
	 * @param roomName
	 * @param gamerName
	 * @return true if gamer added to the room successfully
	 */
	
	public synchronized boolean addGamer ( String roomName, String gamerName) {
		String currRoom = _gamersCurrRoom.get(gamerName);
		if (!_gamersCurrRoom.containsKey(gamerName) || !_isInGame.get(currRoom)) { //the player is not registered yet to a room or the room is not in middle of game
			if (_gamersCurrRoom.containsKey(gamerName)) { //if was in another room
				_rooms.get(currRoom).remove(gamerName);
			}
			if (_rooms.containsKey(roomName)) { //the room already exist
				if (_isInGame.get(roomName)) //if the game is already in progress
						return false;
				else {
					_rooms.get(roomName).add(gamerName); //game available
					_gamersCurrRoom.put(gamerName, roomName);				
					return true;
				}
			}
			else { //the room isnt exist
				LinkedList<String> emptyRoom = new LinkedList<String>(); //create new list
				emptyRoom.add(gamerName);
				_rooms.put(roomName, emptyRoom);
				_isInGame.put(roomName, false);
				_gamersCurrRoom.put(gamerName, roomName);
				return true;
			}
		}
		return false; //game cant join new room because game is played in curr room			
	}
	
	/**
	 * start game - can happen only by one player each time in a room
	 * @param gameType
	 * @param room
	 */
	
	public void startGame (String gameType, String room) {
		if (gameType.equals("BLUFFER")) {
			Game blufferGame = new Bluffer(gameType,room, _rooms.get(room), _questionsFileName);
			_liveGame.put(room, blufferGame);
			for(int i = 0; i<_rooms.get(room).size(); i++) {
				String nickTemp = _rooms.get(room).get(i);
				blufferGame.addCallback(nickTemp, _nicksToCallbacks.get(nickTemp));
			}
			_isInGame.put(room,true);
			blufferGame.startGame();					
		}
	}
	
	/**
	 * add answer to the answers list
	 * @param ans
	 * @param room
	 * @param nick
	 */
	
	public void addAnswer(String ans, String room, String nick) {
		_liveGame.get(room).addAnswer(nick, ans);
		
	}	
	
	/**
	 * 
	 * @param room
	 * @return list of answers
	 */
	
	public ArrayList<String> getAnswers(String room) {
		return _liveGame.get(room).getAnswers();
	}
	
	/**
	 * check chosen answer for points
	 * @param nick
	 * @param room
	 * @param ans
	 */
	
	public void chosenAnswer(String nick,String room, int ans) {
		_liveGame.get(room).chosenAnswer(nick, ans);		
	}
	
	/**
	 * get the name of the question file 
	 * @param fileName
	 */
	
	public void addQuestionsFileName(String fileName) {
		_questionsFileName=fileName;		
	}
	
	/**
	 * finish the game 
	 * @param room
	 */
	
	public void finish(String room) {
		_liveGame.remove(room);
		_isInGame.put(room, false);		
	}
	
	/**
	 * start next round
	 * @param room
	 */
	
	public void nextRound(String room) {
		_liveGame.get(room).nextRound();		
	}		
	
	/**
	 * 
	 * @param room
	 * @return number of answers
	 */
	
	public int getAnswersSize(String room) {
		return _liveGame.get(room).getAnswersSize();
	}
	
	/**
	 * 
	 * @param room
	 * @return the correct answer
	 */
	
	public String getCorrectAns(String room) {
		return _liveGame.get(room).getCorrectAns();
	}
	
	/**
	 * 
	 * @param room
	 * @return number of players
	 */
	
	public int getNumberOfPlayers(String room) {
		System.out.println(_rooms.get(room).size());
		return _rooms.get(room).size();
	}
}
