package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.AsmCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.AssignCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.BlockCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.FuncCallCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.IfCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.StructFuncCallCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.VarDeclCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.WhileCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.CastVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class SimpleSourceFileParser extends SimpleExportFileParser {
	
	public SimpleSourceFileParser(InputStream in, String file, BiFunction<String, String, SimpleDependency> dep) {
		super(in, file, dep);
		
	}
	
	public SimpleSourceFileParser(SimpleTokenStream in, BiFunction<String, String, SimpleDependency> dep) {
		super(in, dep);
	}
	
	public SimpleFile parse(String runtimePath) {
		return (SimpleFile) super.parse(runtimePath, false);
	}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleDependency parse(String runtimePath, boolean isMeDep) {
		throw new UnsupportedOperationException("parse(String,boolean)");
	}
	
	@Override
	protected void parseDependency(SimpleFile sf) {
		this.in.consume();
		String name;
		if ( this.in.tok() == ME ) {
			this.in.consume();
			name = "<ME>";
		} else {
			expectToken(NAME, "expected to get `[NAME] [STRING] ;´ after `dep´");
			name = this.in.consumeDynTokSpecialText();
		}
		expectToken(STRING, "expected to get `[STRING] ;´ after `dep [NAME]´");
		String srcPath = this.in.consumeDynTokSpecialText();
		String binPath = null;
		if ( this.in.tok() != SEMI ) {
			expectToken(STRING, "expected to get `( [STRING] )? ;´ after `dep [NAME] [STRING]´");
			binPath = this.in.consumeDynTokSpecialText();
		}
		consumeToken(SEMI, "expected to get `;´ after `dep [NAME] [STRING]´");
		sf.dependency(this.dep.apply(srcPath, binPath), name, this.in.ctx());
	}
	
	@Override
	protected SimpleVariable parseSFScopeVariable(SimpleFile sf) {
		return parseAnyScopeVariable(sf);
	}
	
	@Override
	protected void parseFunction(SimpleFile sf) {
		this.in.consume();
		int flags = 0;
		if ( this.in.tok() != NAME ) {
			consumeToken(EXP, "expected `[NAME]´ or `exp [NAME]´ after `func´");
			expectToken(NAME, "expected `[NAME]´ after `func exp´");
			flags = FuncType.FLAG_EXPORT;
		}
		String       name = this.in.consumeDynTokSpecialText();
		ErrorContext ctx  = this.in.ctx();
		SimpleType   type = parseType(sf);
		if ( !( type instanceof FuncType ftype ) || ( ftype.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			ctx.setOffendingTokenCach(type.toString());
			throw new CompileError(ctx, "the [TYPE] of a function MUST be a function address type: " + type);
		}
		if ( flags != 0 ) {
			ftype = FuncType.create(ftype.resMembers(), ftype.argMembers(), flags, ctx);
		}
		BlockCmd       block = new BlockCmd(SimpleScope.newFuncScope(sf, ftype, ctx));
		SimpleFunction func  = new SimpleFunction(name, ftype, block);
		sf.function(func, ctx);
		parseCmdBlock(block);
	}
	
	private void parseCmdBlock(BlockCmd block) {
		consumeToken(BLOCK_OPEN, "expected `{´ to start the block command");
		while ( true ) {
			if ( this.in.tok() == BLOCK_CLOSE ) {
				this.in.consume();
				block.seal();
				return;
			}
			block.addCmd(parseCommand(block.currentAsParent()));
		}
	}
	
	private SimpleCommand parseCommand(SimpleScope scope) {
		return switch ( this.in.tok() ) {
		case BLOCK_OPEN -> {
			BlockCmd cmd = new BlockCmd(scope);
			parseCmdBlock(cmd);
			yield cmd;
		}
		case CONST -> parseCmdVarDecl(scope, SimpleVariable.FLAG_CONSTANT);
		case VAR -> parseCmdVarDecl(scope, 0);
		case CALL -> parseCmdCallFStruct(scope);
		case WHILE -> parseCmdWhile(scope);
		case IF -> parseCmdIf(scope);
		case ASM -> parseCmdAsm(scope);
		default -> parseCmdDefault(scope);
		};
	}
	
	protected record ValueOrType(SimpleValue value, SimpleType type) {}
	
	protected Object parseValueOrType(SimpleScope scope) {
		switch ( this.in.tok() ) {
		case NUM, UNUM, FPNUM, FPDWORD, DWORD, UDWORD, WORD, UWORD, BYTE, UBYTE, STRUCT, FSTRUCT, FUNC, NOPAD, LT:
			return parseType(scope);
		case STRING, CHARACTER, NUMBER, PLUS, MINUS, BIT_AND, BIT_NOT, BOOL_AND:
			return parseValue(scope);
		case SMALL_OPEN: {
			ErrorContext ctx = this.in.ctx();
			this.in.consume();
			if ( this.in.tok() == SMALL_CLOSE ) {
				return FuncType.create(List.of(), List.of(), FuncType.FLAG_FUNC_ADDRESS, ctx);
			}
			SimpleType type0 = parseType(scope);
			if ( this.in.tok() != NAME ) {
				SimpleValue unaryVal = parseValueUnaryExp(ctx, scope, 0, null);
				SimpleValue castVal  = CastVal.create(unaryVal, type0, ctx);
				return parseValue(scope, CAST_MAGIC, castVal);
			}
			String               name = this.in.consumeDynTokSpecialText();
			List<SimpleVariable> list = new ArrayList<>();
			list.add(new SimpleVariable(type0, name, null, 0));
			int t = this.in.consumeTok();
			switch ( t ) {
			case COMMA:
				if ( parseNamedTypeList(SMALL_CLOSE, COMMA, false, scope, list).isEmpty() ) {
					throw new CompileError(ctx, List.of("[TYPE]"),
						"expected `[TYPE] [NAME] (, [TYPE] [NAME])* \\)´ after `[TYPE] [NAME] ,´");
				}
				//$FALL-THROUGH$
			case SMALL_CLOSE:
				break;
			default:
				throw new CompileError(ctx, List.of("[TYPE]", ")"), "expected `[TYPE] | \\)´ after `[TYPE] [NAME]´");
			}
			return FuncType.create(List.of(), list, FuncType.FLAG_FUNC_ADDRESS, ctx);
		}
		case NAME: {
			ErrorContext ctx = this.in.ctx();
			String name = this.in.consumeDynTokSpecialText();
			Object typeOrDep = scope.nameTypeOrDepOrFuncOrNull(name, ctx);
			SimpleValue valOrDep = scope.nameValueOrNull(name, ctx);
			// TODO
		}
		// TODO
		}
	}
	
	private SimpleCommand parseCmdDefault(SimpleScope scope) {
		SimpleValue       val0    = parseValue(scope);
		List<SimpleValue> results = null;
		int               t       = this.in.consumeTok();
		switch ( t ) {
		case LARROW: {
			SimpleValue val1 = parseValue(scope);
			consumeToken(SEMI, "expected `;´ after `[VALUE] <-- [VALUE]´");
			return AssignCmd.create(scope, val0, val1, this.in.ctx());
		}
		case LT:
			if ( this.in.tok() != GT ) {
				results = parseCommaSepValues(scope);
			}
			consumeToken(GT,
				"expected `> <-- \\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call < ( [VALUE] ( , [VALUE] )* )?´");
			consumeToken(LARROW, "expected `<-- \\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call [FUNC_CALL_RESULT]´");
			consumeToken(SMALL_OPEN,
				"expected `\\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call [FUNC_CALL_RESULT] <--´");
			//$FALL-THROUGH$
		case SMALL_OPEN:
			List<SimpleValue> args = List.of();
			if ( this.in.tok() != SMALL_CLOSE ) {
				args = parseCommaSepValues(scope);
			}
			consumeToken(SMALL_CLOSE,
				"expected `\\)´ after `call ( [FUNC_CALL_RESULT] <-- )? \\( ( [VALUE] ( , [VALUE] )* )?´");
			consumeToken(SEMI, "expected `;´ after `call [VALUE]  ( [FUNC_CALL_RESULT] <-- )? [FUNC_CALL_ARGS]´");
			return FuncCallCmd.create(scope, val0, results, args, this.in.ctx());
		default:
			ErrorContext ctx = this.in.ctx();
			ctx.setOffendingTokenCach(name(t));
			throw new CompileError(ctx, List.of(name(LARROW), name(LT), name(SMALL_OPEN)));
		}
	}
	
	private SimpleCommand parseCmdVarDecl(SimpleScope scope, int flags) {
		SimpleType type = parseType(scope);
		expectToken(NAME, "expected `[NAME]´ after `( const | var ) [TYPE]´");
		String      name       = this.in.consumeDynTokSpecialText();
		SimpleValue initialVal = null;
		if ( flags != 0 ) { // the Constant flag is the only one permitted in this context
			assert flags == SimpleVariable.FLAG_CONSTANT;
			expectToken(LARROW, "expected `<-- [VALUE] ;´ after ´const [TYPE] [NAME]´");
			initialVal = parseValue(scope);
		} else if ( this.in.tok() == LARROW ) {
			this.in.consume();
			initialVal = parseValue(scope);
		}
		consumeToken(SEMI, "expected `;´ after `( const | var ) [TYPE] [NAME] ( <-- [VALUE] )?´");
		SimpleVariable sv = new SimpleVariable(type, name, initialVal, flags);
		return VarDeclCmd.create(scope, sv, this.in.ctx());
	}
	
	private SimpleCommand parseCmdAsm(SimpleScope scope) {
		List<AsmCmd.AsmParam> params = null;
		if ( this.in.tok() == STRING ) {
			params = new ArrayList<>();
			while ( true ) {
				String target = this.in.consumeDynTokSpecialText();
				consumeToken(LARROW, "expected `<--´ after `asm ( [STRING] <-- [VALUE] , )* [STRING]´");
				SimpleValue val = parseValue(scope);
				params.add(AsmCmd.AsmParam.create(target, val, this.in.ctx()));
				if ( this.in.tok() != COMMA ) break;
				this.in.consume();
				expectToken(STRING, "expected `[STRING]´ after `asm ( [STRING] <-- [VALUE] , )+");
			}
		}
		expectToken(ASM_BLOCK,
			"expected `[ASM_BLOCK]´ after `asm ( [STRING] <-- [VALUE] ( , [STRING] <-- [VALUE] )* )?´");
		String                 asmBlock = this.in.consumeDynTokSpecialText();
		List<AsmCmd.AsmResult> results  = null;
		if ( this.in.tok() != SEMI ) {
			results = new ArrayList<>();
			while ( true ) {
				SimpleValue target = parseValue(scope);
				consumeToken(LARROW,
					"expected `<--´ after `[ASM_BLOCK] ( [VALUE] <-- [STRING] ( , [VALUE] <-- [STRING] )* )? [VALUE]´");
				expectToken(STRING,
					"expected `[STRING]´ after `[ASM_BLOCK] ( [VALUE] <-- [STRING] ( , [VALUE] <-- [STRING] )* )? [VALUE] <--´");
				String source = this.in.consumeDynTokSpecialText();
				results.add(AsmCmd.AsmResult.create(target, source, this.in.ctx()));
				if ( this.in.tok() != COMMA ) break;
				this.in.consume();
			}
		}
		return AsmCmd.create(scope, params, asmBlock, results, this.in.ctx());
	}
	
	private SimpleCommand parseCmdIf(SimpleScope scope) {
		this.in.consume();
		consumeToken(SMALL_OPEN, "expected `(´ after `if´");
		SimpleValue cond = parseValue(scope);
		consumeToken(SMALL_CLOSE, "expected `)´ after `if ( [VALUE]´");
		SimpleCommand cmd     = parseCommand(scope);
		SimpleCommand elseCmd = null;
		if ( this.in.tok() == ELSE ) {
			this.in.consume();
			elseCmd = parseCommand(scope);
		}
		return IfCmd.create(scope, cond, cmd, elseCmd, this.in.ctx());
	}
	
	private SimpleCommand parseCmdWhile(SimpleScope scope) {
		this.in.consume();
		consumeToken(SMALL_OPEN, "expected `(´ after `while´");
		SimpleValue cond = parseValue(scope);
		consumeToken(SMALL_CLOSE, "expected `)´ after `while ( [VALUE]´");
		SimpleCommand cmd = parseCommand(scope);
		return WhileCmd.create(scope, cond, cmd, this.in.ctx());
	}
	
	private SimpleCommand parseCmdCallFStruct(SimpleScope scope) {
		SimpleValue func   = super.parseValue(scope);
		SimpleValue fstuct = super.parseValue(scope);
		consumeToken(SEMI, "expected `;´ after `call [VALUE] [VALUE]´");
		return StructFuncCallCmd.create(scope, func, fstuct, this.in.ctx());
	}
	
	private List<SimpleValue> parseCommaSepValues(SimpleScope scope) {
		List<SimpleValue> values = new ArrayList<>();
		while ( true ) {
			values.add(parseValue(scope));
			if ( this.in.tok() != COMMA ) return values;
			this.in.consume();
		}
	}
	
}
