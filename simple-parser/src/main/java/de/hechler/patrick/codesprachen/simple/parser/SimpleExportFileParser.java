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
package de.hechler.patrick.codesprachen.simple.parser;

import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.codesprachen.simple.parser.error.CompileError;
import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleTypedef;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.ArrayType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.StructType;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.BinaryOpVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.CastVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.CondVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.DataVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.NameVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.ScalarNumericVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.UnaryOpVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.BinaryOpVal.BinaryOp;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.UnaryOpVal.UnaryOp;

public class SimpleExportFileParser {
	
	protected final SimpleTokenStream                          in;
	protected final BiFunction<String,String,SimpleDependency> dep;
	
	public SimpleExportFileParser(InputStream in, String file, BiFunction<String,String,SimpleDependency> dep) {
		this(new SimpleTokenStream(in, file), dep);
	}
	
	public SimpleExportFileParser(SimpleTokenStream in, BiFunction<String,String,SimpleDependency> dep) {
		this.in = in;
		this.dep = dep;
	}
	
	
	public SimpleDependency parse(String runtimePath, boolean isMeDep) {
		SimpleFile sf = new SimpleFile(runtimePath);
		parse(sf, isMeDep);
		return sf;
	}
	
	public void parse(SimpleFile sf, boolean isMeDep) {
		while ( true ) {
			switch ( this.in.tok() ) {
			case DEP:
				parseDependency(sf);
				break;
			case TYPEDEF:
				parseTypedef(sf, isMeDep ? SimpleTypedef.FLAG_FROM_ME_DEP : 0);
				break;
			case FUNC:
				parseFunction(sf);
				break;
			case EOF:
				return;
			default:
				parseSFScopeVariable(sf);
				break;
			}
		}
	}
	
	protected void parseDependency(SimpleFile sf) {
		this.in.consume();
		expectToken(NAME, "expected to get `[NAME] [STRING] ;´ after `dep´");
		String name = this.in.consumeDynTokSpecialText();
		expectToken(STRING, "expected to get `[STRING] ;´ after `dep [NAME]´");
		String path = this.in.consumeDynTokSpecialText();
		consumeToken(SEMI, "expected to get `;´ after `dep [NAME] [STRING]´");
		sf.dependency(this.dep.apply(name, path), name, this.in.ctx());
	}
	
	protected SimpleVariable parseSFScopeVariable(SimpleFile sf) {
		SimpleVariable sv = parseAnyScopeVariable(sf);
		if ( ( sv.flags() & SimpleVariable.FLAG_CONSTANT ) == 0 && sv.initialValue() != null ) {
			throw new CompileError(this.in.ctx(), "a non constant value must not have a initial value set");
		}
		return sv;
	}
	
	protected SimpleVariable parseAnyScopeVariable(SimpleScope scope) {
		int flags = 0;
		switch ( this.in.tok() ) {
		case CONST:
			flags |= SimpleVariable.FLAG_CONSTANT;
			this.in.consume();
			if ( this.in.tok() != EXP ) break;
			//$FALL-THROUGH$
		case EXP:
			flags |= SimpleVariable.FLAG_EXPORT;
			this.in.consume();
			//$FALL-THROUGH$
		default:
		}
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after ´(const)? (exp)? [TYPE]´");
		String name = this.in.consumeDynTokSpecialText();
		SimpleValue initialValue = null;
		if ( this.in.tok() == LARROW ) {
			this.in.consume();
			initialValue = parseValue(scope);
		} else if ( ( flags & SimpleVariable.FLAG_CONSTANT ) != 0 ) {
			throw new CompileError(this.in.ctx(), "a constant value must have a initial value set");
		}
		return new SimpleVariable(type, name, initialValue, flags);
	}
	
	protected SimpleValue parseValue(SimpleScope scope) {
		return parseValueCondExp(scope, 0, null);
	}
	
	protected SimpleValue parseValue(SimpleScope scope, int magic, SimpleValue mvalue) {
		return parseValueCondExp(scope, magic, mvalue);
	}
	
	protected static final int COND_MAGIC = 1;
	
