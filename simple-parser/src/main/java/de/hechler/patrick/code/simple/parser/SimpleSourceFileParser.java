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

import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.*;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ASM_BLOCK;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BLOCK_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.BLOCK_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CALL;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.COMMA;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.CONST;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ELSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.EXP;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.GT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.IF;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.INIT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LARROW;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.LT;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.MAIN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.ME;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.QUESTION;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SEMI;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SMALL_CLOSE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.SMALL_OPEN;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.STRING;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.WHILE;
import static de.hechler.patrick.code.simple.parser.SimpleTokenStream.name;
import static de.hechler.patrick.code.simple.parser.error.ErrorContext.NO_CONTEXT;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.code.simple.parser.error.CompileError;
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
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleExportable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFunction;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
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
	
	public SimpleSourceFileParser(InputStream in, String file, BiFunction<String, String, SimpleDependency> dep) {
		super(in, file, dep);
		
	}
	
	public SimpleSourceFileParser(SimpleTokenStream in, BiFunction<String, String, SimpleDependency> dep) {
		super(in, dep);
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
			try {
				name = this.in.consumeDynTokSpecialText();
			} catch (@SuppressWarnings("unused") AssertionError ae) {
				name = name(this.in.consumeTok());
			}
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
			this.in.handleError(this.in.ctx(), "could not find the dependency \"" + srcPath + "\""
				+ ( binPath == null ? "" : " \"" + binPath + "\"" ));
		} else {
			try {
				sf.dependency(dependency, name, this.in.ctx());
			} catch (CompileError ce) {
				this.in.handleError(ce);
			}
		}
		exitState(STATE_DEPENDENCY, enter, name == null ? dependency : name);
	}
	
	@Override
	protected SimpleVariable parseSFScopeVariable(SimpleFile sf) {
		return parseAnyScopeVariable(sf, SimpleVariable.FLAG_GLOBAL);
	}
	
	@Override
	protected void parseFunction(SimpleFile sf) {
		final Object enter = enterState(STATE_FUNCTION);
		this.in.consume();
		int flags = FuncType.FLAG_FUNC_ADDRESS;
		String name = null;
		if ( this.in.tok() == NAME ) {
			name = this.in.consumeDynTokSpecialText();
		} else {
			if ( this.in.tok() == EXP ) {
				flags |= SimpleExportable.FLAG_EXPORT;
				consumeToken(EXP, "expected `[NAME]´ or `exp [NAME]´ after `func´");
			}
			if ( this.in.tok() == INIT ) {
				this.in.consume();
				flags |= FuncType.FLAG_INIT;
			} else if ( this.in.tok() == MAIN ) {
				this.in.consume();
				flags |= FuncType.FLAG_MAIN;
			} else if ( ( flags & SimpleExportable.FLAG_EXPORT ) == 0 ) {
				this.in.handleError(this.in.ctx(), List.of(name(NAME), name(EXP), name(INIT), name(MAIN)),
					"expected `exp (main | init)? [NAME] | (main | init) [NAME]? | [NAME]´ after `func´");
			}
			if ( this.in.tok() == NAME ) {
				name = this.in.consumeDynTokSpecialText();
			} else if ( ( flags & SimpleExportable.FLAG_EXPORT ) != 0 ) {
				this.in.handleError(this.in.ctx(), List.of(name(NAME), name(EXP), name(INIT), name(MAIN)),
					"expected `[NAME]´ after `func exp (main | init)?´");
			}
		}
		SimpleType type = parseType(sf);
		FuncType ftype;
		if ( !( type instanceof FuncType ftype0 ) || ( ftype0.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			this.in.handleError(this.in.ctx(), "the [TYPE] of a function MUST be a function address type: " + type);
			ftype = FuncType.create(List.of(), List.of(), FuncType.FLAG_FUNC_ADDRESS, NO_CONTEXT);
		} else ftype = ftype0;
		BlockCmd block;
		try {
			ftype = FuncType.create(ftype.resMembers(), ftype.argMembers(), flags, this.in.ctx());
			block = new BlockCmd(SimpleScope.newFuncScope(sf, ftype, this.in.ctx()));
		} catch (CompileError ce) {
			this.in.handleError(ce);
			ftype = FuncType.create(List.of(), List.of(), flags, this.in.ctx());
			block = new BlockCmd(SimpleScope.newFuncScope(sf, ftype, this.in.ctx()));
		}
		SimpleFunction func = new SimpleFunction(sf, name, ftype, block);
		try {
			sf.function(func, this.in.ctx());
		} catch (CompileError ce) {
			this.in.handleError(ce);
		}
		parseCmdBlock(block);
		exitState(STATE_FUNCTION, enter, func);
	}
	
	private void parseCmdBlock(BlockCmd block) {
		final Object enter = enterState(STATE_EX_CODE_BLOCK);
		consumeToken(BLOCK_OPEN, "expected `{´ to start the block command");
		while ( true ) {
			if ( this.in.tok() == BLOCK_CLOSE  ) {
				this.in.consume();
				block.seal();
				exitState(STATE_EX_CODE_BLOCK, enter, block);
				return;
			} else if (this.in.tok() == EOF) {
				this.in.handleError(this.in.ctx(), "expected `}´ or `[COMMAND]´ after `{ [COMMAND]*´");
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
			SimpleCommand res;
			try {
				res = AssignCmd.create(scope, val0, val1, this.in.ctx());
			} catch (CompileError ce) {
				this.in.handleError(ce);
				BlockCmd bc = new BlockCmd(scope);
				bc.seal();
				res = bc;
			}
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
				"expected `> <-- \\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `[SHIFT_EXP] < ( [VALUE] ( , [VALUE] )* )?´");
			consumeToken(LARROW,
				"expected `<-- \\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `[SHIFT_EXP] [FUNC_CALL_RESULT]´");
			consumeToken(SMALL_OPEN,
				"expected `\\( ( [VALUE] ( , [VALUE] )* )? \\)´ after `call [FUNC_CALL_RESULT] <--´");
		}
		List<SimpleValue> args = List.of();
		if ( this.in.tok() != SMALL_CLOSE ) {
			args = parseCommaSepValues(scope, false);
		}
		consumeToken(SMALL_CLOSE,
			"expected `\\)´ after `[SHIFT_EXP] ( [FUNC_CALL_RESULT] <-- )? \\( ( [VALUE] ( , [VALUE] )* )?´");
		consumeToken(SEMI, "expected `;´ after `call [VALUE]  ( [FUNC_CALL_RESULT] <-- )? [FUNC_CALL_ARGS]´");
		SimpleCommand res;
		try {
			res = FuncCallCmd.create(scope, func, results, args, this.in.ctx());
		} catch (CompileError ce) {
			this.in.handleError(ce);
			BlockCmd bc = new BlockCmd(scope);
			bc.seal();
			res = bc;
		}
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
		this.in.consume();
		SimpleType type = parseType(scope);
		return parseCmdVarDecl0(scope, SimpleVariable.FLAG_CONSTANT, type, enter);
	}
	
	private SimpleCommand parseCmdVarDecl0(SimpleScope scope, int flags, SimpleType type, Object enter) {
		expectToken(NAME, "expected `[NAME]´ after `( const )? [TYPE]´");
		String name;
		try {
			name = this.in.consumeDynTokSpecialText();
		} catch (@SuppressWarnings("unused") AssertionError ae) {
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
		SimpleCommand res;
		try {
			SimpleVariable sv = new SimpleVariable(type, name, initialVal, flags);
			res = VarDeclCmd.create(scope, sv, this.in.ctx());
		} catch (NullPointerException | CompileError err) {
			if ( err instanceof CompileError ce ) {
				this.in.handleError(ce);
			} else if ( name != null ) {
				throw new AssertionError(err);
			}
			BlockCmd bc = new BlockCmd(scope);
			bc.seal();
			res = bc;
		}
		exitState(STATE_EX_CODE_VAR_DECL, enter, res);
		return res;
	}
	
	private SimpleCommand parseCmdAsm(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_ASM);
		this.in.consume();
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
		SimpleCommand res;
		try {
			res = AsmCmd.create(scope, params, asmBlock, results, this.in.ctx());
		} catch (CompileError ce) {
			this.in.handleError(ce);
			BlockCmd bc = new BlockCmd(scope);
			bc.seal();
			res = bc;
		}
		exitState(STATE_EX_CODE_ASM, enter, res);
		return res;
	}
	
	private void addAsmParamPair(SimpleScope scope, List<AsmCmd.AsmParam> params) {
		final Object enter = enterState(STATE_EX_CODE_ASM_PARAM_PAIR);
		String target = this.in.consumeDynTokSpecialText();
		consumeToken(LARROW, "expected `<--´ after `asm ( [STRING] <-- [VALUE] , )* [STRING]´");
		SimpleValue val = parseValue(scope);
		AsmCmd.AsmParam asmParam;
		try {
			asmParam = AsmCmd.AsmParam.create(target, val, this.in.ctx());
			params.add(asmParam);
		} catch (CompileError ce) {
			this.in.handleError(ce);
			asmParam = null;
		}
		exitState(STATE_EX_CODE_ASM_PARAM_PAIR, enter, asmParam);
	}
	
	private void addAsmCorruptResult(List<AsmCmd.AsmResult> results) {
		final Object enter = enterState(STATE_EX_CODE_ASM_RESULT_IGNORE);
		String ignoreReg = this.in.consumeDynTokSpecialText();
		consumeToken(LARROW,
			"expected `<-- \\?´ after `[ASM_BLOCK] ( ( [VALUE] <-- [STRING] | [STRING] <-- \\? ) ( , [VALUE] <-- [STRING] | [STRING] <-- \\? )* )? [STRING]´");
		consumeToken(QUESTION,
			"expected `\\?´ after `[ASM_BLOCK] ( [VALUE] <-- [STRING] ( , [VALUE] <-- [STRING] )* )? [VALUE] <--´");
		AsmCmd.AsmResult asmRes;
		try {
			asmRes = AsmCmd.AsmResult.create(ignoreReg, this.in.ctx());
			results.add(asmRes);
		} catch (CompileError ce) {
			this.in.handleError(ce);
			asmRes = null;
		}
		exitState(STATE_EX_CODE_ASM_RESULT_IGNORE, enter, asmRes);
	}
	
	private void addAsmResultPair(SimpleScope scope, List<AsmCmd.AsmResult> results) {
		final Object enter = enterState(STATE_EX_CODE_ASM_RESULT_PAIR);
		SimpleValue target = parseValue(scope);
		consumeToken(LARROW,
			"expected `<-- ?´ after `[ASM_BLOCK] ( ( [VALUE] <-- [STRING] | [STRING] <-- \\? ) ( , [VALUE] <-- [STRING] | [STRING] <-- \\? )* )? [VALUE]´");
		expectToken(STRING,
			"expected `<-- ?´ after `[ASM_BLOCK] ( ( [VALUE] <-- [STRING] | [STRING] <-- \\? ) ( , [VALUE] <-- [STRING] | [STRING] <-- \\? )* )? [VALUE] <--´");
		String source = this.in.consumeDynTokSpecialText();
		AsmCmd.AsmResult asmRes;
		try {
			asmRes = AsmCmd.AsmResult.create(target, source, this.in.ctx());
			results.add(asmRes);
		} catch (CompileError ce) {
			this.in.handleError(ce);
			asmRes = null;
		}
		exitState(STATE_EX_CODE_ASM_RESULT_PAIR, enter, asmRes);
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
		SimpleCommand res;
		try {
			res = IfCmd.create(scope, cond, cmd, elseCmd, this.in.ctx());
		} catch (CompileError ce) {
			this.in.handleError(ce);
			BlockCmd bc = new BlockCmd(scope);
			bc.seal();
			res = bc;
		}
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
		SimpleCommand res;
		try {
			res = WhileCmd.create(scope, cond, cmd, this.in.ctx());
		} catch (CompileError ce) {
			this.in.handleError(ce);
			BlockCmd bc = new BlockCmd(scope);
			bc.seal();
			res = bc;
		}
		exitState(STATE_EX_CODE_WHILE, enter, res);
		return res;
	}
	
	private SimpleCommand parseCmdCallFStruct(SimpleScope scope) {
		final Object enter = enterState(STATE_EX_CODE_CALL_FSTRUCT);
		this.in.consume();
		SimpleValue func = super.parseValueShiftExp(scope, 0, null, null);
		SimpleValue fstuct = super.parseValue(scope);
		consumeToken(SEMI, "expected `;´ after `call [VALUE] [VALUE]´");
		SimpleCommand res;
		try {
			res = StructFuncCallCmd.create(scope, func, fstuct, this.in.ctx());
		} catch (CompileError ce) {
			this.in.handleError(ce);
			BlockCmd bc = new BlockCmd(scope);
			bc.seal();
			res = bc;
		}
		exitState(STATE_EX_CODE_CALL_FSTRUCT, enter, res);
		return res;
	}
	
}
