package de.hechler.patrick.code.simple.parser;

import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ASM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ASM_BLOCK;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BIT_AND;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BIT_NOT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BLOCK_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BLOCK_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BOOL_AND;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BYTE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CALL;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CHARACTER;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.COLON;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.COMMA;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CONST;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.DWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ELSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.EXP;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FPDWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FPNUM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FSTRUCT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.FUNC;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.GT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.IF;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.INIT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LARROW;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.MAIN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ME;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.MINUS;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NOPAD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NUM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NUMBER;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.PLUS;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.QUESTION;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SEMI;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SMALL_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SMALL_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.STRING;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.STRUCT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UBYTE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UDWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UNUM;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.UWORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.WHILE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.WORD;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.name;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.cmd.AsmCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.AssignCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.BlockCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.FuncCallCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.IfCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.SimpleCommand;
import de.hechler.patrick.code.simple.parser.objects.cmd.StructFuncCallCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.VarDeclCmd;
import de.hechler.patrick.code.simple.parser.objects.cmd.WhileCmd;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.value.CastVal;
import de.hechler.patrick.code.simple.parser.objects.value.FunctionVal;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class SimpleSourceFileParser extends SimpleExportFileParser {
	
	public static final int STATE_EX_CODE                   = 30;
	public static final int STATE_EX_CODE_BLOCK             = 31;
	public static final int STATE_EX_CODE_VAR_DECL          = 32;
	public static final int STATE_EX_CODE_ASSIGN_CMD        = 33;
	public static final int STATE_EX_CODE_CALL_FUNC         = 34;
	public static final int STATE_EX_CODE_CALL_FSTRUCT      = 35;
	public static final int STATE_EX_CODE_WHILE             = 36;
	public static final int STATE_EX_CODE_IF                = 37;
	public static final int STATE_EX_CODE_ASM               = 38;
	public static final int STATE_EX_CODE_ASM_PARAM_PAIR    = 39;
	public static final int STATE_EX_CODE_ASM_RESULT_PAIR   = 40;
	public static final int STATE_EX_CODE_ASM_RESULT_IGNORE = 41;
	public static final int STATE_EX_VALUE_LIST             = 42;
	
	public SimpleSourceFileParser(InputStream in, String file, BiFunction<String,String,SimpleDependency> dep) {
		super(in, file, dep);
		
	}
	
	public SimpleSourceFileParser(SimpleTokenStream in, BiFunction<String,String,SimpleDependency> dep) {
		super(in, dep);
	}
	
	@Override
	public void parse(SimpleFile sf) {
		super.parse(sf);
	}
	
	@Override
	protected void parseDependency(SimpleFile sf) {
		final Object enter = enterState(STATE_DEPENDENCY);
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
		SimpleDependency dependency = this.dep.apply(srcPath, binPath);
		if ( dependency == null ) {
			throw new CompileError(this.in.ctx(), "could not find the dependency \"" + srcPath + "\""
				+ ( binPath == null ? "" : " \"" + binPath + "\"" ));
		}
		sf.dependency(dependency, name, this.in.ctx());
		exitState(STATE_DEPENDENCY, enter, name == null ? dependency : name);
	}
	
	@Override
	protected SimpleVariable parseSFScopeVariable(SimpleFile sf) {
		return parseAnyScopeVariable(sf);
	}
	
	@Override
	protected void parseFunction(SimpleFile sf) {
		final Object enter = enterState(STATE_FUNCTION);
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
		exitState(STATE_FUNCTION, enter, func);
	}
	
	private void parseCmdBlock(BlockCmd block) {
		final Object enter = enterState(STATE_EX_CODE_BLOCK);
		consumeToken(BLOCK_OPEN, "expected `{´ to start the block command");
		while ( true ) {
			if ( this.in.tok() == BLOCK_CLOSE ) {
				this.in.consume();
				block.seal();
				exitState(STATE_EX_CODE_BLOCK, enter, block);
				return;
			}
			block.addCmd(parseCommand(block.currentAsParent()));
		}
	}
	
	private SimpleCommand parseCommand(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE);
		SimpleCommand res = switch ( this.in.tok() ) {
		case BLOCK_OPEN -> {
			BlockCmd cmd = new BlockCmd(scope);
			parseCmdBlock(cmd);
			yield cmd;
		}
		case CONST -> parseCmdConstDecl(scope);
		case CALL -> parseCmdCallFStruct(scope);
		case WHILE -> parseCmdWhile(scope);
		case IF -> parseCmdIf(scope);
		case ASM -> parseCmdAsm(scope);
		default -> parseCmdDefault(scope);
		};
		exitState(STATE_EX_CODE, enter, res);
		return res;
	}
	
	protected Object parseShiftExpOrType(SimpleScope scope) {
		switch ( this.in.tok() ) {
		case NUM, UNUM, FPNUM, FPDWORD, DWORD, UDWORD, WORD, UWORD, BYTE, UBYTE, STRUCT, FSTRUCT, FUNC, NOPAD, LT:
			return parseType(scope);
		case STRING, CHARACTER, NUMBER, PLUS, MINUS, BIT_AND, BIT_NOT, BOOL_AND:
			return parseValueShiftExp(scope, 0, null, null);
		case SMALL_OPEN: {
			final Object undecided = enterUnknownState();
			this.in.consume();
			if ( this.in.tok() == SMALL_CLOSE ) {
				final Object enter = decidedState(STATE_TYPE_FUNC_ADDR, undecided);
				this.in.consume();
				FuncType res = FuncType.create(List.of(), List.of(), FuncType.FLAG_FUNC_ADDRESS, this.in.ctx());
				exitState(STATE_TYPE_FUNC_ADDR, enter, res);
				return res;
			}
			SimpleType type0 = parseType(scope);
			if ( this.in.tok() != NAME ) {
				int[] decidedStates = new int[COND_MAGIC + 2 - ( CAST_MAGIC - 1 )];
				for (int i = 0; i < COND_MAGIC + 2 - ( CAST_MAGIC - 1 ); i++) {
					decidedStates[i] = STATE_VAL_CAST + i;
				}
				Object[] arr = decidedStates(decidedStates, scope);
				SimpleValue unaryVal = parseValueUnaryExp(scope, 0, null, null);
				SimpleValue castVal = CastVal.create(unaryVal, type0, this.in.ctx());
				exitState(STATE_VAL_CAST, arr[0], castVal);
				return parseValue(scope, CAST_MAGIC, castVal, arr);
			}
			final Object[] enters = decidedStates(new int[]{ STATE_TYPE_FUNC_ADDR, STATE_TYPE }, undecided);
			final Object subEnter = enterState(STATE_NAMED_TYPE_LIST);
			String name = this.in.consumeDynTokSpecialText();
			List<SimpleVariable> list = new ArrayList<>();
			list.add(new SimpleVariable(type0, name, null, 0));
			switch ( this.in.tok() ) {
			case COMMA:
				this.in.consume();
				if ( parseNamedTypeList(SMALL_CLOSE, COMMA, false, scope, list, subEnter).isEmpty() ) {
					throw new CompileError(this.in.ctx(), List.of("[TYPE]"),
						"expected `[TYPE] [NAME] (, [TYPE] [NAME])* \\)´ after `[TYPE] [NAME] ,´");
				}
				break;
			case SMALL_CLOSE:
				exitState(STATE_NAMED_TYPE_LIST, subEnter, list);
				this.in.consume();
				break;
			default:
				throw new CompileError(this.in.ctx(), List.of(name(COMMA), name(SMALL_CLOSE)),
					"expected `, | \\)´ after `[TYPE] [NAME]´");
			}
			FuncType ftype = FuncType.create(List.of(), list, FuncType.FLAG_FUNC_ADDRESS, this.in.ctx());
			exitState(STATE_TYPE_FUNC_ADDR, enters == null ? null : enters[0], ftype);
			return parseTypePostfix(scope, ftype, enters == null ? null : enters[1]);
		}
		case NAME: {
			final Object undecided = enterUnknownState();
			Object firstEnd = null;
			boolean hasMid = false;
			while ( true ) {
				String name = this.in.consumeDynTokSpecialText();
				Object typeOrDep = scope.nameTypeOrDepOrFuncOrNull(name, this.in.ctx());
				switch ( typeOrDep ) {
				case SimpleDependency dep -> {
					scope = dep;
					if ( !hasMid ) {
						hasMid = true;
						firstEnd = maybeFinishUnknownState();
					}
					consumeToken(COLON, "expected `: [NAME]´ after the `[NAME]´ of a dependency");
					expectToken(NAME, "expected `[NAME]´ after `[NAME] :´ where the [NAME] is of a dependency");
					continue;
				}
				case SimpleType type -> {
					Object[] enters = decidedStates(new int[]{ STATE_TYPE_TYPEDEFED_TYPE, STATE_TYPE }, undecided);
					exitState(STATE_TYPE_TYPEDEFED_TYPE, enters == null ? null : enters[0], type);
					return parseTypePostfix(scope, type, enters == null ? null : enters[1]);
				}
				case null, default -> {
					int[] decidedStates = new int[COND_MAGIC + 1];
					for (int i = 0; i < COND_MAGIC + 1; i++) {
						decidedStates[i] = STATE_VAL_DIRECT + i;
					}
					Object[] enters = decidedStates(decidedStates, undecided);
					SimpleValue value = scope.nameValueOrErr(name, this.in.ctx());
					if ( hasMid ) {
						remenberExitedState(STATE_VAL_DIRECT, enters == null ? null : enters[0], firstEnd, value);
					} else {
						exitState(STATE_VAL_DIRECT, enters == null ? null : enters[0], value);
					}
					return parseValueShiftExp(scope, DIRECT_MAGIC, value, enters);
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
		final Object undecided = enterUnknownState();
		Object obj = parseShiftExpOrType(scope);
		if ( obj instanceof FunctionVal func ) {
			final Object enter = decidedState(STATE_EX_CODE_CALL_FUNC, undecided);
			return parseCmdNoStructFuncCall(scope, func, enter);
		}
		if ( obj instanceof SimpleValue val0 ) {
			final Object enter = decidedState(STATE_EX_CODE_ASSIGN_CMD, undecided);
			consumeToken(LARROW, "expected `<-- [VALUE] ;´ after `[VALUE]´");
			SimpleValue val1 = parseValue(scope);
			consumeToken(SEMI, "expected `;´ after `[VALUE] <-- [VALUE]´");
			AssignCmd res = AssignCmd.create(scope, val0, val1, this.in.ctx());
			exitState(STATE_EX_CODE_ASSIGN_CMD, enter, res);
			return res;
		}
		final Object enter = decidedState(STATE_EX_CODE_VAR_DECL, undecided);
		SimpleType type = (SimpleType) obj;
		return parseCmdVarDecl0(scope, 0, type, enter);
	}
	
	private SimpleCommand parseCmdNoStructFuncCall(SimpleScope scope, FunctionVal func, Object enter) {
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
		FuncCallCmd res = FuncCallCmd.create(scope, func, results, args, this.in.ctx());
		exitState(STATE_EX_CODE_CALL_FUNC, enter, res);
		return res;
	}
	
	private List<SimpleValue> parseCommaSepValues(SimpleScope scope, boolean allowIgnoreValuesAndUseShiftExp) {
		final Object enter = enterState(STATE_EX_VALUE_LIST);
		List<SimpleValue> values = new ArrayList<>();
		while ( true ) {
			if ( allowIgnoreValuesAndUseShiftExp ) {
				if ( this.in.tok() == QUESTION ) {
					this.in.consume();
					values.add(null);
				} else {
					values.add(parseValueShiftExp(scope, 0, null, null));
				}
			} else values.add(parseValue(scope));
			if ( this.in.tok() != COMMA ) {
				exitState(STATE_EX_VALUE_LIST, enter, values);
				return values;
			}
			this.in.consume();
		}
	}
	
	private SimpleCommand parseCmdConstDecl(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_VAR_DECL);
		SimpleType type = parseType(scope);
		return parseCmdVarDecl0(scope, SimpleVariable.FLAG_CONSTANT, type, enter);
	}
	
	private SimpleCommand parseCmdVarDecl0(SimpleScope scope, int flags, SimpleType type, Object enter) {
		expectToken(NAME, "expected `[NAME]´ after `( const )? [TYPE]´");
		String name;
		try {
			name = this.in.consumeDynTokSpecialText();
		} catch ( @SuppressWarnings("unused") AssertionError ae ) {
			name = name(this.in.consumeTok());
		}
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
		VarDeclCmd res = VarDeclCmd.create(scope, sv, this.in.ctx());
		exitState(STATE_EX_CODE_VAR_DECL, enter, res);
		return res;
	}
	
	private SimpleCommand parseCmdAsm(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_ASM);
		List<AsmCmd.AsmParam> params = List.of();
		if ( this.in.tok() == STRING ) {
			params = new ArrayList<>();
			while ( true ) {
				addAsmParamPair(scope, params);
				if ( this.in.tok() != COMMA ) break;
				this.in.consume();
				expectToken(STRING, "expected `[STRING]´ after `asm ( [STRING] <-- [VALUE] , )+");
			}
		}
		expectToken(ASM_BLOCK,
			"expected `[ASM_BLOCK]´ after `asm ( [STRING] <-- [VALUE] ( , [STRING] <-- [VALUE] )* )?´");
		String asmBlock = this.in.consumeDynTokSpecialText();
		List<AsmCmd.AsmResult> results = List.of();
		if ( this.in.tok() != SEMI ) {
			results = new ArrayList<>();
			while ( true ) {
				if ( this.in.tok() == STRING ) {
					addAsmCorruptResult(results);
				} else {
					addAsmResultPair(scope, results);
				}
				if ( this.in.tok() != COMMA ) break;
				this.in.consume();
			}
		}
		consumeToken(SEMI, "expected `;´ after `asm [ASM_PARAMS] [ASM_BLOCK] [ASM_RESULTS]´");
		AsmCmd res = AsmCmd.create(scope, params, asmBlock, results, this.in.ctx());
		exitState(STATE_EX_CODE_ASM, enter, res);
		return res;
	}
	
	private void addAsmParamPair(SimpleScope scope, List<AsmCmd.AsmParam> params) {
		final Object subEnter = enterState(STATE_EX_CODE_ASM_PARAM_PAIR);
		String target = this.in.consumeDynTokSpecialText();
		consumeToken(LARROW, "expected `<--´ after `asm ( [STRING] <-- [VALUE] , )* [STRING]´");
		SimpleValue val = parseValue(scope);
		AsmCmd.AsmParam asmParam = AsmCmd.AsmParam.create(target, val, this.in.ctx());
		params.add(asmParam);
		exitState(STATE_EX_CODE_ASM_PARAM_PAIR, subEnter, asmParam);
	}
	
	private void addAsmCorruptResult(List<AsmCmd.AsmResult> results) {
		final Object subEnter = enterState(STATE_EX_CODE_ASM_RESULT_IGNORE);
		String ignoreReg = this.in.consumeDynTokSpecialText();
		consumeToken(LARROW,
			"expected `<-- \\?´ after `[ASM_BLOCK] ( ( [VALUE] <-- [STRING] | [STRING] <-- \\? ) ( , [VALUE] <-- [STRING] | [STRING] <-- \\? )* )? [STRING]´");
		expectToken(QUESTION,
			"expected `\\?´ after `[ASM_BLOCK] ( [VALUE] <-- [STRING] ( , [VALUE] <-- [STRING] )* )? [VALUE] <--´");
		AsmCmd.AsmResult asmRes = AsmCmd.AsmResult.create(ignoreReg, this.in.ctx());
		results.add(asmRes);
		exitState(STATE_EX_CODE_ASM_RESULT_IGNORE, subEnter, asmRes);
	}
	
	private void addAsmResultPair(SimpleScope scope, List<AsmCmd.AsmResult> results) {
		final Object subEnter = enterState(STATE_EX_CODE_ASM_RESULT_PAIR);
		SimpleValue target = parseValue(scope);
		consumeToken(LARROW,
			"expected `<-- ?´ after `[ASM_BLOCK] ( ( [VALUE] <-- [STRING] | [STRING] <-- \\? ) ( , [VALUE] <-- [STRING] | [STRING] <-- \\? )* )? [VALUE]´");
		expectToken(STRING,
			"expected `<-- ?´ after `[ASM_BLOCK] ( ( [VALUE] <-- [STRING] | [STRING] <-- \\? ) ( , [VALUE] <-- [STRING] | [STRING] <-- \\? )* )? [VALUE] <--´");
		String source = this.in.consumeDynTokSpecialText();
		AsmCmd.AsmResult asmRes = AsmCmd.AsmResult.create(target, source, this.in.ctx());
		results.add(asmRes);
		exitState(STATE_EX_CODE_ASM_RESULT_PAIR, subEnter, asmRes);
	}
	
	private SimpleCommand parseCmdIf(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_WHILE);
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
		IfCmd res = IfCmd.create(scope, cond, cmd, elseCmd, this.in.ctx());
		exitState(STATE_EX_CODE_IF, enter, res);
		return res;
	}
	
	private SimpleCommand parseCmdWhile(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_WHILE);
		this.in.consume();
		consumeToken(SMALL_OPEN, "expected `(´ after `while´");
		SimpleValue cond = parseValue(scope);
		consumeToken(SMALL_CLOSE, "expected `)´ after `while ( [VALUE]´");
		SimpleCommand cmd = parseCommand(scope);
		WhileCmd res = WhileCmd.create(scope, cond, cmd, this.in.ctx());
		exitState(STATE_EX_CODE_WHILE, enter, res);
		return res;
	}
	
	private SimpleCommand parseCmdCallFStruct(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_CALL_FSTRUCT);
		this.in.consume();
		SimpleValue func = super.parseValueShiftExp(scope, 0, null, null);
		SimpleValue fstuct = super.parseValue(scope);
		consumeToken(SEMI, "expected `;´ after `call [VALUE] [VALUE]´");
		StructFuncCallCmd res = StructFuncCallCmd.create(scope, func, fstuct, this.in.ctx());
		exitState(STATE_EX_CODE_CALL_FSTRUCT, enter, res);
		return res;
	}
	
}
