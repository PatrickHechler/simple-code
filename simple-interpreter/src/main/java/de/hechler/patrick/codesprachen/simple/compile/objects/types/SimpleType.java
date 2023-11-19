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
package de.hechler.patrick.codesprachen.simple.compile.objects.types;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;

public interface SimpleType {
	
	long size();
	
	int align();
	
	SimpleType commonType(SimpleType type, ErrorContext ctx) throws CompileError;
	
	void checkCastable(SimpleType type, ErrorContext ctx, boolean explicit) throws CompileError;

	String toStringSingleLine();

	String toString(String indention);
	
	static < T > T castErrExplicit(SimpleType from, SimpleType to, ErrorContext ctx) {
		throw new CompileError(ctx, "can't cast from " + from.toStringSingleLine() + " to " + toToSLS(to));
	}
	
	static < T > T castErrImplicit(SimpleType from, SimpleType to, ErrorContext ctx) {
		throw new CompileError(ctx, "can't implicitly cast from " + from.toStringSingleLine() + " to " + toToSLS(to));
	}
	
	static String toToSLS(SimpleType to) { return to == null ? "a native type" : to.toStringSingleLine(); }
	
	static < T > T castErrExplicit(SimpleType from, String to, ErrorContext ctx) {
		throw new CompileError(ctx, "can't cast from " + from.toStringSingleLine() + " to " + to);
	}
	
	static < T > T castErrImplicit(SimpleType from, String to, ErrorContext ctx) {
		throw new CompileError(ctx, "can't implicitly cast from " + from.toStringSingleLine() + " to " + to);
	}
	
}
