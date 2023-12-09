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
package de.hechler.patrick.code.simple.parser.objects.simplefile.scope;

import java.util.List;
import java.util.Set;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.code.simple.parser.objects.value.VariableVal;

public interface SimpleScope {
	
	Set<String> availableNames();
	
	SimpleValue nameValueOrNull(String name, ErrorContext ctx);
	
	default SimpleValue nameValueOrErr(String name, ErrorContext ctx) throws CompileError {
		SimpleValue val = nameValueOrNull(name, ctx);
		if ( val != null ) return val;
		throw new CompileError(ctx, "nothing with the name '" + name + "' could be found");
	}
	
	Object nameTypeOrDepOrFuncOrNull(String typedefName);
	
	static SimpleScope newFuncScope(SimpleFile sf, FuncType ft, ErrorContext ctx) {
		checkDuplicates(sf, ctx, ft.argMembers());
		checkDuplicates(sf, ctx, ft.resMembers());
		assert ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) != 0; // NOSONAR
		return new SimpleScope() {
			
			@Override
			public Set<String> availableNames() {
				Set<String> result = sf.availableNames();
				ft.argMembers().stream().map(SimpleVariable::name).forEach(result::add);
				ft.resMembers().stream().map(SimpleVariable::name).forEach(result::add);
				return result;
			}
			
			@Override
			public SimpleValue nameValueOrNull(String name, ErrorContext ctx) {
				SimpleVariable sv = ft.memberOrNull(name, ctx, true);
				if ( sv != null ) return VariableVal.create(sv, ctx);
				return sf.nameValueOrNull(name, ctx);
			}
			
			@Override
			public Object nameTypeOrDepOrFuncOrNull(String typedefName) {
				return sf.nameTypeOrDepOrFuncOrNull(typedefName);
			}
			
		};
	}
	
	static void checkDuplicates(SimpleScope parent, ErrorContext ctx, List<SimpleVariable> members) {
		for (SimpleVariable arg : members) {
			if ( parent.nameValueOrNull(arg.name(), ctx) != null ) {
				throw new CompileError(ctx, "there is alredy a value associated with the name " + arg.name());
			}
		}
	}
	
}
