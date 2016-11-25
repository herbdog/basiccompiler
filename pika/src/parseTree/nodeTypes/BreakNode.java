package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class BreakNode extends ParseNode {

	public BreakNode(ParseNode node) {
		super(node);
	}
	public BreakNode(Token token) {
		super(token);
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
