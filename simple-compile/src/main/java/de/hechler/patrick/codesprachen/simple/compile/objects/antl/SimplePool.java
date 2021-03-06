package de.hechler.patrick.codesprachen.simple.compile.objects.antl;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.values.SimpleValueDataPointer;

public interface SimplePool {
	
	SimpleStructType getStructure(String name);
	
	SimplePool newSubPool();
	
	void initBlock(SimpleCommandBlock block);
	
	void registerDataValue(SimpleValueDataPointer dataVal);
	
	SimpleValue newNameUseValue(String name);
	
	SimpleFunction getFunction(String name);
	
	SimpleDependency getDependency(String name);
	
}
