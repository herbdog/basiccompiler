package semanticAnalyzer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.PikaLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.*;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.types.*;
import symbolTable.Binding;
import symbolTable.Scope;
import tokens.LextantToken;
import tokens.Token;

class SemanticAnalysisVisitor extends ParseNodeVisitor.Default {
	@Override
	public void visitLeave(ParseNode node) {
		throw new RuntimeException("Node class unimplemented in SemanticAnalysisVisitor: " + node.getClass());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// constructs larger than statements
	@Override
	public void visitEnter(ProgramNode node) {
		enterProgramScope(node);
	}
	public void visitLeave(ProgramNode node) {
		leaveScope(node);
	}
	public void visitEnter(MainBlockNode node) {
		enterSubscope(node);
	}
	public void visitLeave(MainBlockNode node) {
		leaveScope(node);
	}
	public void visitEnter(BlockNode node) {
		if (node.getParent() instanceof LambdaNode) {
			enterProcedureScope(node);
		}
		else {
			enterSubscope(node);
		}
	}
	public void visitLeave(BlockNode node) {
		leaveScope(node);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// helper methods for scoping.
	private void enterProgramScope(ParseNode node) {
		Scope scope = Scope.createProgramScope();
		node.setScope(scope);
	}	
	private void enterParameterScope(ParseNode node) {
		Scope scope = Scope.createParameterScope();
		node.setScope(scope);
	}
	private void enterProcedureScope(ParseNode node) {
		Scope scope = Scope.createProcedureScope();
		node.setScope(scope);
	}
	private void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}		
	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}
	public void visitEnter(FunctionNode node) {
		enterParameterScope(node);
	}
	public void visitLeave(FunctionNode node) {		// maybe it should be determined here after it is done rather than before read assignment page
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		identifier.setScope(node.getParent().getLocalScope());
		ParseNode identype = node.child(1).child(0);
		identifier.setType(identype.getType());
		addBinding(identifier, identifier.getType().getType());
		leaveScope(node);
	}									//in some block node afterwards should be the start of the procedural scope i.e. variables instantiated within the function
	public void visitLeave(LambdaNode node) {
	}
	public void visitEnter(LambdaParamType node) {
		assert node.nChildren() == 2;
		Type lambdatype = new LambdaType();
		node.setType(lambdatype);
	}
	public void visitLeave(LambdaParamType node) {
		Type lambdatype = node.getType().getType();
		Type returntype = node.child(1).getType().getType();
		lambdatype.setType(returntype);
		node.setType(lambdatype);
	}
	public void visitLeave(ParamList node) {
		ParseNode parent = node.getParent();
		node.setType(parent.getType());
	}
	public void visitLeave(ParameterSpecification node) {
		assert node.nChildren() == 2;
		IdentifierNode identifier = (IdentifierNode) node.child(1);
		Type type = node.child(0).getType().getType();
		ParseNode lambdaparamtype = node.getParent().getParent();
		Type lambdatype = lambdaparamtype.getType().getType();
		lambdatype.setType(type);
		lambdaparamtype.setType(lambdatype);
		
		if (type == TypeLiteral.INT) {
			identifier.setType(PrimitiveType.INTEGER);
			addBinding(identifier,PrimitiveType.INTEGER);
		}
		else if (type == TypeLiteral.FLOAT) {
			identifier.setType(PrimitiveType.FLOAT);
			addBinding(identifier,PrimitiveType.FLOAT);
		}
		else if (type == TypeLiteral.CHAR) {
			identifier.setType(PrimitiveType.CHAR);
			addBinding(identifier,PrimitiveType.CHAR);
		}
		else if (type == TypeLiteral.BOOL) {
			identifier.setType(PrimitiveType.BOOLEAN);
			addBinding(identifier,PrimitiveType.BOOLEAN);
		}
		else if (type == TypeLiteral.STRING) {
			identifier.setType(PrimitiveType.STRING);
			addBinding(identifier,PrimitiveType.STRING);
		}
		else if (type == TypeLiteral.RAT) {
			identifier.setType(PrimitiveType.RATIONAL);
			addBinding(identifier,PrimitiveType.RATIONAL);
		}
		else if (type == TypeLiteral.VOID) {
			logError(type + "can't be used as a parameter type");
		}
		node.setType(type);
	}
	public void visitLeave(ReturnNode node) {			// match types with lambdatype is necessary
		
	}
	///////////////////////////////////////////////////////////////////////////
	// statements and declarations
	@Override
	public void visitLeave(PrintStatementNode node) {
	}
	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode blocknode = node.getParent();
		ParseNode initializer = node.child(1);
		
