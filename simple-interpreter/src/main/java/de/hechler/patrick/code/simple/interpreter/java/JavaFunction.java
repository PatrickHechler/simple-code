package de.hechler.patrick.code.simple.interpreter.java;

import java.util.List;

import de.hechler.patrick.code.simple.interpreter.SimpleInterpreter;

public interface JavaFunction {
	
	List<ConstantValue> execute(SimpleInterpreter si, List<ConstantValue> args);
	
}
