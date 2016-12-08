package lexicalAnalyzer;


import logging.PikaLogger;

import inputHandler.InputHandler;
import inputHandler.LocatedChar;
import inputHandler.LocatedCharStream;
import inputHandler.PushbackCharStream;
import inputHandler.TextLocation;
import tokens.*;
import static lexicalAnalyzer.PunctuatorScanningAids.*;

public class LexicalAnalyzer extends ScannerImp implements Scanner {
	public static LexicalAnalyzer make(String filename) {
		InputHandler handler = InputHandler.fromFilename(filename);
		PushbackCharStream charStream = PushbackCharStream.make(handler);
		return new LexicalAnalyzer(charStream);
	}

	public LexicalAnalyzer(PushbackCharStream input) {
		super(input);
	}

	
	//////////////////////////////////////////////////////////////////////////////
	// Token-finding main dispatch	

	@Override
	protected Token findNextToken() {
		LocatedChar ch = nextNonWhitespaceChar();
		if (ch.isChar('#')) {
			return comments(ch);
		}
		else if(ch.isDigit()) {
			return scanNumber(ch);
		}
		else if(ch.isLowerCase() || ch.isUpperCase()) {
			return scanIdentifier(ch);
		}
		else if(isPunctuatorStart(ch)) {
			if (((ch.getCharacter() == '+') || (ch.getCharacter() == '-')) && (!isLiteral()) && (!wasBracket()) && (!wasIndex()) && (!input.peek().isChar('>'))) {
				return scanNumber(ch);
			}
			if ((ch.getCharacter() == '.') && (input.peek().isDigit())) {
				return scanNumber(ch);
			}
			return PunctuatorScanner.scan(ch, input);
		}
		else if(ch.isChar('"')) {
			return scanString(ch);
		}
		else if(ch.isChar('^')) {
			return scanCharacter(ch);
		}
		else if(isEndOfInput(ch)) {
			return NullToken.make(ch.getLocation());
		}
		else {
			lexicalError(ch, "invalid char");
			return findNextToken();
		}
	}


