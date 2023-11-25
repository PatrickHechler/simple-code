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
package de.hechler.patrick.code.simple.parser.objects.value;

import java.util.List;
import java.util.function.UnaryOperator;

import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;

public record DependencyVal(SimpleDependency dep, ErrorContext ctx) implements SimpleValue {
	
	public static SimpleValue create(SimpleDependency dep, ErrorContext ctx) {
		return new DependencyVal(dep, ctx);
	}
	
	@Override
	public SimpleType type() {
		new Throwable("DependencyVal.type() called").printStackTrace();
		return StructType.create(List.of(), StructType.FLAG_NOUSE, ErrorContext.NO_CONTEXT);
	}
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public SimpleValue simplify(@SuppressWarnings("unused") UnaryOperator<SimpleValue> op) { return this; }
	
	@Override
	public String toString() {
		return "(dependency: " + dep.binaryTarget + ")";
	}
	
}
