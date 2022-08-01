package de.hechler.patrick.codesprachen.simple.compile.objects.values;

public class SimpleCharacterValue extends SimpleNumberValue {
	
	public SimpleCharacterValue(char value) {
		super(16, false, 0xFFFF & value);
	}
	
	@Override
	public String toString() {
		return Character.toString((char) value);
	}
	
}
