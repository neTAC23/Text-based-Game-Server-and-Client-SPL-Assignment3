package reactor;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;

import protocol.*;
import tokenizer.*;



/**
 * This class supplies some data to the protocol, which then processes the data,
 * possibly returning a reply. This class is implemented as an executor task.
 * 
 */
public class ProtocolTask<T> implements Runnable {

	private final ServerProtocol<T> _protocol;
	private final MessageTokenizer<T> _tokenizer;
	private final ConnectionHandler<T> _handler;
	private final ProtocolCallback<T> _callback;

	public ProtocolTask(final ServerProtocol<T> protocol, final MessageTokenizer<T> tokenizer, final ConnectionHandler<T> h) {
		this._protocol = protocol;
		this._tokenizer = tokenizer;
		this._handler = h;
		this._callback = msg-> { //construct the callback - get message from the tokenizer and handle it.
            try {
                ByteBuffer bytes = _tokenizer.getBytesForMessage(msg);
                this._handler.addOutData(bytes);
             } catch (CharacterCodingException e) { e.printStackTrace(); }          
		};
	}

	// we synchronize on ourselves, in case we are executed by several threads
	// from the thread pool.
	public synchronized void run() {
      // go over all complete messages and process them.
      while (_tokenizer.hasMessage()) { 
         T msg = _tokenizer.nextMessage();
         this._protocol.processMessage(msg ,_callback);          
      }      
	}

	public void addBytes(ByteBuffer b) {
		_tokenizer.addBytes(b);
	}
}
