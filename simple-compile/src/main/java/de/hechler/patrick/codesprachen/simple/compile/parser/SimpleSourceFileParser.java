package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.ASM;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.BLOCK_CLOSE;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.BLOCK_OPEN;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.CALL;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.ELSE;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.EXP;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.IF;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.LT;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.NAME;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.SEMI;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.SMALL_CLOSE;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.SMALL_OPEN;
import static de.hechler.patrick.codesprachen.simple.compile.parser.SimpleTokenStream.WHILE;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.BlockCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.IfCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.StructFuncCallCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.cmd.WhileCmd;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
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
		BlockCmd       block = new BlockCmd(sf);
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
		case CALL -> parseCmdCall(scope);
		case WHILE -> parseCmdWhile(scope);
		case IF -> parseCmdIf(scope);
		case ASM -> parseCmdAsm(scope);
		default -> parseCmdAssignOrVarDecl(scope);
		};
	}
	
	private SimpleCommand parseCmdAssignOrVarDecl(SimpleScope scope) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private SimpleCommand parseCmdAsm(SimpleScope scope) {
		// TODO Auto-generated method stub
		return null;
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
	
	private SimpleCommand parseCmdCall(SimpleScope scope) {
		SimpleValue func = super.parseValue(scope);
		if ( this.in.tok() == LT ) {
			this.in.consume();
			List<SimpleValue> results = parseCommaSepValues();
			
		} else {
			SimpleValue fstuct = super.parseValue(scope);
			consumeToken(SEMI, "expected `;´ after `call [VALUE] [VALUE]´");
			return StructFuncCallCmd.create(scope, func, fstuct, this.in.ctx());
		}
	}
	
	private List<SimpleValue> parseCommaSepValues() {
		// TODO Auto-generated method stub
	}
	
}