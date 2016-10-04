package lexicalAnalyzer;

import inputHandler.PushbackCharStream;
import tokens.*;

public abstract class ScannerImp implements Scanner {
	private Token nextToken;
	protected final PushbackCharStream input;
	
	protected abstract Token findNextToken();

	public ScannerImp(PushbackCharStream input) {
		super();
		this.input = input;
		nextToken = findNextToken();
	}

	// Iterator<Token> implementation
	@Override
	public boolean hasNext() {
		return !(nextToken instanceof NullToken);
	}

	@Override
	public Token next() {
		Token result = nextToken;
		nextToken = findNextToken();
		return result;
	}
	
	public boolean isLiteral() {
		Token result = nextToken;
		if ((result instanceof IdentifierToken) || (result instanceof IntToken) || (result instanceof FloatToken) || (result instanceof StringToken)) {
			return true;
		}
		return false;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}