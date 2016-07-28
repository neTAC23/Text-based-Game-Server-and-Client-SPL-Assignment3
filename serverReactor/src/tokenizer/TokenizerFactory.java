package tokenizer;

//TokenizerFactory - give us the option to decide which Tokenizer we want to use without changing the actual code

public interface TokenizerFactory<T> { 
   MessageTokenizer<T> create();
}
