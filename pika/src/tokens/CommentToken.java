package tokens;

import inputHandler.TextLocation;

public class CommentToken extends TokenImp {
	
	protected CommentToken(TextLocation location, String lexeme) {
		super(location, lexeme);
	}
	
	public static CommentToken make(TextLocation location, String lexeme) {
		CommentToken result = new CommentToken(location, lexeme);
		return result;
	}

	@Override
	protected String rawString() {
		return "COMMENTS";
	}
}