	protected SimpleValue parseValueCondExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == COND_MAGIC ) return mvalue;
		SimpleValue a = parseValueLOrExp(scope, magic, mvalue);
		if ( this.in.tok() == QUESTION ) {
			this.in.consume();
			SimpleValue b = parseValue(scope);
			consumeToken(BOOL_NOT, "expected `! [COND_EXP]´ after `[LOR_EXP] ? [VALUE]´");
			SimpleValue c = parseValueCondExp(scope, 0, null);
			a = CondVal.create(a, b, c, this.in.ctx());
		}
		return a;
	}
	
	protected static final int LOR_MAGIC = 2;
	
	protected SimpleValue parseValueLOrExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == LOR_MAGIC ) return mvalue;
		SimpleValue a = parseValueLAndExp(scope, magic, mvalue);
		while ( this.in.tok() == BOOL_OR ) {
			this.in.consume();
			SimpleValue b = parseValueLAndExp(scope, 0, null);
			a = BinaryOpVal.create(a, BinaryOp.BOOL_OR, b, this.in.ctx());
		}
		return a;
	}
	
	protected static final int LAND_MAGIC = 3;
	
	protected SimpleValue parseValueLAndExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == LAND_MAGIC ) return mvalue;
		SimpleValue a = parseValueOrExp(scope, magic, mvalue);
		while ( this.in.tok() == BOOL_AND ) {
			this.in.consume();
			SimpleValue b = parseValueOrExp(scope, 0, null);
			a = BinaryOpVal.create(a, BinaryOp.BOOL_AND, b, this.in.ctx());
		}
		return a;
	}
	
	protected static final int OR_MAGIC = 4;
	
	protected SimpleValue parseValueOrExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == OR_MAGIC ) return mvalue;
		SimpleValue a = parseValueXOrExp(scope, magic, mvalue);
		while ( this.in.tok() == BIT_OR ) {
			this.in.consume();
			SimpleValue b = parseValueXOrExp(scope, 0, null);
			a = BinaryOpVal.create(a, BinaryOp.BIT_OR, b, this.in.ctx());
		}
		return a;
	}
	
	protected static final int XOR_MAGIC = 5;
	
	protected SimpleValue parseValueXOrExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == XOR_MAGIC ) return mvalue;
		SimpleValue a = parseValueAndExp(scope, magic, mvalue);
		while ( this.in.tok() == BIT_XOR ) {
			this.in.consume();
			SimpleValue b = parseValueAndExp(scope, 0, null);
			a = BinaryOpVal.create(a, BinaryOp.BIT_XOR, b, this.in.ctx());
		}
		return a;
	}
	
	protected static final int AND_MAGIC = 6;
	
	protected SimpleValue parseValueAndExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == AND_MAGIC ) return mvalue;
		SimpleValue a = parseValueEqExp(scope, magic, mvalue);
		while ( this.in.tok() == BIT_AND ) {
			this.in.consume();
			SimpleValue b = parseValueEqExp(scope, 0, null);
			a = BinaryOpVal.create(a, BinaryOp.BIT_AND, b, this.in.ctx());
		}
		return a;
	}
	
	protected static final int EQ_MAGIC = 7;
	
	protected SimpleValue parseValueEqExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == EQ_MAGIC ) return mvalue;
		SimpleValue a = parseValueRelExp(scope, magic, mvalue);
		int t = this.in.tok();
		while ( t == EQ || t == NOT_EQ ) {
			this.in.consume();
			SimpleValue b = parseValueRelExp(scope, 0, null);
			a = BinaryOpVal.create(a, t == EQ ? BinaryOp.CMP_EQ : BinaryOp.CMP_NEQ, b, this.in.ctx());
			t = this.in.tok();
		}
		return a;
	}
	
	protected static final int REL_MAGIC = 8;
	
	protected SimpleValue parseValueRelExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == REL_MAGIC ) return mvalue;
		SimpleValue a = parseValueShiftExp(scope, magic, mvalue);
		int t = this.in.tok();
		while ( t == GT || t == GE || t == LE || t == LT ) {
			this.in.consume();
			SimpleValue b = parseValueShiftExp(scope, 0, null);
			a = BinaryOpVal.create(a, switch ( t ) {
			case GT -> BinaryOp.CMP_GT;
			case GE -> BinaryOp.CMP_GE;
			case LE -> BinaryOp.CMP_LE;
			case LT -> BinaryOp.CMP_LT;
			default -> throw new AssertionError();
			}, b, this.in.ctx());
			t = this.in.tok();
		}
		return a;
	}
	
	protected static final int SHIFT_MAGIC = 9;
	
	protected SimpleValue parseValueShiftExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == SHIFT_MAGIC ) return mvalue;
		SimpleValue a = parseValueAddExp(scope, magic, mvalue);
		int t = this.in.tok();
		while ( t == SHIFT_LEFT || t == SHIFT_RIGTH_LOG || t == SHIFT_RIGTH_ARI ) {
			this.in.consume();
			SimpleValue b = parseValueAddExp(scope, 0, null);
			a = BinaryOpVal.create(a, switch ( t ) {
			case SHIFT_LEFT -> BinaryOp.SHIFT_LEFT;
			case SHIFT_RIGTH_ARI -> BinaryOp.SHIFT_ARITMETIC_RIGTH;
			case SHIFT_RIGTH_LOG -> BinaryOp.SHIFT_LOGIC_RIGTH;
			default -> throw new AssertionError();
			}, b, this.in.ctx());
			t = this.in.tok();
		}
		return a;
	}
	
	protected static final int ADD_MAGIC = 10;
	
	protected SimpleValue parseValueAddExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == ADD_MAGIC ) return mvalue;
		SimpleValue a = parseValueMulExp(scope, magic, mvalue);
		int t = this.in.tok();
		while ( t == PLUS || t == MINUS ) {
			this.in.consume();
			SimpleValue b = parseValueMulExp(scope, 0, null);
			a = BinaryOpVal.create(a, t == PLUS ? BinaryOp.MATH_ADD : BinaryOp.MATH_SUB, b, this.in.ctx());
			t = this.in.tok();
		}
		return a;
	}
	
	protected static final int MUL_MAGIC = 11;
	
	protected SimpleValue parseValueMulExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == MUL_MAGIC ) return mvalue;
		SimpleValue a = parseValueCastExp(scope, magic, mvalue);
		int t = this.in.tok();
		while ( t == STAR || t == DIV || t == MOD ) {
			this.in.consume();
			SimpleValue b = parseValueCastExp(scope, 0, null);
			a = BinaryOpVal.create(a, switch ( t ) {
			case STAR -> BinaryOp.MATH_MUL;
			case DIV -> BinaryOp.MATH_DIV;
			case MOD -> BinaryOp.MATH_MOD;
			default -> throw new AssertionError();
			}, b, this.in.ctx());
			t = this.in.tok();
		}
		return a;
	}
	
	protected static final int CAST_MAGIC = 12;
	
	protected SimpleValue parseValueCastExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == CAST_MAGIC ) return mvalue;
		if ( magic != 0 ) return parseValueUnaryExp(scope, magic, mvalue);
		if ( this.in.tok() == SMALL_OPEN ) {
			this.in.consume();
			SimpleType t = parseType(scope);
			consumeToken(SMALL_CLOSE, "expected `) [VALUE]´ after `( [TYPE]´");
			SimpleValue a = parseValueUnaryExp(scope, 0, null);
			return CastVal.create(a, t, this.in.ctx());
		}
		return parseValueUnaryExp(scope, 0, null);
	}
	
	protected static final int UNARY_MAGIC = 13;
	
	protected SimpleValue parseValueUnaryExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == UNARY_MAGIC ) return mvalue; // if magic is set, just delegate
		if ( magic != 0 ) return parseValuePostfixExp(scope, magic, mvalue);
		UnaryOp op;
		switch ( this.in.tok() ) {
		case PLUS -> op = UnaryOp.PLUS;
		case MINUS -> op = UnaryOp.MINUS;
		case BIT_AND -> op = UnaryOp.ADDRESS_OF;
		case BIT_NOT -> op = UnaryOp.BIT_NOT;
		case BOOL_NOT -> op = UnaryOp.BOOL_NOT;
		default -> { return parseValuePostfixExp(scope, 0, null); }
		}
		SimpleValue a = parseValuePostfixExp(scope, 0, null);
		return UnaryOpVal.create(op, a, this.in.ctx());
	}
	
	protected static final int POSTFIX_MAGIC = 14;
	
	protected SimpleValue parseValuePostfixExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == POSTFIX_MAGIC ) return mvalue;
		SimpleValue a = parseValueDirectExp(scope, magic, mvalue);
		while ( true ) {
			switch ( this.in.tok() ) {
			case DIAMOND -> {
				this.in.consume();
				a = UnaryOpVal.create(UnaryOp.DEREF_PNTR, a, this.in.ctx());
			}
			case ARR_OPEN -> {
				this.in.consume();
				SimpleValue b = parseValue(scope);
				consumeToken(ARR_CLOSE, "expected `\\]´ after `\\[ [VALUE]´");
				a = BinaryOpVal.create(a, BinaryOp.ARR_PNTR_INDEX, b, this.in.ctx());
			}
			case COLON -> {
				this.in.consume();
				expectToken(NAME, "expected `[NAME]´ after `:´");
				String name = this.in.consumeDynTokSpecialText();
				NameVal b = new NameVal(name);
				a = BinaryOpVal.create(a, BinaryOp.DEREF_BY_NAME, b, this.in.ctx());
			}
			default -> { return a; }
			}
		}
	}
	
	protected static final int DIRECT_MAGIC = 15;
	
	protected SimpleValue parseValueDirectExp(SimpleScope scope, int magic, SimpleValue mvalue) {
		if ( magic == DIRECT_MAGIC ) return mvalue;
		int t = this.in.consumeTok();
		switch ( t ) {
		case STRING -> {
			String str = this.in.dynTokSpecialText();
			if ( this.in.tok() != STRING ) {
				return DataVal.createString(str, this.in.ctx());
			}
			StringBuilder sb = new StringBuilder(str);
			do {
				str = this.in.consumeDynTokSpecialText();
				sb.append(str);
			} while ( this.in.tok() == STRING );
			return DataVal.createString(sb, this.in.ctx());
		}
		case CHARACTER -> {
			char value = this.in.dynTokSpecialText().charAt(0);
			return ScalarNumericVal.create(NativeType.UBYTE, value, this.in.ctx());
		}
		case NUMBER -> {
			return parseNumber(this.in.ctx());
		}
		case NAME -> {
			return scope.nameValueOrErr(this.in.dynTokSpecialText(), this.in.ctx());
		}
		case SMALL_OPEN -> {
			SimpleValue a = parseValue(scope);
			consumeToken(SMALL_CLOSE, "expected `)´ after `( [VALUE]´");
			return a;
		}
		default -> {
			ErrorContext ctx = this.in.ctx();
			ctx.setOffendingTokenCach(t == ASM_BLOCK ? ":::" + this.in.dynTokSpecialText() + ">>>" : name(t));
			throw new CompileError(ctx,
				List.of(name(STRING), name(CHARACTER), name(NUMBER), name(NAME), name(SMALL_OPEN)));
		}
		}
	}
	
	private SimpleValue parseNumber(ErrorContext ctx) throws AssertionError {
		String value = this.in.dynTokSpecialText();
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
			if ( sign ) throw new CompileError(ctx, "a negative value can not be unsigned!");
		}
		default -> len++;
		}
		if ( len != value.length() && len > 0 ) {
			switch ( value.charAt(--len) ) {
			case 'S', 's' -> sign = true;
			case 'U', 'u' -> {
				if ( sign ) throw new CompileError(ctx, "a negative value can not be unsigned!");
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
			return ScalarNumericVal.create(NativeType.UNUM, Long.parseUnsignedLong(value.substring(off, len), sys),
				ctx);
		case 32:
			return ScalarNumericVal.create(NativeType.UDWORD, Integer.parseUnsignedInt(value.substring(off, len), sys),
				ctx);
		case 16:
			return ScalarNumericVal.create(NativeType.UWORD, Integer.parseUnsignedInt(value.substring(off, len), sys),
				ctx);
		case 8:
			return ScalarNumericVal.create(NativeType.UBYTE, Integer.parseUnsignedInt(value.substring(off, len), sys),
				ctx);
		default:
			throw new AssertionError();
		}
	}
	
	protected SimpleType parseType(SimpleScope scope) {
		ErrorContext ctx = this.in.ctx();
		SimpleType type;
		switch ( this.in.consumeTok() ) {
		case NUM -> type = NativeType.NUM;
		case UNUM -> type = NativeType.UNUM;
		case FPNUM -> type = NativeType.FPNUM;
		case FPDWORD -> type = NativeType.FPDWORD;
		case DWORD -> type = NativeType.DWORD;
		case UDWORD -> type = NativeType.UDWORD;
		case WORD -> type = NativeType.WORD;
		case UWORD -> type = NativeType.UWORD;
		case BYTE -> type = NativeType.BYTE;
		case UBYTE -> type = NativeType.UBYTE;
		case NAME -> type = parseTypeTypedef(ctx, scope);
		case STRUCT -> type = parseTypeStruct(ctx, scope);
		case FSTRUCT -> type = parseTypeFStruct(ctx, scope, false);
		case FUNC -> type = parseTypeFStruct(ctx, scope, true);
		case NOPAD -> type = parseTypeFuncType0(NOPAD, ctx, scope);
		case LT -> type = parseTypeFuncType0(LT, ctx, scope);
		case SMALL_OPEN -> type = parseTypeFuncType0(SMALL_OPEN, ctx, scope);
		default -> throw new CompileError(ctx,
			List.of(name(NUM), name(UNUM), name(FPNUM), name(FPDWORD), name(DWORD), name(WORD), name(UWORD), name(BYTE),
				name(UBYTE), name(STRUCT), name(FSTRUCT), name(NOPAD), name(LT), name(SMALL_OPEN), name(NAME)));
		}
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
			default -> { return type; }
			}
		}
	}
	
	private SimpleType parseTypeTypedef(ErrorContext ctx, SimpleScope scope) {
		return parseTypeNamedType(ctx, scope, SimpleType.class);
	}
	
	private < T > T parseTypeNamedType(ErrorContext ctx, SimpleScope scope, Class<T> cls) {
		while ( true ) {
			Object obj = scope.nameTypeOrDepOrFuncOrNull(this.in.dynTokSpecialText(), ctx);
			if ( cls.isInstance(obj) ) return cls.cast(obj);
			if ( !( obj instanceof SimpleDependency nscope ) ) {
				String simpleName = cls.getSimpleName().startsWith("Simple")
					? cls.getSimpleName().substring("Simple".length())
					: cls.getSimpleName();
				if ( obj == null ) {
					throw new CompileError(this.in.ctx(), "expected the `[NAME]´ to be the [NAME] of a " + simpleName
						+ " or a dependency, but there is nothing with the given name");
				}
				String objSimpleName = obj.getClass().getSimpleName().startsWith("Simple")
					? obj.getClass().getSimpleName().substring("Simple".length())
					: obj.getClass().getSimpleName();
				throw new CompileError(this.in.ctx(), "expected the `[NAME]´ to be the [NAME] of a " + simpleName
					+ " or a dependency, but it is " + objSimpleName + " : " + obj);
			}
			scope = nscope;
			consumeToken(COLON, "expected `:[NAME]´ after a dependency `[NAME]´");
			consumeToken(NAME, "expected `[NAME]´ after a dependency `[NAME]:´");
		}
	}
	
	// STRUCT has already been consumed
	protected SimpleType parseTypeStruct(ErrorContext ctx, SimpleScope scope) {
		ctx = ctx.copy();
		int flags = 0;
		if ( this.in.tok() == NOPAD ) {
			this.in.consume();
			flags |= StructType.FLAG_NOPAD;
		}
		consumeToken(BLOCK_OPEN, "expected `{´ after `struct (nopad)?´");
		return StructType.create(parseNamedTypeList(BLOCK_CLOSE, SEMI, true, scope), flags, ctx);
	}
	
	protected List<SimpleVariable> parseNamedTypeList(final int end, final int sep, final boolean sepBeforeEnd,
		SimpleScope scope) {
		return parseNamedTypeList(end, sep, sepBeforeEnd, scope, null);
	}
	
	protected List<SimpleVariable> parseNamedTypeList(final int end, final int sep, final boolean sepBeforeEnd,
		SimpleScope scope, List<SimpleVariable> add) {
		if ( this.in.tok() == end ) {
			this.in.consume();
			return List.of();
		}
		List<SimpleVariable> members = add == null ? new ArrayList<>() : add;
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after `[TYPE]´");
		String name = this.in.consumeDynTokSpecialText();
		members.add(new SimpleVariable(type, name, null, 0));
		while ( true ) {
			int t = this.in.consumeTok();
			if ( t != sep ) {
				if ( sepBeforeEnd || t != end ) {
					ErrorContext ctx = this.in.ctx();
					if ( t < FIRST_DYN ) {
						ctx.setOffendingTokenCach(name(t));
					}
					List<String> list;
					String msg;
					if ( sepBeforeEnd ) {
						list = List.of(name(sep));
						msg = "expected `" + name(sep) + "´ after `[NAMED_TYPE]´";
					} else {
						list = List.of(name(sep), name(end));
						msg = "expected to end the [NAMED_TYPE_LIST] with `" + name(end)
							+ "´ or seperate two named types with `" + name(sep) + "´";
					}
					throw new CompileError(ctx, list, msg);
				}
				return members;
			} else if ( sepBeforeEnd ) {
				t = this.in.tok();
				if ( t == end ) {
					this.in.consume();
					return members;
				}
			}
			type = parseType(scope);
			expectToken(NAME, "expected `[NAME]´ after `[TYPE]´");
			name = this.in.consumeDynTokSpecialText();
			members.add(new SimpleVariable(type, name, null, 0));
		}
	}
	
	protected SimpleType parseTypeFStruct(ErrorContext ctx, SimpleScope scope, boolean address) {
		consumeToken(NAME, "expected `[NAME]´ after `fstruct´");
		FuncType func = parseTypeNamedType(ctx, scope, SimpleFunction.class).type();
		if ( address ) return func;
		return func.asFStruct();
	}
	
	protected SimpleType parseTypeFuncType0(int t, ErrorContext ctx, SimpleScope scope) {
		int flags = FuncType.FLAG_FUNC_ADDRESS;
		List<SimpleVariable> results = List.of();
		switch ( t ) {
		case NOPAD:
			flags |= FuncType.FLAG_NOPAD;
			t = this.in.consumeTok();
			if ( t != LT ) {
				if ( t == SMALL_OPEN ) break;
				throw new CompileError(ctx, List.of(name(LT), name(SMALL_OPEN)),
					"expected `< [NAMED_TYPE_LIST] > <--´ or `\\( [NAMED_TYPE_LIST] \\)´ after `nopad´");
			}
			//$FALL-THROUGH$
		case LT:
			results = parseNamedTypeList(GT, COMMA, false, scope);
			consumeToken(LARROW, "expectedd `<-- \\( [NAMED_TYPE_LIST] \\)´ after `(nopad)? < [NAMED_TYPE_LIST] >´");
			consumeToken(SMALL_OPEN,
				"expectedd `\\( [NAMED_TYPE_LIST] \\)´ after `(nopad)? < [NAMED_TYPE_LIST] > <--´");
			//$FALL-THROUGH$
		case SMALL_OPEN:
			break;
		default:
			throw new AssertionError("invalid token passed to parseTypeFuncType0: " + t + " : " + name(t));
		}
		List<SimpleVariable> args = parseNamedTypeList(SMALL_CLOSE, COMMA, false, scope);
		return FuncType.create(results, args, flags, ctx);
	}
	
	protected void parseTypedef(SimpleFile sf, int flags) {
		ErrorContext ctx = this.in.ctx();
		this.in.consume();
		if ( this.in.tok() == EXP ) {
			this.in.consume();
			flags |= SimpleTypedef.FLAG_EXPORT;
		}
		SimpleType type = parseType(sf);
		expectToken(NAME, "expected `[NAME] ;´ after `typedef (exp)? [TYPE]´");
		String name = this.in.consumeDynTokSpecialText();
		consumeToken(SEMI, "expected `;´ after `typedef (exp)? [TYPE] [NAME]´");
		sf.typedef(new SimpleTypedef(name, flags, type), ctx);
	}
	
	protected void parseFunction(SimpleFile sf) {
		this.in.consume();
		int flags = 0;
		if ( this.in.tok() != NAME ) {
			consumeToken(EXP, "expected `[NAME]´ or `exp [NAME]´ after `func´");
			expectToken(NAME, "expected `[NAME]´ after `func exp´");
			flags = FuncType.FLAG_EXPORT;
		}
		String name = this.in.consumeDynTokSpecialText();
		ErrorContext ctx = this.in.ctx();
		SimpleType type = parseType(sf);
		if ( !( type instanceof FuncType ftype ) || ( ftype.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			ctx.setOffendingTokenCach(type.toString());
			throw new CompileError(ctx, "the [TYPE] of a function MUST be a function address type: " + type);
		}
		consumeToken(SEMI, "expected `;´ after `(exp)? [NAME] [TYPE]´");
		if ( flags != 0 ) {
			ftype = FuncType.create(ftype.resMembers(), ftype.argMembers(), flags, ctx);
		}
		SimpleFunction func = new SimpleFunction(sf, name, ftype);
		sf.function(func, ctx);
	}
	
	protected void expectToken(int tok, String msg) {
		if ( this.in.tok() != tok ) {
			throw new CompileError(this.in.ctx(), List.of(name(tok)), msg);
		}
	}
	
	protected void consumeToken(int tok, String msg) {
		int t = this.in.consumeTok();
		if ( t != tok ) {
			// do NOT use consume()! the code relies on the dynToks special Text to not be consumed
			ErrorContext ctx = this.in.ctx();
			if ( t >= SimpleTokenStream.FIRST_DYN ) {
				ctx.setOffendingTokenCach(this.in.dynTokSpecialText());
			} else {
				ctx.setOffendingTokenCach(name(t));
			}
			throw new CompileError(ctx, List.of(name(tok)), msg);
		}
	}
	
}
