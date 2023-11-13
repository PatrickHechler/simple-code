package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.ArrayType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.PointerType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.StructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class SimpleExportFileParser {
	
	private final SimpleTokenStream                          in;
	private final BiFunction<String,String,SimpleDependency> dep;
	
	public SimpleExportFileParser(InputStream in, String file, BiFunction<String,String,SimpleDependency> dep) {
		this(new SimpleTokenStream(in, file), dep);
	}
	
	public SimpleExportFileParser(SimpleTokenStream in, BiFunction<String,String,SimpleDependency> dep) {
		this.in = in;
		this.dep = dep;
	}
	
	
	public SimpleDependency parse(String runtimePath) {
		SimpleFile sf = new SimpleFile(runtimePath);
		while ( true ) {
			switch ( in.tok() ) {
			case DEP:
				parseDependency(sf);
				break;
			case TYPEDEF:
				parseTypedef(sf);
				break;
			case FUNC:
				parseFunction(sf);
				break;
			default:
				parseSFScopeVariable(sf);
			case EOF:
				return sf;
			}
		}
	}
	
	protected void parseDependency(SimpleFile sf) {
		in.consume();
		expectToken(NAME, "expected to get `[NAME] [STRING] ;´ after `dep´");
		String name = in.consumeDynTokSpecialText();
		expectToken(STRING, "expected to get `[STRING] ;´ after `dep [NAME]´");
		String path = in.consumeDynTokSpecialText();
		consumeToken(SEMI, "expected to get `;´ after `dep [NAME] [STRING]´");
		sf.dependency(dep.apply(name, path), name, in.ctx());
	}
	
	protected SimpleVariable parseSFScopeVariable(SimpleFile sf) {
		ErrorContext ctx = in.ctx();
		SimpleVariable sv = parseAnyScopeVariable(sf, ctx);
		if ( ( sv.flags() & SimpleVariable.FLAG_CONSTANT ) == 0 && sv.initialValue() != null ) {
			throw new CompileError(ctx, "a non constant value must not have a initial value set");
		}
		return sv;
	}
	
	protected SimpleVariable parseAnyScopeVariable(SimpleScope scope, ErrorContext ctx) {
		int flags = 0;
		switch ( in.tok() ) {
		case CONST:
			flags |= SimpleVariable.FLAG_CONSTANT;
			in.consume();
			if ( in.tok() != EXP ) break;
		case EXP:
			flags |= SimpleVariable.FLAG_EXPORT;
			in.consume();
		default:
		}
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after ´(const)? (exp)? [TYPE]´");
		String name = in.consumeDynTokSpecialText();
		SimpleValue initialValue = null;
		if ( in.tok() == LARROW ) {
			in.consume();
			initialValue = parseValue(scope);
		} else if ( ( flags & SimpleVariable.FLAG_CONSTANT ) != 0 ) {
			throw new CompileError(ctx, "a constant value must have a initial value set");
		}
		return new SimpleVariable(type, name, initialValue, flags);
	}
	
	private SimpleValue parseValue(SimpleScope scope) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected SimpleType parseType(SimpleScope scope) {
		ErrorContext ctx = in.ctx();
		SimpleType type = switch ( in.consumeTok() ) {
		case NUM -> NativeType.NUM;
		case UNUM -> NativeType.UNUM;
		case FPNUM -> NativeType.FPNUM;
		case FPDWORD -> NativeType.FPDWORD;
		case DWORD -> NativeType.DWORD;
		case UDWORD -> NativeType.UDWORD;
		case WORD -> NativeType.WORD;
		case UWORD -> NativeType.UWORD;
		case BYTE, CHAR -> NativeType.BYTE;
		case UBYTE -> NativeType.UBYTE;
		case NAME -> parseTypeTypedef(ctx, scope);
		case STRUCT -> parseTypeStruct(ctx, scope);
		case FSTRUCT -> parseTypeFStruct(ctx, scope, false);
		case FUNC -> parseTypeFStruct(ctx, scope, true);
		case NOPAD -> parseTypeFuncType0(NOPAD, ctx, scope);
		case LT -> parseTypeFuncType0(LT, ctx, scope);
		case SMALL_OPEN -> parseTypeFuncType0(SMALL_OPEN, ctx, scope);
		default -> throw new CompileError(ctx,
			List.of(name(NUM), name(UNUM), name(FPNUM), name(FPDWORD), name(DWORD), name(WORD), name(UWORD), name(BYTE),
				name(CHAR), name(UBYTE), name(STRUCT), name(FSTRUCT), name(NOPAD), name(LT), name(SMALL_OPEN)));
		};
		while ( true ) {
			switch ( in.tok() ) {
			case DIAMOND:
				type = PointerType.create(type, in.ctx());
			case ARR_OPEN:
				in.consume();
				SimpleValue len;
				if ( in.tok() == ARR_CLOSE ) {
					in.consume();
					len = null;
				} else {
					len = parseValue(scope);
					consumeToken(ARR_CLOSE, "expected `\\]´ after `\\[ [VALUE]");
				}
				type = ArrayType.create(type, len, in.ctx());
			}
		}
	}
	
	private SimpleType parseTypeTypedef(ErrorContext ctx, SimpleScope scope) {
		while ( true ) {
			Object obj = scope.nameTypeOrDepOrFuncOrNull(in.dynTokSpecialText(), ctx);
			if ( obj instanceof SimpleType t ) return t;
			if ( !( obj instanceof SimpleDependency dep ) ) throw new CompileError(in.ctx(),
				"expected `[NAME]´ to be a typedef'ed [NAME] or the [NAME] of a dependency (to use a function as a type use `(fstruct | fadr) [NAME]´");
			scope = (SimpleDependency) obj;
			consumeToken(COLON, "expected `:[NAME]´ after a dependency `[NAME]´");
			consumeToken(NAME, "expected `[NAME]´ after a dependency `[NAME]:´");
		}
	}
	
	protected SimpleType parseTypeStruct(ErrorContext ctx, SimpleScope scope) {
		ctx = ctx.copy();
		int flags = 0;
		int t = in.consumeTok();
		if ( t == NOPAD ) {
			flags = StructType.FLAG_NOPAD;
			t = in.consumeTok();
		}
		consumeToken(BLOCK_OPEN, "expected `{´ after `struct (nopad)?´");
		return StructType.create(parseNamedTypeList(BLOCK_CLOSE, scope), flags, ctx);
	}
	
	protected List<SimpleVariable> parseNamedTypeList(final int end, SimpleScope scope) {
		if ( in.tok() == end ) {
			in.consume();
			return List.of();
		}
		List<SimpleVariable> members = new ArrayList<>();
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after `[TYPE]´");
		String name = in.consumeDynTokSpecialText();
		members.add(new SimpleVariable(type, name, null, 0));
		while ( true ) {
			int t = in.consumeTok();
			if ( t != COMMA ) {
				if ( t != end ) {
					throw new CompileError(in.ctx(), List.of(name(end)),
						"expected to end the [NAMED_TYPE_LIST] with the given token");
				}
				return members;
			}
			type = parseType(scope);
			expectToken(NAME, "expected `[NAME]´ after `[TYPE]´");
			name = in.consumeDynTokSpecialText();
			members.add(new SimpleVariable(type, name, null, 0));
		}
	}
	
	protected SimpleType parseTypeFStruct(ErrorContext ctx, SimpleScope scope, boolean address) {
		while ( true ) {
			Object obj = scope.nameTypeOrDepOrFuncOrNull(in.dynTokSpecialText(), ctx);
			if ( obj instanceof SimpleFunction t ) return address ? t.type.asFAdr() : t.type;
			if ( !( obj instanceof SimpleDependency dep ) ) throw new CompileError(in.ctx(),
				"expected `[NAME]´ to be a typedef'ed [NAME] or the [NAME] of a dependency (to use a function as a type use `(fstruct | fadr) [NAME]´");
			scope = (SimpleDependency) obj;
			consumeToken(COLON, "expected `:[NAME]´ after a dependency `[NAME]´");
			consumeToken(NAME, "expected `[NAME]´ after a dependency, colon `[NAME]:´");
		}
	}
	
	protected SimpleType parseTypeFuncType0(int t, ErrorContext ctx, SimpleScope scope) {
		int flags = 0;
		List<SimpleVariable> results = List.of();
		switch ( t ) {
		case NOPAD:
			flags |= FuncType.FLAG_NOPAD;
			t = in.consumeTok();
			if ( t != LT ) {
				if ( t == SMALL_OPEN ) break;
				throw new CompileError(ctx, List.of(name(LT), name(SMALL_OPEN)),
					"expected `< [NAMED_TYPE_LIST] > <--´ or `\\( [NAMED_TYPE_LIST] \\)´ after `nopad´");
			}
		case LT:
			results = parseNamedTypeList(GT, scope);
			consumeToken(LARROW, "expectedd `<-- \\( [NAMED_TYPE_LIST] \\)´ after `(nopad)? < [NAMED_TYPE_LIST] >´");
			consumeToken(SMALL_OPEN,
				"expectedd `\\( [NAMED_TYPE_LIST] \\)´ after `(nopad)? < [NAMED_TYPE_LIST] > <--´");
		case SMALL_OPEN:
			break;
		default:
			throw new AssertionError("invalid token passed to parseTypeFuncType0: " + t + " : " + name(t));
		}
		List<SimpleVariable> args = parseNamedTypeList(SMALL_CLOSE, scope);
		return FuncType.create(results, args, flags, ctx);
	}
	
	protected void parseTypedef(SimpleFile sf) {
		// TODO Auto-generated method stub
	}
	
	protected void parseFunction(SimpleFile sf) {
		// TODO Auto-generated method stub
	}
	
	protected void expectToken(int tok, String msg) {
		if ( in.tok() != tok ) {
			throw new CompileError(in.ctx(), List.of(name(tok)), msg);
		}
	}
	
	protected void consumeToken(int tok, String msg) {
		if ( in.consumeTok() != tok ) {
			// do not use consume()! the code relies on the dynToks special Text to not be consumed
			throw new CompileError(in.ctx(), List.of(name(tok)), msg);
		}
	}
	
}
