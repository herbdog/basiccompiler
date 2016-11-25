package asmCodeGenerator;

import java.util.HashMap;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;
import asmCodeGenerator.runtime.MemoryManager;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.*;
import semanticAnalyzer.types.ArrayType;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import semanticAnalyzer.types.TypeLiteral;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	ParseNode root;
	public HashMap<String,String> stringlist;
	public HashMap<String,ParseNode> arraylist;
	public HashMap<String,String[]> looplist;
	int offset;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
		stringlist = new HashMap<String,String>();
		arraylist = new HashMap<String,ParseNode>();
		looplist = new HashMap<String,String[]>();
		offset = 0;
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.append( MemoryManager.codeForInitialization() );
		code.append( RunTime.getEnvironment() );
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
		code.append( MemoryManager.codeForAfterApplication() );
		
		return code;
	}
	private ASMCodeFragment globalVariableBlockASM() {
		assert root.hasScope();
		Scope scope = root.getScope();
		int globalBlockSize = scope.getAllocatedSize();
		
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		code.add(DLabel, RunTime.GLOBAL_MEMORY_BLOCK);
		code.add(DataZ, globalBlockSize);
		return code;
	}
	private ASMCodeFragment programASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);
		
		code.add(    Label, RunTime.MAIN_PROGRAM_LABEL);
		code.append( programCode());
		code.add(    Halt );
		
		return code;
	}
	private ASMCodeFragment programCode() {
		CodeVisitor visitor = new CodeVisitor();
		root.accept(visitor);
		return visitor.removeRootCode(root);
	}


	protected class CodeVisitor extends ParseNodeVisitor.Default {
		private Map<ParseNode, ASMCodeFragment> codeMap;
		ASMCodeFragment code;
		
		public CodeVisitor() {
			codeMap = new HashMap<ParseNode, ASMCodeFragment>();
		}


		////////////////////////////////////////////////////////////////////
        // Make the field "code" refer to a new fragment of different sorts.
		private void newAddressCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_ADDRESS);
			codeMap.put(node, code);
		}
		private void newValueCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VALUE);
			codeMap.put(node, code);
		}
		private void newVoidCode(ParseNode node) {
			code = new ASMCodeFragment(GENERATES_VOID);
			codeMap.put(node, code);
		}

	    ////////////////////////////////////////////////////////////////////
        // Get code from the map.
		private ASMCodeFragment getAndRemoveCode(ParseNode node) {
			ASMCodeFragment result = codeMap.get(node);
			codeMap.remove(result);
			return result;
		}
	    public  ASMCodeFragment removeRootCode(ParseNode tree) {
			return getAndRemoveCode(tree);
		}		
		ASMCodeFragment removeValueCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			makeFragmentValueCode(frag, node);
			return frag;
		}		
		ASMCodeFragment removeAddressCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isAddress();
			return frag;
		}		
		ASMCodeFragment removeVoidCode(ParseNode node) {
			ASMCodeFragment frag = getAndRemoveCode(node);
			assert frag.isVoid();
			return frag;
		}
		
	    ////////////////////////////////////////////////////////////////////
        // convert code to value-generating code.
		private void makeFragmentValueCode(ASMCodeFragment code, ParseNode node) {
			assert !code.isVoid();
			
			if(code.isAddress()) {
				turnAddressIntoValue(code, node);
			}	
		}
		private void turnAddressIntoValue(ASMCodeFragment code, ParseNode node) {
			if(node.getType().getType().getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}	
			else if(node.getType().getType().getType() == PrimitiveType.FLOAT) {
				code.add(LoadF);
			}
			else if(node.getType().getType().getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}	
			else if(node.getType().getType().getType() == PrimitiveType.STRING) {
				code.add(LoadI);
			}
			else if(node.getType().getType().getType() == PrimitiveType.CHAR) {
				code.add(LoadC);
			}
			else if(node.getType().getType().getType() == PrimitiveType.RATIONAL) {
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.add(LoadI);
				code.add(Exchange);
				code.add(LoadI);
			}
			else if(node.getType().getType() instanceof ArrayType) {
				code.add(LoadI);
			}
			else {
				assert false : "node " + node;
			}
			code.markAsValue();
		}
		
	    ////////////////////////////////////////////////////////////////////
        // ensures all types of ParseNode in given AST have at least a visitLeave	
		public void visitLeave(ParseNode node) {
			assert false : "node " + node + " not handled in ASMCodeGenerator";
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructs larger than statements
		public void visitLeave(ProgramNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(MainBlockNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}
		public void visitLeave(BlockNode node) {
			newVoidCode(node);
			for(ParseNode child : node.getChildren()) {
				ASMCodeFragment childCode = removeVoidCode(child);
				code.append(childCode);
			}
		}

		///////////////////////////////////////////////////////////////////////////
		// statements and declarations

		public void visitLeave(PrintStatementNode node) {
			newVoidCode(node);
			new PrintStatementGenerator(code, this, stringlist, arraylist).generate(node);	
		}
		public void visit(NewlineNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.NEWLINE_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(TabNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.TAB_PRINT_FORMAT);
			code.add(Printf);
		}
		public void visit(SpaceNode node) {
			newVoidCode(node);
			code.add(PushD, RunTime.SPACE_PRINT_FORMAT);
			code.add(Printf);
		}
		

		public void visitLeave(DeclarationNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));	
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			if (node.child(1).getType().getType() == PrimitiveType.RATIONAL) {
				code.append(lvalue);
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.append(rvalue);
				code.add(Duplicate);
				code.add(PushI, 11000);
				code.add(Exchange);
				code.add(StoreI);
				code.add(Pop);
				code.add(StoreI);
				code.add(PushI, 11000);
				code.add(LoadI);
				code.add(StoreI);
			}
			else {
				IdentifierNode idennode = (IdentifierNode) node.child(0);
				if(node.child(1).getType().getType() == PrimitiveType.STRING) {
					if(node.child(1).getToken().toString().contains("CAST")) {
						ParseNode tempnode = node.child(1);
						while(!tempnode.getChildren().isEmpty()) {
							tempnode = tempnode.child(0);
						}
						String label = stringlist.get(tempnode.getToken().getLexeme().replaceAll("\"", ""));
						stringlist.put(idennode.findVariableBinding().toString(), label);
					}
					else {
						String label = stringlist.get(node.child(1).getToken().getLexeme().replaceAll("\"", ""));
						stringlist.put(idennode.findVariableBinding().toString(),label);
					}
				}
				else if (node.child(1).getType().getType() instanceof ArrayType) {
					arraylist.put(idennode.findVariableBinding().toString(),node.child(1));
				}
			
				code.append(lvalue);
				code.append(rvalue);
				Type type = null;
				if (node.child(1) instanceof IndexNode) {
					removeValueCode(node.child(1).child(0));
					type = node.child(1).child(0).getType().getType();
				}
				else {
					type = node.getType().getType().getType();
				}
				code.add(opcodeForStore(type));
			}
		}
		private ASMOpcode opcodeForStore(Type type) {
			if(type == PrimitiveType.INTEGER) {
				return StoreI;
			}
			if(type == PrimitiveType.FLOAT) {
				return StoreF;
			}
			if(type == PrimitiveType.BOOLEAN) {
				return StoreC;
			}
			if(type == PrimitiveType.STRING) {
				return StoreI;
			}
			if(type == PrimitiveType.CHAR) {
				return StoreC;
			}
			if(type instanceof ArrayType) {
				return StoreI;
			}
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}

		public void visitLeave(AssignNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			if ((node.child(1).getType().getType() == PrimitiveType.RATIONAL) && (node.child(1).toString().contains("BinaryOperatorNode (OVER) RATIONAL"))) {
				
				code.append(lvalue);
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.append(rvalue);
				code.add(Duplicate);
				code.add(PushI, 11000);
				code.add(Exchange);
				code.add(StoreI);
				code.add(Pop);
				code.add(StoreI);
				code.add(PushI, 11000);
				code.add(LoadI);
				code.add(StoreI);
				
			}
			else if ((node.child(1).getType().getType() == PrimitiveType.RATIONAL) && (node.child(1).toString().contains("IntegerConstantNode"))) {
				
				code.append(lvalue);
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.append(rvalue);
				code.add(PushI, 1);
				code.add(Duplicate);
				code.add(PushI, 11000);
				code.add(Exchange);
				code.add(StoreI);
				code.add(Pop);
				code.add(StoreI);
				code.add(PushI, 11000);
				code.add(LoadI);
				code.add(StoreI);
				
			}
			else if ((node.child(1).getType().getType() == PrimitiveType.RATIONAL) && (node.child(1).toString().contains("FloatConstantNode"))) {
				
				code.append(lvalue);
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.append(rvalue);
				code.add(ConvertI);
				code.add(PushI, 1);
				code.add(Duplicate);
				code.add(PushI, 11000);
				code.add(Exchange);
				code.add(StoreI);
				code.add(Pop);
				code.add(StoreI);
				code.add(PushI, 11000);
				code.add(LoadI);
				code.add(StoreI);
				
			}
			else if ((node.child(1).getType().getType() == PrimitiveType.RATIONAL) && (node.child(1).toString().contains("CharacterConstantNode"))) {
				
				code.append(lvalue);
				code.add(Duplicate);
				code.add(PushI, 4);
				code.add(Add);
				code.append(rvalue);
				code.add(PushI, 1);
				code.add(Duplicate);
				code.add(PushI, 11000);
				code.add(Exchange);
				code.add(StoreI);
				code.add(Pop);
				code.add(StoreI);
				code.add(PushI, 11000);
				code.add(LoadI);
				code.add(StoreI);
				
			}
			else {
				if(node.child(1).getType().getType() == PrimitiveType.STRING) {
					IdentifierNode idennode = (IdentifierNode) node.child(0);
					ParseNode childnode = node.child(1);
					if (childnode instanceof BinaryOperatorNode) {
						while (childnode instanceof BinaryOperatorNode) {
							childnode = childnode.child(0);
						}
						String label1 = stringlist.get(childnode.getToken().getLexeme().replaceAll("\"", ""));
						stringlist.put(idennode.findVariableBinding().toString(),label1);
					}
					else {
						String label = stringlist.get(idennode.getDeclarationScope().toString());
						stringlist.put(idennode.findVariableBinding().toString(),label);
					}
				}
				
				code.append(lvalue);
				code.append(rvalue);
				if ((node.child(1).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).toString().contains("FloatConstantNode"))) {
					code.add(ConvertI);
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).toString().contains("OVER"))) {
					code.add(Divide);
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).toString().contains("CharacterConstantNode"))) {
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.FLOAT) && (node.child(1).toString().contains("OVER"))) {
					code.add(ConvertF);
					code.add(Exchange);
					code.add(ConvertF);
					code.add(Exchange);
					code.add(FDivide);
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.FLOAT) && (node.child(1).toString().contains("IntegerConstantNode"))) {
					code.add(ConvertF);
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.FLOAT) && (node.child(1).toString().contains("CharacterConstantNode"))) {
					code.add(ConvertF);
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.CHAR) && (node.child(1).toString().contains("OVER"))) {
					code.add(Divide);
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.CHAR) && (node.child(1).toString().contains("IntegerConstantNode"))) {
				}
				else if ((node.child(1).getType().getType() == PrimitiveType.CHAR) && (node.child(1).toString().contains("FloatConstantNode"))) {
					code.add(ConvertI);
				}
				Type type = node.child(1).getType().getType();
				node.child(0).setType(type);
				code.add(opcodeForStore(type));
			}
		}
		
		public void visitLeave(IfNode node) {
			if (node.nChildren() == 2) {
				newVoidCode(node);
				ASMCodeFragment exprval = removeValueCode(node.child(0));
				ASMCodeFragment block = removeVoidCode(node.child(1));
				Labeller label = new Labeller("if");
				
				String startLabel = label.newLabel("start");
				String blockLabel = label.newLabel("block");
				String endLabel = label.newLabel("end");
				
				code.add(Label, startLabel);
				code.append(exprval);
				code.add(JumpFalse, endLabel);
				code.add(Label, blockLabel);
				code.append(block);
			
				code.add(Label, endLabel);
			}
			else {
				newVoidCode(node);
				ASMCodeFragment exprval = removeValueCode(node.child(0));
				ASMCodeFragment block = removeVoidCode(node.child(1));
				ASMCodeFragment elseblock = removeVoidCode(node.child(2));
				Labeller label = new Labeller("if");
				
				String startLabel = label.newLabel("start");
				String blockLabel = label.newLabel("block");
				String elseLabel = label.newLabel("else");
				String endLabel = label.newLabel("end");
				
				code.add(Label, startLabel);
				code.append(exprval);
				code.add(JumpFalse, elseLabel);
				code.add(Label, blockLabel);
				code.append(block);
				code.add(Jump, endLabel);
				
				code.add(Label, elseLabel);
				code.append(elseblock);
				code.add(Jump, endLabel);
				code.add(Label, endLabel);
			}
		}
		
		public void visitLeave(WhileNode node) {
			newVoidCode(node);
			ASMCodeFragment exprval = removeValueCode(node.child(0));
			ASMCodeFragment block = removeVoidCode(node.child(1));
			
			String[] labels = looplist.get(node.getLocalScope().toString());
			
			if (labels != null) {
				String startLabel = labels[0];
				String blockLabel = labels[1];
				String endLabel = labels[2];
				
				code.add(Label, startLabel);
				code.append(exprval);
				code.add(JumpFalse, endLabel);
				code.add(Label, blockLabel);
				code.append(block);
				code.add(Jump, startLabel);
				
				code.add(Label, endLabel);
			}
			else {
				Labeller label = new Labeller("while");
				
				String startLabel = label.newLabel("start");
				String blockLabel = label.newLabel("block");
				String endLabel = label.newLabel("end");
				
				code.add(Label, startLabel);
				code.append(exprval);
				code.add(JumpFalse, endLabel);
				code.add(Label, blockLabel);
				code.append(block);
				code.add(Jump, startLabel);
				
				code.add(Label, endLabel);
			}
		}
		
		public void visit(BreakNode node) {
			newVoidCode(node);
			
			String[] labels = null;
			for (ParseNode nodes : node.pathToRoot()) {
				if (nodes instanceof WhileNode) {
					labels = looplist.get(nodes.getLocalScope().toString());
					break;
				}
			}
			
			if (labels == null) {
				Labeller label = new Labeller("while");
				
				String startLabel = label.newLabel("start");
				String blockLabel = label.newLabel("block");
				String endLabel = label.newLabel("end");
				String[] labels1 = {startLabel, blockLabel, endLabel};
				for (ParseNode nodes : node.pathToRoot()) {
					if (nodes instanceof WhileNode) {
						looplist.put(nodes.getLocalScope().toString(),labels1);
						break;
					}
				}
				code.add(Jump, endLabel);
			}
			else {
				code.add(Jump, labels[2]);
			}
		}
		
		public void visit(ContinueNode node) {
			newVoidCode(node);
			
			String[] labels = null;
			for (ParseNode nodes : node.pathToRoot()) {
				if (nodes instanceof WhileNode) {
					labels = looplist.get(nodes.getLocalScope().toString());
					break;
				}
			}
			
			if (labels == null) {
				Labeller label = new Labeller("while");
				
				String startLabel = label.newLabel("start");
				String blockLabel = label.newLabel("block");
				String endLabel = label.newLabel("end");
				String[] labels1 = {startLabel, blockLabel, endLabel};
				for (ParseNode nodes : node.pathToRoot()) {
					if (nodes instanceof WhileNode) {
						looplist.put(nodes.getLocalScope().toString(),labels1);
						break;
					}
				}
				code.add(Jump, startLabel);
			}
			else {
				code.add(Jump, labels[0]);
			}
			
		}
		///////////////////////////////////////////////////////////////////////////
		// expressions
		public void visitLeave(BinaryOperatorNode node) {
			Lextant operator = node.getOperator();

			if(operator == Punctuator.GREATER) {
				visitGreaterOperatorNode(node, operator);
			}
			else if(operator == Punctuator.GREATER_EQUAL) {
				visitGreaterEqualOperatorNode(node,operator);
			}
			else if(operator == Punctuator.LESS) {
				visitLessOperatorNode(node,operator);
			}
			else if(operator == Punctuator.LESS_EQUAL) {
				visitLessEqualOperatorNode(node,operator);
			}
			else if(operator == Punctuator.EQUAL) {
				visitEqualEqualOperatorNode(node, operator);
			}
			else if(operator == Punctuator.NOTEQUAL) {
				visitNotEqualOperatorNode(node, operator);
			}
			else if(operator == Punctuator.CAST) {
				visitCastOperatorNode(node, operator);
			}
			else if(operator == Punctuator.AND) {
				visitAndOperatorNode(node, operator);
			}
			else if(operator == Punctuator.OR) {
				visitOrOperatorNode(node, operator);
			}
			else {
				visitNormalBinaryOperatorNode(node);
			}
		}
		public void visitLeave(NotOperatorNode node) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			
			Labeller labeller = new Labeller("and");
			
			String startLabel = labeller.newLabel("arg1");
			String joinLabel  = labeller.newLabel("join");
			String trueLabel = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(JumpFalse, trueLabel);
			code.add(Jump, falseLabel);
			
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		private void visitGreaterOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(JumpPos, trueLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(JumpPos, trueLabel);
			}
			else {
				code.add(JumpFPos, trueLabel);
			}
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		
		private void visitGreaterEqualOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(JumpNeg, falseLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(JumpNeg, falseLabel);
			}
			else {
				code.add(JumpFNeg, falseLabel);
			}
			code.add(Jump, trueLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		
		private void visitLessOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(JumpNeg, trueLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(JumpNeg, trueLabel);
			}
			else {
				code.add(JumpFNeg, trueLabel);
			}
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		private void visitLessEqualOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(JumpPos, falseLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(JumpPos, falseLabel);
			}
			else {
				code.add(JumpFPos, falseLabel);
			}
			code.add(Jump, trueLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		
		private void visitEqualEqualOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType().getType() == PrimitiveType.BOOLEAN)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(JumpFalse, trueLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(JumpFalse, trueLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType().getType() == PrimitiveType.BOOLEAN)) {
				code.add(JumpFalse, trueLabel);
			}
			else {
				code.add(JumpFZero, trueLabel);
			}
			code.add(Jump, falseLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		private void visitNotEqualOperatorNode(BinaryOperatorNode node,
				Lextant operator) {

			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("compare");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String subLabel   = labeller.newLabel("sub");
			String trueLabel  = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			String joinLabel  = labeller.newLabel("join");
			
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Label, subLabel);
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType().getType() == PrimitiveType.BOOLEAN)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == PrimitiveType.INTEGER)) {
				code.add(JumpFalse, falseLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.CHAR) && (node.child(1).getType().getType() == PrimitiveType.CHAR)) {
				code.add(JumpFalse, falseLabel);
			}
			else if ((node.child(0).getType().getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType().getType() == PrimitiveType.BOOLEAN)) {
				code.add(JumpFalse, falseLabel);
			}
			else {
				code.add(JumpFZero, falseLabel);
			}
			code.add(Jump, trueLabel);

			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);

		}
		private void visitCastOperatorNode(BinaryOperatorNode node, Lextant operator) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			code.append(arg1);	
			if(((node.child(0).getType().getType() == PrimitiveType.INTEGER) || (node.child(0).getType().getType() == PrimitiveType.CHAR)) && (node.child(1).getType().getType() == TypeLiteral.RAT))
				castrational();
			if((node.child(0).getType().getType() == PrimitiveType.FLOAT) && (node.child(1).getType().getType() == TypeLiteral.RAT)) {
				castfrational();
				lowestterms();
			}
			if((node.child(0).getType().getType() == PrimitiveType.INTEGER) && (node.child(1).getType().getType() == TypeLiteral.FLOAT))
				code.add(ConvertF);
			if((node.child(0).getType().getType() == PrimitiveType.FLOAT) && (node.child(1).getType().getType() == TypeLiteral.INT))
				code.add(ConvertI);
		}
		private void visitAndOperatorNode(BinaryOperatorNode node, Lextant operator) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("and");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String joinLabel  = labeller.newLabel("join");
			String falseLabel = labeller.newLabel("false");
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(JumpFalse, falseLabel);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(Jump, joinLabel);
			
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
			
		}
		private void visitOrOperatorNode(BinaryOperatorNode node, Lextant operator) {
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			
			Labeller labeller = new Labeller("and");
			
			String startLabel = labeller.newLabel("arg1");
			String arg2Label  = labeller.newLabel("arg2");
			String joinLabel  = labeller.newLabel("join");
			String trueLabel = labeller.newLabel("true");
			String falseLabel = labeller.newLabel("false");
			newValueCode(node);
			code.add(Label, startLabel);
			code.append(arg1);
			code.add(JumpTrue, trueLabel);
			code.add(Label, arg2Label);
			code.append(arg2);
			code.add(JumpFalse, falseLabel);
			code.add(Jump, trueLabel);
			
			code.add(Label, falseLabel);
			code.add(PushI, 0);
			code.add(Jump, joinLabel);
			code.add(Label, trueLabel);
			code.add(PushI, 1);
			code.add(Jump, joinLabel);
			code.add(Label, joinLabel);
		}
		private void visitNormalBinaryOperatorNode(BinaryOperatorNode node) {
			newValueCode(node);
			ASMCodeFragment arg1 = removeValueCode(node.child(0));
			ASMCodeFragment arg2 = removeValueCode(node.child(1));
			Type arg1type = null;
			Type arg2type = null;
			String arg1pred = node.child(0).toString().split("\n")[0];
			String arg2pred = node.child(1).toString().split("\n")[0];
			
			code.append(arg1);
			if (node.child(0) instanceof IndexNode) {
				removeValueCode(node.child(0).child(0));
				arg1type = node.child(0).child(0).getType().getType().getType().getType();
			}
			else {
				arg1type = node.child(0).getType().getType();
			}
			if (arg1type == PrimitiveType.INTEGER) {
				if (arg1pred.contains("FloatConstantNode")) {
					code.add(ConvertI);
					code.append(arg2);
					if (arg2pred.contains("BinaryOperatorNode (OVER)"))
						code.add(Divide);
				}
				else if (arg1pred.contains("CharacterConstantNode")) {
					code.append(arg2);
					if (arg2pred.contains("FloatConstantNode"))
						code.add(ConvertI);
					else if (arg2pred.contains("BinaryOperatorNode (OVER)"))
						code.add(Divide);
				}
				else if ((arg1pred.contains("BinaryOperatorNode (OVER)")) && (!node.getToken().isLextant(Punctuator.OVER))) {
					code.add(Divide);
					code.append(arg2);
					if (arg2pred.contains("FloatConstantNode"))
						code.add(ConvertI);
				}
				else if (node.child(0).nChildren() > 0) {
					if (node.child(0).child(0).getType() == PrimitiveType.FLOAT) {
						code.add(ConvertI);
						code.append(arg2);
						if (arg2pred.contains("BinaryOperatorNode (OVER)"))
							code.add(Divide);
						else if (arg2pred.contains("FloatConstantNode"))
							code.add(ConvertI);
					}
					else if (node.child(0).child(0).getType() == PrimitiveType.CHAR) {
						code.append(arg2);
						if (arg2pred.contains("FloatConstantNode"))
							code.add(ConvertI);
						else if (arg2pred.contains("BinaryOperatorNode (OVER)"))
							code.add(Divide);
					}
					else if (node.child(0).child(0).getType() == PrimitiveType.RATIONAL) {
						code.add(Divide);
						code.append(arg2);
					}
					else {
						code.append(arg2);
					}
				}
				else {
					code.append(arg2);
				}
			}
			else if (arg1type == PrimitiveType.FLOAT) {
				if (node.child(0).nChildren() > 0) {
					if (node.child(0).child(0).getType() == PrimitiveType.INTEGER) {
						code.add(ConvertF);
						code.append(arg2);
						if (arg2pred.contains("BinaryOperatorNode (OVER)"))
							code.add(Divide);
					}
				}
				else if ((arg1pred.contains("IntegerConstantNode"))) {
					code.add(ConvertF);
					code.append(arg2);
					if (arg2pred.contains("BinaryOperatorNode (OVER)"))
						code.add(Divide);
				}
				else {
					code.append(arg2);
				}
			}
			else if (arg1type == PrimitiveType.RATIONAL) {
				if (arg1pred.contains("IntegerConstantNode")) {
					code.add(PushI, 1);
					code.append(arg2);
					if (arg2pred.contains("FloatConstantNode"))
						code.add(ConvertI);
				}
				else if (node.child(0).nChildren() > 0) {
					if ((node.child(0).child(0).getType() == PrimitiveType.INTEGER) && (!node.child(0).getToken().isLextant(Punctuator.OVER))) {
						code.add(PushI, 1);
						code.append(arg2);
						if (arg2pred.contains("FloatConstantNode"))
							code.add(ConvertI);
					}
					else {
						code.append(arg2);
					}
				}
				else {
					code.append(arg2);
				}
			}
			else {
				code.append(arg2);
			}
			if (node.child(1) instanceof IndexNode) {
				removeValueCode(node.child(1).child(0));
				arg2type = node.child(1).child(0).getType().getType().getType().getType();
			}
			else {
				arg2type = node.child(1).child(0).getType().getType();
			}
			
			ASMOpcode opcode = opcodeForOperator(node.getOperator(),arg1type,arg2type);
			code.add(opcode);							// type-dependent! (opcode is different for floats and for ints)
		}
		private ASMOpcode opcodeForOperator(Lextant lextant, Type arg1, Type arg2) {
			assert(lextant instanceof Punctuator);
			Punctuator punctuator = (Punctuator)lextant;
			switch(punctuator) {
			case ADD: 	   		
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER))
				return Add;	
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT))
				return FAdd;
			if ((arg1 == PrimitiveType.RATIONAL) && (arg2 == PrimitiveType.RATIONAL)) {
				rationaladd();
				lowestterms();
				return Nop;
			}
			case SUBTRACT:
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER))
				return Subtract;	
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT))
				return FSubtract;
			if ((arg1 == PrimitiveType.RATIONAL) && (arg2 == PrimitiveType.RATIONAL)) {
				rationalsub();
				lowestterms();
				return Nop;
			}
			case MULTIPLY: 
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER))
				return Multiply;	
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT))
				return FMultiply;
			if ((arg1 == PrimitiveType.RATIONAL) && (arg2 == PrimitiveType.RATIONAL)) {
				rationalmul();
				lowestterms();
				return Nop;
			}
			case DIVIDE: 
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER)) {
				code.add(Duplicate);
				code.add(JumpFalse, "$$i-divide-by-zero" );
				return Divide;	
			}
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT)) {
				code.add(Duplicate);
				code.add(JumpFZero, "$$i-divide-by-zero" );
				return FDivide;
			}
			if ((arg1 == PrimitiveType.RATIONAL) && (arg2 == PrimitiveType.RATIONAL)) {
				rationaldiv();
				lowestterms();
				return Nop;
			}
			case OVER:
				code.add(Duplicate);
				code.add(JumpFalse, "$$i-divide-by-zero" );
				lowestterms();
				return Nop;
			case EXPRESS_OVER:
			if (arg1 == PrimitiveType.RATIONAL)
				ratexpressover();
			if (arg1 == PrimitiveType.FLOAT)
				floatexpressover();
				return Nop;
			case RATIONALIZE:
			if (arg1 == PrimitiveType.RATIONAL) {
				rationalizerat();
				lowestterms();
			}
			if (arg1 == PrimitiveType.FLOAT) {
				rationalizefloat();
				lowestterms();
			}
				return Nop;
			default:
				assert false : "unimplemented operator in opcodeForOperator";
			}
			return null;
		}
		
		private void rationaladd() {
			
			code.add(Duplicate);
			code.add(PushI, 40000);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Exchange);
			code.add(Duplicate);
			code.add(PushI, 40004);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Pop);
			code.add(Pop);
			code.add(Duplicate);
			code.add(PushI, 40008);
			code.add(Exchange);
			code.add(StoreI);
			code.add(PushI, 40004);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Exchange);
			code.add(PushI, 40000);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);
			code.add(PushI, 40000);
			code.add(LoadI);
			code.add(PushI, 40008);
			code.add(LoadI);
			code.add(Multiply);
			
		}
		
		private void rationalsub() {
			
			code.add(Duplicate);
			code.add(PushI, 40000);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Exchange);
			code.add(Duplicate);
			code.add(PushI, 40004);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Pop);
			code.add(Pop);
			code.add(Duplicate);
			code.add(PushI, 40008);
			code.add(Exchange);
			code.add(StoreI);
			code.add(PushI, 40004);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Exchange);
			code.add(PushI, 40000);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Exchange);
			code.add(Subtract);
			code.add(PushI, 40000);
			code.add(LoadI);
			code.add(PushI, 40008);
			code.add(LoadI);
			code.add(Multiply);
		}
		
		private void rationalmul() {
			
			code.add(Exchange);
			code.add(PushI, 40004);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Multiply);
			code.add(Exchange);
			code.add(PushI, 40004);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Exchange);
			
		}
		
		private void rationaldiv() {
			
			code.add(PushI, 40004);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Multiply);
			code.add(Exchange);
			code.add(PushI, 40004);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Exchange);
			
		}
		
		private void lowestterms() {
			Labeller label = new Labeller("gcd");
			String whileloop = label.newLabel("whileloop");
			String lowerterms = label.newLabel("lower");
			
			code.add(Duplicate);
			code.add(PushI, 20000);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Exchange);
			code.add(Duplicate);
			code.add(PushI, 20004);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Exchange);
			code.add(Exchange);
			code.add(Jump, whileloop);

			code.add(Label, whileloop);
			code.add(Exchange);
			code.add(Duplicate);
			code.add(PushI, 19996);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Remainder);
			code.add(Duplicate);
			code.add(PushI, 19996);
			code.add(LoadI);
			code.add(Exchange);
			code.add(JumpTrue, whileloop);
			code.add(Jump, lowerterms);
			
			code.add(Label, lowerterms);
			code.add(Exchange);
			code.add(Pop);
			code.add(PushI, 20004);
			code.add(LoadI);
			code.add(Exchange);
			code.add(Divide);
			code.add(PushI, 20000);
			code.add(LoadI);
			code.add(PushI, 19996);
			code.add(LoadI);
			code.add(Divide);
		}

		private void ratexpressover() {
			
			code.add(PushI, 30000);
			code.add(Exchange);
			code.add(StoreI);
			code.add(Exchange);
			code.add(PushI, 30000);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Exchange);
			code.add(Divide);
			
		}
		
		private void floatexpressover() {
			
			code.add(ConvertF);
			code.add(FMultiply);
			code.add(ConvertI);
			
		}
		
		private void rationalizerat() {
			
			ratexpressover();
			code.add(PushI, 30000);
			code.add(LoadI);
			
		}
		
		private void rationalizefloat() {
			
			code.add(Duplicate);
			code.add(PushI, 30000);
			code.add(Exchange);
			code.add(StoreI);
			floatexpressover();
			code.add(PushI, 30000);
			code.add(LoadI);
			
		}
		
		private void castrational() {
			
			code.add(PushI, 1);
			
		}
		
		private void castfrational() {
			
			code.add(PushI, 223092870);
			rationalizefloat();
			
		}
		
		//array node
		
		public void visitLeave(ArrayNode node) {
			newAddressCode(node);
			newValueCode(node);
			
			visitLeave2(node);
		}
		
		public void visitLeave2(ArrayNode node) {
			Labeller label = new Labeller("array");
			String typeid = label.newLabel("typeid");
			String status = label.newLabel("status");
			String length = label.newLabel("length");
			String subtypesize = label.newLabel("subtypesize");
			String data = label.newLabel("data");
			Type argtype = node.child(0).getType().getType();
			arraylist.put(node.toString(), node);

			code.add(PushD, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(PushI, offset);
			code.add(Add);
			code.add(Duplicate);
			code.add(DataZ, 16 + node.getType().getType().getSize() * node.nChildren());
			code.add(Label, typeid);
			code.add(Duplicate);
			code.add(PushI, 4);
			code.add(Add);
			code.add(Exchange);
			code.add(PushI, 1);
			code.add(StoreI);
			code.add(Label, status);
			code.add(Duplicate);
			code.add(PushI, 4);
			code.add(Add);
			code.add(Exchange);
			code.add(PushI, 2);
			code.add(StoreI);
			code.add(Label, subtypesize);
			code.add(Duplicate);
			code.add(PushI, 4);
			code.add(Add);
			code.add(Exchange);
			code.add(PushI, node.getType().getType().getSize());
			code.add(StoreI);
			code.add(Label, length);
			code.add(Duplicate);
			code.add(PushI, 4);
			code.add(Add);
			code.add(Exchange);
			code.add(PushI, node.nChildren());
			code.add(StoreI);	
			code.add(Label, data);

			offset = offset + (16 + argtype.getSize()*node.nChildren());
			
			for(ParseNode childnodes : node.getChildren()) {

				code.add(Duplicate);
				code.add(PushI, argtype.getSize());
				code.add(Add);
				code.add(Exchange);
				
				if (childnodes instanceof IntegerConstantNode) {
					code.add(PushI, ((IntegerConstantNode) childnodes).getValue());
				}
				else if (childnodes instanceof FloatConstantNode) {
					code.add(PushF, ((FloatConstantNode) childnodes).getValue());
				}
				else if (childnodes instanceof CharacterConstantNode) {
					code.add(PushI, ((CharacterConstantNode) childnodes).getValue());
				}
				else if (childnodes instanceof StringConstantNode) {
					visit2((StringConstantNode) childnodes);
				}
				else if (childnodes instanceof BooleanConstantNode) {
					code.add(PushI, ((BooleanConstantNode) childnodes).getValue() ? 1 : 0);
				}
				else if (childnodes instanceof BinaryOperatorNode) {
					code.add(Duplicate);
					code.add(PushI, 4);
					code.add(Add);
					
					BinaryOperatorNode bnode = (BinaryOperatorNode) childnodes;
					ASMCodeFragment arg1 = removeValueCode(childnodes.child(0));
					ASMCodeFragment arg2 = removeValueCode(childnodes.child(1));
					Type arg1type = childnodes.child(0).getType().getType();
					Type arg2type = childnodes.child(1).getType().getType();
					String arg1pred = childnodes.child(0).toString().split("\n")[0];
					String arg2pred = childnodes.child(1).toString().split("\n")[0];
					code.append(arg1);
					if (arg1type == PrimitiveType.INTEGER) {
						if (arg1pred.contains("FloatConstantNode")) {
							code.add(ConvertI);
							code.append(arg2);
							if (arg2pred.contains("BinaryOperatorNode (OVER)"))
								code.add(Divide);
						}
						else if (arg1pred.contains("CharacterConstantNode")) {
							code.append(arg2);
							if (arg2pred.contains("FloatConstantNode"))
								code.add(ConvertI);
							else if (arg2pred.contains("BinaryOperatorNode (OVER)"))
								code.add(Divide);
						}
						else if ((arg1pred.contains("BinaryOperatorNode (OVER)")) && (!childnodes.getToken().isLextant(Punctuator.OVER))) {
							code.add(Divide);
							code.append(arg2);
							if (arg2pred.contains("FloatConstantNode"))
								code.add(ConvertI);
						}
						else if (childnodes.child(0).nChildren() > 0) {
							if (childnodes.child(0).child(0).getType() == PrimitiveType.FLOAT) {
								code.add(ConvertI);
								code.append(arg2);
								if (arg2pred.contains("BinaryOperatorNode (OVER)"))
									code.add(Divide);
								else if (arg2pred.contains("FloatConstantNode"))
									code.add(ConvertI);
							}
							else if (childnodes.child(0).child(0).getType() == PrimitiveType.CHAR) {
								code.append(arg2);
								if (arg2pred.contains("FloatConstantNode"))
									code.add(ConvertI);
								else if (arg2pred.contains("BinaryOperatorNode (OVER)"))
									code.add(Divide);
							}
							else if (childnodes.child(0).child(0).getType() == PrimitiveType.RATIONAL) {
								code.add(Divide);
								code.append(arg2);
							}
							else {
								code.append(arg2);
							}
						}
						else {
							code.append(arg2);
						}
					}
					else if (arg1type == PrimitiveType.FLOAT) {
						if (childnodes.child(0).nChildren() > 0) {
							if (node.child(0).child(0).getType() == PrimitiveType.INTEGER) {
								code.add(ConvertF);
								code.append(arg2);
								if (arg2pred.contains("BinaryOperatorNode (OVER)"))
									code.add(Divide);
							}
						}
						else if ((arg1pred.contains("IntegerConstantNode"))) {
							code.add(ConvertF);
							code.append(arg2);
							if (arg2pred.contains("BinaryOperatorNode (OVER)"))
								code.add(Divide);
						}
						else {
							code.append(arg2);
						}
					}
					else if (arg1type == PrimitiveType.RATIONAL) {
						if (arg1pred.contains("IntegerConstantNode")) {
							code.add(PushI, 1);
							code.append(arg2);
							if (arg2pred.contains("FloatConstantNode"))
								code.add(ConvertI);
						}
						else if (childnodes.child(0).nChildren() > 0) {
							if ((childnodes.child(0).child(0).getType() == PrimitiveType.INTEGER) && (!childnodes.child(0).getToken().isLextant(Punctuator.OVER))) {
								code.add(PushI, 1);
								code.append(arg2);
								if (arg2pred.contains("FloatConstantNode"))
									code.add(ConvertI);
							}
							else {
								code.append(arg2);
							}
						}
						else {
							code.append(arg2);
						}
					}
					else {
						code.append(arg2);
					}
					
					
					ASMOpcode opcode = opcodeForOperator(bnode.getOperator(),arg1type,arg2type);
					code.add(opcode);
					
					code.add(Duplicate);
					code.add(PushI, 11000);
					code.add(Exchange);
					code.add(StoreI);
					code.add(Pop);
					code.add(StoreI);
					code.add(PushI, 11000);
					code.add(LoadI);
					code.add(StoreI);
					continue;
				}
				else if (childnodes.getType().getType() instanceof ArrayType) {
					ArrayNode constantnode = (ArrayNode) childnodes;
					visitLeave2(constantnode);
				}
				code.add(opcodeForStore(argtype));
			}
			code.add(Pop);
		}
		
		public void visitLeave(IndexNode node) {
			newAddressCode(node);
			IdentifierNode iden = (IdentifierNode) node.child(0);
			ASMCodeFragment index = removeValueCode(node.child(1));
			
			Binding binding = iden.getBinding();
			binding.generateAddress(code);
			code.add(LoadI);
			code.add(Duplicate);
			code.add(PushI, 90000);
			code.add(Exchange);
			code.add(StoreI);
			code.add(PushI, 12);
			code.add(Add);
			code.add(LoadI);
			code.add(PushI, 1);
			code.add(Subtract);								//highest index possible
			code.append(index);
			code.add(Duplicate);
			code.add(PushI, 76000);
			code.add(Exchange);
			code.add(StoreI);
			code.add(JumpNeg, "$$index-out-of-range");		//negative index
			code.add(PushI, 76000);
			code.add(LoadI);
			code.add(Subtract);								//if it is less than 0, it is out of range
			code.add(JumpNeg, "$$index-out-of-range");
			
			code.add(PushI, 90000);
			code.add(LoadI);
			code.add(PushI, 16);
			code.add(Add);
			code.add(PushI, 76000);
			code.add(LoadI);
			code.add(PushI, 90000);
			code.add(LoadI);
			code.add(PushI, 8);
			code.add(Add);
			code.add(LoadI);
			code.add(Multiply);
			code.add(Add);									//this is the address of a[i]
		}
		
		///////////////////////////////////////////////////////////////////////////
		// leaf nodes (ErrorNode not necessary)
		public void visit(BooleanConstantNode node) {
			newValueCode(node);
			code.add(PushI, node.getValue() ? 1 : 0);
		}
		public void visit(IdentifierNode node) {
			newAddressCode(node);
			Binding binding = node.getBinding();
			
			binding.generateAddress(code);
		}
		public void visit(IntegerConstantNode node) {
			newValueCode(node);

			code.add(PushI, node.getValue());
		}
		public void visit(FloatConstantNode node) {
			newValueCode(node);
			
			code.add(PushF, node.getValue());
		}
		public void visit(StringConstantNode node) {
			newAddressCode(node);
			newValueCode(node);
			
			visit2(node);
		}
		public void visit2(StringConstantNode node) {
			Labeller label = new Labeller("StringConstant");
			stringlist.put(node.getValue(), label.newLabel(""));
			code.add(PushD, MemoryManager.MEM_MANAGER_ALLOCATE);
			code.add(PushI, offset);
			code.add(Add);
			code.add(DLabel, label.newLabel(""));
			code.add(DataS, node.getValue());
			
			offset = offset + node.getValue().length();
		}
		public void visit(CharacterConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}
	}

}
