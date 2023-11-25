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

import java.util.function.UnaryOperator;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;

public record AddressOfVal(SimpleValue a, SimpleType type, ErrorContext ctx) implements SimpleValue {
	
	private static final String NO_RUNTIME_ADDRESS =
		"can not calculate the runtime address of a numeric constant or address of value: ";
	
	public static SimpleValue create(SimpleValue a, ErrorContext ctx) {
		isConst(a, ctx);
		return new AddressOfVal(a, PointerType.create(a.type(), ctx), ctx);
	}
	
	@Override
	public boolean isConstant() {
		return isConst(this.a, this.ctx);
	}
	
	private static boolean isConst(SimpleValue val, ErrorContext ctx) {
		return switch ( val ) {
		case CastVal a -> isConst(a.value(), ctx);
		case DataVal _a -> true; // thats a lie
		case FunctionVal _a -> true; // here comes another lie
		case VariableVal _a -> true; // no more lies, I hope
		case CondVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case BinaryOpVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case AddressOfVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case ScalarNumericVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case FPNumericVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case DependencyVal _a -> throw new CompileError(ctx, NO_RUNTIME_ADDRESS + val);
		case NameVal _a ->
			throw new AssertionError("NameVal is only allowed to be used in BinaryOperator :[NAME] things");
		default -> throw new AssertionError("unknown SimpleValue type: " + val.getClass().getName());
		};
	}
	
	@Override
	public SimpleValue simplify() { return this; }
	
	@Override
	public SimpleValue simplify(@SuppressWarnings("unused") UnaryOperator<SimpleValue> op) { return this; }
	
	@Override
	public String toString() {
		return "( &" + this.a + " )";
	}
	
}
