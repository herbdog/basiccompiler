package semanticAnalyzer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import asmCodeGenerator.codeStorage.ASMOpcode;
import lexicalAnalyzer.Keyword;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import logging.PikaLogger;
import parseTree.ParseNode;
import parseTree.ParseNodeVisitor;
import parseTree.nodeTypes.*;
import semanticAnalyzer.signatures.FunctionSignature;
import semanticAnalyzer.signatures.FunctionSignatures;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeLiteral;
import semanticAnalyzer.types.TypeVariable;
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
		enterSubscope(node);
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
	private void enterSubscope(ParseNode node) {
		Scope baseScope = node.getLocalScope();
		Scope scope = baseScope.createSubscope();
		node.setScope(scope);
	}		
	private void leaveScope(ParseNode node) {
		node.getScope().leave();
	}
	
	///////////////////////////////////////////////////////////////////////////
	// statements and declarations
	@Override
	public void visitLeave(PrintStatementNode node) {
	}
	@Override
	public void visitLeave(DeclarationNode node) {
		IdentifierNode identifier = (IdentifierNode) node.child(0);
		ParseNode initializer = node.child(1);
		
		Type declarationType = initializer.getType();
		node.setType(declarationType);
		
		identifier.setType(declarationType);
		addBinding(identifier, declarationType);
	}
	
	public void visitLeave(AssignNode node) {
		CharSequence Decnode = "DeclarationNode (CONST)";
		CharSequence identifiernode = "IdentifierNode";
		CharSequence variablename = node.child(0).getToken().getLexeme();
		for (ParseNode globalnode : node.pathToRoot()) {
			for (int i = 0; i < globalnode.getChildren().size(); i++) {
				ParseNode localnode = globalnode.child(i);
				if ((localnode.getScope() == node.getScope()) && (localnode.toString().contains(Decnode)) && (localnode.toString().contains(identifiernode)) && (localnode.toString().contains(variablename))) {
					logError("identifier declared as const may not be reassigned");
				}
			}
		}
		ParseNode left = node.child(0);
		ParseNode right = node.child(1);
		List<Type> childTypes = Arrays.asList(left.getType().getType().getType(), right.getType().getType());
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
	
	public void visitLeave(IfNode node) {
		assert node.child(0).getType().getType() == PrimitiveType.BOOLEAN;
	}
	
	public void visitLeave(WhileNode node) {
		assert node.child(0).getType().getType() == PrimitiveType.BOOLEAN;
	}
	///////////////////////////////////////////////////////////////////////////
	// expressions
	@Override
	public void visitLeave(BinaryOperatorNode node) {
		assert node.nChildren() == 2;
		ParseNode left  = node.child(0);
		ParseNode right = node.child(1);
		if (node.toString().contains("CAST")) {
			if (left.toString().contains("Identifier")) {
				CharSequence Decnode = "DeclarationNode (CONST)";
				CharSequence identifiernode = "IdentifierNode";
				CharSequence variablename = node.child(0).getToken().getLexeme();
				for (ParseNode globalnode : node.pathToRoot()) {
					for (int i = 0; i < globalnode.getChildren().size(); i++) {
						ParseNode localnode = globalnode.child(i);
						if ((localnode.getScope() == node.getScope()) && (localnode.toString().contains(Decnode)) && (localnode.toString().contains(identifiernode)) && (localnode.toString().contains(variablename))) {
							logError("identifier declared as const may not be casted");
						}
					}
				}
			}
		}
		
		List<Type> childTypes = Arrays.asList(left.getType().getType(), right.getType().getType());
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
		Type[] childTypes = new Type[children.size()+1]; // heres where we try to determine each type and then see if how it constructs the above subtype
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
	}
	private boolean isBeingDeclared(IdentifierNode node) {
		ParseNode parent = node.getParent();
		return (parent instanceof DeclarationNode) && (node == parent.child(0));
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