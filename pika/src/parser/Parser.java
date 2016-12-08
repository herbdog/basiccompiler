package parser;

import java.util.Arrays;

import logging.PikaLogger;
import parseTree.*;
import parseTree.nodeTypes.*;
import tokens.*;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import lexicalAnalyzer.Scanner;



public class Parser {
	private Scanner scanner;
	private Token nowReading;
	private Token previouslyRead;
	
	public static ParseNode parse(Scanner scanner) {
		Parser parser = new Parser(scanner);
		return parser.parse();
	}
	public Parser(Scanner scanner) {
		super();
		this.scanner = scanner;
	}
	
	public ParseNode parse() {
		readToken();
		return parseProgram();
	}

	////////////////////////////////////////////////////////////
	// "program" is the start symbol S
	// S -> FuncDef -> EXEC mainBlock
	
	private ParseNode parseProgram() {
		if(!startsProgram(nowReading)) {				//main check for exec is moved down
			return syntaxErrorNode("program");			//since the program can now offer definitions before hand
		}
		
		ParseNode program = new ProgramNode(nowReading);
		
		while (!(nowReading.isLextant(Keyword.EXEC))) {
			ParseNode globalDefinition = parseFunction();
			program.appendChild(globalDefinition);
		}
		
		if(!startsMain(nowReading)) {
			return syntaxErrorNode("main program");
		}
		
		expect(Keyword.EXEC);
		ParseNode mainBlock = parseMainBlock();
		program.appendChild(mainBlock);
		
		if(!(nowReading instanceof NullToken)) {
			return syntaxErrorNode("end of program");
		}
		
		return program;
	}
	private boolean startsProgram(Token token) {
		return (token.isLextant(Keyword.EXEC)) || (token.isLextant(Keyword.FUNCTION));
	}
	private boolean startsMain(Token token) {
		return token.isLextant(Keyword.EXEC);
	}
	
