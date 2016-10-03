package tokens;

import inputHandler.TextLocation;

public class IntToken extends TokenImp {
	protected int value;
	
	protected IntToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	protected void setValue(int value) {
		this.value = value;
	}
	public int getValue() {
		return value;
	}
	
	public static IntToken make(TextLocation location, String lexeme) {
		IntToken result = new IntToken(location, lexeme);
		result.setValue(Integer.parseInt(lexeme));
		return result;
	}
	
	@Override
	protected String rawString() {
		return "number, " + value;
	}
}
