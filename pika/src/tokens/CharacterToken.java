package tokens;

import inputHandler.TextLocation;

public class CharacterToken extends TokenImp {
	protected char value;
	
	protected CharacterToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	public static CharacterToken make(TextLocation location, String lexeme) {
		CharacterToken result = new CharacterToken(location, lexeme);
		result.setValue(lexeme.charAt(1));
		return result;
	}

	protected void setValue(char value) {
		this.value = value;
	}
	
	public char getValue() {
		return value;
	}
	@Override
	protected String rawString() {
		return "character";
	}
}