package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.Jump;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpTrue;
import static asmCodeGenerator.codeStorage.ASMOpcode.Label;
import static asmCodeGenerator.codeStorage.ASMOpcode.Printf;
import static asmCodeGenerator.codeStorage.ASMOpcode.PushD;

import java.util.List;

import parseTree.ParseNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.ASMCodeGenerator;
import asmCodeGenerator.runtime.RunTime;

public class PrintStatementGenerator {
	ASMCodeFragment code;
	ASMCodeGenerator.CodeVisitor visitor;
	public List<String> stringlist;
	
	
	public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor, List<String> stringlist) {
		super();
		this.code = code;
		this.visitor = visitor;
		this.stringlist = stringlist;
	}

	public void generate(PrintStatementNode node) {
		for(ParseNode child : node.getChildren()) {
			if(child instanceof NewlineNode || child instanceof SpaceNode || child instanceof TabNode) {
				ASMCodeFragment childCode = visitor.removeVoidCode(child);
				code.append(childCode);
			}
			else {
				appendPrintCode(child);
			}
		}
	}

	private void appendPrintCode(ParseNode node) {
		String format = null;
		if (node.getType() == PrimitiveType.STRING) {
			format = printString(node);
		}
		else {
			format = printFormat(node.getType());
		}
		
		code.append(visitor.removeValueCode(node));
		convertToStringIfBoolean(node);
		code.add(PushD, format);
		code.add(Printf);
	}
	private void convertToStringIfBoolean(ParseNode node) {
		if(node.getType() != PrimitiveType.BOOLEAN) {
			return;
		}
		
		Labeller labeller = new Labeller("print-boolean");
		String trueLabel = labeller.newLabel("true");
		String endLabel = labeller.newLabel("join");

		code.add(JumpTrue, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_FALSE_STRING);
		code.add(Jump, endLabel);
		code.add(Label, trueLabel);
		code.add(PushD, RunTime.BOOLEAN_TRUE_STRING);
		code.add(Label, endLabel);
	}
	
	private String printString(ParseNode node) {
		String format = null;
		int iterator = stringlist.size();
		iter:
		for (int i = stringlist.size()-1; i > 0; i--) {
			if(stringlist.get(i).toString().contains(node.getToken().getLexeme().replaceAll("\"",""))) {
				break iter;
			}
			if(node.toString().contains("CAST")) {
				ParseNode tempnode = node.child(0);
				while(!tempnode.getChildren().isEmpty()) {
					if(stringlist.get(i).contains(tempnode.getToken().getLexeme().replaceAll("\"", ""))) {
						break iter;
					}
					tempnode = tempnode.child(0);
				}
				if(stringlist.get(i).contains(tempnode.getToken().getLexeme().replaceAll("\"", ""))) {
					break iter;
				}
			}
			iterator--;
		}
		format = "-StringConstant-" + iterator + "-";
		return format;
	}


	private static String printFormat(Type type) {
		assert type instanceof PrimitiveType;
		
		switch((PrimitiveType)type) {
		case INTEGER:	return RunTime.INTEGER_PRINT_FORMAT;
		case FLOAT:		return RunTime.FLOAT_PRINT_FORMAT;
		case STRING:	return RunTime.STRING_PRINT_FORMAT;
		case CHAR:		return RunTime.CHARACTER_PRINT_FORMAT;
		case BOOLEAN:	return RunTime.BOOLEAN_PRINT_FORMAT;
		default:		
			assert false : "Type " + type + " unimplemented in PrintStatementGenerator.printFormat()";
			return "";
		}
	}
}
