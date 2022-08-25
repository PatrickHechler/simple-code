package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.util.Map;

import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;

public interface SimplePool {
	
	SimpleStructType getStructure(String name);
	
	SimplePool newSubPool();
	
	void initBlock(SimpleCommandBlock block);
	
	void registerDataValue(SimpleValueDataPointer dataVal);
	
	SimpleValue newNameUseValue(String name);
	
	SimpleFunction getFunction(String name);
	
	SimpleDependency getDependency(String name);
	
	Map <String, SimpleConstant> getConstants();
	
}
