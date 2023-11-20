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
package de.hechler.patrick.codesprachen.simple.parser.objects.value;

import java.util.function.UnaryOperator;

import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;

public record CondVal(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue, ErrorContext ctx)
	implements SimpleValue {
	
	public static SimpleValue create(SimpleValue condition, SimpleValue trueValue, SimpleValue falseValue,
		ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx, false);
		SimpleType ct = trueValue.type().commonType(falseValue.type(), ctx);
		trueValue  = CastVal.create(trueValue, ct, ctx);
		falseValue = CastVal.create(trueValue, ct, ctx);
		return new CondVal(condition, trueValue, falseValue, ctx);
	}
	
	@Override
	public SimpleType type() {
		return this.trueValue.type();
	}
	
	@Override
	public void checkAssignable(SimpleType type, ErrorContext ctx) {
		this.trueValue.checkAssignable(type, ctx);
		this.falseValue.checkAssignable(type, ctx);
	}
	
	@Override
	public SimpleValue simplify(UnaryOperator<SimpleValue> op) {
		SimpleValue c = op.apply(this.condition);
		if ( c instanceof ScalarNumericVal snv && snv.value() != 0L || c instanceof AddressOfVal
			|| c instanceof DataVal ) {
			return op.apply(this.trueValue);
		} else if ( c instanceof ScalarNumericVal ) {
			return op.apply(this.falseValue);
		}
		return create(c, op.apply(this.trueValue), op.apply(this.falseValue), this.ctx);
	}
	
	@Override
	public String toString() {
		return "(" + this.condition + " ? " + this.trueValue + " : " + this.falseValue + ")";
	}
	
}
