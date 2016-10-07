package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.Token;

public class AssignNode extends ParseNode {
	
	public AssignNode(Token token) {
		super(token);
	}
	public AssignNode(ParseNode node) {
		super(node);
	}
	
	
	////////////////////////////////////////////////////////////
	// attributes
		
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static AssignNode withChildren(Token token, ParseNode identifier, ParseNode assignment) {
		AssignNode node = new AssignNode(token);
		node.appendChild(identifier);
		node.appendChild(assignment);
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
