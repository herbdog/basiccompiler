package tokens;

import inputHandler.TextLocation;

public class StringToken extends TokenImp {
	protected String value;
	
	protected StringToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	public static StringToken make(TextLocation location, String lexeme) {
		StringToken result = new StringToken(location, lexeme);
		result.setValue(lexeme);
		return result;
	}

	protected void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value.substring(1, value.length()-1);
	}
	
	@Override
	protected String rawString() {
		return "STRINGS";
	}
}