		Type declarationType = initializer.getType();
		node.setType(declarationType);
		
		identifier.setType(declarationType);
		addBinding(identifier, declarationType);
		while ((blocknode instanceof BlockNode) || (blocknode instanceof IfNode) || (blocknode instanceof WhileNode)) {
			if (blocknode.getParent().containsBindingOf(identifier.getToken().getLexeme())) {
				logError("Variable " + identifier.getToken().getLexeme() + "cannot be declared in this scope because the value cannot be decided");
			}
			blocknode = blocknode.getParent();
		}
	}
	
	public void visitLeave(AssignNode node) {
		IdentifierNode identifier = null;
		ParseNode left = null;
		ParseNode right = node.child(1);
		List<Type> childTypes = null;
		
		ParseNode child = node.child(0);
		if (node.child(0) instanceof IndexNode) {
			child = node.child(0);
			Type identype = null;
			while (child instanceof IndexNode) {
				child = child.child(0);
			}
			identifier = (IdentifierNode) child;
			left = child;
			identype = identifier.getType();
			while (!(identype instanceof PrimitiveType)) {
				identype = identype.getType();
			}
			childTypes = Arrays.asList(identype, right.getType().getType());
		}
		else {
			identifier = (IdentifierNode) node.child(0);
			left = node.child(0);
			childTypes = Arrays.asList(left.getType().getType().getType(), right.getType().getType());
		}
		for (ParseNode nodes : node.pathToRoot()) {
			for (ParseNode childnodes : nodes.getChildren()) {
				if ((childnodes.getLocalScope() == identifier.getDeclarationScope()) && (childnodes instanceof DeclarationNode)) {
					DeclarationNode decnode = (DeclarationNode) childnodes;
					if (decnode.getDeclarationType() == Keyword.CONST) {
						logError("Can't Assign a const declared variable");
					}
				}
				else if ((childnodes.getLocalScope() == identifier.getDeclarationScope()) && (childnodes instanceof ParameterSpecification)) {
					break;
				}
			}
		}

		FunctionSignature signature = FunctionSignature.signatureOf(Punctuator.ASSIGN, childTypes);
		if (signature.accepts(childTypes)) {
			node.setType(signature.resultType());
		}
		else {
			if ((left.getType().getType() == PrimitiveType.INTEGER) && (right.getType().getType() == PrimitiveType.FLOAT)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.INTEGER) && (right.getType().getType() == PrimitiveType.CHAR)){
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.INTEGER) && (right.getType().getType() == PrimitiveType.RATIONAL)){
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.INTEGER)) {
				right.setType(PrimitiveType.FLOAT);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.CHAR)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.RATIONAL)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.CHAR) && (right.getType().getType() == PrimitiveType.INTEGER)) {
				right.setType(PrimitiveType.CHAR);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.CHAR) && (right.getType().getType() == PrimitiveType.FLOAT)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.CHAR) && (right.getType().getType() == PrimitiveType.RATIONAL)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.RATIONAL) && (right.getType().getType() == PrimitiveType.INTEGER)) {
				right.setType(PrimitiveType.RATIONAL);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.RATIONAL) && (right.getType().getType() == PrimitiveType.CHAR)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.RATIONAL) && (right.getType().getType() == PrimitiveType.FLOAT)) {
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else {
				logError("Assign not defined for types: " + childTypes);
			}
		}
	}
	
	public void visitLeave(IndexNode node) {
		ParseNode child = node.child(0);
		assert child.getType().getType() instanceof ArrayType || child.getType().match(PrimitiveType.STRING);
		for (int i = 1; i < node.nChildren(); i++) {
			assert node.child(i).getType().getType() == PrimitiveType.INTEGER;
		}
		
		node.setType(node.child(0).getType().getType().getType());

	}
	
	public void visitLeave(IfNode node) {
		assert node.child(0).getType().getType() == PrimitiveType.BOOLEAN;
	}
	
	public void visitLeave(WhileNode node) {
		assert node.child(0).getType().getType() == PrimitiveType.BOOLEAN;
	}
	
	public void visit(BreakNode node) {
		ParseNode extracheck = node;
		for (ParseNode nodes : node.pathToRoot()) {
			extracheck = nodes;
			if (nodes instanceof WhileNode) {
				break;
			}
		}
		if (extracheck.getParent() == null) {
			logError("break statement must be inside of a loop");
		}
	}
	
	public void visit(ContinueNode node) {
		ParseNode extracheck = node;
		for (ParseNode nodes : node.pathToRoot()) {
			extracheck = nodes;
			if (nodes instanceof WhileNode) {
				break;
			}
		}
		if (extracheck.getParent() == null) {
			logError("continue statement must be inside of a loop");
		}
	}
	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(BinaryOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode left  = node.child(0);
		ParseNode right = node.child(1);
		if (node.getToken().isLextant(Punctuator.CAST)) {
			IdentifierNode identifier = (IdentifierNode) node.child(0);
			for (ParseNode nodes : node.pathToRoot()) {
				for (ParseNode childnodes : nodes.getChildren()) {
					if ((childnodes.getLocalScope() == identifier.getDeclarationScope()) && (childnodes instanceof DeclarationNode)) {
						DeclarationNode decnode = (DeclarationNode) childnodes;
						if (decnode.getDeclarationType() == Keyword.CONST) {
							logError("Can't cast a const declared variable");
						}
					}
				}
			}
		}
		List<Type> childTypes = null;
		if (left instanceof IndexNode) {
			if (right instanceof IndexNode) {
				ParseNode child = node.child(0);
				Type identype = null;
				while (child instanceof IndexNode) {
					child = child.child(0);
				}
				left = child;
				identype = left.getType();
				while (!(identype instanceof PrimitiveType)) {
					identype = identype.getType();
				}
				ParseNode rightchild = node.child(1);
				Type identype2 = null;
				while (rightchild instanceof IndexNode) {
					rightchild = rightchild.child(0);
				}
				right = rightchild;
				identype2 = right.getType();
				while (!(identype2 instanceof PrimitiveType)) {
					identype2 = identype2.getType();
				}
				childTypes = Arrays.asList(identype, identype2);
			}
			else {
				ParseNode child = node.child(0);
				Type identype = null;
				while (child instanceof IndexNode) {
					child = child.child(0);
				}
				left = child;
				identype = left.getType();
				while (!(identype instanceof PrimitiveType)) {
					identype = identype.getType();
				}
				childTypes = Arrays.asList(identype, right.getType().getType());
			}
		}
		else {
			childTypes = Arrays.asList(left.getType().getType(), right.getType().getType());
		}
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignature.signatureOf(operator, childTypes);
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());
		}
		else {
			if ((left.getType().getType() == PrimitiveType.INTEGER) && ((right.getType().getType() == PrimitiveType.FLOAT) || (right.getType().getType() == PrimitiveType.RATIONAL))) {
				left.setType(right.getType().getType());
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.INTEGER)) {
				left.setType(right.getType().getType());
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.RATIONAL) && (right.getType().getType() == PrimitiveType.INTEGER)) {
				left.setType(right.getType().getType());
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.INTEGER) && (right.getType().getType() == PrimitiveType.CHAR)) {
				right.setType(left.getType().getType());
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.CHAR)) {
				left.setType(PrimitiveType.INTEGER);
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.RATIONAL) && (right.getType().getType() == PrimitiveType.CHAR)) {
				left.setType(PrimitiveType.INTEGER);
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.CHAR) && ((right.getType().getType() == PrimitiveType.FLOAT) || (right.getType().getType() == PrimitiveType.RATIONAL))) {
				left.setType(PrimitiveType.INTEGER);
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.CHAR) && (right.getType().getType() == PrimitiveType.INTEGER)) {
				left.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.RATIONAL)) {
				left.setType(PrimitiveType.INTEGER);
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.RATIONAL) && (right.getType().getType() == PrimitiveType.FLOAT)) {
				left.setType(PrimitiveType.INTEGER);
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else if ((left.getType().getType() == PrimitiveType.FLOAT) && (right.getType().getType() == PrimitiveType.FLOAT)) {
				left.setType(PrimitiveType.INTEGER);
				right.setType(PrimitiveType.INTEGER);
				visitLeave(node);
			}
			else {
				typeCheckError(node, childTypes);
				node.setType(PrimitiveType.ERROR);
			}
		}
	}
	
	public void visitLeave(ArrayNode node) {
		List<Integer> integers = new LinkedList<Integer>();
		List<Integer> floats = new LinkedList<Integer>();
		List<Integer> characters = new LinkedList<Integer>();
		List<Integer> rational = new LinkedList<Integer>();
		List<ParseNode> children = node.getChildren();
		int i = 0;
		for (i = 0; i < children.size(); i++) {		// but this is also where we need to determine a function signature that encompasses all the types we want and see if possible promotions exist
			if (children.get(i).getType().getType() == PrimitiveType.INTEGER) {
				integers.add(i);
			}
			else if (children.get(i).getType().getType() == PrimitiveType.FLOAT) {
				floats.add(i);
			}
			else if (children.get(i).getType().getType() == PrimitiveType.CHAR) {
				characters.add(i);
			}
			else if (children.get(i).getType().getType() == PrimitiveType.RATIONAL) {
				rational.add(i);
			}
		}
		// going to go by which type most frequently exists in the entire array to decide what to change to
		// ie if integers > all others, then they all change to integer
		if (integers.size() >= floats.size() + characters.size() + rational.size()) { // int dominant, change to all to int
			for (int j = 0; j < floats.size(); j++) {
				node.child(floats.get(j)).setType(PrimitiveType.INTEGER);
			}
			for (int j = 0; j < rational.size(); j++) {
				node.child(rational.get(j)).setType(PrimitiveType.INTEGER);
			}
			for (int j = 0; j < characters.size(); j++) {
				node.child(characters.get(j)).setType(PrimitiveType.INTEGER);
			}
		}
		else if (floats.size() >= integers.size() + characters.size() + rational.size()) { 
			for (int j = 0; j < integers.size(); j++) {
				node.child(integers.get(j)).setType(PrimitiveType.FLOAT);
			}
			for (int j = 0; j < rational.size(); j++) {
				node.child(rational.get(j)).setType(PrimitiveType.FLOAT);
			}
			for (int j = 0; j < characters.size(); j++) {
				node.child(characters.get(j)).setType(PrimitiveType.FLOAT);
			}
		}
		else if (characters.size() >= floats.size() + integers.size() + rational.size()) { 
			for (int j = 0; j < floats.size(); j++) {
				node.child(floats.get(j)).setType(PrimitiveType.CHAR);
			}
			for (int j = 0; j < rational.size(); j++) {
				node.child(rational.get(j)).setType(PrimitiveType.CHAR);
			}
			for (int j = 0; j < integers.size(); j++) {
				node.child(integers.get(j)).setType(PrimitiveType.CHAR);
			}
		}
		else if (rational.size() >= floats.size() + integers.size() + characters.size()) { 
			for (int j = 0; j < floats.size(); j++) {
				node.child(floats.get(j)).setType(PrimitiveType.RATIONAL);
			}
			for (int j = 0; j < integers.size(); j++) {
				node.child(integers.get(j)).setType(PrimitiveType.RATIONAL);
			}
			for (int j = 0; j < characters.size(); j++) {
				node.child(characters.get(j)).setType(PrimitiveType.RATIONAL);
			}
		}
		
		Type arraytype = new ArrayType();
		arraytype.setType(node.child(0).getType());
		node.setType(arraytype);
	}
	
	public void visitLeave(NotOperatorNode node) {
		assert node.nChildren() == 1;
		ParseNode right = node.child(0);
		
		List<Type> childTypes = Arrays.asList(right.getType().getType());
		Lextant operator = operatorFor(node);
		FunctionSignature signature = FunctionSignature.signatureOf(operator, childTypes);
		
		if(signature.accepts(childTypes)) {
			node.setType(signature.resultType());
		}
		else {
			typeCheckError(node, childTypes);
			node.setType(PrimitiveType.ERROR);
		}
	}
	private Lextant operatorFor(BinaryOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
	private Lextant operatorFor(NotOperatorNode node) {
		LextantToken token = (LextantToken) node.getToken();
		return token.getLextant();
	}
	
	public void visitLeave(LengthNode node) {
		assert node.nChildren() == 1;
		ParseNode stringnode = node.child(0);
		
		if ((stringnode instanceof StringConstantNode) || (stringnode instanceof IdentifierNode)) {
			if (stringnode instanceof IdentifierNode) {
				IdentifierNode iden = (IdentifierNode) stringnode;
				assert iden.getType().match(PrimitiveType.STRING);
			}
		}
		else {
			logError("length expression for strings");
		}
		
		node.setType(PrimitiveType.INTEGER);
	}


	///////////////////////////////////////////////////////////////////////////
	// simple leaf nodes
	@Override
	public void visit(BooleanConstantNode node) {
		node.setType(PrimitiveType.BOOLEAN);
	}
	@Override
	public void visit(ErrorNode node) {
		node.setType(PrimitiveType.ERROR);
	}
	@Override
	public void visit(IntegerConstantNode node) {
		node.setType(PrimitiveType.INTEGER);
	}
	public void visit(FloatConstantNode node) {
		node.setType(PrimitiveType.FLOAT);
	}
	public void visit(StringConstantNode node) {
		node.setType(PrimitiveType.STRING);
	}
	public void visit(CharacterConstantNode node) {
		node.setType(PrimitiveType.CHAR);
	}
	@Override
	public void visit(NewlineNode node) {
	}
	public void visit(TabNode node) {
	}
	@Override
	public void visit(SpaceNode node) {
	}
	public void visit(TypeNode node) {
		if(node.getToken().isLextant(Keyword.INT)) {
			node.setType(TypeLiteral.INT);
		}
		if(node.getToken().isLextant(Keyword.FLOAT)) {
			node.setType(TypeLiteral.FLOAT);
		}
		if(node.getToken().isLextant(Keyword.CHAR)) {
			node.setType(TypeLiteral.CHAR);
		}
		if(node.getToken().isLextant(Keyword.STRING)) {
			node.setType(TypeLiteral.STRING);
		}
		if(node.getToken().isLextant(Keyword.BOOL)) {
			node.setType(TypeLiteral.BOOL);
		}
		if(node.getToken().isLextant(Keyword.RAT)) {
			node.setType(TypeLiteral.RAT);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// IdentifierNodes, with helper methods
	@Override
	public void visit(IdentifierNode node) {
		if(!isBeingDeclared(node)) {		
			Binding binding = node.findVariableBinding();
			
			node.setType(binding.getType().getType());
			node.setBinding(binding);
		}
		// else parent DeclarationNode does the processing.
		// such is for parameter specification and function node
	}
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		if ((parent instanceof DeclarationNode) && (node == parent.child(0))) {
			return true;
		}
		else if ((parent instanceof FunctionNode) && (node == parent.child(0))) { 
			return true;
		}
		else if ((parent instanceof ParameterSpecification) && (node == parent.child(1))) {
			return true;
		}
		else {
			return false;
		}
	}
	private void addBinding(IdentifierNode identifierNode, Type type) {
		Scope scope = identifierNode.getLocalScope();
		Binding binding = scope.createBinding(identifierNode, type);
		identifierNode.setBinding(binding);
	}
	
	///////////////////////////////////////////////////////////////////////////
	// error logging/printing

	private void typeCheckError(ParseNode node, List<Type> operandTypes) {
		Token token = node.getToken();
		
		logError("operator " + token.getLexeme() + " not defined for types " 
				 + operandTypes  + " at " + token.getLocation());	
	}
	private void logError(String message) {
		PikaLogger log = PikaLogger.getLogger("compiler.semanticAnalyzer");
		log.severe(message);
	}
}