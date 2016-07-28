package server;

import java.io.*;
import java.net.*;

interface ServerProtocol<T> {	//server interface
	void processMessage(T msg, ProtocolCallback<T> callback);	
	boolean isEnd(T msg);	
}

interface ServerProtocolFactory { //protocol factory - creating ourserverProtocol
   OurServerProtocol create();
}

class OurServerProtocolFactory implements ServerProtocolFactory { //Our server factory
	public OurServerProtocol create() {
		return new OurServerProtocol();
	}
}

class ConnectionHandler implements Runnable {
	
	private BufferedReader in;
	private PrintWriter out;
	Socket clientSocket;
	OurServerProtocol protocol;
	ProtocolCallback<String> callback;
	
	/**
	 * connectionHandler constructor
	 * @param acceptedSocket
	 * @param ourServerProtocol
	 */
	
	public ConnectionHandler(Socket acceptedSocket, OurServerProtocol ourServerProtocol) {
		in = null;
		out = null;
		clientSocket = acceptedSocket;
		protocol = ourServerProtocol;
		System.out.println("Accepted connection from client!");
		System.out.println("The client is from: " + acceptedSocket.getInetAddress() + ":" + acceptedSocket.getPort());
	}
	
	/**
	 * connectionhandler run - going to initiliaize and process
	 */
	
	public void run() {	
		try {
			initialize();
		}
		catch (IOException e) {
			System.out.println("Error in initializing I/O");
		}

		try {
			process();
		} 
		catch (IOException e) {
			System.out.println("Error in I/O");
		} 
		
		System.out.println("Connection closed - bye bye...");
		close();

	}
	/**
	 * receiving and processing next message from client
	 * if signal for End arrives - finish
	 * @throws IOException
	 */
	
	public void process() throws IOException
	{
		String msg;		
		while ((msg = in.readLine()) != null) {
			System.out.println("Received \"" + msg + "\" from client");			
			protocol.processMessage(msg, this.callback);			
			if (protocol.isEnd(msg)) {
				break;
			}			
		}
	}
	
	/**
	 * Start communication with the client. 
	 * @throws IOException
	 */
	
	// Starts listening
	public void initialize() throws IOException
	{
		// Initialize I/O
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF-8"));
		out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"), true);
		System.out.println("I/O initialized");
		out.println("please enter nick name using: NICK <name>");		
		this.callback = response-> {
			out.println(response);
		};
	}
	
	/**
	 * closes the connection
	 */

	public void close()
	{
		try {
			if (in != null)
			{
				in.close();
			}
			if (out != null)
			{
				out.close();
			}
			
			clientSocket.close();
		}
		catch (IOException e)
		{
			System.out.println("Exception in closing I/O");
		}
	}
	
}

class MultipleClientProtocolServer implements Runnable {
	private ServerSocket serverSocket;
	private int listenPort;
	private ServerProtocolFactory factory;
	
	
	public MultipleClientProtocolServer(int port, ServerProtocolFactory p)
	{
		serverSocket = null;
		listenPort = port;
		factory = p;
	}
	
	public void run()
	{
		try {
			serverSocket = new ServerSocket(listenPort); //create new server socket to listen for new clients
			System.out.println("Listening...");
		}
		catch (IOException e) {
			System.out.println("Cannot listen on port " + listenPort);
		}
		
		while (true)
		{
			try {
				ConnectionHandler newConnection = new ConnectionHandler(serverSocket.accept(), factory.create()); //create new connection handler for each thread
            new Thread(newConnection).start();
			}
			catch (IOException e)
			{
				System.out.println("Failed to accept on port " + listenPort);
			}
		}
	}
	

	// Closes the connection
	public void close() throws IOException
	{
		serverSocket.close();
	}
	
	public static void main(String[] args) throws IOException
	{
		// Get port
		int port = Integer.decode(args[0]).intValue();	
		GameRooms game = GameRooms.getInstance(); //create instance of gameRooms. add type game that server support and send link to questions file
		game.addGame("BLUFFER"); 
		game.addQuestionsFileName(args[1]);
		MultipleClientProtocolServer server = new MultipleClientProtocolServer(port, new OurServerProtocolFactory()); //create new server
		Thread serverThread = new Thread(server); //create new thread and start it.
		serverThread.start();
		try {
			serverThread.join();
		}
		catch (InterruptedException e)
		{
			System.out.println("Server stopped");
		}				
	}
}
