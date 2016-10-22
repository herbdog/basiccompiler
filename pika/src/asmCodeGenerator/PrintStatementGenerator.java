package asmCodeGenerator;

import static asmCodeGenerator.codeStorage.ASMOpcode.Jump;
import static asmCodeGenerator.codeStorage.ASMOpcode.JumpTrue;
import static asmCodeGenerator.codeStorage.ASMOpcode.Label;
import static asmCodeGenerator.codeStorage.ASMOpcode.Printf;
import static asmCodeGenerator.codeStorage.ASMOpcode.PushD;
import parseTree.ParseNode;
import parseTree.nodeTypes.NewlineNode;
import parseTree.nodeTypes.TabNode;
import parseTree.nodeTypes.PrintStatementNode;
import parseTree.nodeTypes.SpaceNode;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import asmCodeGenerator.ASMCodeGenerator.CodeVisitor;
import asmCodeGenerator.codeStorage.ASMCodeFragment;
import asmCodeGenerator.runtime.RunTime;

public class PrintStatementGenerator {
	ASMCodeFragment code;
	ASMCodeGenerator.CodeVisitor visitor;
	
	
	public PrintStatementGenerator(ASMCodeFragment code, CodeVisitor visitor) {
		super();
		this.code = code;
		this.visitor = visitor;
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
		CharSequence Decnode = "DeclarationNode";
		CharSequence Assnode = "AssignNode";
		CharSequence identifier = "IdentifierNode";
		CharSequence isstring = "STRING";
		CharSequence casted = "CAST";
		CharSequence stringnode = "StringConstantNode";
		CharSequence variablename = node.getToken().getLexeme();
		if (node.getType() == PrimitiveType.STRING) {
			if ((node.toString().contains(identifier) && (node.toString().contains(isstring)))) { //printing via identifier
				ParseNode globalnode = node.getParent();
				pathtoroot:
				while (!globalnode.containsBindingOf("ProgramNode (EXEC)")) {
					globalnode = globalnode.getParent();
					for (int i = globalnode.getChildren().size() - 1; i >= 0 ; i--) { //goes from most recent to last, since declaration must come before assignment
						ParseNode localnode = globalnode.child(i);
						if ((localnode.getScope() == node.getScope()) && ((localnode.toString().contains(Decnode)) || (localnode.toString().contains(Assnode))) && (localnode.toString().contains(identifier)) && (localnode.toString().contains(isstring)) && (localnode.toString().contains(variablename))) {
							if (localnode.child(0).getChildren().isEmpty()) { //lowest level of declaration nod
								if ((localnode.child(1).toString().contains(casted)) && (localnode.child(1).child(0).toString().contains(identifier))) {
									variablename = localnode.child(1).child(0).toString();
									continue;
								}
								if ((localnode.child(1).toString().contains(casted)) && (localnode.child(1).child(0).toString().contains(stringnode))) {
									format = localnode.child(1).child(0).getToken().getLexeme().replace("\"","");
									break pathtoroot;
								}
								format = localnode.child(1).getToken().getLexeme().replace("\"","");
								break pathtoroot;
							}
						}	
					}
				}
			}
			else {
				format = node.getToken().getLexeme().replace("\"","");
			}
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
