package semanticAnalyzer.signatures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import semanticAnalyzer.types.*;
import lexicalAnalyzer.Punctuator;
import asmCodeGenerator.codeStorage.ASMOpcode;

public class FunctionSignatures extends ArrayList<FunctionSignature> {
	private static final long serialVersionUID = -4907792488209670697L;
	private static Map<Object, FunctionSignatures> signaturesForKey = new HashMap<Object, FunctionSignatures>();
	
	Object key;
	
	public FunctionSignatures(Object key, FunctionSignature ...functionSignatures) {
		this.key = key;
		for(FunctionSignature functionSignature: functionSignatures) {
			add(functionSignature);
		}
		signaturesForKey.put(key, this);
	}
	
	public Object getKey() {
		return key;
	}
	public boolean hasKey(Object key) {
		return this.key.equals(key);
	}
	
	public FunctionSignature acceptingSignature(List<Type> types) {
		for(FunctionSignature functionSignature: this) {
			if(functionSignature.accepts(types)) {
				return functionSignature;
			}
		}
		return FunctionSignature.nullInstance();
	}
	public boolean accepts(List<Type> types) {
		return !acceptingSignature(types).isNull();
	}

	
	/////////////////////////////////////////////////////////////////////////////////
	// access to FunctionSignatures by key object.
	
	public static FunctionSignatures nullSignatures = new FunctionSignatures(0, FunctionSignature.nullInstance());

	public static FunctionSignatures signaturesOf(Object key) {
		if(signaturesForKey.containsKey(key)) {
			return signaturesForKey.get(key);
		}
		return nullSignatures;
	}
	public static FunctionSignature signature(Object key, List<Type> types) {
		FunctionSignatures signatures = FunctionSignatures.signaturesOf(key);
		return signatures.acceptingSignature(types);
	}

	
	
	/////////////////////////////////////////////////////////////////////////////////
	// Put the signatures for operators in the following static block.
	
	static {
		// here's one example to get you started with FunctionSignatures: the signatures for addition.		
		// for this to work, you should statically import PrimitiveType.*

		new FunctionSignatures(Punctuator.ADD,
		    new FunctionSignature(ASMOpcode.Add, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
		    new FunctionSignature(ASMOpcode.FAdd, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
		    new FunctionSignature(ASMOpcode.Nop, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		
		new FunctionSignatures(Punctuator.SUBTRACT,
			new FunctionSignature(ASMOpcode.Subtract, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.FSubtract, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			 new FunctionSignature(ASMOpcode.Nop, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		
		new FunctionSignatures(Punctuator.MULTIPLY,
			new FunctionSignature(ASMOpcode.Multiply, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.FMultiply, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			 new FunctionSignature(ASMOpcode.Nop, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		
		new FunctionSignatures(Punctuator.DIVIDE,
			new FunctionSignature(ASMOpcode.Divide, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.FDivide, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.FLOAT),
			 new FunctionSignature(ASMOpcode.Nop, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL, PrimitiveType.RATIONAL)
		);
		
		new FunctionSignatures(Punctuator.OVER,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.RATIONAL)
		);
		
		new FunctionSignatures(Punctuator.EXPRESS_OVER,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.INTEGER)
		);
		
		new FunctionSignatures(Punctuator.RATIONALIZE,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.RATIONAL, PrimitiveType.INTEGER, PrimitiveType.RATIONAL),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.INTEGER, PrimitiveType.RATIONAL)
		);
		
		new FunctionSignatures(Punctuator.GREATER,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN)
		);
		
		new FunctionSignatures(Punctuator.GREATER_EQUAL,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.LESS,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.LESS_EQUAL,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.EQUAL,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.NOTEQUAL,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, PrimitiveType.INTEGER, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, PrimitiveType.FLOAT, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, PrimitiveType.CHAR, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.CAST,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, TypeLiteral.CHAR, PrimitiveType.CHAR),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, TypeLiteral.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, TypeLiteral.BOOL, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, TypeLiteral.INT, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.INTEGER, TypeLiteral.RAT, PrimitiveType.RATIONAL),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, TypeLiteral.INT, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, TypeLiteral.FLOAT, PrimitiveType.FLOAT),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.FLOAT, TypeLiteral.RAT, PrimitiveType.RATIONAL),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, TypeLiteral.INT, PrimitiveType.INTEGER),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, TypeLiteral.BOOL, PrimitiveType.BOOLEAN),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, TypeLiteral.CHAR, PrimitiveType.CHAR),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.CHAR, TypeLiteral.RAT, PrimitiveType.RATIONAL),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.STRING, TypeLiteral.STRING, PrimitiveType.STRING),
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, TypeLiteral.BOOL, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.AND,
			new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.OR,
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
		new FunctionSignatures(Punctuator.NOT,
				new FunctionSignature(ASMOpcode.Nop, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN)
		);
	}

}
