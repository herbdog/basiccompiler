package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class ContinueNode extends ParseNode {

	public ContinueNode(ParseNode node) {
		super(node);
	}
	public ContinueNode(Token token) {
		super(token);
	}
	
	
	///////////////////////////////////////////////////////////
	// boilerplate for visitors
			
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}
}