	// Global Function Definitions 
	private ParseNode parseFunction() {
		if (!startsFunction(nowReading)) {
			return syntaxErrorNode("function definition");
		}
		ParseNode function = new FunctionNode(nowReading);
		expect(Keyword.FUNCTION);
		if (!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		ParseNode iden = new IdentifierNode(nowReading);
		function.appendChild(iden);
		readToken();
		if (!startsLambda(nowReading)) {
			return syntaxErrorNode("lambda");
		}
		ParseNode lambda = parseLambda();
		function.appendChild(lambda);
		return function;
	}
	private boolean startsFunction(Token token) {
		return token.isLextant(Keyword.FUNCTION);
	}
	
	private ParseNode parseLambda() {
		if (!startsLambda(nowReading)) {
			return syntaxErrorNode("lambda");
		}
		Token start = nowReading;
		ParseNode paramtype = parseLambdaParamType();		//not going to do another check for no reason its the same exact thing
		if (!startsBlock(nowReading)) {
			return syntaxErrorNode("function body");
		}
		ParseNode body = parseBlock();
		
		return LambdaNode.withChildren(start, paramtype, body);
	}
	private boolean startsLambda(Token token) {
		return token.isLextant(Punctuator.LESS);
	}
	
	private ParseNode parseLambdaParamType() {
		if (!startsLambda(nowReading)) {
			return syntaxErrorNode("lambda param type");
		}	// <
		Token start = nowReading;
		if (!startsParamList(nowReading)) {
			return syntaxErrorNode("parameter list");
		}
		ParseNode paramlist = parseParamList();
		expect(Punctuator.GREATER);
		expect(Punctuator.SUBTRACT);
		expect(Punctuator.GREATER);		//the arow ->
		if (!startsTypeNodeLambda(nowReading)) {
			return syntaxErrorNode("lambda return type");
		}
		ParseNode returntype = new TypeNode(nowReading);
		readToken();
		return LambdaParamType.withChildren(start, paramlist, returntype);
	}
	
	private ParseNode parseParamList() {
		if (!startsParamList(nowReading)) {
			return syntaxErrorNode("parameter list");
		}
		ParseNode paramlist = new ParamList(nowReading);
		readToken();					// .. > ->
		while (!(nowReading.isLextant(Punctuator.GREATER))) {
			if (!startsTypeNodeCast(nowReading)) {
				return syntaxErrorNode("param type");
			}
			ParseNode type = new TypeNode(nowReading);
			readToken();
			if (!startsIdentifier(nowReading)) {
				return syntaxErrorNode("param identifier");
			}
			ParseNode iden = new IdentifierNode(nowReading);
			readToken();		// , .. > ->
			paramlist.appendChild(ParameterSpecification.withChildren(nowReading,type,iden)); //could be > or , shouldnt matter
			if (nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();		//go to next
			}
		}
		return paramlist;
	}
	private boolean startsParamList(Token token) {
		return token.isLextant(Punctuator.LESS);
	}
	
	// mainBlock -> { statement* }
	private ParseNode parseMainBlock() {
		if(!startsMainBlock(nowReading)) {
			return syntaxErrorNode("mainBlock");
		}
		ParseNode mainBlock = new MainBlockNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			mainBlock.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return mainBlock;
	}
	private boolean startsMainBlock(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	// inner block statements
	private ParseNode parseBlock() {
		if (!startsBlock(nowReading)) {
			return syntaxErrorNode("Block");
		}
		ParseNode Block = new BlockNode(nowReading);
		expect(Punctuator.OPEN_BRACE);
		
		while(startsStatement(nowReading)) {
			ParseNode statement = parseStatement();
			Block.appendChild(statement);
		}
		expect(Punctuator.CLOSE_BRACE);
		return Block;
	}
	private boolean startsBlock(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACE);
	}
	
	///////////////////////////////////////////////////////////
	// statements
	
	// statement-> declaration | printStmt
	private ParseNode parseStatement() {
		if(!startsStatement(nowReading)) {
			return syntaxErrorNode("statement");
		}
		if(startsBlock(nowReading)) {
			return parseBlock();
		}
		if(startsDeclaration(nowReading)) {
			return parseDeclaration();
		}
		if(startsAssign(nowReading)) {
			return parseAssign();
		}
		if(startsIf(nowReading)) {
			return parseIf();
		}
		if(startsWhile(nowReading)) {
			return parseWhile();
		}
		if(startsBreak(nowReading)) {
			return parseBreak();
		}
		if(startsContinue(nowReading)) {
			return parseContinue();
		}
		if(startsReturn(nowReading)) {
			return parseReturn();
		}
		if(startsPrintStatement(nowReading)) {
			return parsePrintStatement();
		}
		return syntaxErrorNode("statement");
	}
	private boolean startsStatement(Token token) {
		return startsPrintStatement(token) || startsDeclaration(token) || startsBlock(token) ||
				startsAssign(token) || startsIf(token) || startsWhile(token) || startsBreak(token) ||
				startsContinue(token) || startsReturn(token);
	}
	// printStmt -> PRINT printExpressionList .
	private ParseNode parsePrintStatement() {
		if(!startsPrintStatement(nowReading)) {
			return syntaxErrorNode("print statement");
		}
		PrintStatementNode result = new PrintStatementNode(nowReading);
		
		readToken();
		result = parsePrintExpressionList(result);
		
		expect(Punctuator.TERMINATOR);
		return result;
	}
	private boolean startsPrintStatement(Token token) {
		return token.isLextant(Keyword.PRINT);
	}	

	// This adds the printExpressions it parses to the children of the given parent
	// printExpressionList -> printExpression* bowtie (,|;)  (note that this is nullable)

	private PrintStatementNode parsePrintExpressionList(PrintStatementNode parent) {
		while(startsPrintExpression(nowReading) || startsPrintSeparator(nowReading)) {
			parsePrintExpression(parent);
			parsePrintSeparator(parent);
		}
		return parent;
	}
	

	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> (expr | nl)?     (nullable)
	
	private void parsePrintExpression(PrintStatementNode parent) {
		if(startsExpression(nowReading)) {
			ParseNode child = parseExpression();
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Keyword.NEWLINE)) {
			readToken();
			ParseNode child = new NewlineNode(previouslyRead);
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Keyword.TAB)) {
			readToken();
			ParseNode child = new TabNode(previouslyRead);
			parent.appendChild(child);
		}
		// else we interpret the printExpression as epsilon, and do nothing
	}
	private boolean startsPrintExpression(Token token) {
		return startsExpression(token) || token.isLextant(Keyword.NEWLINE) || token.isLextant(Keyword.TAB) ;
	}
	
	
	// This adds the printExpression it parses to the children of the given parent
	// printExpression -> expr? ,? nl? 
	
	private void parsePrintSeparator(PrintStatementNode parent) {
		if(!startsPrintSeparator(nowReading) && !nowReading.isLextant(Punctuator.TERMINATOR)) {
			ParseNode child = syntaxErrorNode("print separator");
			parent.appendChild(child);
			return;
		}
		
		if(nowReading.isLextant(Punctuator.SPACE)) {
			readToken();
			ParseNode child = new SpaceNode(previouslyRead);
			parent.appendChild(child);
		}
		else if(nowReading.isLextant(Punctuator.SEPARATOR)) {
			readToken();
		}		
		else if(nowReading.isLextant(Punctuator.TERMINATOR)) {
			// we're at the end of the bowtie and this printSeparator is not required.
			// do nothing.  Terminator is handled in a higher-level nonterminal.
		}
	}
	private boolean startsPrintSeparator(Token token) {
		return token.isLextant(Punctuator.SEPARATOR, Punctuator.SPACE) ;
	}
	
	
	// declaration -> CONST identifier := expression .
	private ParseNode parseDeclaration() {
		if(!startsDeclaration(nowReading)) {
			return syntaxErrorNode("declaration");
		}
		Token declarationToken = nowReading;
		readToken();
		
		ParseNode identifier = parseIdentifier();
		expect(Punctuator.ASSIGN);
		ParseNode initializer = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return DeclarationNode.withChildren(declarationToken, identifier, initializer);
	}
	private boolean startsDeclaration(Token token) {
		return token.isLextant(Keyword.CONST) || token.isLextant(Keyword.VAR);
	}
	
	//assignment identifier := expression (VAR ONLY)
	private ParseNode parseAssign() {
		if(!startsAssign(nowReading)) {
			return syntaxErrorNode("assignment");
		}
		Token assigntoken = nowReading;
		
		ParseNode identifier = parseExpression();
		expect(Punctuator.ASSIGN);
		ParseNode assignment = parseExpression();
		expect(Punctuator.TERMINATOR);
		
		return AssignNode.withChildren(assigntoken, identifier, assignment);
	}
	private boolean startsAssign(Token token) {
		return (token instanceof IdentifierToken);
	}
	
	
	//if statements 
	private ParseNode parseIf() {
		if (!startsIf(nowReading)) {
			return syntaxErrorNode("if");
		}
		Token iftoken = nowReading;
		readToken();
		expect(Punctuator.OPEN_BRACKET);
		ParseNode expression = parseExpression();
		expect(Punctuator.CLOSE_BRACKET);
		if (!nowReading.isLextant(Punctuator.OPEN_BRACE)) {
			return syntaxErrorNode("if");
		}
		ParseNode blockstmt = parseBlock();

		if (nowReading.isLextant(Keyword.ELSE)) {
			readToken();
			ParseNode elsenode = parseBlock();
			return IfNode.withChildren(iftoken, expression, blockstmt, elsenode);
		}
		return IfNode.withChildren(iftoken, expression, blockstmt);
	}
	private boolean startsIf(Token token) {
		return (token.isLextant(Keyword.IF));
	}
	
	//while statements
	private ParseNode parseWhile() {
		if (!startsWhile(nowReading)) {
			return syntaxErrorNode("while");
		}
		Token whiletoken = nowReading;
		readToken();
		expect(Punctuator.OPEN_BRACKET);
		ParseNode expression = parseExpression();
		expect(Punctuator.CLOSE_BRACKET);
		if (!nowReading.isLextant(Punctuator.OPEN_BRACE)) {
			return syntaxErrorNode("if");
		}
		ParseNode blockstmt = parseBlock();
		
		return WhileNode.withChildren(whiletoken, expression, blockstmt);
	}
	private boolean startsWhile(Token token) {
		return (token.isLextant(Keyword.WHILE));
	}
	
	private ParseNode parseBreak() {
		if (!startsBreak(nowReading)) {
			return syntaxErrorNode("break");
		}
		Token breaktoken = nowReading;
		readToken();
		expect(Punctuator.TERMINATOR);
		return new BreakNode(breaktoken);
	}
	private boolean startsBreak(Token token) {
		return (token.isLextant(Keyword.BREAK));
	}
	
	private ParseNode parseContinue() {
		if (!startsContinue(nowReading)) {
			return syntaxErrorNode("continue");
		}
		Token continuetoken = nowReading;
		readToken();
		expect(Punctuator.TERMINATOR);
		return new ContinueNode(continuetoken);
	}
	
	private boolean startsContinue(Token token) {
		return (token.isLextant(Keyword.CONTINUE));
	}
	
	// function return statement			ONLY TO BE USED FOR FUNCTION DEFINITIONS cannot be used elsewhere
	private ParseNode parseReturn() {
		if(!startsReturn(nowReading)) {
			return syntaxErrorNode("return");
		}
		ParseNode returnnode = new ReturnNode(nowReading);
		for (ParseNode nodes : returnnode.pathToRoot()) {
			if (nodes instanceof FunctionNode) {
				break;
			}
			if (nodes instanceof MainBlockNode) {
				return syntaxErrorNode("return statement not in function body");
			}
		}
		readToken();				// -> expression
		if (startsExpression(nowReading)) {
			ParseNode expr = parseExpression();
			returnnode.appendChild(expr);
			expect(Punctuator.TERMINATOR);
			return returnnode;
		}
		expect(Punctuator.TERMINATOR);
		return returnnode;				//0 children means returns nothing, check that the lambda return type is void in semantic
	}
	private boolean startsReturn(Token token) {
		return token.isLextant(Keyword.RETURN);
	}
	// starting keywords for types
	
	///////////////////////////////////////////////////////////
	// expressions
	// expr                     -> comparisonExpression
	// comparisonExpression     -> additiveExpression [> additiveExpression]?
	// additiveExpression       -> multiplicativeExpression [+ multiplicativeExpression]*  (left-assoc)
	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*  (left-assoc)
	// atomicExpression         -> literal
	// literal                  -> intNumber | identifier | booleanConstant

	// expr  -> comparisonExpression
	private ParseNode parseExpression() {	
		if(!startsExpression(nowReading)) {
			return syntaxErrorNode("expression");
		}
		return parseComparisonExpression();
	}
	private boolean startsExpression(Token token) {
		return startsComparisonExpression(token);
	}
	
	private ParseNode parseNotExpression() {
		if (!startsNotExpression(nowReading)) {
			return syntaxErrorNode("not expression");
		}
		Token nottoken = nowReading;
		readToken();
		ParseNode right = parseAdditiveExpression();
		return NotOperatorNode.withChildren(nottoken, right);
	}
	private boolean startsNotExpression(Token token) {
		return token.isLextant(Punctuator.NOT);
	}

	// comparisonExpression -> additiveExpression [> additiveExpression]?
	private ParseNode parseComparisonExpression() {
		if(!startsComparisonExpression(nowReading)) {
			return syntaxErrorNode("comparison expression");
		}
		ParseNode left = parseAdditiveExpression();
		if(nowReading.isLextant(Punctuator.GREATER)) {
			Token greatercompareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();

			return BinaryOperatorNode.withChildren(greatercompareToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.GREATER_EQUAL)) {
			Token greaterequalcompareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(greaterequalcompareToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.LESS)) {
			Token lesscompareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(lesscompareToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.LESS_EQUAL)) {
			Token lessequalcompareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(lessequalcompareToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.EQUAL)) {
			Token equalcompareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(equalcompareToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.NOTEQUAL)) {
			Token notequalcompareToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(notequalcompareToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.AND)) {
			Token andToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(andToken, left, right);
		}
		else if(nowReading.isLextant(Punctuator.OR)) {
			Token orToken = nowReading;
			readToken();
			ParseNode right = parseAdditiveExpression();
			
			return BinaryOperatorNode.withChildren(orToken, left, right);
		}
		return left;

	}
	private boolean startsComparisonExpression(Token token) {
		return startsAdditiveExpression(token);
	}

	// additiveExpression -> multiplicativeExpression [+ multiplicativeExpression]*  (left-assoc)
	private ParseNode parseAdditiveExpression() {
		if(!startsAdditiveExpression(nowReading)) {
			return syntaxErrorNode("additiveExpression");
		}
		
		ParseNode left = parseMultiplicativeExpression();
		while((nowReading.isLextant(Punctuator.ADD)) || (nowReading.isLextant(Punctuator.SUBTRACT))) {
			Token additiveToken = nowReading;
			readToken();
			ParseNode right = parseMultiplicativeExpression();
			
			left = BinaryOperatorNode.withChildren(additiveToken, left, right);
		}
		return left;
	}
	private boolean startsAdditiveExpression(Token token) {
		return startsMultiplicativeExpression(token);
	}

	// multiplicativeExpression -> atomicExpression [MULT atomicExpression]*  (left-assoc)
	private ParseNode parseMultiplicativeExpression() {
		if(!startsMultiplicativeExpression(nowReading)) {
			return syntaxErrorNode("multiplicativeExpression");
		}
		
		ParseNode left = parseAtomicExpression();
		while((nowReading.isLextant(Punctuator.MULTIPLY)) || (nowReading.isLextant(Punctuator.DIVIDE)) || (nowReading.isLextant(Punctuator.OVER)) || (nowReading.isLextant(Punctuator.EXPRESS_OVER)) || (nowReading.isLextant(Punctuator.RATIONALIZE))) {
			Token multiplicativeToken = nowReading;
			readToken();
			ParseNode right = parseAtomicExpression();
			
			left = BinaryOperatorNode.withChildren(multiplicativeToken, left, right);
		}
		return left;
	}	
	private boolean startsMultiplicativeExpression(Token token) {
		return startsAtomicExpression(token);
	}
	
	// atomicExpression -> literal
	private ParseNode parseAtomicExpression() {
		if(!startsAtomicExpression(nowReading)) {
			return syntaxErrorNode("atomic expression");
		}
		if(startsBracketExpression(nowReading)) {
			return parseBracketExpression();
		}
		if(startsSquareBracketsExpression(nowReading)) {
			return parseSquareBracketsExpression();
		}
		if(startsNotExpression(nowReading)) {
			return parseNotExpression();
		}
		if(startsLengthExpression(nowReading)) {
			return parseLengthExpression();
		}
		return parseLiteral();
	}
	private boolean startsAtomicExpression(Token token) {
		return startsLiteral(token) || startsBracketExpression(token) || startsSquareBracketsExpression(token) || startsNotExpression(token) || startsLengthExpression(token);
	}
	// BRACKETS 
	private ParseNode parseBracketExpression() {
		if(!startsBracketExpression(nowReading)) {
			return syntaxErrorNode("BracketExpression");
		}
		expect(Punctuator.OPEN_BRACKET);
		ParseNode bracketnode = parseExpression();
		expect(Punctuator.CLOSE_BRACKET);
		return  bracketnode;
	}
	private boolean startsBracketExpression(Token token) {
		return token.isLextant(Punctuator.OPEN_BRACKET);
	}
	//casting
	private ParseNode parseSquareBracketsExpression() {
		if (!startsSquareBracketsExpression(nowReading)) {
			return syntaxErrorNode("CastingExpression");
		}
		expect(Punctuator.OPEN_SQUARE_BRACKET);
		ParseNode expr = parseExpression();
		if (nowReading.isLextant(Punctuator.CAST)) {
			Token cast = nowReading;
			readToken();
			if (!startsTypeNodeCast(nowReading)) {
				return syntaxErrorNode("type node");
			}
			ParseNode type = new TypeNode(nowReading);
			expr = BinaryOperatorNode.withChildren(cast, expr, type);
			readToken();
			expect(Punctuator.CLOSE_SQUARE_BRACKET);
			return expr;
		}
		else {
			ParseNode arraynode = new ArrayNode(previouslyRead);
			arraynode.appendChild(expr);
			while (nowReading.isLextant(Punctuator.SEPARATOR)) {
				readToken();
				ParseNode elementnodes = parseExpression();
				arraynode.appendChild(elementnodes);
			}
			expect(Punctuator.CLOSE_SQUARE_BRACKET);
			return arraynode;
		}
	}
	private boolean startsSquareBracketsExpression(Token token) {
		return token.isLextant(Punctuator.OPEN_SQUARE_BRACKET);
	}
	private boolean startsTypeNodeLambda(Token token) {
		return (token.isLextant(Keyword.INT)) || (token.isLextant(Keyword.FLOAT)) || (token.isLextant(Keyword.BOOL)) || (token.isLextant(Keyword.RAT)) || (token.isLextant(Keyword.STRING)) || (token.isLextant(Keyword.CHAR)) || (token.isLextant(Keyword.VOID));
	}
	private boolean startsTypeNodeCast(Token token) {
		return (token.isLextant(Keyword.INT)) || (token.isLextant(Keyword.FLOAT)) || (token.isLextant(Keyword.BOOL)) || (token.isLextant(Keyword.RAT)) || (token.isLextant(Keyword.STRING)) || (token.isLextant(Keyword.CHAR));
	}
	private ParseNode parseLengthExpression() {
		if (!startsLengthExpression(nowReading)) {
			return syntaxErrorNode("length");
		}
		Token length = nowReading;
		LengthNode lnode = new LengthNode(length);
		readToken();
		ParseNode string = parseExpression();
		lnode.appendChild(string);
		return lnode;
	}
	private boolean startsLengthExpression(Token token) {
		return token.isLextant(Keyword.LENGTH);
	}
	// literal -> number | identifier | booleanConstant
	private ParseNode parseLiteral() {
		if(!startsLiteral(nowReading)) {
			return syntaxErrorNode("literal");
		}
		if(startsIntNumber(nowReading)) {
			return parseIntNumber();
		}
		if(startsFloatNumber(nowReading)) {
			return parseFloatNumber();
		}
		if(startsIdentifier(nowReading)) {
			return parseIdentifier();
		}
		if(startsBooleanConstant(nowReading)) {
			return parseBooleanConstant();
		}
		if(startsStringConstant(nowReading)) {
			return parseStringConstant();
		}
		if(startsCharacterConstant(nowReading)) {
			return parseCharacterConstant();
		}

		return syntaxErrorNode("literal");
	}
	private boolean startsLiteral(Token token) {
		return startsIntNumber(token) || startsIdentifier(token) || startsBooleanConstant(token) || startsFloatNumber(token) || startsStringConstant(token) || startsCharacterConstant(token);
	}

	// number (terminal)
	private ParseNode parseIntNumber() {
		if(!startsIntNumber(nowReading)) {
			return syntaxErrorNode("integer constant");
		}
		readToken();
		return new IntegerConstantNode(previouslyRead);
	}
	private boolean startsIntNumber(Token token) {
		return token instanceof IntToken;
	}
	
	// float number 
	private ParseNode parseFloatNumber() {
		if (!startsFloatNumber(nowReading)) {
			return syntaxErrorNode("float constant");
		}
		readToken();
		return new FloatConstantNode(previouslyRead);
	}
	private boolean startsFloatNumber(Token token) {
		return token instanceof FloatToken;
	}

	// identifier (terminal)
	private ParseNode parseIdentifier() {
		if(!startsIdentifier(nowReading)) {
			return syntaxErrorNode("identifier");
		}
		readToken();
		IdentifierNode iden = new IdentifierNode(previouslyRead);
		if (nowReading.isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
			Token index = nowReading;
			IndexNode indexnode = new IndexNode(index);
			indexnode.appendChild(iden);
			readToken();
			ParseNode expr = parseExpression();
			indexnode.appendChild(expr);
			if (nowReading.isLextant(Punctuator.CLOSE_SQUARE_BRACKET)) {
				readToken();
				while(nowReading.isLextant(Punctuator.OPEN_SQUARE_BRACKET)) {
					readToken();
					ParseNode expr2 = parseExpression();
					if (nowReading.isLextant(Punctuator.CLOSE_SQUARE_BRACKET)) {
						indexnode.appendChild(expr2);
						readToken();
						continue;
					}
				}
			}
			return indexnode;
		}
		
		
		return iden;
	}
	private boolean startsIdentifier(Token token) {
		return token instanceof IdentifierToken;
	}

	// boolean constant (terminal)
	private ParseNode parseBooleanConstant() {
		if(!startsBooleanConstant(nowReading)) {
			return syntaxErrorNode("boolean constant");
		}
		readToken();
		return new BooleanConstantNode(previouslyRead);
	}
	private boolean startsBooleanConstant(Token token) {
		return token.isLextant(Keyword.TRUE, Keyword.FALSE);
	}
	
	//string constants
	private ParseNode parseStringConstant() {
		if (!startsStringConstant(nowReading)) {
			return syntaxErrorNode("string constant");
		}
		readToken();
		return new StringConstantNode(previouslyRead);
	}
	private boolean startsStringConstant(Token token) {
		return token instanceof StringToken;
	}
	
	private void readToken() {
		previouslyRead = nowReading;
		nowReading = scanner.next();
		while (nowReading.getClass() == CommentToken.class) {
			nowReading = scanner.next();
		}
	}	
	//character constant
	private ParseNode parseCharacterConstant() {
		if (!startsCharacterConstant(nowReading)) {
			return syntaxErrorNode("character constant");
		}
		readToken();
		return new CharacterConstantNode(previouslyRead);
	}
	private boolean startsCharacterConstant(Token token) {
		return token instanceof CharacterToken;
	}
	// if the current token is one of the given lextants, read the next token.
	// otherwise, give a syntax error and read next token (to avoid endless looping).
	private void expect(Lextant ...lextants ) {
		if(!nowReading.isLextant(lextants)) {
			syntaxError(nowReading, "expecting " + Arrays.toString(lextants));
		}
		readToken();
	}	
	private ErrorNode syntaxErrorNode(String expectedSymbol) {
		syntaxError(nowReading, "expecting " + expectedSymbol);
		ErrorNode errorNode = new ErrorNode(nowReading);
		readToken();
		return errorNode;
	}
	private void syntaxError(Token token, String errorDescription) {
		String message = "" + token.getLocation() + " " + errorDescription;
		error(message);
	}
	private void error(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.Parser");
		log.severe("syntax error: " + message);
	}	
	
}