	private LocatedChar nextNonWhitespaceChar() {
		LocatedChar ch = input.next();
		while(ch.isWhitespace()) {
			ch = input.next();
		}
		return ch;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Integer lexical analysis	

	private Token scanNumber(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentDigits(buffer);
		
		if ((buffer.toString().contains(".")) || (buffer.toString().contains("E"))) {
			return FloatToken.make(firstChar.getLocation(), buffer.toString());
		}
		return IntToken.make(firstChar.getLocation(), buffer.toString());
	}
	private void appendSubsequentDigits(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		if (c.isChar('.')) {
			LocatedChar cnext = input.peek();
			if (cnext.isDigit()) {
				if ((buffer.length() <= 1) && ((buffer.toString().equals("+")) || (buffer.toString().equals("-")))) {
					buffer.append('0');
				}
				buffer.append(c.getCharacter());
				c = input.next();
			}
		}
		while (c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		if (c.isChar('E')) {
			LocatedChar cnext = input.peek();
			if ((cnext.isChar('+')) || (cnext.isChar('-'))) {
				buffer.append(c.getCharacter());
				c = input.next();
				cnext = input.peek();
				if (cnext.isDigit()) {
					buffer.append(c.getCharacter());
					c = input.next();
				}
				else {
					lexicalError(c, "invalid char");
				}
			}
			else if (cnext.isDigit()) {
				buffer.append(c.getCharacter());
				c = input.next();
			}
			else {
				lexicalError(c, "invalid char");
			}
		}
		while (c.isDigit()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Identifier and keyword lexical analysis	

	private Token scanIdentifier(LocatedChar firstChar) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(firstChar.getCharacter());
		appendSubsequentLowercase(buffer);
		if (buffer.length() > 32) {
			lexicalError(firstChar,"identifier");
		}

		String lexeme = buffer.toString();
		if(Keyword.isAKeyword(lexeme)) {
			return LextantToken.make(firstChar.getLocation(), lexeme, Keyword.forLexeme(lexeme));
		}
		else {
			return IdentifierToken.make(firstChar.getLocation(), lexeme);
		}
	}
	private void appendSubsequentLowercase(StringBuffer buffer) {
		LocatedChar c = input.next();
		while(c.isLowerCase() || c.isDigit() || c.isUpperCase()) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
		input.pushback(c);
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Punctuator lexical analysis	
	// old method left in to show a simple scanning method.
	// current method is the algorithm object PunctuatorScanner.java

	@SuppressWarnings("unused")
	private Token oldScanPunctuator(LocatedChar ch) {
		TextLocation location = ch.getLocation();
		
		switch(ch.getCharacter()) {
		case '*':
			return LextantToken.make(location, "*", Punctuator.MULTIPLY);
		case '+':
			return LextantToken.make(location, "+", Punctuator.ADD);
		case '>':
			return LextantToken.make(location, ">", Punctuator.GREATER);
		case ':':
			if(ch.getCharacter()=='=') {
				return LextantToken.make(location, ":=", Punctuator.ASSIGN);
			}
			else {
				throw new IllegalArgumentException("found : not followed by = in scanOperator");
			}
		case ',':
			return LextantToken.make(location, ",", Punctuator.SEPARATOR);
		case ';':
			return LextantToken.make(location, ";", Punctuator.TERMINATOR);
		default:
			throw new IllegalArgumentException("bad LocatedChar " + ch + "in scanOperator");
		}
	}

	

	//////////////////////////////////////////////////////////////////////////////
	// Character-classification routines specific to Pika scanning.	

	private boolean isPunctuatorStart(LocatedChar lc) {
		char c = lc.getCharacter();
		return isPunctuatorStartingCharacter(c);
	}

	private boolean isEndOfInput(LocatedChar lc) {
		return lc == LocatedCharStream.FLAG_END_OF_INPUT;
	}
	
	
	//////////////////////////////////////////////////////////////////////////////
	// Error-reporting	

	private void lexicalError(LocatedChar ch, String string) {
		PikaLogger log = PikaLogger.getLogger("compiler.lexicalAnalyzer");
		if (string.equals("identifier")) {
			log.severe("Lexical error: identifier length cannot exceed 32 " + ch);
		}
		else {
			log.severe("Lexical error: invalid character " + ch);
		}
	}
	
	//dealing with comments
	private Token comments(LocatedChar ch) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(ch.getCharacter());
		appendcomment(buffer);
		return CommentToken.make(ch.getLocation(), buffer.toString());
	}

	private void appendcomment(StringBuffer buffer) {
		LocatedChar c = input.next();
		while (!c.isChar('#') && !c.isChar('\n')) {
			buffer.append(c.getCharacter());
			c = input.next();
		}
	}
	
	//dealing with string constants;
	private Token scanString(LocatedChar ch) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(ch.getCharacter());
		appendString(buffer);
		return StringToken.make(ch.getLocation(), buffer.toString());
	}
	
	private void appendString(StringBuffer buffer) {
		LocatedChar c = input.next();
		while (!c.isChar('"')) {
			buffer.append(c.getCharacter());
			c = input.next();
			if (c.isChar('\n')) {
				lexicalError(c, "invalid character");
			}
		}
		buffer.append(c.getCharacter());
	}
	
	//dealing with characters ^a^
	private Token scanCharacter(LocatedChar ch) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(ch.getCharacter());
		appendCharacter(buffer);
		return CharacterToken.make(ch.getLocation(), buffer.toString());
	}
	
	private void appendCharacter(StringBuffer buffer) {
		LocatedChar c = input.next();
		if ((c.getCharacter() < 32) || (c.getCharacter() > 126)) {
			lexicalError(c, "invalid char");
		}
		else {
			buffer.append(c.getCharacter());
			c = input.next();
			buffer.append(c.getCharacter());
		}
	}
}
