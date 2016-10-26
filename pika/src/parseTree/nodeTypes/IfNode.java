package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import lexicalAnalyzer.Lextant;
import tokens.LextantToken;
import tokens.Token;

public class IfNode extends ParseNode {

	public IfNode(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public IfNode(ParseNode node) {
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
	public static IfNode withChildren(Token token, ParseNode expr, ParseNode blockstmt) {
		IfNode node = new IfNode(token);
		node.appendChild(expr);
		node.appendChild(blockstmt);
		return node;
	}
	
	public static IfNode withChildren(Token token, ParseNode expr, ParseNode blockstmt, ParseNode elsenode) {
		IfNode node = new IfNode(token);
		node.appendChild(expr);
		node.appendChild(blockstmt);
		node.appendChild(elsenode);
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
