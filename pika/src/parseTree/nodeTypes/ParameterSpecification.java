package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class ParameterSpecification extends ParseNode {

	public ParameterSpecification(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public ParameterSpecification(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static ParameterSpecification withChildren(Token token, ParseNode left, ParseNode right) {
		ParameterSpecification node = new ParameterSpecification(token);
		node.appendChild(left);
		node.appendChild(right);
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
