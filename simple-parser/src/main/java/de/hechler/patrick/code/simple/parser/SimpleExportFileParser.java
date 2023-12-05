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
package de.hechler.patrick.code.simple.parser;

import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ARR_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ARR_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BIT_AND;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BIT_NOT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BIT_OR;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BIT_XOR;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BLOCK_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BLOCK_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BOOL_AND;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BOOL_NOT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BOOL_OR;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BYTE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CHARACTER;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.COLON;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.COMMA;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CONST;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.DEP;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.DIAMOND;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.DIV;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.DWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.EOF;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.EQ;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.EXP;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FPDWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FPNUM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FSTRUCT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FUNC;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.GE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.GT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LARROW;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.MINUS;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.MOD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NOPAD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NOT_EQ;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NUM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NUMBER;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.PLUS;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.QUESTION;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SEMI;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SHIFT_LEFT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SHIFT_RIGTH;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SMALL_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SMALL_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.STAR;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.STRING;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.STRUCT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.TYPEDEF;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UBYTE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UDWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UNUM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.WORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.name;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleExportable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleTypedef;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;
import de.hechler.patrick.code.simple.parser.objects.value.BinaryOpVal;
import de.hechler.patrick.code.simple.parser.objects.value.BinaryOpVal.BinaryOp;
import de.hechler.patrick.code.simple.parser.objects.value.CastVal;
import de.hechler.patrick.code.simple.parser.objects.value.CondVal;
import de.hechler.patrick.code.simple.parser.objects.value.DataVal;
import de.hechler.patrick.code.simple.parser.objects.value.NameVal;
import de.hechler.patrick.code.simple.parser.objects.value.ScalarNumericVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.code.simple.parser.objects.value.UnaryOpVal;
import de.hechler.patrick.code.simple.parser.objects.value.UnaryOpVal.UnaryOp;

public class SimpleExportFileParser {
	
	public static final int[] EMPTY_IARR = new int[0];
	
	public static final int STATE_GLOBAL               = 0;
	public static final int STATE_DEPENDENCY           = 1;
	public static final int STATE_VARIALBE             = 2;
	public static final int STATE_TYPEDEF              = 3;
	public static final int STATE_FUNCTION             = 4;
	public static final int STATE_VAL_DIRECT           = 5;
	public static final int STATE_VAL_POSTFIX          = 6;
	public static final int STATE_VAL_UNARY            = 7;
	public static final int STATE_VAL_CAST             = 8;
	public static final int STATE_VAL_MUL              = 9;
	public static final int STATE_VAL_ADD              = 10;
	public static final int STATE_VAL_SHIFT            = 11;
	public static final int STATE_VAL_REL              = 12;
	public static final int STATE_VAL_EQ               = 13;
	public static final int STATE_VAL_AND              = 14;
	public static final int STATE_VAL_XOR              = 15;
	public static final int STATE_VAL_OR               = 16;
	public static final int STATE_VAL_LAND             = 17;
	public static final int STATE_VAL_LOR              = 18;
	public static final int STATE_VAL_COND             = 19;
	public static final int STATE_VAL                  = 20;
	public static final int STATE_NAMED_TYPE_LIST      = 21;
	public static final int STATE_TYPE                 = 22;
	public static final int STATE_TYPE_TYPEDEFED_TYPE  = 23;
	public static final int STATE_TYPE_STRUCT          = 24;
	public static final int STATE_TYPE_FUNC_STRUCT     = 25;
	public static final int STATE_TYPE_FUNC_STRUCT_REF = 26;
	public static final int STATE_TYPE_FUNC_ADDR_REF   = 27;
	public static final int STATE_TYPE_FUNC_ADDR       = 28;
	public static final int STATE_TYPE_POSTFIX         = 29;
	
	protected final SimpleTokenStream                          in;
	protected final BiFunction<String,String,SimpleDependency> dep;
	
	public SimpleExportFileParser(InputStream in, String file, BiFunction<String,String,SimpleDependency> dep) {
		this(new SimpleTokenStream(in, file), dep);
	}
	
	public SimpleExportFileParser(SimpleTokenStream in, BiFunction<String,String,SimpleDependency> dep) {
		this.in = in;
		this.dep = dep;
	}
	
	public void parse(SimpleFile sf) {
		final Object enter = enterState(STATE_GLOBAL);
		while ( true ) {
			switch ( this.in.tok() ) {
			case DEP:
				parseDependency(sf);
				break;
			case TYPEDEF:
				parseTypedef(sf);
				break;
			case FUNC:
				parseFunction(sf);
				break;
			case EOF:
				exitState(STATE_GLOBAL, enter, null);
				return;
			default:
				parseSFScopeVariable(sf);
				break;
			}
		}
	}
	
	protected void parseDependency(SimpleFile sf) {
		final Object enter = enterState(STATE_DEPENDENCY);
		this.in.consume();
		expectToken(NAME, "expected to get `[NAME] [STRING] ;´ after `dep´");
		String name;
		try {
			name = this.in.consumeDynTokSpecialText();
		} catch ( @SuppressWarnings("unused") AssertionError ae ) {
			// assertion enabled (for token stream)
			// expectToken() failed
			// handleError() overwritten & returned normally
			// if parsing is only allowed to fail if handleError fails
			name = null;
		}
		expectToken(STRING, "expected to get `[STRING] ;´ after `dep [NAME]´");
		String path;
		try {
			path = this.in.consumeDynTokSpecialText();
		} catch ( @SuppressWarnings("unused") AssertionError ae ) {
			path = "invalid path\0";
		}
		consumeToken(SEMI, "expected to get `;´ after `dep [NAME] [STRING]´");
		SimpleDependency dependency = this.dep.apply(path, null);
		if ( dependency == null ) {
			this.in.handleError(this.in.ctx(), "could not find the dependency \"" + path + "\"");
			dependency = new SimpleFile(path, null);
		}
		try {
			sf.dependency(dependency, name, this.in.ctx());
		} catch ( NullPointerException npe ) {
			if ( name != null ) throw npe;
		}
		exitState(STATE_DEPENDENCY, enter, name);
	}
	
