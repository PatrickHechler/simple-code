//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
	
	void initRegMax(int max);
	
	int regMax();
	
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
