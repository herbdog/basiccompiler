package semanticAnalyzer.types;

public class TypeVariable implements Type{
	
	Type currtype;
	
	public TypeVariable() {
		currtype = PrimitiveType.ANY;
	}
	
	public void resetFunction() {
		setType(PrimitiveType.ANY);
	}
	
	public void setType(Type t) {
		currtype = t;
	}
	
	public Type getType() {
		return currtype;
	}

	public int getSize() {
		return currtype.getSize();
	}

	public String infoString() {
		return currtype.infoString();
	};

	public boolean match(Type t) {
		if (currtype == PrimitiveType.ANY) {
			setType(t);
			return true;
		}
		else {
			return currtype.match(t);
		}
	}
}
