package semanticAnalyzer.types;


public enum TypeLiteral implements Type {
	INT(1),
	FLOAT(1),
	STRING(1),
	CHAR(1),
	BOOL(1),
	ERROR(0),			// use as a value when a syntax error has occurred
	NO_TYPE(0, "");		// use as a value when no type has been assigned.
	
	private int sizeInBytes;
	private String infoString;
	
	private TypeLiteral(int size) {
		this.sizeInBytes = size;
		this.infoString = toString();
	}
	private TypeLiteral(int size, String infoString) {
		this.sizeInBytes = size;
		this.infoString = infoString;
	}
	public int getSize() {
		return sizeInBytes;
	}
	public String infoString() {
		return infoString;
	}
}
