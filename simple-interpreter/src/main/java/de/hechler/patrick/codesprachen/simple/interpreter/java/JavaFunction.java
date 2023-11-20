package de.hechler.patrick.codesprachen.simple.interpreter.java;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.interpreter.SimpleInterpreter;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;

public interface JavaFunction {
	
	List<ConstantValue> execute(SimpleInterpreter si, List<ConstantValue> args);
	
	sealed interface ConstantValue {
		
		record DataValue(SimpleType type, byte[] data) implements ConstantValue {}
		
		record ScalarValue(SimpleType type, long value) implements ConstantValue {}
		
		record FP64Value(double value) implements ConstantValue {}
		
		record FP32Value(float value) implements ConstantValue {}
		
	}
	
}
