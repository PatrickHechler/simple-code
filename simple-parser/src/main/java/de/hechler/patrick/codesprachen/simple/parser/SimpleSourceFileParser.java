package de.hechler.patrick.codesprachen.simple.parser;

import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.ASM;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.ASM_BLOCK;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.BIT_AND;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.BIT_NOT;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.BLOCK_CLOSE;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.BLOCK_OPEN;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.BOOL_AND;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.BYTE;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.CALL;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.CHARACTER;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.COLON;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.COMMA;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.CONST;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.DWORD;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.ELSE;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.EXP;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.FPDWORD;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.FPNUM;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.FSTRUCT;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.FUNC;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.GT;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.IF;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.INIT;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.LARROW;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.LT;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.MAIN;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.ME;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.MINUS;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.NOPAD;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.NUM;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.NUMBER;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.PLUS;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.QUESTION;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.SEMI;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.SMALL_CLOSE;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.SMALL_OPEN;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.STRING;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.STRUCT;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.UBYTE;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.UDWORD;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.UNUM;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.UWORD;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.WHILE;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.WORD;
import static de.hechler.patrick.codesprachen.simple.parser.SimpleTokenStream.name;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.codesprachen.simple.parser.error.CompileError;
import de.hechler.patrick.codesprachen.simple.parser.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.AsmCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.AssignCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.BlockCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.FuncCallCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.IfCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.StructFuncCallCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.VarDeclCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.cmd.WhileCmd;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.CastVal;
import de.hechler.patrick.codesprachen.simple.parser.objects.value.SimpleValue;

public class SimpleSourceFileParser extends SimpleExportFileParser {
	
	public SimpleSourceFileParser(InputStream in, String file, BiFunction<String, String, SimpleDependency> dep) {
		super(in, file, dep);
		
	}
	
	public SimpleSourceFileParser(SimpleTokenStream in, BiFunction<String, String, SimpleDependency> dep) {
		super(in, dep);
	}
	
	public SimpleFile parse(String runtimePath) {
		SimpleFile sf = new SimpleFile(runtimePath);
		parse(sf);
		return sf;
	}
	
	public void parse(SimpleFile sf) {
		super.parse(sf, false);
	}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleDependency parse(String runtimePath, boolean isMeDep) {
		throw new UnsupportedOperationException("parse(String,boolean)");
	}
	
	@Override
	@SuppressWarnings("unused")
	public void parse(SimpleFile sf, boolean isMeDep) {
		throw new UnsupportedOperationException("parse(SimpleFile,boolean)");
	}
	
