package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleSubPool;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;

public interface SimplePool {
	
	default SimpleStructType getStructure(String first, String second) {
		if (second == null) {
			return getStructure(first);
		} else {
			SimpleExportable se = getDependency(first).get(second);
			if (!(se instanceof SimpleStructType s)) {
				throw new IllegalArgumentException("the dependency " + first + " has no structure " + second);
			}
			return s;
		}
	}
	
	SimpleStructType getStructure(String name);
	
	SimpleSubPool newSubPool();
	
	void registerDataValue(SimpleValueDataPointer dataVal);
	
	SimpleValue newNameUseValue(String name);
	
	SimpleFunction getFunction(String name);
	
	SimpleDependency getDependency(String name);
	
	Map<String, SimpleConstant> getConstants();
	
	Map<String, SimpleStructType> getStructures();
	
	Map<String, SimpleVariable> getVariables();
	
	void addCmd(SimpleCommand add);
	
	void seal();
	
	default SimpleType getFuncType(String first, String second) {
		if (second == null) {
			return getFunction(first).type;
		} else {
			SimpleExportable se = getDependency(first).get(second);
			if (!(se instanceof SimpleFunction f)) { throw new IllegalArgumentException("the dependency " + first + " has no function " + second); }
			return f.type;
		}
	}
	
}
