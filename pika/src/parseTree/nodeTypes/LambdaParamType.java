package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.LextantToken;
import tokens.Token;

public class LambdaParamType extends ParseNode {

	public LambdaParamType(Token token) {
		super(token);
		assert(token instanceof LextantToken);
	}

	public LambdaParamType(ParseNode node) {
		super(node);
	}
	
	////////////////////////////////////////////////////////////
	// convenience factory
	
	public static LambdaParamType withChildren(Token token, ParseNode left, ParseNode right) {
		LambdaParamType node = new LambdaParamType(token);
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
