package parseTree.nodeTypes;

import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import tokens.NumberToken;
import tokens.Token;

public class IntegerConstantNode extends ParseNode {
	public IntegerConstantNode(Token token) {
		super(token);
		assert(token instanceof IntToken);
	}
	public IntegerConstantNode(ParseNode node) {
		super(node);
	}

////////////////////////////////////////////////////////////
// attributes
	
	public int getValue() {
		return numberToken().getValue();
	}

	public IntToken numberToken() {
		return (IntToken)token;
	}	

///////////////////////////////////////////////////////////
// accept a visitor
	
	public void accept(ParseNodeVisitor visitor) {
		visitor.visit(this);
	}

}
