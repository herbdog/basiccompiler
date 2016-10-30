package semanticAnalyzer.signatures;

import java.util.List;

import semanticAnalyzer.types.TypeLiteral;
import semanticAnalyzer.types.PrimitiveType;
import semanticAnalyzer.types.Type;
import lexicalAnalyzer.Lextant;
import lexicalAnalyzer.Punctuator;
import semanticAnalyzer.signatures.FunctionSignatures;

//immutable
public class FunctionSignature {
	private static final boolean ALL_TYPES_ACCEPT_ERROR_TYPES = true;
	private Type resultType;
	private Type[] paramTypes;
	Object whichVariant;
	
	
	///////////////////////////////////////////////////////////////
	// construction
	
	public FunctionSignature(Object whichVariant, Type ...types) {
		assert(types.length >= 1);
		storeParamTypes(types);
		resultType = types[types.length-1];
		this.whichVariant = whichVariant;
	}
	private void storeParamTypes(Type[] types) {
		paramTypes = new Type[types.length-1];
		for(int i=0; i<types.length-1; i++) {
			paramTypes[i] = types[i];
		}
	}
	
	
	///////////////////////////////////////////////////////////////
	// accessors
	
	public Object getVariant() {
		return whichVariant;
	}
	public Type resultType() {
		return resultType;
	}
	public boolean isNull() {
		return false;
	}
	
	
	///////////////////////////////////////////////////////////////
	// main query

	public boolean accepts(List<Type> types) {
		if(types.size() != paramTypes.length) {
			return false;
		}
		
		for(int i=0; i<paramTypes.length; i++) {
			if(!assignableTo(paramTypes[i], types.get(i))) {
				return false;
			}
		}		
		return true;
	}
	private boolean assignableTo(Type variableType, Type valueType) {
		if(valueType == PrimitiveType.ERROR && ALL_TYPES_ACCEPT_ERROR_TYPES) {
			return true;
		}	
		return variableType.equals(valueType);
	}
	
	// Null object pattern
	private static FunctionSignature neverMatchedSignature = new FunctionSignature(1, PrimitiveType.ERROR) {
		public boolean accepts(List<Type> types) {
			return false;
		}
		public boolean isNull() {
			return true;
		}
	};
	public static FunctionSignature nullInstance() {
		return neverMatchedSignature;
	}
	
	///////////////////////////////////////////////////////////////////
	// Signatures for pika-0 operators
	// this section will probably disappear in pika-1 (in favor of FunctionSignatures)
	

	
	// the switch here is ugly compared to polymorphism.  This should perhaps be a method on Lextant.
	public static FunctionSignature signatureOf(Lextant lextant, List<Type> TypeList) {
		assert(lextant instanceof Punctuator);	
		Punctuator punctuator = (Punctuator)lextant;
		
		switch(punctuator) {
		case ASSIGN:	return FunctionSignatures.signaturesOf(Punctuator.ASSIGN).acceptingSignature(TypeList);
		case ADD:		return FunctionSignatures.signaturesOf(Punctuator.ADD).acceptingSignature(TypeList);
		case SUBTRACT:	return FunctionSignatures.signaturesOf(Punctuator.SUBTRACT).acceptingSignature(TypeList);
		case MULTIPLY:	return FunctionSignatures.signaturesOf(Punctuator.MULTIPLY).acceptingSignature(TypeList);
		case DIVIDE:	return FunctionSignatures.signaturesOf(Punctuator.DIVIDE).acceptingSignature(TypeList);
		case OVER:		return FunctionSignatures.signaturesOf(Punctuator.OVER).acceptingSignature(TypeList);
		case EXPRESS_OVER:	return FunctionSignatures.signaturesOf(Punctuator.EXPRESS_OVER).acceptingSignature(TypeList);
		case RATIONALIZE:	return FunctionSignatures.signaturesOf(Punctuator.RATIONALIZE).acceptingSignature(TypeList);
		case GREATER:	return FunctionSignatures.signaturesOf(Punctuator.GREATER).acceptingSignature(TypeList);
		case GREATER_EQUAL:	return FunctionSignatures.signaturesOf(Punctuator.GREATER_EQUAL).acceptingSignature(TypeList);
		case LESS:		return FunctionSignatures.signaturesOf(Punctuator.LESS).acceptingSignature(TypeList);
		case LESS_EQUAL:	return FunctionSignatures.signaturesOf(Punctuator.LESS_EQUAL).acceptingSignature(TypeList);
		case EQUAL:		return FunctionSignatures.signaturesOf(Punctuator.EQUAL).acceptingSignature(TypeList);
		case NOTEQUAL:	return FunctionSignatures.signaturesOf(Punctuator.NOTEQUAL).acceptingSignature(TypeList);
		case CAST:		return FunctionSignatures.signaturesOf(Punctuator.CAST).acceptingSignature(TypeList);
		case AND:		return FunctionSignatures.signaturesOf(Punctuator.AND).acceptingSignature(TypeList);
		case OR:		return FunctionSignatures.signaturesOf(Punctuator.OR).acceptingSignature(TypeList);
		case NOT:		return FunctionSignatures.signaturesOf(Punctuator.NOT).acceptingSignature(TypeList);

		default:
			return neverMatchedSignature;
		}
	}

}