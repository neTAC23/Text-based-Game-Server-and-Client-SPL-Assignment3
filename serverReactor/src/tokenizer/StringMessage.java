package tokenizer;

//class that wrap a message and contains a string

public class StringMessage implements Message<StringMessage> {
	private final String message;
	
	public StringMessage(String message){
		this.message=message;
	}

	public String getMessage(){
		return message;
	}
	
	@Override
	public String toString() {
		return message;
	}
	
	@Override
	public boolean equals(Object other) {
		return message.equals(other);
	}
}
