package asmCodeGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.codeStorage.ASMOpcode;
import asmCodeGenerator.runtime.RunTime;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import parseTree.*;
import parseTree.nodeTypes.*;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import symbolTable.Binding;
import symbolTable.Scope;
import static asmCodeGenerator.codeStorage.ASMCodeFragment.CodeType.*;
import static asmCodeGenerator.codeStorage.ASMOpcode.*;

// do not call the code generator if any errors have occurred during analysis.
public class ASMCodeGenerator {
	ParseNode root;
	public List<String> stringlist;

	public static ASMCodeFragment generate(ParseNode syntaxTree) {
		ASMCodeGenerator codeGenerator = new ASMCodeGenerator(syntaxTree);
		return codeGenerator.makeASM();
	}
	public ASMCodeGenerator(ParseNode root) {
		super();
		this.root = root;
		stringlist = new ArrayList<String>();
	}
	
	public ASMCodeFragment makeASM() {
		ASMCodeFragment code = new ASMCodeFragment(GENERATES_VOID);

		code.append( RunTime.getEnvironment() );
		code.append( globalVariableBlockASM() );
		code.append( programASM() );
//		code.append( MemoryManager.codeForAfterApplication() );
		
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
			if(node.getType() == PrimitiveType.INTEGER) {
				code.add(LoadI);
			}	
			else if(node.getType() == PrimitiveType.FLOAT) {
				code.add(LoadF);
			}
			else if(node.getType() == PrimitiveType.BOOLEAN) {
				code.add(LoadC);
			}	
			else if(node.getType() == PrimitiveType.STRING) {
				code.add(Nop);
			}
			else if(node.getType() == PrimitiveType.CHAR) {
				code.add(LoadC);
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
			new PrintStatementGenerator(code, this, stringlist).generate(node);	
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
			if(node.child(1).getType() == PrimitiveType.STRING) {
				if(node.child(1).getToken().toString().contains("CAST")) {
					String temp = stringlist.get(stringlist.size()-1);
					String temp2 = node.child(0).getToken().getLexeme().toString();
					stringlist.remove(stringlist.size()-1);
					stringlist.add(temp.concat(temp2));
				}
				else {
					stringlist.remove(stringlist.size()-1);
					stringlist.add(node.child(0).getToken().getLexeme());
				}
			}
		
			code.append(lvalue);
			code.append(rvalue);
			Type type = node.getType();
			code.add(opcodeForStore(type));
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
				return Nop;
			}
			if(type == PrimitiveType.CHAR) {
				return StoreC;
			}
			assert false: "Type " + type + " unimplemented in opcodeForStore()";
			return null;
		}

		public void visitLeave(AssignNode node) {
			newVoidCode(node);
			ASMCodeFragment lvalue = removeAddressCode(node.child(0));
			ASMCodeFragment rvalue = removeValueCode(node.child(1));
			if(node.child(1).getType() == PrimitiveType.STRING) {
				if(node.child(1).getToken().toString().contains("CAST")) {
					String temp = stringlist.get(stringlist.size()-1);
					String temp2 = node.child(0).getToken().getLexeme().toString();
					stringlist.remove(stringlist.size()-1);
					stringlist.add(temp.concat(temp2));
				}
				else {
					stringlist.remove(stringlist.size()-1);
					stringlist.add(node.child(0).getToken().getLexeme());
				}
			}
			
			
			code.append(lvalue);
			code.append(rvalue);
			Type type = node.child(1).getType();
			code.add(opcodeForStore(type));
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
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(JumpPos, trueLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
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
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(JumpNeg, falseLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
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
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(JumpNeg, trueLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
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
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(JumpPos, falseLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
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
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType() == PrimitiveType.BOOLEAN)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(JumpFalse, trueLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(JumpFalse, trueLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType() == PrimitiveType.BOOLEAN)) {
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
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(Subtract);
			}
			else if ((node.child(0).getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType() == PrimitiveType.BOOLEAN)) {
				code.add(Subtract);
			}
			else {
				code.add(FSubtract);
			}
			
			if ((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.INTEGER)) {
				code.add(JumpFalse, falseLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.CHAR) && (node.child(1).getType() == PrimitiveType.CHAR)) {
				code.add(JumpFalse, falseLabel);
			}
			else if ((node.child(0).getType() == PrimitiveType.BOOLEAN) && (node.child(1).getType() == PrimitiveType.BOOLEAN)) {
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
			if((node.child(0).getType() == PrimitiveType.INTEGER) && (node.child(1).getType() == PrimitiveType.FLOAT))
				code.add(ConvertF);
			if((node.child(0).getType() == PrimitiveType.FLOAT) && (node.child(1).getType() == PrimitiveType.INTEGER))
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
			Type arg1type = node.child(0).getType();
			Type arg2type = node.child(1).getType();
			code.append(arg1);
			code.append(arg2);
			
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
			case SUBTRACT:
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER))
				return Subtract;	
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT))
				return FSubtract;
			case MULTIPLY: 
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER))
				return Multiply;	
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT))
				return FMultiply;
			case DIVIDE: 
			if ((arg1 == PrimitiveType.INTEGER) && (arg2 == PrimitiveType.INTEGER))
				return Divide;	
			if  ((arg1 == PrimitiveType.FLOAT) && (arg2 == PrimitiveType.FLOAT))
				return FDivide;
			default:
				assert false : "unimplemented operator in opcodeForOperator";
			}
			return null;
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
			Labeller label = new Labeller("StringConstant");
			stringlist.add(node.getValue());
			code.add(DLabel, label.newLabel(""));
			code.add(DataS, node.getValue());
		}
		public void visit(CharacterConstantNode node) {
			newValueCode(node);
			
			code.add(PushI, node.getValue());
		}
	}

}
