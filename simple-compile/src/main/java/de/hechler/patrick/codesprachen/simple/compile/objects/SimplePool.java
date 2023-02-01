package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleSubPool;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;

public interface SimplePool {
	
	SimpleStructType getStructure(String name);
	
	SimpleSubPool newSubPool();
	
	void registerDataValue(SimpleValueDataPointer dataVal);
	
	SimpleValue newNameUseValue(String name);
	
	SimpleFunction getFunction(String name);
	
	SimpleDependency getDependency(String name);
	
	Map <String, SimpleConstant> getConstants();
	
	void addCmd(SimpleCommand add);
	
	void seal();
	
}
