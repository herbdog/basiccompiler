package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.*;
import java.util.HashMap;
import java.util.Set;

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
	public HashMap<String,String> stringlist;
	
	
	public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor, HashMap<String,String> stringlist) {
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
		if (node.getType() == PrimitiveType.RATIONAL) {
			printRational(node);
			return;
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
		String key = null;
		Set<String> keyset = stringlist.keySet();
		if(node.toString().contains("CAST")) {
			iter:
			for (String keys : keyset) {
				ParseNode tempnode = node.child(0);
				while(!tempnode.getChildren().isEmpty()) {
					if(keys.contains(tempnode.getToken().getLexeme().replaceAll("\"", ""))) {
						key = keys;
						break iter;
					}
					tempnode = tempnode.child(0);
				}
				if(keys.contains(tempnode.getToken().getLexeme().replaceAll("\"", ""))) {
					key = keys;
					break iter;
				}
			}
			format = stringlist.get(key);
		}
		else {
			format = stringlist.get(node.getToken().getLexeme().replaceAll("\"", ""));
		}
		return format;
	}
	
	private void printRational(ParseNode node) {
		Labeller label = new Labeller("rational");
		String sign = label.newLabel("sign");
		String negpart = label.newLabel("negative");
		String division = label.newLabel("division");
		String negatequot = label.newLabel("negatequotient");
		String quot2 = label.newLabel("quot2");
		String zeroremainder = label.newLabel("zeroremainder");
		String fractionpart = label.newLabel("fraction");
		String quotientpart = label.newLabel("quotient");
		String negremainder = label.newLabel("negativeremainder");
		String printremainder = label.newLabel("printremainder");
		String negdenom = label.newLabel("negativedenominator");
		String printdenom = label.newLabel("printdenom");
		String endlabel = label.newLabel("endlabel");
		
		code.add(PushI, 9997);
		code.add(PushI, 45);
		code.add(StoreC);
		code.add(PushI, 9998);
		code.add(PushI, 95);
		code.add(StoreC);
		code.add(PushI, 9999);
		code.add(PushI, 47);
		code.add(StoreC);
		
		code.append(visitor.removeValueCode(node));
		code.add(Duplicate);
		code.add(PushI, 10000);
		code.add(Exchange);
		code.add(StoreI);
		code.add(Exchange);
		code.add(Duplicate);
		code.add(PushI, 10004);
		code.add(Exchange);
		code.add(StoreI);
		code.add(Exchange);
		
		code.add(JumpNeg, sign);
		code.add(JumpNeg, negpart);
		code.add(Jump, division);
		
		code.add(Label, sign);
		code.add(JumpPos, negpart);
		code.add(Jump, division);
		code.add(Label, negpart);
		code.add(PushI, 9997);
		code.add(LoadC);
		code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, division);
		
		code.add(Label, division);
		code.add(PushI, 10004);
		code.add(LoadI);
		code.add(PushI, 10000);
		code.add(LoadI);
		code.add(Divide);
		code.add(Duplicate);
		code.add(JumpNeg, negatequot);
		code.add(Jump, quot2);
		
		code.add(Label, negatequot);
		code.add(Negate);
		code.add(Jump, quot2);
		
		code.add(Label, quot2);
		code.add(Duplicate);
		code.add(JumpFalse, zeroremainder);
		code.add(Jump, quotientpart);
		
		code.add(Label, quotientpart);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, zeroremainder);
		
		code.add(Label, zeroremainder);
		code.add(PushI, 10004);
		code.add(LoadI);
		code.add(PushI, 10000);
		code.add(LoadI);
		code.add(Remainder);
		code.add(JumpFalse, endlabel);
		code.add(Jump, fractionpart);
		
		code.add(Label, fractionpart);
		code.add(PushI, 9998);
		code.add(LoadC);
		code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
		code.add(Printf);
		code.add(PushI, 10004);
		code.add(LoadI);
		code.add(PushI, 10000);
		code.add(LoadI);
		code.add(Remainder);
		code.add(Duplicate);
		code.add(JumpNeg, negremainder);
		code.add(Jump, printremainder);
		
		code.add(Label, negremainder);
		code.add(Negate);
		code.add(Jump, printremainder);
		
		code.add(Label, printremainder);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);
		code.add(PushI, 9999);
		code.add(LoadC);
		code.add(PushD, RunTime.CHARACTER_PRINT_FORMAT);
		code.add(Printf);
		code.add(PushI, 10000);
		code.add(LoadI);
		code.add(Duplicate);
		code.add(JumpNeg, negdenom);
		code.add(Jump, printdenom);
		
		code.add(Label, negdenom);
		code.add(Negate);
		code.add(Jump, printdenom);
		
		code.add(Label, printdenom);
		code.add(PushD, RunTime.INTEGER_PRINT_FORMAT);
		code.add(Printf);
		code.add(Jump, endlabel);
		
		code.add(Label, endlabel);
	}


	private static String printFormat(Type type) {
		assert type instanceof PrimitiveType;
		
		switch((PrimitiveType)type) {
		case INTEGER:	return RunTime.INTEGER_PRINT_FORMAT;
		case RATIONAL:	return RunTime.RATIONAL_PRINT_FORMAT;
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
