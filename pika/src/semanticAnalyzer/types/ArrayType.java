package semanticAnalyzer.types;

public class ArrayType implements Type{
	
	Type subtype;
	
	public ArrayType() {
		subtype = new TypeVariable();
	}
	
	public void setType(Type t) {
		subtype = t;
	}
	
	public Type getType() {
		return subtype;
	}

	public int getSize() {
		return subtype.getSize();
	}

	public String infoString() {
		return subtype.infoString();
	};

	public boolean match(Type t) {
		if (!(t instanceof ArrayType)) {
			return false;
		}
		return subtype.match(t.getType());
	}

}