	protected SimpleVariable parseSFScopeVariable(SimpleFile sf) {
		SimpleVariable sv = parseAnyScopeVariable(sf, SimpleVariable.FLAG_GLOBAL);
		if ( ( sv.flags() & SimpleVariable.FLAG_CONSTANT ) == 0 && sv.initialValue() != null ) {
			this.in.handleError(this.in.ctx(), "a non constant value must not have a initial value set");
		}
		return sv;
	}
	
	protected SimpleVariable parseAnyScopeVariable(SimpleScope scope, int flags) {
		final Object enter = enterState(STATE_VARIALBE);
		switch ( this.in.tok() ) {
		case CONST:
			flags |= SimpleVariable.FLAG_CONSTANT;
			this.in.consume();
			if ( this.in.tok() != EXP ) break;
			//$FALL-THROUGH$
		case EXP:
			flags |= SimpleExportable.FLAG_EXPORT;
			this.in.consume();
			//$FALL-THROUGH$
		default:
		}
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after ´(const)? (exp)? [TYPE]´");
		String name;
		try {
			name = this.in.consumeDynTokSpecialText();
		} catch ( @SuppressWarnings("unused") AssertionError ae ) {
			name = null;
		}
		SimpleValue initialValue = null;
		if ( this.in.tok() == LARROW ) {
			this.in.consume();
			initialValue = parseValue(scope);
		} else if ( ( flags & SimpleVariable.FLAG_CONSTANT ) != 0 ) {
			this.in.handleError(this.in.ctx(), "a constant value must have a initial value set");
		}
		SimpleVariable result;
		try {
			result = new SimpleVariable(type, name, initialValue, flags);
		} catch ( NullPointerException npe ) {
			if ( name != null ) throw npe;
			result = new SimpleVariable(StructType.create(List.of(), StructType.FLAG_NOUSE, ErrorContext.NO_CONTEXT), "invalid\0",
				initialValue, flags);
		}
		exitState(STATE_VARIALBE, enter, result);
		return result;
	}
	
	protected SimpleValue parseValue(SimpleScope scope) {
		return parseValueCondExp(scope, 0, null, null);
	}
	
