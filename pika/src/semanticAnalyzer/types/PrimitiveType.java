package semanticAnalyzer.types;


public enum PrimitiveType implements Type {
	BOOLEAN(1),
	INTEGER(4),
	FLOAT(8),
	STRING(4),
	CHAR(1),
	RATIONAL(8),
	ANY(0),
	ERROR(0),			// use as a value when a syntax error has occurred
	NO_TYPE(0, "");		// use as a value when no type has been assigned.
	
	private int sizeInBytes;
	private String infoString;
	
	private PrimitiveType(int size) {
		this.sizeInBytes = size;
		this.infoString = toString();
	}
	private PrimitiveType(int size, String infoString) {
		this.sizeInBytes = size;
		this.infoString = infoString;
	}
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return infoString;
	}
	public Type getType() {
		return this;
	}
	public boolean match(Type t) {
		return this == t;
	}
	@Override
	public void setType(Type type) {
		
	}
}
