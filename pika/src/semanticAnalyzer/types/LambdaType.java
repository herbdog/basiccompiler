package semanticAnalyzer.types;

import java.util.LinkedList;

public class LambdaType implements Type{
	
	LinkedList<Type> typelist;
	
	public LambdaType() {
		typelist = new LinkedList<Type>();
	}
	
	public void setType(Type t) {		//iteratively add them, the last in this list should be the return type
		TypeVariable tvar = new TypeVariable();
		tvar.setType(t);
		typelist.add(t);		//should be list of type literals but as T's
	}
	
	public Type getType() {				//returns the return type of the lambda
		return typelist.get(typelist.size()-1);		
	}

	public int getSize() {
		return typelist.get(typelist.size()-1).getSize();		//return type size
	}

	public String infoString() {
		return typelist.get(typelist.size()-1).infoString();
	};

	public boolean match(Type t) {
		return t instanceof LambdaType;
	}
}