	protected SimpleValue parseValue(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL);
		else enter = enterValues == null ? null : enterValues[COND_MAGIC + 1 - magic];
		SimpleValue result = parseValueCondExp(scope, magic, mvalue, enterValues);
		exitState(STATE_VAL, enter, result);
		return result;
	}
	
	protected static final int COND_MAGIC = 15;
	
	protected SimpleValue parseValueCondExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == COND_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_COND);
		else enter = enterValues == null ? null : enterValues[COND_MAGIC - magic];
		SimpleValue a = parseValueLOrExp(scope, magic, mvalue, enterValues);
		if ( this.in.tok() == QUESTION ) {
			this.in.consume();
			SimpleValue b = parseValue(scope);
			consumeToken(BOOL_NOT, "expected `! [COND_EXP]´ after `[LOR_EXP] ? [VALUE]´");
			SimpleValue c = parseValueCondExp(scope, 0, null, null);
			try {
				a = CondVal.create(a, b, c, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_VAL_COND, enter, a);
		return a;
	}
	
	protected static final int LOR_MAGIC = 14;
	
	protected SimpleValue parseValueLOrExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == LOR_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_LOR);
		else enter = enterValues == null ? null : enterValues[LOR_MAGIC - magic];
		SimpleValue a = parseValueLAndExp(scope, magic, mvalue, enterValues);
		while ( this.in.tok() == BOOL_OR ) {
			this.in.consume();
			SimpleValue b = parseValueLAndExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, BinaryOp.BOOL_OR, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_VAL_LOR, enter, a);
		return a;
	}
	
	protected static final int LAND_MAGIC = 13;
	
	protected SimpleValue parseValueLAndExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == LAND_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_LAND);
		else enter = enterValues == null ? null : enterValues[LAND_MAGIC - magic];
		SimpleValue a = parseValueOrExp(scope, magic, mvalue, enterValues);
		while ( this.in.tok() == BOOL_AND ) {
			this.in.consume();
			SimpleValue b = parseValueOrExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, BinaryOp.BOOL_AND, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_VAL_LAND, enter, a);
		return a;
	}
	
	protected static final int OR_MAGIC = 12;
	
	protected SimpleValue parseValueOrExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == OR_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_OR);
		else enter = enterValues == null ? null : enterValues[OR_MAGIC - magic];
		SimpleValue a = parseValueXOrExp(scope, magic, mvalue, enterValues);
		while ( this.in.tok() == BIT_OR ) {
			this.in.consume();
			SimpleValue b = parseValueXOrExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, BinaryOp.BIT_OR, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_VAL_OR, enter, a);
		return a;
	}
	
	protected static final int XOR_MAGIC = 11;
	
	protected SimpleValue parseValueXOrExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == XOR_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_XOR);
		else enter = enterValues == null ? null : enterValues[XOR_MAGIC - magic];
		SimpleValue a = parseValueAndExp(scope, magic, mvalue, enterValues);
		while ( this.in.tok() == BIT_XOR ) {
			this.in.consume();
			SimpleValue b = parseValueAndExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, BinaryOp.BIT_XOR, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_VAL_XOR, enter, a);
		return a;
	}
	
	protected static final int AND_MAGIC = 10;
	
	protected SimpleValue parseValueAndExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == AND_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_AND);
		else enter = enterValues == null ? null : enterValues[AND_MAGIC - magic];
		SimpleValue a = parseValueEqExp(scope, magic, mvalue, enterValues);
		while ( this.in.tok() == BIT_AND ) {
			this.in.consume();
			SimpleValue b = parseValueEqExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, BinaryOp.BIT_AND, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_VAL_AND, enter, a);
		return a;
	}
	
	protected static final int EQ_MAGIC = 9;
	
	protected SimpleValue parseValueEqExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == EQ_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_EQ);
		else enter = enterValues == null ? null : enterValues[EQ_MAGIC - magic];
		SimpleValue a = parseValueRelExp(scope, magic, mvalue, enterValues);
		int t = this.in.tok();
		while ( t == EQ || t == NOT_EQ ) {
			this.in.consume();
			SimpleValue b = parseValueRelExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, t == EQ ? BinaryOp.CMP_EQ : BinaryOp.CMP_NEQ, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
			t = this.in.tok();
		}
		exitState(STATE_VAL_EQ, enter, a);
		return a;
	}
	
	protected static final int REL_MAGIC = 8;
	
	protected SimpleValue parseValueRelExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == REL_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_REL);
		else enter = enterValues == null ? null : enterValues[REL_MAGIC - magic];
		SimpleValue a = parseValueShiftExp(scope, magic, mvalue, enterValues);
		int t = this.in.tok();
		while ( t == GT || t == GE || t == LE || t == LT ) {
			this.in.consume();
			SimpleValue b = parseValueShiftExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, switch ( t ) {
				case GT -> BinaryOp.CMP_GT;
				case GE -> BinaryOp.CMP_GE;
				case LE -> BinaryOp.CMP_LE;
				case LT -> BinaryOp.CMP_LT;
				default -> throw new AssertionError();
				}, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
			t = this.in.tok();
		}
		exitState(STATE_VAL_REL, enter, a);
		return a;
	}
	
	protected static final int SHIFT_MAGIC = 7;
	
	protected SimpleValue parseValueShiftExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == SHIFT_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_SHIFT);
		else enter = enterValues == null ? null : enterValues[SHIFT_MAGIC - magic];
		SimpleValue a = parseValueAddExp(scope, magic, mvalue, enterValues);
		int t = this.in.tok();
		while ( t == SHIFT_LEFT || t == SHIFT_RIGTH ) {
			this.in.consume();
			SimpleValue b = parseValueAddExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, switch ( t ) {
				case SHIFT_LEFT -> BinaryOp.SHIFT_LEFT;
				case SHIFT_RIGTH -> BinaryOp.SHIFT_RIGTH;
				default -> throw new AssertionError();
				}, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
			t = this.in.tok();
		}
		exitState(STATE_VAL_SHIFT, enter, a);
		return a;
	}
	
	protected static final int ADD_MAGIC = 6;
	
	protected SimpleValue parseValueAddExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == ADD_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_ADD);
		else enter = enterValues == null ? null : enterValues[ADD_MAGIC - magic];
		SimpleValue a = parseValueMulExp(scope, magic, mvalue, enterValues);
		int t = this.in.tok();
		while ( t == PLUS || t == MINUS ) {
			this.in.consume();
			SimpleValue b = parseValueMulExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, t == PLUS ? BinaryOp.MATH_ADD : BinaryOp.MATH_SUB, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
			t = this.in.tok();
		}
		exitState(STATE_VAL_ADD, enter, a);
		return a;
	}
	
	protected static final int MUL_MAGIC = 5;
	
	protected SimpleValue parseValueMulExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == MUL_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_MUL);
		else enter = enterValues == null ? null : enterValues[MUL_MAGIC - magic];
		SimpleValue a = parseValueCastExp(scope, magic, mvalue, enterValues);
		int t = this.in.tok();
		while ( t == STAR || t == DIV || t == MOD ) {
			this.in.consume();
			SimpleValue b = parseValueCastExp(scope, 0, null, null);
			try {
				a = BinaryOpVal.create(a, switch ( t ) {
				case STAR -> BinaryOp.MATH_MUL;
				case DIV -> BinaryOp.MATH_DIV;
				case MOD -> BinaryOp.MATH_MOD;
				default -> throw new AssertionError();
				}, b, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
			t = this.in.tok();
		}
		exitState(STATE_VAL_MUL, enter, a);
		return a;
	}
	
	protected static final int CAST_MAGIC = 4;
	
	protected SimpleValue parseValueCastExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == CAST_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_CAST);
		else {
			enter = enterValues == null ? null : enterValues[CAST_MAGIC - magic];
			SimpleValue res = parseValueUnaryExp(scope, magic, mvalue, enterValues);
			exitState(STATE_VAL_CAST, enter, res);
			return res;
		}
		if ( this.in.tok() == SMALL_OPEN ) {
			this.in.consume();
			SimpleType t = parseType(scope);
			consumeToken(SMALL_CLOSE, "expected `> [UNARY_EXP]´ after `< [TYPE]´");
			SimpleValue a = parseValueUnaryExp(scope, 0, null, null);
			try {
				a = CastVal.create(a, t, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
			}
			exitState(STATE_VAL_CAST, enter, a);
			return a;
		}
		SimpleValue res = parseValueUnaryExp(scope, 0, null, null);
		exitState(STATE_VAL_CAST, enter, res);
		return res;
	}
	
	protected static final int UNARY_MAGIC = 3;
	
	protected SimpleValue parseValueUnaryExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == UNARY_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_UNARY);
		else {
			enter = enterValues == null ? null : enterValues[UNARY_MAGIC - magic];
			SimpleValue res = parseValuePostfixExp(scope, magic, mvalue, enterValues);
			exitState(STATE_VAL_UNARY, enter, res);
			return res;
		}
		UnaryOp op;
		switch ( this.in.tok() ) {
		case PLUS -> op = UnaryOp.PLUS;
		case MINUS -> op = UnaryOp.MINUS;
		case BIT_AND -> op = UnaryOp.ADDRESS_OF;
		case BIT_NOT -> op = UnaryOp.BIT_NOT;
		case BOOL_NOT -> op = UnaryOp.BOOL_NOT;
		default -> {
			SimpleValue res = parseValuePostfixExp(scope, 0, null, null);
			exitState(STATE_VAL_UNARY, enter, res);
			return res;
		}
		}
		this.in.consume();
		SimpleValue a = parseValuePostfixExp(scope, 0, null, null);
		try {
			a = UnaryOpVal.create(op, a, this.in.ctx());
		} catch ( CompileError ce ) {
			this.in.handleError(ce);
		}
		exitState(STATE_VAL_UNARY, enter, a);
		return a;
	}
	
	protected static final int POSTFIX_MAGIC = 2;
	
	protected SimpleValue parseValuePostfixExp(SimpleScope scope, int magic, SimpleValue mvalue, Object[] enterValues) {
		if ( magic == POSTFIX_MAGIC ) return mvalue;
		final Object enter;
		if ( magic == 0 ) enter = enterState(STATE_VAL_POSTFIX);
		else enter = enterValues == null ? null : enterValues[POSTFIX_MAGIC - magic];
		SimpleValue a = parseValueDirectExp(scope, magic, mvalue);
		while ( true ) {
			switch ( this.in.tok() ) {
			case DIAMOND -> {
				this.in.consume();
				try {
					a = UnaryOpVal.create(UnaryOp.DEREF_PNTR, a, this.in.ctx());
				} catch ( CompileError ce ) {
					this.in.handleError(ce);
				}
			}
			case ARR_OPEN -> {
				this.in.consume();
				SimpleValue b = parseValue(scope);
				consumeToken(ARR_CLOSE, "expected `\\]´ after `\\[ [VALUE]´");
				try {
					a = BinaryOpVal.create(a, BinaryOp.ARR_PNTR_INDEX, b, this.in.ctx());
				} catch ( CompileError ce ) {
					this.in.handleError(ce);
				}
			}
			case COLON -> {
				this.in.consume();
				expectToken(NAME, "expected `[NAME]´ after `:´");
				String name;
				try {
					name = this.in.consumeDynTokSpecialText();
				} catch ( @SuppressWarnings("unused") AssertionError ae ) {
					name = name(this.in.consumeTok());
				}
				NameVal b = new NameVal(name);
				try {
					a = BinaryOpVal.create(a, BinaryOp.DEREF_BY_NAME, b, this.in.ctx());
				} catch ( CompileError ce ) {
					this.in.handleError(ce);
				}
			}
			default -> {
				exitState(STATE_VAL_POSTFIX, enter, a);
				return a;
			}
			}
		}
	}
	
	protected static final int DIRECT_MAGIC = 1;
	
	protected SimpleValue parseValueDirectExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == DIRECT_MAGIC ) return mvalue;
		final Object enter = enterState(STATE_VAL_DIRECT);
		SimpleValue res = switch ( this.in.tok() ) {
		case STRING -> {
			String str = this.in.consumeDynTokSpecialText();
			if ( this.in.tok() != STRING ) {
				yield DataVal.createString(str, this.in.ctx());
			}
			StringBuilder sb = new StringBuilder(str);
			do {
				str = this.in.consumeDynTokSpecialText();
				sb.append(str);
			} while ( this.in.tok() == STRING ); // no compile error possible
			yield DataVal.createString(sb, this.in.ctx());
		}
		case CHARACTER -> {
			char value = this.in.consumeDynTokSpecialText().charAt(0);
			try {
				yield ScalarNumericVal.create(NativeType.UBYTE, value, this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
				yield ScalarNumericVal.createAllowTruncate(NativeType.UBYTE, value, this.in.ctx());
			}
		}
		case NUMBER -> parseNumber();
		case NAME -> {
			try {
				yield scope.nameValueOrErr(this.in.consumeDynTokSpecialText(), this.in.ctx());
			} catch ( CompileError ce ) {
				this.in.handleError(ce);
				yield new DataVal(new byte[0], StructType.create(List.of(), StructType.FLAG_NOUSE, this.in.ctx()), this.in.ctx());
			}
		}
		case SMALL_OPEN -> {
			this.in.consume();
			SimpleValue a = parseValue(scope);
			consumeToken(SMALL_CLOSE, "expected `)´ after `( [VALUE]´");
			yield a;
		}
		default -> {
			ErrorContext ctx = this.in.ctx();
			this.in.handleError(ctx, List.of(name(STRING), name(CHARACTER), name(NUMBER), name(NAME), name(SMALL_OPEN)));
			this.in.consume();
			yield new DataVal(new byte[0], StructType.create(List.of(), StructType.FLAG_NOUSE, ctx), ctx);
		}
		};
		exitState(STATE_VAL_DIRECT, enter, res);
		return res;
	}
	
	private SimpleValue parseNumber() throws AssertionError {
		ErrorContext ctx = this.in.ctx();
		String value = this.in.consumeDynTokSpecialText();
		boolean sign = false;
		int off = 4;
		int sys;
		switch ( value.charAt(0) ) {
		case 'H' -> sys = 16;
		case 'D' -> sys = 10;
		case 'O' -> sys = 8;
		case 'B' -> sys = 2;
		case 'N' -> {
			sign = true;
			switch ( value.charAt(1) ) {
			case 'H' -> sys = 16;
			case 'D' -> sys = 10;
			case 'O' -> sys = 8;
			case 'B' -> sys = 2;
			default -> throw new AssertionError();
			}
		}
		case '-' -> {
			sign = true;
			sys = 10;
			off = 1;
		}
		default -> {
			sys = 10;
			off = 0;
		}
		}
		int bits = 64;
		int len = value.length() - 1;
		switch ( value.charAt(len) ) {
		case 'B', 'b' -> {
			if ( sys < 12 ) bits = 8;
		}
		case 'H', 'h' -> bits = 8;
		case 'W', 'w' -> bits = 16;
		case 'D', 'd' -> bits = 32;
		case 'N', 'n', 'Q', 'q' -> bits = 64;
		case 'S', 's' -> sign = true;
		case 'U', 'u' -> {
			if ( sign ) {
				this.in.handleError(ctx, "a negative value can not be unsigned!");
			}
		}
		default -> len++;
		}
		if ( len != value.length() && len > 0 ) {
			switch ( value.charAt(--len) ) {
			case 'S', 's' -> sign = true;
			case 'U', 'u' -> {
				if ( sign ) {
					this.in.handleError(ctx, "a negative value can not be unsigned!");
				}
			}
			default -> len++;
			}
		}
		if ( sign ) {
			switch ( bits ) {
			case 64:
				return ScalarNumericVal.create(NativeType.NUM, Long.parseLong(value.substring(off, len), sys), ctx);
			case 32:
				return ScalarNumericVal.create(NativeType.DWORD, Integer.parseInt(value.substring(off, len), sys), ctx);
			case 16:
				return ScalarNumericVal.create(NativeType.WORD, Short.parseShort(value.substring(off, len), sys), ctx);
			case 8:
				return ScalarNumericVal.create(NativeType.BYTE, Byte.parseByte(value.substring(off, len), sys), ctx);
			default:
				throw new AssertionError();
			}
		}
		switch ( bits ) {
		case 64:
			return ScalarNumericVal.create(NativeType.UNUM, Long.parseUnsignedLong(value.substring(off, len), sys), ctx);
		case 32:
			return ScalarNumericVal.create(NativeType.UDWORD, Integer.parseUnsignedInt(value.substring(off, len), sys), ctx);
		case 16:
			return ScalarNumericVal.create(NativeType.UWORD, Integer.parseUnsignedInt(value.substring(off, len), sys), ctx);
		case 8:
			return ScalarNumericVal.create(NativeType.UBYTE, Integer.parseUnsignedInt(value.substring(off, len), sys), ctx);
		default:
			throw new AssertionError();
		}
	}
	
	protected SimpleType parseType(SimpleScope scope) {
		final Object enter = enterState(STATE_TYPE);
		SimpleType type;
		switch ( this.in.tok() ) {
		case NUM -> {
			this.in.consume();
			type = NativeType.NUM;
		}
		case UNUM -> {
			this.in.consume();
			type = NativeType.UNUM;
		}
		case FPNUM -> {
			this.in.consume();
			type = NativeType.FPNUM;
		}
		case FPDWORD -> {
			this.in.consume();
			type = NativeType.FPDWORD;
		}
		case DWORD -> {
			this.in.consume();
			type = NativeType.DWORD;
		}
		case UDWORD -> {
			this.in.consume();
			type = NativeType.UDWORD;
		}
		case WORD -> {
			this.in.consume();
			type = NativeType.WORD;
		}
		case UWORD -> {
			this.in.consume();
			type = NativeType.UWORD;
		}
		case BYTE -> {
			this.in.consume();
			type = NativeType.BYTE;
		}
		case UBYTE -> {
			this.in.consume();
			type = NativeType.UBYTE;
		}
		case NAME -> type = parseTypeTypedef(scope);
		case STRUCT -> type = parseTypeStruct(scope);
		case FSTRUCT -> {
			final Object unknownRes = enterUnknownState();
			this.in.consume();
			if ( this.in.tok() == NAME ) {
				final Object subEnter = decidedState(STATE_TYPE_FUNC_STRUCT_REF, unknownRes);
				type = parseTypeFStructAddress(scope, false, subEnter);
			} else {
				final Object subEnter = decidedState(STATE_TYPE_FUNC_STRUCT, unknownRes);
				type = parseTypeFuncType0(this.in.consumeTok(), scope, 0, subEnter);
			}
		}
		case FUNC -> {
			final Object subEnter = enterState(STATE_TYPE_FUNC_ADDR_REF);
			type = parseTypeFStructAddress(scope, true, subEnter);
		}
		case NOPAD -> {
			final Object subEnter = enterState(STATE_TYPE_FUNC_ADDR);
			this.in.consume();
			type = parseTypeFuncType0(NOPAD, scope, FuncType.FLAG_FUNC_ADDRESS, subEnter);
		}
		case LT -> {
			final Object subEnter = enterState(STATE_TYPE_FUNC_ADDR);
			this.in.consume();
			type = parseTypeFuncType0(LT, scope, FuncType.FLAG_FUNC_ADDRESS, subEnter);
		}
		case SMALL_OPEN -> {
			final Object subEnter = enterState(STATE_TYPE_FUNC_ADDR);
			this.in.consume();
			type = parseTypeFuncType0(SMALL_OPEN, scope, FuncType.FLAG_FUNC_ADDRESS, subEnter);
		}
		default -> {
			this.in.handleError(this.in.ctx(),
				List.of(name(NUM), name(UNUM), name(FPNUM), name(FPDWORD), name(DWORD), name(WORD), name(UWORD), name(BYTE),
					name(UBYTE), name(STRUCT), name(FSTRUCT), name(NOPAD), name(LT), name(SMALL_OPEN), name(NAME)));
			type = StructType.create(List.of(), StructType.FLAG_NOUSE, this.in.ctx());
		}
		}
		return parseTypePostfix(scope, type, enter);
	}
	
	protected SimpleType parseTypePostfix(SimpleScope scope, SimpleType type, Object parentEnter) {
		final Object enter = enterState(STATE_TYPE_POSTFIX);
		while ( true ) {
			switch ( this.in.tok() ) {
			case DIAMOND -> {
				this.in.consume();
				type = PointerType.create(type, this.in.ctx());
			}
			case ARR_OPEN -> {
				this.in.consume();
				SimpleValue len;
				if ( this.in.tok() == ARR_CLOSE ) {
					this.in.consume();
					len = null;
				} else {
					len = parseValue(scope);
					consumeToken(ARR_CLOSE, "expected `\\]´ after `\\[ [VALUE]");
				}
				type = ArrayType.create(type, len, this.in.ctx());
			}
			default -> {
				exitState(STATE_TYPE_POSTFIX, enter, type);
				exitState(STATE_TYPE, parentEnter, type);
				return type;
			}
			}
		}
	}
	
	private SimpleType parseTypeTypedef(SimpleScope scope) {
		final Object enter = enterState(STATE_TYPE_TYPEDEFED_TYPE);
		SimpleType res = parseTypeNamedType(scope, SimpleType.class);
		exitState(STATE_TYPE_TYPEDEFED_TYPE, enter, res);
		return res;
	}
	
	@SuppressWarnings("unchecked")
	private < T > T parseTypeNamedType(SimpleScope scope, Class<T> cls) {
		while ( true ) {
			Object obj = scope.nameTypeOrDepOrFuncOrNull(this.in.consumeDynTokSpecialText());
			if ( cls.isInstance(obj) ) return cls.cast(obj);
			if ( !( obj instanceof SimpleDependency nscope ) ) {
				String simpleName = cls.getSimpleName().startsWith("Simple") ? cls.getSimpleName().substring("Simple".length())
					: cls.getSimpleName();
				if ( obj == null ) {
					this.in.handleError(this.in.ctx(), "expected the `[NAME]´ to be the [NAME] of a " + simpleName
						+ " or a dependency, but there is nothing with the given name");
					if ( cls == SimpleType.class ) {
						return (T) StructType.create(List.of(), StructType.FLAG_NOUSE, this.in.ctx());
					}
					if ( cls == SimpleFunction.class ) {
						return (T) new SimpleFunction(new SimpleFile("invalid\0", "invalid\0"), "<invalid>",
							FuncType.create(List.of(), List.of(), 0, this.in.ctx()));
					}
					return null;
				}
				String objSimpleName = obj.getClass().getSimpleName().startsWith("Simple")
					? obj.getClass().getSimpleName().substring("Simple".length())
					: obj.getClass().getSimpleName();
				this.in.handleError(this.in.ctx(), "expected the `[NAME]´ to be the [NAME] of a " + simpleName
					+ " or a dependency, but it is " + objSimpleName + " : " + obj);
				if ( cls == SimpleType.class ) {
					return (T) StructType.create(List.of(), StructType.FLAG_NOUSE, this.in.ctx());
				}
				if ( cls == SimpleFunction.class ) {
					return (T) new SimpleFunction(new SimpleFile("invalid\0", "invalid\0"), "<invalid>",
						FuncType.create(List.of(), List.of(), 0, this.in.ctx()));
				}
				return null;
			}
			scope = nscope;
			consumeToken(COLON, "expected `:[NAME]´ after a dependency `[NAME]´");
			expectToken(NAME, "expected `[NAME]´ after a dependency `[NAME]:´");
		}
	}
	
	protected SimpleType parseTypeStruct(SimpleScope scope) {
		final Object enter = enterState(STATE_TYPE_STRUCT);
		this.in.consume();
		int flags = 0;
		if ( this.in.tok() == NOPAD ) {
			this.in.consume();
			flags |= StructType.FLAG_NOPAD;
		}
		consumeToken(BLOCK_OPEN, "expected `{´ after `struct (nopad)?´");
		StructType res = StructType.create(parseNamedTypeList(BLOCK_CLOSE, SEMI, true, scope), flags, this.in.ctx());
		exitState(STATE_TYPE_STRUCT, enter, res);
		return res;
	}
	
	protected List<SimpleVariable> parseNamedTypeList(final int end, final int sep, final boolean sepBeforeEnd,
		SimpleScope scope) {
		final Object enter = enterState(STATE_NAMED_TYPE_LIST);
		return parseNamedTypeList(end, sep, sepBeforeEnd, scope, null, enter);
	}
	
	protected List<SimpleVariable> parseNamedTypeList(final int end, final int sep, final boolean sepBeforeEnd, SimpleScope scope,
		List<SimpleVariable> add, final Object enter) {
		if ( this.in.tok() == end ) {
			exitState(STATE_NAMED_TYPE_LIST, enter, add == null ? List.of() : add);
			this.in.consume();
			return List.of();
		}
		List<SimpleVariable> members = add == null ? new ArrayList<>() : add;
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after `[TYPE]´");
		String name;
		try {
			name = this.in.consumeDynTokSpecialText();
		} catch ( @SuppressWarnings("unused") AssertionError ae ) {
			name = name(this.in.consumeTok());
		}
		members.add(new SimpleVariable(type, name, null, 0));
		while ( true ) {
			if ( this.in.tok() != sep ) {
				if ( sepBeforeEnd || this.in.tok() != end ) {
					List<String> list;
					String msg;
					if ( sepBeforeEnd ) {
						list = List.of(name(sep));
						msg = "expected `" + name(sep) + "´ after `[NAMED_TYPE]´";
					} else {
						list = List.of(name(sep), name(end));
						msg = "expected to end the [NAMED_TYPE_LIST] with `" + name(end) + "´ or seperate two named types with `"
							+ name(sep) + "´";
					}
					this.in.handleError(this.in.ctx(), list, msg);
					exitState(STATE_NAMED_TYPE_LIST, enter, add);
					return members;
				}
				exitState(STATE_NAMED_TYPE_LIST, enter, add);
				this.in.consume();
				return members;
			}
			this.in.consume();
			if ( sepBeforeEnd ) {
				if ( this.in.tok() == end ) {
					exitState(STATE_NAMED_TYPE_LIST, enter, add);
					this.in.consume();
					return members;
				}
			}
			type = parseType(scope);
			expectToken(NAME, "expected `[NAME]´ after `[TYPE]´");
			try {
				name = this.in.consumeDynTokSpecialText();
			} catch ( @SuppressWarnings("unused") AssertionError ae ) {
				name = name(this.in.consumeTok());
			}
			members.add(new SimpleVariable(type, name, null, 0));
		}
	}
	
	protected SimpleType parseTypeFStructAddress(SimpleScope scope, boolean address, final Object enter) {
		consumeToken(NAME, "expected `[NAME]´ after `( fstruct | func )´");
		FuncType func = parseTypeNamedType(scope, SimpleFunction.class).type();
		if ( address ) {
			exitState(STATE_TYPE_FUNC_ADDR_REF, enter, func);
		} else {
			func = func.asFStruct();
			exitState(STATE_TYPE_FUNC_STRUCT_REF, enter, func);
		}
		return func;
	}
	
	protected SimpleType parseTypeFuncType0(int t, SimpleScope scope, int flags, final Object enter) {
		List<SimpleVariable> results = List.of();
		switch ( t ) {
		case NOPAD:
			flags |= FuncType.FLAG_NOPAD;
			t = this.in.consumeTok();
			if ( t != LT ) {
				if ( t == SMALL_OPEN ) break;
				this.in.handleError(this.in.ctx(), List.of(name(LT), name(SMALL_OPEN)),
					"expected `< [NAMED_TYPE_LIST] > <--´ or `\\( [NAMED_TYPE_LIST] \\)´ after `nopad´");
			}
			//$FALL-THROUGH$
		case LT:
			results = parseNamedTypeList(GT, COMMA, false, scope);
			consumeToken(LARROW, "expectedd `<-- \\( [NAMED_TYPE_LIST] \\)´ after `(nopad)? < [NAMED_TYPE_LIST] >´");
			consumeToken(SMALL_OPEN, "expectedd `\\( [NAMED_TYPE_LIST] \\)´ after `(nopad)? < [NAMED_TYPE_LIST] > <--´");
			//$FALL-THROUGH$
		case SMALL_OPEN:
			break;
		default:
			throw new AssertionError("invalid token passed to parseTypeFuncType0: " + t + " : " + name(t));
		}
		List<SimpleVariable> args = parseNamedTypeList(SMALL_CLOSE, COMMA, false, scope);
		FuncType res = FuncType.create(results, args, flags, this.in.ctx());
		if ( ( flags & FuncType.FLAG_FUNC_ADDRESS ) != 0 ) {
			exitState(STATE_TYPE_FUNC_ADDR, enter, res);
		} else {
			exitState(STATE_TYPE_FUNC_STRUCT, enter, res);
		}
		return res;
	}
	
	protected void parseTypedef(SimpleFile sf) {
		final Object enter = enterState(STATE_TYPEDEF);
		this.in.consume();
		int flags = 0;
		if ( this.in.tok() == EXP ) {
			this.in.consume();
			flags |= SimpleTypedef.FLAG_EXPORT;
		}
		SimpleType type = parseType(sf);
		expectToken(NAME, "expected `[NAME] ;´ after `typedef (exp)? [TYPE]´");
		String name = this.in.consumeDynTokSpecialText();
		consumeToken(SEMI, "expected `;´ after `typedef (exp)? [TYPE] [NAME]´");
		SimpleTypedef typedef = new SimpleTypedef(name, flags, type);
		sf.typedef(typedef, this.in.ctx());
		exitState(STATE_TYPEDEF, enter, typedef);
	}
	
	protected void parseFunction(SimpleFile sf) {
		final Object enter = enterState(STATE_FUNCTION);
		this.in.consume();
		int flags = FuncType.FLAG_FUNC_ADDRESS;
		if ( this.in.tok() != NAME ) {
			consumeToken(EXP, "expected `[NAME]´ or `exp [NAME]´ after `func´");
			expectToken(NAME, "expected `[NAME]´ after `func exp´");
			flags |= SimpleExportable.FLAG_EXPORT;
		}
		String name = this.in.consumeDynTokSpecialText();
		ErrorContext ctx = this.in.ctx();
		SimpleType type = parseType(sf);
		FuncType ftype;
		if ( !( type instanceof FuncType ftype0 ) || ( ftype0.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			ctx.setOffendingTokenCach(type.toString());
			this.in.handleError(ctx, "the [TYPE] of a function MUST be a function address type: " + type);
			if ( this.in.tok() == SEMI ) {
				this.in.consume();
			}
			exitState(STATE_FUNCTION, enter, null);
			return;
		}
		ftype = ftype0;
		consumeToken(SEMI, "expected `;´ after `(exp)? [NAME] [TYPE]´");
		ftype = FuncType.create(ftype.resMembers(), ftype.argMembers(), flags, ctx);
		SimpleFunction func = new SimpleFunction(sf, name, ftype);
		sf.function(func, ctx);
		exitState(STATE_FUNCTION, enter, func);
	}
	
	protected void expectToken(int tok, String msg) {
		if ( this.in.tok() != tok ) {
			this.in.handleError(this.in.ctx(), List.of(name(tok)), msg);
		}
	}
	
	protected void consumeToken(int tok, String msg) {
		int t = this.in.consumeTok();
		if ( t != tok ) {
			this.in.tok(t);
			this.in.handleError(this.in.ctx(), List.of(name(tok)), msg);
			this.in.consumeTok();
		}
	}
	
	/**
	 * this method can be overwritten to be notified when the parser enters a state<br>
	 * note that if this method is overwritten {@link #enterUnknownState()} should also be overwritten
	 * 
	 * @param state the state which is entered
	 * 
	 * @return the object to be passed to {@link #exitState(int, Object, Object)} when the state is exited
	 * 
	 * @see #exitState(int, Object, Object)
	 * @see #enterUnknownState()
	 */
	@SuppressWarnings("unused")
	protected Object enterState(int state) {
		return null;
	}
	
	/**
	 * this method can be overwritten to be notified when the parser stars to read more input to know to which state it
	 * should go
	 * <ul>
	 * <li>if multiple states start at the current token the result of this method MUST NOT be passed multiple times to
	 * {@link #decidedState(int, Object)} or {@link #decidedStates(int[], Object)}</li>
	 * <li>if multiple states start at the current token the result of this method may be passed to
	 * {@link #decidedStates(int[], Object)} with an array which contains the states that are now decided</li>
	 * <li>it is guaranteed that at least one state starts at the current position when this method is called</li>
	 * <li>note that by the time {@link #decidedState(int, Object)} is called various sub-states may have already been
	 * processed</li>
	 * <li>if some states are decided at different times {@link #enterUnknownState()} is called multiple times (or
	 * both)</li>
	 * <li>additional the result of this call may be used for
	 * {@link #remenberExitedState(int, Object, Object, Object)}.<br>
	 * the parser may do this multiple times with the same end marker (which is the result of this call)</li>
	 * </ul>
	 * 
	 * @return the value to be passed to {@link #decidedState(int, Object)} when the parser decided its state
	 */
	protected Object enterUnknownState() {
		return null;
	}
	
	/**
	 * this method is simmilar to {@link #enterUnknownState()}, but it <b>not</b> allowed to be passed to
	 * {@link #decidedState(int, Object)}/{@link #decidedStates(int[], Object)}.<br>
	 * the result of this method will only be passed to {@link #remenberExitedState(int, Object, Object, Object)} as end
	 * marker<br>
	 * also no state needs to start at the current position and also no
	 * 
	 * @return an end marker suitable to be passed to {@link #remenberExitedState(int, Object, Object, Object)} as such
	 */
	protected Object maybeFinishUnknownState() {
		return null;
	}
	
	/**
	 * this method can be overwritten to be notified when the parser previously was not sure which state it should be in
	 * but now decided its state.
	 * <p>
	 * note that if this method is overwritten {@link #decidedStates(int[], Object)} should also be overwritten
	 * <p>
	 * the default implementation just returns {@code unknownStateResult}<br>
	 * if this is enough only {@link #enterState(int)}, {@link #enterUnknownState()} and
	 * {@link #exitState(int, Object, Object)} needs to be implemented.
	 * 
	 * @param state              the state of the parser
	 * @param unknownStateResult the result of {@link #enterUnknownState()}
	 * 
	 * @return the value to be passed to {@link #exitState(int, Object, Object)} when the state is exited
	 */
	@SuppressWarnings("unused")
	protected Object decidedState(int state, Object unknownStateResult) {
		return null;
	}
	
	/**
	 * this method can be overwritten to be notified when the parser previously was not sure which state it should be in
	 * but now decided its state.
	 * <p>
	 * the states are ordered by their nesting:<br>
	 * the innermost state has the lowest index ({@code 0})<br>
	 * the result array must either be {@code null} or be as large as the passed states array<br>
	 * the entries in the result array are assumed to be ordered by the same logic as the entries in the states array
	 * <p>
	 * note that if this method is overwritten {@link #decidedState(int, Object)} should also be overwritten
	 * 
	 * @param states             the decided states
	 * @param unknownStateResult the result of {@link #enterUnknownState()}
	 * 
	 * @return the values to be passed to {@link #exitState(int, Object, Object)} when the states are exited
	 */
	protected Object[] decidedStates(int[] states, Object unknownStateResult) {
		if ( unknownStateResult == null ) {
			return null;
		}
		Object[] result = new Object[states.length];
		Arrays.fill(result, unknownStateResult);
		return result;
	}
	
	/**
	 * this method can be overwritten to be notified when the parser exits a state
	 * <p>
	 * if {@code additionalData} has a nun-{@code null} value, it describes the result of the sub parsing process done
	 * while in the given state
	 * <p>
	 * note that if this method is overwritten, {@link #remenberExitedState(int, Object, Object, Object)} should also be
	 * overwritten
	 * 
	 * @param state          the state which is finished now
	 * @param enterResult    the result of {@link #enterState(int)} when the parser entered the state
	 * @param additionalData some additional data, potentially <code>null</code> if the state does not support
	 *                           additional data
	 */
	@SuppressWarnings("unused")
	protected void exitState(int state, Object enterResult, Object additionalData) {}
	
	/**
	 * this method can be overwritten to be notified when the parser exits a state
	 * <p>
	 * this method is like {@link #exitState(int, Object, Object)}, but instead of using the current state of the parser
	 * this method should use the end marker {@code enterUnknownEndMarker}, which was obtained by calling
	 * {@link #enterUnknownState()}
	 * <p>
	 * if {@code additionalData} has a nun-{@code null} value,it describes the result of the sub parsing process done
	 * while in the given state
	 * <p>
	 * note that if this method is overwritten, {@link #exitState(int, Object, Object)} should also be overwritten
	 * 
	 * @param state                 the state which is finished now
	 * @param enterResult           the result of {@link #enterState(int)} when the parser entered the state
	 * @param enterUnknownEndMarker the end marker of the already previously state
	 * @param additionalData        some additional data, potentially <code>null</code> if the state does not support
	 *                                  additional data
	 */
	@SuppressWarnings("unused")
	protected void remenberExitedState(int state, Object enterResult, Object enterUnknownEndMarker, Object additionalData) {}
	
}
