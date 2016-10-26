package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class WhileNode extends ParseNode {

	public WhileNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public WhileNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
	
	public Lextant getOperator() {
		return lextantToken().getLextant();
	}
	public LextantToken lextantToken() {
		return (LextantToken)token;
	}	
	
	
	////////////////////////////////////////////////////////////
	// convenience factory
	public static WhileNode withChildren(Token token, ParseNode expr, ParseNode blockstmt) {
		WhileNode node = new WhileNode(token);
		node.appendChild(expr);
		node.appendChild(blockstmt);
		return node;
	}
		
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visitEnter(this);
		visitChildren(visitor);
		visitor.visitLeave(this);
	}
}