	@Override
	protected void parseDependency(SimpleFile sf) {
		this.in.consume();
		String name;
		if ( this.in.tok() == ME ) {
			this.in.consume();
			name = null;
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
		int flags = FuncType.FLAG_FUNC_ADDRESS;
		String name;
		if ( this.in.tok() == NAME ) {
			name = this.in.consumeDynTokSpecialText();
		} else if ( this.in.tok() == EXP ) {
			flags |= FuncType.FLAG_EXPORT;
			consumeToken(EXP, "expected `[NAME]´ or `exp [NAME]´ after `func´");
			if ( this.in.tok() == INIT ) {
				this.in.consume();
				flags |= FuncType.FLAG_INIT;
			} else if ( this.in.tok() == MAIN ) {
				this.in.consume();
				flags |= FuncType.FLAG_MAIN;
			}
			expectToken(NAME, "expected `[NAME]´ after `func exp´");
			name = this.in.consumeDynTokSpecialText();
		} else if ( this.in.tok() == INIT ) {
			this.in.consume();
			name = null;
			flags |= FuncType.FLAG_INIT;
		} else if ( this.in.tok() == MAIN ) {
			this.in.consume();
			name = null;
			flags |= FuncType.FLAG_MAIN;
		} else {
			throw new CompileError(this.in.ctx(), List.of(name(NAME), name(EXP), name(INIT), name(MAIN)),
				"expected `exp (main | init)? [NAME] | (main | init) [NAME]? | [NAME]´ after `func´");
		}
		ErrorContext ctx = this.in.ctx();
		SimpleType type = parseType(sf);
		if ( !( type instanceof FuncType ftype ) || ( ftype.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			ctx.setOffendingTokenCach(type.toString());
			throw new CompileError(ctx, "the [TYPE] of a function MUST be a function address type: " + type);
		}
		ftype = FuncType.create(ftype.resMembers(), ftype.argMembers(), flags, ctx);
		BlockCmd block = new BlockCmd(SimpleScope.newFuncScope(sf, ftype, ctx));
		SimpleFunction func = new SimpleFunction(sf, name, ftype, block);
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
		case CONST -> parseCmdConstDecl(scope);
		case CALL -> parseCmdCall(scope);
		case WHILE -> parseCmdWhile(scope);
		case IF -> parseCmdIf(scope);
		case ASM -> parseCmdAsm(scope);
		default -> parseCmdDefault(scope);
		};
	}
	
	protected Object parseValueOrType(SimpleScope scope) {
		switch ( this.in.tok() ) {
		case NUM, UNUM, FPNUM, FPDWORD, DWORD, UDWORD, WORD, UWORD, BYTE, UBYTE, STRUCT, FSTRUCT, FUNC, NOPAD, LT:
			return parseType(scope);
		case STRING, CHARACTER, NUMBER, PLUS, MINUS, BIT_AND, BIT_NOT, BOOL_AND:
			return parseValue(scope);
		case SMALL_OPEN: {
			this.in.consume();
			if ( this.in.tok() == SMALL_CLOSE ) {
				return FuncType.create(List.of(), List.of(), FuncType.FLAG_FUNC_ADDRESS, this.in.ctx());
			}
			SimpleType type0 = parseType(scope);
			if ( this.in.tok() != NAME ) {
				SimpleValue unaryVal = parseValueUnaryExp(scope, 0, null);
				SimpleValue castVal = CastVal.create(unaryVal, type0, this.in.ctx());
				return parseValue(scope, CAST_MAGIC, castVal);
			}
			String name = this.in.consumeDynTokSpecialText();
			List<SimpleVariable> list = new ArrayList<>();
			list.add(new SimpleVariable(type0, name, null, 0));
			int t = this.in.consumeTok();
			switch ( t ) {
			case COMMA:
				if ( parseNamedTypeList(SMALL_CLOSE, COMMA, false, scope, list).isEmpty() ) {
					throw new CompileError(this.in.ctx(), List.of("[TYPE]"),
						"expected `[TYPE] [NAME] (, [TYPE] [NAME])* \\)´ after `[TYPE] [NAME] ,´");
				}
				//$FALL-THROUGH$
			case SMALL_CLOSE:
				break;
			default:
				throw new CompileError(this.in.ctx(), List.of("[TYPE]", ")"),
					"expected `[TYPE] | \\)´ after `[TYPE] [NAME]´");
			}
			return FuncType.create(List.of(), list, FuncType.FLAG_FUNC_ADDRESS, this.in.ctx());
		}
		case NAME: {
			while ( true ) {
				String name = this.in.consumeDynTokSpecialText();
				Object typeOrDep = scope.nameTypeOrDepOrFuncOrNull(name, this.in.ctx());
				switch ( typeOrDep ) {
				case SimpleDependency dep -> {
					scope = dep;
					consumeToken(COLON, "expected `: [NAME]´ after the `[NAME]´ of a dependency");
					expectToken(NAME, "expected `[NAME]´ after `[NAME] :´ where the [NAME] is of a dependency");
					continue;
				}
				case SimpleType type -> {
					return parseTypePostfix(scope, type);
				}
				case null, default -> {
					SimpleValue value = scope.nameValueOrErr(name, this.in.ctx());
					// Technically this is no direct expression after the first iteration
					// but the start (or complete) post-fix expression
					return parseValue(scope, DIRECT_MAGIC, value);
				}
				}
			}
		}
		default:
			throw new CompileError(this.in.ctx(),
				List.of(name(NUM), name(UNUM), name(FPNUM), name(FPDWORD), name(DWORD), name(UDWORD), name(WORD),
					name(UWORD), name(BYTE), name(UBYTE), name(STRUCT), name(FSTRUCT), name(FUNC), name(NOPAD),
					name(LT), name(STRING), name(CHARACTER), name(NUMBER), name(PLUS), name(MINUS), name(BIT_AND),
					name(BIT_NOT), name(BOOL_AND), name(SMALL_OPEN), name(NAME)),
				"expected a `[TYPE]´ or a `[VALUE]´");
		}
	}
	
	private SimpleCommand parseCmdDefault(SimpleScope scope) {
		Object obj = parseValueOrType(scope);
		if ( obj instanceof SimpleValue val0 ) {
			consumeToken(LARROW, "expected `<-- [VALUE] ;´ after `[VALUE]´");
			SimpleValue val1 = parseValue(scope);
			consumeToken(SEMI, "expected `;´ after `[VALUE] <-- [VALUE]´");
			return AssignCmd.create(scope, val0, val1, this.in.ctx());
		}
		SimpleType type = (SimpleType) obj;
		return parseCmdVarDecl0(scope, 0, type);
	}
	
	private SimpleCommand parseCmdConstDecl(SimpleScope scope) {
		SimpleType type = parseType(scope);
		return parseCmdVarDecl0(scope, SimpleVariable.FLAG_CONSTANT, type);
	}
	
	private SimpleCommand parseCmdVarDecl0(SimpleScope scope, int flags, SimpleType type) {
		expectToken(NAME, "expected `[NAME]´ after `( const | var ) [TYPE]´");
		String name = this.in.consumeDynTokSpecialText();
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
		String asmBlock = this.in.consumeDynTokSpecialText();
		List<AsmCmd.AsmResult> results = null;
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
		SimpleCommand cmd = parseCommand(scope);
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
	
	private SimpleCommand parseCmdCall(SimpleScope scope) {
		this.in.consume();
		SimpleValue func = super.parseValueShiftExp(scope, 0, null);
		if ( this.in.tok() != LT && this.in.tok() != SMALL_OPEN ) {
			SimpleValue fstuct = super.parseValue(scope);
			consumeToken(SEMI, "expected `;´ after `call [VALUE] [VALUE]´");
			return StructFuncCallCmd.create(scope, func, fstuct, this.in.ctx());
		}
		List<SimpleValue> results = List.of();
		if ( this.in.consumeTok() == LT ) {
			if ( this.in.tok() != GT ) {
				results = parseCommaSepValues(scope, true);
			}
			consumeToken(GT,
				"expected `> <-- \\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call < ( [VALUE] ( , [VALUE] )* )?´");
			consumeToken(LARROW, "expected `<-- \\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call [FUNC_CALL_RESULT]´");
			consumeToken(SMALL_OPEN,
				"expected `\\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call [FUNC_CALL_RESULT] <--´");
		}
		List<SimpleValue> args = List.of();
		if ( this.in.tok() != SMALL_CLOSE ) {
			args = parseCommaSepValues(scope, false);
		}
		consumeToken(SMALL_CLOSE,
			"expected `\\)´ after `call ( [FUNC_CALL_RESULT] <-- )? \\( ( [VALUE] ( , [VALUE] )* )?´");
		consumeToken(SEMI, "expected `;´ after `call [VALUE]  ( [FUNC_CALL_RESULT] <-- )? [FUNC_CALL_ARGS]´");
		return FuncCallCmd.create(scope, func, results, args, this.in.ctx());
	}
	
	private List<SimpleValue> parseCommaSepValues(SimpleScope scope, boolean allowIgnoreValuesAndUseShiftExp) {
		List<SimpleValue> values = new ArrayList<>();
		while ( true ) {
			if ( allowIgnoreValuesAndUseShiftExp ) {
				if ( this.in.tok() == QUESTION ) values.add(null);
				else values.add(parseValueShiftExp(scope, 0, null));
			} else values.add(parseValue(scope));
			if ( this.in.tok() != COMMA ) return values;
			this.in.consume();
		}
	}
	
}
