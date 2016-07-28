package protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import protocol.AsyncServerProtocol;
import reactor.GameRooms;
import tokenizer.StringMessage;

public class ReactorProtocol implements AsyncServerProtocol<StringMessage> {	
	
	private boolean isEnd=false;  //flag for ending
	GameRooms _game = GameRooms.getInstance(); //singelton	
	public ReactorProtocol() {};
	boolean _shouldClose=false;
	boolean _isTXTRESPNext = true; //flag to check if next message turn is TEXTRESP or SELECTRESP 
	
	/**
	 * process every message, separate between NICK, JOIN, TXTRESP, SELECTRESP, QUIT, MSG, LISTGAMES, STARTGAME
	 */
	
	public void processMessage(StringMessage _msg, ProtocolCallback<StringMessage> callback) {
		String msg = _msg.getMessage();
		try {
			if (msg.startsWith("NICK")) {  //check if nick exist and if user already have a nick
				String nick=msg.substring(5);
				if (_game.userExist(callback)) { 
					callback.sendMessage(new StringMessage("SYSMSG NICK REJECTED: user already has a nickname"));
				}
				else {
					if (_game.nickExist(nick)) {
							callback.sendMessage(new StringMessage("SYSMSG NICK REJECTED: Choose Different Nickname"));
					}
					else {
						_game.insertNick(nick, callback);
						callback.sendMessage(new StringMessage("SYSMSG NICK ACCEPTED"));
					}				
				}
			}
			
			if (msg.startsWith("JOIN")) {
				String room = msg.substring(5);
				String name = _game.getName(callback);
				if (name==null) { //check if user has a nick
					callback.sendMessage(new StringMessage("SYSMSG JOING REJECTED - please register with a nickname"));							
				}
				else {
					boolean ans=_game.addGamer(room, name);
					if (ans) {  //check if user can be added to a room right now and the room is avialable
						callback.sendMessage (new StringMessage("SYSMSG JOIN ACCEPTED"));
					}
					else 
						callback.sendMessage(new StringMessage("SYSMSG JOIN REJECTED"));		
				}
			}
			
			if (msg.startsWith("MSG")) {
				String message = msg.substring(4);
				String nick = _game.getName(callback);
				if (isLegal("MSG", callback)) { //check if user has a nick and inside a room
					String room = _game.getCurrRoom(nick);
					LinkedList<String> gamersInRoom = _game.getGamersList(room);
					Iterator<String> iter = gamersInRoom.iterator();
					while (iter.hasNext()) { //send the message to every one in the room
						_game.getCallback(iter.next()).sendMessage(new StringMessage("USRMSG from " + nick + ": " + message));
					}
				}
			}
			if (msg.startsWith("LISTGAMES")) { //show list of games
				callback.sendMessage(new StringMessage("SYSMSG LISTGAMES " + _game.getGames()));				
			}
			
			if (msg.startsWith("QUIT")) {
				String name = _game.getName(callback);
				boolean ans = _game.removeGamerFromRoom(name);
				if (ans) { //check if can quit. not in a livegame.
					_game.disconnectGamer(name, callback);
					callback.sendMessage(new StringMessage("SYSMSG QUIT ACCEPTED"));
					isEnd=true;
				}
				else {
					callback.sendMessage(new StringMessage("SYSMSG QUIT REJECTED - please finish game first"));
				}				
			}
			
			if (msg.startsWith("STARTGAME")) {  //start a new game
				String play = msg.substring(10);
				String nick = _game.getName(callback);
				if (isLegal("STARTGAME", callback)) { //check if the game exist and can be started
					String room = _game.getCurrRoom(nick);
					if (_game.gameExist(play)) {
						callback.sendMessage(new StringMessage("SYSMSG STARTGAME ACCEPTED - enjoy!"));	
						_game.startGame(play, room);
					}
					else {
						callback.sendMessage(new StringMessage("SYSMSG STARTGAME REJECTED - doesnt support this game"));
					}
				}
			}
			
			if (msg.startsWith("TXTRESP")) { //send bluffed answer 
				if (!_isTXTRESPNext) { //check if its time to txtresp
					callback.sendMessage(new StringMessage(
							"SYSMSG TXTRESP REJECTED - Please use SELECTRESP command"));
				}
				else {

					String ans = msg.substring(8);
					String nick = _game.getName(callback);
					if (isLegal("TXTRESP", callback)) { //check if user can use this command right now
						ans=ans.toLowerCase();
						String room = _game.getCurrRoom(nick);
						if (ans.equals(_game.getCorrectAns(room))) { //check if bluffed answered is the correct answer
							callback.sendMessage(new StringMessage(
									"SYSMSG TXTRESP REJECTED - Please enter a different bluffed answer (You know the correct answer ;))"));
						}
						else {
							_game.addAnswer(ans, room, nick);
							_isTXTRESPNext=false;
							callback.sendMessage(new StringMessage("SYSMSG TXTRESP ACCEPTED"));
							ArrayList<String> answers;
							answers = _game.getAnswers(room);
							if (answers!=null) {		//check if everybody returned their answer and play the next move - askchocies		
								LinkedList<String> gamersInRoom = _game.getGamersList(room);
								Iterator<String> iter = gamersInRoom.iterator();
								while (iter.hasNext()) {
									_game.getCallback(iter.next()).sendMessage(new StringMessage("ASKCHOICES " + answers));
								}
							}						
						}	
					}
				}
			}
			
			if (msg.startsWith("SELECTRESP")) { //send number to represent user choice for correct answer
				if (_isTXTRESPNext) { //check if its selectresp turn
					callback.sendMessage(new StringMessage("SYSMSG SELECTRESP REJECTED - Please use TXTRESP command"));						
				}
				else {
					int ans = Integer.parseInt(msg.substring(11));	//get the number	
					String nick = _game.getName(callback);
					if (isLegal("SELECTRESP", callback)) {
						String room = _game.getCurrRoom(nick);
						int check = _game.getAnswersSize(room)-1;
						if (ans> check) { //check if the number that was chosen fits
							callback.sendMessage(new StringMessage("SYSMSG SELECTRESP REJECTED - Choose number between 0 and " + check));							
						}
						else {
							_isTXTRESPNext=true;
							callback.sendMessage(new StringMessage("SYSMSG SELECTRESP ACCETPED"));
							_game.chosenAnswer(nick,room, ans);
							_game.nextRound(room);													
						}
					}
				}
			}
		}			
		
		catch (IOException e ) {
			e.printStackTrace();
		}
	}
	private synchronized boolean isLegal (String type, ProtocolCallback<StringMessage> callback ) { //check if its a legal command - nick and room
		try {
			String nick = _game.getName(callback);
			if (nick !=null) {
				String room = _game.getCurrRoom(nick);
				if (room!=null) {
					return true;
				}
				else {
					callback.sendMessage(new StringMessage("SYSMSG " + type + " REJECTED - please join a room"));
					return false;				
				}
			}
			else {
				callback.sendMessage(new StringMessage("SYSMSG " + type + " REJECTED - please register with a nickname"));
				return false;							
			}	
		}
		catch (IOException e) {
			e.printStackTrace();			
		}
		return false;
	}
	
	/**
	 * determine whether the given message is the termination message
	 * @param msg the message to examine
	 * @return true if the message is the termination message, false otherwise
	 */
	
	@Override
	public boolean isEnd(StringMessage msg) {
		return isEnd;
	}	

	/**
	 * Is the protocol in a closing state?.
	 * When a protocol is in a closing state, it's handler should write out all pending data, 
	 * and close the connection.
	 * @return true if the protocol is in closing state.
	 */
	
	@Override
	public boolean shouldClose() {
		return _shouldClose;
	}
	@Override
	
	/**
	 * Indicate to the protocol that the client disconnected.
	 */
	
	public void connectionTerminated() {
		_shouldClose=true;
		
	}	
}

