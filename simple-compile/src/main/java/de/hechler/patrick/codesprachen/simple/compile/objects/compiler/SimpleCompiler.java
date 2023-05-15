// This file is part of the Simple Code Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.A_SR;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_NUM;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.B_REG;
import static de.hechler.patrick.codesprachen.primitive.assemble.objects.Param.ParamBuilder.build;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants.SP;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants.X_ADD;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;

import de.hechler.patrick.codesprachen.primitive.assemble.PrimitiveFileGrammarParser.ParseContext;
import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.assemble.enums.CompilerCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Command;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.CompilerCommandCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.ConstantPoolCommand;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.Param;
import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;
import de.hechler.patrick.codesprachen.primitive.core.objects.PrimitiveConstant;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser;
import de.hechler.patrick.codesprachen.simple.compile.interfaces.TriFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.UsedData;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommand;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAsm;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAsm.AsmParam;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandAssign;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandFuncCall;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandIf;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandVarDecl;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandWhile;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue.StackUseListener;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValue.VarLoader;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueConst;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueDataPointer;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleValueNoConst;
import de.hechler.patrick.codesprachen.simple.compile.objects.values.SimpleVariableValue;
import de.hechler.patrick.codesprachen.simple.compile.utils.StdLib.StdLibFunc;
import de.hechler.patrick.codesprachen.simple.compile.utils.TwoInts;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunctionSymbol;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleFunctionVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypeArray;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;
import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.File;

@SuppressWarnings({ "javadoc" })
public class SimpleCompiler extends StepCompiler<SimpleCompiler.SimpleTU> {
	
	// X00 .. X1F are reserved for interrupts and asm blocks
	// X20 .. X3F are reserved for special compiler registers
	// X40 .. X3F are reserved for variables (there are not variable registers
	// supported yet)
	// X40 .. XF9 are reserved for temporary values
	public static final int MIN_COMPILER_REGISTER = X_ADD + 0x20;
	public static final int REG_FUNC_STRUCT       = MIN_COMPILER_REGISTER;
	public static final int REG_VAR_PNTR          = X_ADD + 0x21;
	public static final int MIN_VAR_REGISTER      = X_ADD + 0x40;
	public static final int MAX_VAR_REGISTER      = X_ADD + 0x60;
	public static final int MIN_TMP_VAL_REG       = MAX_VAR_REGISTER + 1;
	public static final int MAX_TMP_VAL_REG       = 0xFF;
	
	// when changed the compilers main call has most likely to be changed too
	public static final SimpleFuncType MAIN_TYPE   = new SimpleFuncType(
			List.of(new SimpleOffsetVariable(SimpleType.NUM, "argc"),
					new SimpleOffsetVariable(new SimpleTypePointer(new SimpleTypePointer(SimpleType.BYTE)), "argv")),
			List.of(new SimpleOffsetVariable(SimpleType.NUM, "exitnum")));
	private static final long          MAIN_LENGTH = 16L;
	private static final long          JMP_LENGTH  = 8L;
	
	static {
		if (MAIN_LENGTH != MAIN_TYPE.byteCount()) throw new AssertionError(MAIN_TYPE.byteCount() + " : " + MAIN_TYPE);
		if (JMP_LENGTH != new Command(Commands.CMD_JMP, Param.createLabel(""), null).length()) throw new AssertionError("JMP_LENGTH has the wrong value");
		if (JMP_LENGTH != new Command(Commands.CMD_JMPAB, Param.createLabel(""), null).length()) throw new AssertionError("JMP_LENGTH has the wrong value");
		if (JMP_LENGTH != new Command(Commands.CMD_JMPERR, Param.createLabel(""), null).length()) throw new AssertionError("JMP_LENGTH has the wrong value");
		if (JMP_LENGTH != new Command(Commands.CMD_JMPGT, Param.createLabel(""), null).length()) throw new AssertionError("JMP_LENGTH has the wrong value");
		if (JMP_LENGTH != new Command(Commands.CMD_JMPEQ, Param.createLabel(""), null).length()) throw new AssertionError("JMP_LENGTH has the wrong value");
	}
	
	private final Charset cs;
	private final Path    srcRoot;
	private final Path[]  lockups;
	
	public SimpleCompiler(Charset cs, Path srcRoot, Path[] lockups) {
		this.cs      = cs;
		this.srcRoot = srcRoot;
		this.lockups = lockups.clone();
	}
	
	@Override
	protected SimpleTU createTU(Path source, File target) {
		return new SimpleTU(source, target);
	}
	
	@Override
	protected void init(SimpleTU tu) throws IOException {
		try (Reader r = Files.newBufferedReader(tu.source, this.cs)) {
			ANTLRInputStream in = new ANTLRInputStream();
			in.load(r, 1024, 1024);
			SimpleGrammarLexer  lexer  = new SimpleGrammarLexer(in);
			CommonTokenStream   toks   = new CommonTokenStream(lexer);
			SimpleGrammarParser parser = new SimpleGrammarParser(toks);
			parser.setErrorHandler(new BailErrorStrategy());
			parser.simpleFile(tu.sf);
		}
	}
	
	@Override
	protected void precompile(SimpleTU tu) throws IOException {
		SimpleFunction main = tu.sf.mainFunction();
		tu.pos = 0;
		if (main != null) {
			addMainCall(tu, main);
		}
		addErrorHandlers(tu);
		addDataValues(tu);
		addVariables(tu);
		addFunction(tu);
	}
	
	private static void addFunction(SimpleTU tu) {
		for (SimpleFunction sf : tu.sf.functions()) {
			addFunc(tu, sf);
		}
	}
	
	private static void addFunc(SimpleTU tu, SimpleFunction func) {
		align(tu, 8L, null);
		label(tu, func.name);
		func.init(tu.pos, tu.sf);
		UsedData ud = func.pool.used();
		ud.currentaddr = ud.maxaddr;
		ud.regs        = ud.maxregs;
		SimpleFile.count(ud, func.body);
		if (ud.maxaddr != 0L) {
			// MOV X00, NEEDED_SIZE
			// INT INT_MEMORY_ALLOC
			// JMPERR OUT_OF_MEMORY
			// MOV REG_VAR, X00
			Command movSizeCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_NUM, ud.maxaddr));
			add(tu, movSizeCmd);
			Command intMallocCmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_MEMORY_ALLOC), null);
			add(tu, intMallocCmd);
			Command jmpErrCmd = new Command(Commands.CMD_JMPERR, build(A_NUM, tu.outOfMem - tu.pos), null);
			add(tu, jmpErrCmd);
			Command movToReg = new Command(Commands.CMD_MOV, build(A_SR, REG_VAR_PNTR), build(A_SR, X_ADD));
			add(tu, movToReg);
		}
		for (int i = 0; i < func.type.arguments.length; i++) {
			long off = func.type.arguments[i].offset();
			long bc  = func.type.arguments[i].type.byteCount();
			copyArgument(tu, func.pool.myargs[i], off, bc);
		}
		addCmdBlock(tu, func.body);
		if (ud.maxaddr != 0L) {
			Command movFromReg = new Command(Commands.CMD_MOV, build(A_SR, REG_VAR_PNTR), build(A_SR, X_ADD));
			add(tu, movFromReg);
			Command intFreeCmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_MEMORY_FREE), null);
			add(tu, intFreeCmd);
		}
		add(tu, new Command(Commands.CMD_RET, null, null));
	}
	
	// only used to preassemble asm blocks
	private static final PrimitiveAssembler PREASSEMBLER = new PrimitiveAssembler(null, null, null, false, true);
	
	private static void addCmdAsm(SimpleTU tu, SimpleCommandAsm c) {
		TwoInts minMax = new TwoInts(-1, Integer.MAX_VALUE);
		findMaxAndMinRegs(minMax, c.asmArguments);
		findMaxAndMinRegs(minMax, c.asmResults);
		push(tu, minMax);
		boolean[] blockedRegs = new boolean[256];
		for (int i = 0; i < minMax.a; i++) {
			blockedRegs[i] = true;
		}
		for (int i = minMax.b + 1; i < 256; i++) {
			blockedRegs[i] = true;
		}
		StackUseListener sul       = new StackUseListener();
		VarLoaderImpl    varLoader = new VarLoaderImpl(minMax, sul);
		for (AsmParam arg : c.asmArguments) {
			tu.pos = arg.value.loadValue(tu.sf, arg.register, blockedRegs, tu.commands, tu.pos, varLoader, sul);
		}
		ParseContext context = PREASSEMBLER.preassemble(tu.source, new ANTLRInputStream(c.asmCode.substring(2, c.asmCode.length() - 2)), consts(tu, c), tu.pos);
		tu.pos = context.pos;
		
		pop(tu, minMax);
	}
	
	private static Map<String, PrimitiveConstant> consts(SimpleTU tu, SimpleCommandAsm c) {
		Map<String, PrimitiveConstant> res = new HashMap<>();
		for (SimpleFunction sf : tu.sf.functions()) {
			SimpleExportable.ImportHelp.convertFunc(res, null, sf, tu.source, false);
		}
		for (SimpleConstant sc : c.pool.getConstants().values()) {
			SimpleExportable.ImportHelp.convertConst(res, null, sc, tu.source);
		}
		for (SimpleDependency sd : tu.sf.dependencies()) {
			String depName = SimpleExportable.ImportHelp.DEPENDENCY_PREFIX + sd.name;
			String comment = "dependency: " + sd.name + "\ndata: " + sd.path;
			res.put(depName, new PrimitiveConstant(depName, comment, sd.path.addr(), tu.source, -1));
			String prefix = depName + '_';
			for (Iterator<SimpleExportable> iter = sd.getAll(); iter.hasNext();) {
				SimpleExportable se = iter.next();
				switch (se) {
				case @SuppressWarnings("preview") SimpleConstant sc -> SimpleExportable.ImportHelp.convertConst(res, prefix, sc, null);
				case @SuppressWarnings("preview") SimpleOffsetVariable sov -> SimpleExportable.ImportHelp.convertVar(res, prefix, sov, null);
				case @SuppressWarnings("preview") SimpleFunction sf -> SimpleExportable.ImportHelp.convertFunc(res, prefix, sf, null, true);
				case @SuppressWarnings("preview") SimpleStructType sst -> SimpleExportable.ImportHelp.convertStrut(res, prefix, sst, null);
				default -> throw new AssertionError("unknown simple exportable type: " + se);
				}
			}
		}
		return res;
	}
	
	private static void pop(SimpleTU tu, TwoInts minMax) {
		if (minMax.b - minMax.a <= 4) {
			for (int reg = minMax.b; reg >= minMax.a; reg--) {
				Command pushCmd = new Command(Commands.CMD_POP, build(A_SR, reg), null);
				add(tu, pushCmd);
			}
		} else {
			Command pushCmd = new Command(Commands.CMD_POPBLK, build(A_NUM, minMax.a), build(A_NUM, (minMax.b - (long) minMax.a) << 3));
			add(tu, pushCmd);
		}
	}
	
	private static void push(SimpleTU tu, TwoInts minMax) {
		if (minMax.b - minMax.a <= 4) {
			for (int reg = minMax.a; reg <= minMax.b; reg++) {
				Command pushCmd = new Command(Commands.CMD_PUSH, build(A_SR, reg), null);
				add(tu, pushCmd);
			}
		} else {
			Command pushCmd = new Command(Commands.CMD_PUSHBLK, build(A_NUM, minMax.a), build(A_NUM, (minMax.b - (long) minMax.a) << 3));
			add(tu, pushCmd);
		}
	}
	
	private static class VarLoaderImpl implements VarLoader {
		
		private final TwoInts          minMax;
		private final StackUseListener sul;
		
		private VarLoaderImpl(TwoInts minMax, StackUseListener sul) {
			this.minMax = minMax;
			this.sul    = sul;
		}
		
		@Override
		public long loadVar(long pos, int targetRegister, List<Command> commands, SimpleFunctionVariable sv) {
			if (sv.reg() < this.minMax.a || sv.reg() > this.minMax.b) return -1L;
			long off = this.minMax.b;
			off -= sv.reg() << 3;
			off  = -off - 8L;
			Param reg = build(A_SR, targetRegister);
			if (sv.type.isPrimitive() || sv.type.isPointer()) {
				Commands op         = switch ((int) sv.type.byteCount()) {
									case 8 -> Commands.CMD_MOV;
									case 4 -> Commands.CMD_MVDW;
									case 2 -> Commands.CMD_MVW;
									case 1 -> Commands.CMD_MVB;
									default -> throw new AssertionError(sv.type);
									};
				Commands op0        = sv.hasOffset() ? Commands.CMD_MOV : op;
				Command  movAddrCmd = new Command(op0, reg, build(A_SR | B_NUM, SP, off - this.sul.size()));
				pos += movAddrCmd.length();
				commands.add(movAddrCmd);
				if (sv.hasOffset()) {
					Param p;
					if (sv.offset() != 0) {
						p = build(A_SR | B_NUM, targetRegister, sv.offset());
					} else {
						p = build(A_SR | B_REG, targetRegister);
					}
					Command movValCmd = new Command(op, reg, p);
					pos += movValCmd.length();
					commands.add(movValCmd);
				}
				return SimpleValueNoConst.addMovCmd(sv.type, commands, pos, reg, reg);
			} else if (sv.hasOffset()) {
				Command movCmd = new Command(Commands.CMD_MVAD, reg, build(A_SR | B_NUM, SP, off - this.sul.size()), build(A_NUM, sv.offset()));
				pos += movCmd.length();
				commands.add(movCmd);
			} else {
				this.sul.setForbidden();
				Command movCmd = new Command(Commands.CMD_MVAD, reg, build(A_SR, SP), build(A_NUM, off - this.sul.size()));
				pos += movCmd.length();
				commands.add(movCmd);
			}
			return pos;
		}
		
		@Override
		public long loadVarPntr(long pos, int targetRegister, List<Command> commands, SimpleFunctionVariable sv) {
			if (sv.reg() < this.minMax.a || sv.reg() > this.minMax.b) { return -1L; }
			long  off = (-(this.minMax.b - (sv.reg() << 3))) - 8L;
			Param reg = build(A_SR, targetRegister);
			if (sv.hasOffset()) {
				Command movCmd = new Command(Commands.CMD_MVAD, reg, build(A_SR | B_NUM, SP, off - this.sul.size()), build(A_NUM, sv.offset()));
				pos += movCmd.length();
				commands.add(movCmd);
			} else {
				this.sul.setForbidden();
				Command movCmd = new Command(Commands.CMD_MVAD, reg, build(A_SR, SP), build(A_NUM, off - this.sul.size()));
				pos += movCmd.length();
				commands.add(movCmd);
			}
			return pos;
		}
		
	}
	
	private static void findMaxAndMinRegs(TwoInts maxMin, AsmParam[] asmArguments) {
		for (AsmParam arg : asmArguments) {
			if (arg.register > maxMin.a) {
				maxMin.a = arg.register;
			}
			if (arg.register < maxMin.b) {
				maxMin.b = arg.register;
			}
		}
	}
	
	@SuppressWarnings("preview")
	private static void addCmdAssign(SimpleTU tu, SimpleCommandAssign c) {
		if (!c.target.type().isPrimitive() && !c.target.type().isPointer()) {
			throw new IllegalStateException("can not assign with array/structure values! (command: " + c + ")");
		}
		if (!c.target.type().equals(c.value.type())) {
			throw new IllegalStateException("target and value type are different (command: " + c + ")");
		}
		boolean[] blockedRegs = new boolean[256];
		if (c.target instanceof SimpleVariableValue svv) {
			switch (svv.sv) {
			case SimpleOffsetVariable sov -> {
				Commands mov = mov(c, (int) c.target.type().byteCount());
				tu.pos = c.value.loadValue(tu.sf, MIN_TMP_VAL_REG, blockedRegs, tu.commands, tu.pos, null, null);
				// MOV [IP + (offset - pos)], value
				Command cmd = new Command(mov, build(A_SR | B_NUM, PrimAsmConstants.IP, sov.offset() - tu.pos), build(A_SR, MIN_TMP_VAL_REG));
				add(tu, cmd);
			}
			case SimpleFunctionVariable sfv when sfv.hasOffset() -> {
				Commands mov = mov(c, (int) c.target.type().byteCount());
				tu.pos = c.value.loadValue(tu.sf, MIN_TMP_VAL_REG, blockedRegs, tu.commands, tu.pos, null, null);
				// MOV [sfv.reg + offset], value
				Command cmd = new Command(mov, build(A_SR | B_NUM, sfv.reg(), sfv.offset()), build(A_SR, MIN_TMP_VAL_REG));
				add(tu, cmd);
			}
			case SimpleFunctionVariable sfv -> {
				tu.pos = c.value.loadValue(tu.sf, sfv.reg(), blockedRegs, tu.commands, tu.pos, null, null);
			}
			default -> throw new AssertionError("unknown variable type: " + svv.sv.getClass());
			}
		} else {
			Commands    mov = mov(c, (int) c.target.type().byteCount());
			SimpleValue t   = c.target.mkPointer(c.pool);
			tu.pos = t.loadValue(tu.sf, MIN_TMP_VAL_REG, blockedRegs, tu.commands, tu.pos, null, null);
			tu.pos = c.value.loadValue(tu.sf, MIN_TMP_VAL_REG + 1, blockedRegs, tu.commands, tu.pos, null, null);
			// MOV [TMP0], TMP1
			Command cmd = new Command(mov, build(A_SR | B_REG, MIN_TMP_VAL_REG), build(A_SR, MIN_TMP_VAL_REG + 1L));
			add(tu, cmd);
		}
	}
	
	private static Commands mov(SimpleCommandAssign c, int bc) throws AssertionError {
		return switch (bc) {
		case 1 -> Commands.CMD_MVB;
		case 2 -> Commands.CMD_MVW;
		case 4 -> Commands.CMD_MVDW;
		case 8 -> Commands.CMD_MOV;
		default -> throw new AssertionError("invalid byte cont type: " + c.target.type());
		};
	}
	
	private static void addCmdBlock(SimpleTU tu, SimpleCommandBlock blk) {
		for (SimpleCommand cmd : blk.commands) {
			addCmd(tu, cmd);
		}
	}
	
	private static void addCmd(SimpleTU tu, SimpleCommand cmd) throws AssertionError {
		switch (cmd) {
		case @SuppressWarnings("preview") SimpleCommandAsm c -> addCmdAsm(tu, c);
		case @SuppressWarnings("preview") SimpleCommandAssign c -> addCmdAssign(tu, c);
		case @SuppressWarnings("preview") SimpleCommandBlock c -> addCmdBlock(tu, c);
		case @SuppressWarnings("preview") SimpleCommandFuncCall c -> addCmdFuncCall(tu, c);
		case @SuppressWarnings("preview") SimpleCommandIf c -> addCmdIf(tu, c);
		case @SuppressWarnings("preview") SimpleCommandVarDecl c -> addCmdVarDecl(tu, c);
		case @SuppressWarnings("preview") SimpleCommandWhile c -> addCmdWhile(tu, c);
		default -> throw new AssertionError(cmd.getClass() + " :  " + cmd);
		}
	}
	
	private static void addCmdFuncCall(SimpleTU tu, SimpleCommandFuncCall c) {
		// load value before pushing to stack, so when modifying values are supported, the changes are saved
		tu.pos = c.function.loadValue(tu.sf, MIN_TMP_VAL_REG, new boolean[256], tu.commands, tu.pos, null, null);
		int min    = MIN_COMPILER_REGISTER;
		int max    = c.pool.regMax();
		int regLen = max - min + 1;
		if (c.secondName == null) {
			push(tu, min, max, regLen);
			SimpleFunction func = tu.sf.getFunction(c.firstName);
			if (!func.type.equals(c.function.type())) {
				throw new IllegalArgumentException("the function call argument needs to have the same type as the functions type! (arg-type: " + c.function.type()
						+ " func: " + func.type + ")");
			}
			// MOV FUNC, TMP0
			// CALL func.name
			Command mov = new Command(Commands.CMD_MOV, build(A_SR, REG_FUNC_STRUCT), build(A_SR, MIN_TMP_VAL_REG));
			add(tu, mov);
			Command call = new Command(Commands.CMD_CALL, Param.createLabel(func.name), null);
			add(tu, call);
		} else {
			SimpleDependency dep = tu.sf.getDependency(c.firstName);
			SimpleExportable exp = dep.get(c.secondName);
			if (!(exp instanceof SimpleFunctionSymbol func)) {
				throw new IllegalArgumentException("the export " + c.firstName + ':' + c.secondName + " is no function: " + exp);
			}
			if (!func.type.equals(c.function.type())) {
				throw new IllegalArgumentException("the function call argument needs to have the same type as the functions type! (arg-type: " + c.function.type()
						+ " func: " + func.type + ")");
			}
			if (func instanceof StdLibFunc slf) {
				int reg = X_ADD;
				for (SimpleOffsetVariable sov : slf.type.arguments) {
					Commands mov = switch (sov.type.byteCount()) {
					case 1L -> Commands.CMD_MVB;
					case 2L -> Commands.CMD_MVW;
					case 4L -> Commands.CMD_MVDW;
					case 8L -> Commands.CMD_MOV;
					default -> throw new AssertionError(sov.type);
					};
					
					Param p = sov.offset() == 0 ? build(A_SR | B_REG, MIN_TMP_VAL_REG) : build(A_SR | B_NUM, MIN_TMP_VAL_REG, sov.offset());
					add(tu, new Command(mov, build(A_SR, reg++), p));
				}
			} else {
				push(tu, min, max, regLen);
				// LEA X00, (dep.pos - tu.pos)
				// INT INT_LOAD_LIB
				// JMPERR LOAD_ERROR
				// MOV FUNC, TMP0
				// CALO X00, func.off
				Command cmd = new Command(Commands.CMD_LEA, build(A_SR, X_ADD), build(A_NUM, dep.path.addr() - tu.pos));
				add(tu, cmd);
				cmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD + 3L), build(A_SR, X_ADD));
				add(tu, cmd);
				cmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_LOAD_LIB), null);
				add(tu, cmd);
				cmd = new Command(Commands.CMD_JMPERR, build(A_NUM, tu.depLoad - tu.pos), null);
				add(tu, cmd);
				cmd = new Command(Commands.CMD_MOV, build(A_SR, REG_FUNC_STRUCT), build(A_SR, MIN_TMP_VAL_REG));
				add(tu, cmd);
				cmd = new Command(Commands.CMD_CALO, build(A_SR, X_ADD), build(A_NUM, func.address()));
				add(tu, cmd);
			}
		}
		if (regLen <= 4) {
			for (int i = max; i >= min; i--) {
				Command cmd = new Command(Commands.CMD_POP, build(A_SR, i), null);
				add(tu, cmd);
			}
		} else {
			Command cmd = new Command(Commands.CMD_POPBLK, build(A_NUM, PrimAsmPreDefines.REGISTER_MEMORY_START + (min << 3)), build(A_NUM, regLen << 3));
			add(tu, cmd);
		}
	}
	
	private static void push(SimpleTU tu, int min, int max, int regLen) {
		if (regLen <= 4) {
			for (int i = min; i <= max; i++) {
				Command cmd = new Command(Commands.CMD_PUSH, build(A_SR, i), null);
				add(tu, cmd);
			}
		} else {
			Command cmd = new Command(Commands.CMD_PUSHBLK, build(A_NUM, PrimAsmPreDefines.REGISTER_MEMORY_START + (min << 3)), build(A_NUM, regLen << 3));
			add(tu, cmd);
		}
	}
	
	private static void addCmdIf(SimpleTU tu, SimpleCommandIf c) {
		boolean[] regs = new boolean[256];
		tu.pos = c.condition.loadValue(tu.sf, MIN_TMP_VAL_REG, regs, tu.commands, tu.pos, null, null);
		Commands jotCmd;
		Commands jofCmd;
		Commands cmpCmd;
		if (c.condition.type().isPointer()) {
			jotCmd = Commands.CMD_JMPGT;
			jofCmd = Commands.CMD_JMPLE;
			cmpCmd = Commands.CMD_SGN;
		} else if (c.condition.type().isPrimitive()) {
			jotCmd = Commands.CMD_JMPNE;
			jofCmd = Commands.CMD_JMPEQ;
			cmpCmd = c.condition.type() == SimpleType.FPNUM ? Commands.CMD_SGNFP : Commands.CMD_SGN;
		} else {
			throw new IllegalStateException("unknown condition type: " + c.condition);
		}
		Command cmp = new Command(cmpCmd, build(A_SR, MIN_TMP_VAL_REG), null);
		add(tu, cmp);
		long jmpPos = tu.pos;
		tu.pos += JMP_LENGTH;
		List<Command> cmds = tu.commands;
		tu.commands = new ArrayList<>();
		if (c.elseCmd == null) {
			// jmpOnFalse end
			// - ifBlock
			// end:
			addCmd(tu, c.ifCmd);
			cmds.add(new Command(jofCmd, build(A_NUM, tu.pos - jmpPos), null));
			cmds.addAll(tu.commands);
		} else {
			// jmpOnTrue trueLabel
			// - elseBlock
			// - JMP end
			// trueLabel:
			// - ifBlock
			// end:
			addCmd(tu, c.elseCmd);
			cmds.add(new Command(jotCmd, build(A_NUM, tu.pos - jmpPos), null));
			cmds.addAll(tu.commands);
			jmpPos = tu.pos;
			tu.commands.clear();
			addCmd(tu, c.ifCmd);
			cmds.add(new Command(Commands.CMD_JMP, build(A_NUM, tu.pos - jmpPos), null));
			cmds.addAll(tu.commands);
		}
		tu.commands = cmds;
	}
	
	private static void addCmdVarDecl(SimpleTU tu, SimpleCommandVarDecl c) {
		if (c.init != null) {
			addCmdAssign(tu, new SimpleCommandAssign(c.pool, new SimpleVariableValue(c), c.init));
		}
	}
	
	private static void addCmdWhile(SimpleTU tu, SimpleCommandWhile c) {
		// loopStart:
		// MOV TMP0, condition
		// SGN TMP0
		// JMPNE end
		// - loopBody
		// - JMP loopStart:
		// end:
		long      loopStartPos = tu.pos;
		boolean[] regs         = new boolean[256];
		tu.pos = c.condition.loadValue(tu.sf, MIN_TMP_VAL_REG, regs, tu.commands, tu.pos, null, null);
		Commands jofCmd;
		Commands cmpCmd;
		if (c.condition.type().isPointer()) {
			jofCmd = Commands.CMD_JMPLE;
			cmpCmd = Commands.CMD_SGN;
		} else if (c.condition.type().isPrimitive()) {
			jofCmd = Commands.CMD_JMPEQ;
			cmpCmd = c.condition.type() == SimpleType.FPNUM ? Commands.CMD_SGNFP : Commands.CMD_SGN;
		} else {
			throw new IllegalStateException("unknown condition type: " + c.condition);
		}
		Command cmp = new Command(cmpCmd, build(A_SR, MIN_TMP_VAL_REG), null);
		add(tu, cmp);
		long jmpPos = tu.pos;
		tu.pos += JMP_LENGTH;
		List<Command> cmds = tu.commands;
		tu.commands = new ArrayList<>();
		addCmd(tu, c.whileCmd);
		Command gotoStart = new Command(Commands.CMD_JMP, build(A_NUM, loopStartPos - tu.pos), null);
		tu.pos += gotoStart.length();
		cmds.add(new Command(jofCmd, build(A_NUM, tu.pos - jmpPos), null));
		cmds.addAll(tu.commands);
		cmds.add(gotoStart);
		tu.commands = cmds;
	}
	
	private static void copyArgument(SimpleTU tu, SimpleFunctionVariable targetVar, long srcOff, long byteCount) {
		int dstReg = targetVar.reg();
		if (byteCount < 32L) { // check if copy manually or use MEM_CPY interrupt
			manualCopy(tu, targetVar, srcOff, byteCount, dstReg);
		} else {
			Command movDstAddrCmd;
			if (targetVar.hasOffset()) {
				long dstOff = targetVar.offset();
				if (dstOff != 0) {
					movDstAddrCmd = new Command(Commands.CMD_MVAD, build(A_SR, X_ADD), build(A_SR, dstReg), build(A_NUM, dstOff));
				} else {
					movDstAddrCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_SR, dstReg));
				}
			} else {
				movDstAddrCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_NUM, PrimAsmPreDefines.REGISTER_MEMORY_START + (dstReg << 3)));
			}
			add(tu, movDstAddrCmd);
			Command movSrcAddrCmd;
			if (srcOff != 0L) {
				movSrcAddrCmd = new Command(Commands.CMD_MVAD, build(A_SR, X_ADD + 1L), build(A_SR, REG_FUNC_STRUCT), build(A_NUM, srcOff));
			} else {
				movSrcAddrCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD + 1L), build(A_SR, REG_FUNC_STRUCT));
			}
			add(tu, movSrcAddrCmd);
			Command movLenCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD + 2L), build(A_NUM, byteCount));
			add(tu, movLenCmd);
		}
	}
	
	private static void manualCopy(SimpleTU tu, SimpleFunctionVariable targetVar, long srcOff, long byteCount, int dstReg) {
		long remain = byteCount;
		long add    = 0;
		while (remain > 0L) {
			Param from;
			if (srcOff != 0L || add != 0L) {
				from = build(A_SR | B_NUM, REG_FUNC_STRUCT, srcOff + add);
			} else {
				from = build(A_SR | B_REG, REG_FUNC_STRUCT);
			}
			Param to;
			int   reg = dstReg;
			if (targetVar.hasOffset()) {
				long off0 = targetVar.offset();
				if (off0 == 0L || add != 0) {
					to = build(A_SR | B_NUM, reg, off0 + add);
				} else {
					to = build(A_SR | B_REG, reg);
				}
			} else {
				if ((add & 7L) != 0L) {
					to = build(A_NUM | B_REG, PrimAsmPreDefines.REGISTER_MEMORY_START + (reg << 3) + add);
				} else {
					to = build(A_SR, reg + (add >>> 3));
				}
			}
			Commands op;
			if (remain >= 8) {
				remain -= 8;
				op      = Commands.CMD_MOV;
			} else if (remain >= 4) {
				op      = Commands.CMD_MVDW;
				remain -= 4;
			} else if (remain >= 2) {
				op      = Commands.CMD_MVW;
				remain -= 2;
			} else {
				op      = Commands.CMD_MVB;
				remain -= 1;
			}
			Command movCmd = new Command(op, to, from);
			add(tu, movCmd);
		}
	}
	
	private static void addVariables(SimpleTU tu) {
		for (SimpleOffsetVariable sv : tu.sf.vars()) {
			ConstantPoolCommand cp = new ConstantPoolCommand();
			align(tu, sv.type.byteCount(), null);
			sv.init(tu.pos, tu.sf);
			int len = (int) sv.type.byteCount();
			if (len != sv.type.byteCount()) {
				throw new IllegalArgumentException("the variable needs too much memory! (the compiler supports only variables with max (2^31)-1 bytes)");
			}
			cp.addBytes(new byte[len]);
			tu.commands.add(cp);
		}
	}
	
	private static void addDataValues(SimpleTU tu) {
		for (SimpleValueDataPointer sv : tu.sf.dataValues()) {
			ConstantPoolCommand cp = new ConstantPoolCommand();
			align(tu, sv.t.byteCount(), cp);
			sv.init(tu.pos);
			cp.addBytes(sv.data);
			tu.pos += sv.data.length;
			tu.commands.add(cp);
		}
	}
	
	private static void align(SimpleTU tu, long bc, ConstantPoolCommand cp) {
		long np = align(tu.pos, bc);
		if (np != tu.pos) {
			int len = (int) (np - tu.pos);
			if (len != np - tu.pos) { throw new AssertionError(); }
			if (cp == null) {
				cp = new ConstantPoolCommand();
				cp.addBytes(new byte[len]);
				tu.commands.add(cp);
			} else {
				cp.addBytes(new byte[len]);
			}
			tu.pos += len;
		}
	}
	
	/**
	 * adds the error handlers:
	 * 
	 * <pre>
	 * <code>
	 * :
	 *     CHARS 'UTF-8' "error: out of memory\n" |> OOM message
	 *     CHARS 'UTF-8' "error: could not load a dependency: '" |> DepLoad message start
	 *     CHARS 'UTF-8' "'\n" |> DepLoad message end
	 * >
	 * |> align
	 * OUT_OF_MEMORY:
	 *     MOV X00, STD_LOG
	 *     MOV X01, OOM_MSG_LENGTH
	 *     LEA X02, RELATIVE_OOM_MSG_POS
	 *     INT INT_STREAMS_WRITE
	 *     MOV X00, 8
	 *     INT INT_EXIT
	 * DEPENDENCY_LOAD_ERROR:
	 *     |> write start
	 *     MSG X03, X00
	 *     MOV X00, STD_LOG
	 *     MOV X01, DLE0_MSG_LENGTH
	 *     LEA X02, RELATIVE_DLE0_MSG_POS
	 *     INT INT_STREAMS_WRITE
	 *     |> write dependency
	 *     MOV X00, X03
	 *     INT INT_STR_LEN
	 *     MOV X02, X03
	 *     MOV X01, X00
	 *     MOV X00, STD_LOG
	 *     INT INT_STREAMS_WRITE
	 *     |> write end
	 *     MOV X01, DLE1_MSG_LENGTH
	 *     LEA X02, RELATIVE_DLE1_MSG_POS
	 *     MOV X00, 7
	 *     INT INT_EXIT
	 * </code>
	 * </pre>
	 * 
	 * @param tu the translation unit
	 */
	private static void addErrorHandlers(SimpleTU tu) {
		ConstantPoolCommand cp = new ConstantPoolCommand();
		cp.addBytes("error: out of memory\n".getBytes(StandardCharsets.UTF_8));
		long oomMsgPos  = tu.pos;
		long oomMsgLen  = cp.length();
		long dleMsg0Pos = oomMsgPos + oomMsgLen;
		cp.addBytes("error: could not load a dependency: '".getBytes(StandardCharsets.UTF_8));
		long dleMsg0Len = cp.length() - dleMsg0Pos;
		long dleMsg1Pos = dleMsg0Pos + oomMsgLen;
		cp.addBytes("'\n".getBytes(StandardCharsets.UTF_8));
		long dleMsg1Len = cp.length() - dleMsg1Pos;
		add(tu, cp);
		align(tu, 8L, cp);
		oomHandler(tu, oomMsgPos, oomMsgLen);
		dleHandler(tu, dleMsg0Pos, dleMsg0Len, dleMsg1Pos, dleMsg1Len);
	}
	
	/**
	 * <pre>
	 * <code>
	 * DEPENDENCY_LOAD_ERROR:
	 *     |> write start
	 *     MSG X03, X00
	 *     MOV X00, STD_LOG
	 *     MOV X01, DLE0_MSG_LENGTH
	 *     LEA X02, RELATIVE_DLE0_MSG_POS
	 *     INT INT_STREAMS_WRITE
	 *     |> write dependency
	 *     MOV X00, X03
	 *     INT INT_STR_LEN
	 *     MOV X02, X03
	 *     MOV X01, X00
	 *     MOV X00, STD_LOG
	 *     INT INT_STREAMS_WRITE
	 *     |> write end
	 *     MOV X01, DLE1_MSG_LENGTH
	 *     LEA X02, RELATIVE_DLE1_MSG_POS
	 *     INT INT_STREAMS_WRITE
	 *     MOV X00, 7
	 *     INT INT_EXIT
	 * </code>
	 * </pre>
	 */
	private static void dleHandler(SimpleTU tu, long dleMsg0Pos, long dleMsg0Len, long dleMsg1Pos, long dleMsg1Len) {
		tu.depLoad = tu.pos;
		label(tu, "DEPENDENCY_LOAD_ERROR");
		Param   x00             = build(A_SR, X_ADD);
		Param   x01             = build(A_SR, X_ADD + 1);
		Param   x02             = build(A_SR, X_ADD + 2);
		Param   x03             = build(A_SR, X_ADD + 3);
		Param   stdLog          = build(A_NUM, PrimAsmPreDefines.STD_LOG);
		Command intStreamsWrite = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_STREAM_WRITE), null);
		add(tu, new Command(Commands.CMD_MOV, x03, x00));
		add(tu, new Command(Commands.CMD_MOV, x00, stdLog));
		add(tu, new Command(Commands.CMD_MOV, x01, build(A_NUM, dleMsg0Len)));
		add(tu, new Command(Commands.CMD_LEA, x02, build(A_NUM, dleMsg0Pos - tu.pos)));
		add(tu, intStreamsWrite);
		add(tu, new Command(Commands.CMD_MOV, x00, x03));
		add(tu, new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_STR_LEN), null));
		add(tu, new Command(Commands.CMD_MOV, x02, x03));
		add(tu, new Command(Commands.CMD_MOV, x01, x00));
		add(tu, new Command(Commands.CMD_MOV, x00, stdLog));
		add(tu, intStreamsWrite);
		add(tu, new Command(Commands.CMD_MOV, x01, build(A_NUM, dleMsg1Len)));
		add(tu, new Command(Commands.CMD_LEA, x02, build(A_NUM, dleMsg1Pos - tu.pos)));
		add(tu, intStreamsWrite);
		add(tu, new Command(Commands.CMD_LEA, x00, build(A_NUM, 7)));
		add(tu, new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_EXIT), null));
	}
	
	private static void oomHandler(SimpleTU tu, long oomMsgPos, long oomMsgLen) throws AssertionError {
		tu.outOfMem = tu.pos;
		label(tu, "OUT_OF_MEMORY");
		Command movIDCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_NUM, PrimAsmPreDefines.STD_LOG));
		add(tu, movIDCmd);
		Command movStrLen = new Command(Commands.CMD_MOV, build(A_SR, X_ADD + 1L), build(A_NUM, oomMsgLen));
		add(tu, movStrLen);
		Command leaAddrCmd = new Command(Commands.CMD_LEA, build(A_SR, X_ADD + 2L), build(A_NUM, oomMsgPos - tu.pos));
		add(tu, leaAddrCmd);
		Command intOutOfMemErrCmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_STREAM_WRITE), null);
		add(tu, intOutOfMemErrCmd);
		Command movExitNumCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_NUM, 8L));
		add(tu, movExitNumCmd);
		Command intExitCmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_EXIT), null);
		add(tu, intExitCmd);
	}
	
	private static void label(SimpleTU tu, String name) throws AssertionError {
		if (tu.labels.put(name, Long.valueOf(tu.pos)) != null) {
			throw new AssertionError("multiple " + name + " labels");
		}
	}
	
	/**
	 * the main call will look like:
	 * 
	 * <pre>
	 * <code>
	 * MOV X02, X00
	 * MOV X00, MAIN_LENGTH
	 * INT INT_MEMORY_ALLOC
	 * MOV [X00], X02
	 * MOV [X00 + 8], X01
	 * MOV REG_METHOD_STRUCT, X00
	 * CALL main
	 * MOV X00, [X00]
	 * INT INT_EXIT
	 * </code>
	 * </pre>
	 * 
	 * @param tu   the translation unit
	 * @param main the main function
	 */
	private static void addMainCall(SimpleTU tu, SimpleFunction main) {
		if (!MAIN_TYPE.equals(main.type)) {
			throw new IllegalArgumentException("main function type is invalid: expected '" + MAIN_TYPE + "' got '" + main.type + "' ('" + main + "')");
		}
		if (tu.pos != 0L) throw new AssertionError(tu.pos);
		Command backupArgcCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD + 2L), build(A_SR, X_ADD));
		add(tu, backupArgcCmd);
		Command movSizeCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_NUM, MAIN_LENGTH));
		add(tu, movSizeCmd);
		Command intAllocCmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_MEMORY_ALLOC), null);
		add(tu, intAllocCmd);
		Command movArgcCmd = new Command(Commands.CMD_MOV, build(A_SR | B_REG, X_ADD), build(A_SR, X_ADD + 2L));
		add(tu, movArgcCmd);
		Command movArgvCmd = new Command(Commands.CMD_MOV, build(A_SR | B_NUM, X_ADD, 8), build(A_SR, X_ADD + 1L));
		add(tu, movArgvCmd);
		Command movFuncCmd = new Command(Commands.CMD_MOV, build(A_SR, REG_FUNC_STRUCT), build(A_SR, X_ADD));
		add(tu, movFuncCmd);
		Command callCmd = new Command(Commands.CMD_CALL, Param.createLabel(main.name), null);
		add(tu, callCmd);
		Command movExitNumCmd = new Command(Commands.CMD_MOV, build(A_SR, X_ADD), build(A_SR | B_REG, REG_FUNC_STRUCT));
		add(tu, movExitNumCmd);
		Command intExitCmd = new Command(Commands.CMD_INT, build(A_NUM, PrimAsmPreDefines.INT_EXIT), null);
		add(tu, intExitCmd);
	}
	
	private static void add(SimpleTU tu, Command cmd) {
		if (tu.pos == -1L) throw new AssertionError();
		tu.pos += cmd.length();
		tu.commands.add(cmd);
	}
	
	// no need for an ordered/single-threaded step
	@Override
	protected boolean skipCompile() { return true; }
	
	@Override
	protected void compile(@SuppressWarnings("unused") SimpleTU tu) throws IOException { throw new AssertionError("compile should be skipped"); }
	
	@Override
	protected void finish(SimpleTU tu) throws IOException {
		writeExports(tu);
		if (tu.target.length() != 0L) {
			tu.target.truncate(0L);
		}
		try (OutputStream bout = new BufferedOutputStream(tu.target.openWrite().asOutputStream(), bufferSize(tu))) {
			PrimitiveAssembler asm = new PrimitiveAssembler(bout, null, new Path[0], false, false);
			asm.assemble(tu.commands, tu.labels);
		}
	}
	
	@SuppressWarnings("unlikely-arg-type")
	private static int bufferSize(SimpleTU tu) throws IOException, ClosedChannelException {
		FS  fs = tu.target.fs();
		int bs = fs.blockSize();
		try {
			if (FSProvider.ofName(FSProvider.PATR_FS_PROVIDER_NAME).loadedFS().contains(fs)) {
				bs -= 8;
			}
		} catch (NoSuchProviderException e) {
			e.printStackTrace();
		}
		while (bs < 512) bs <<= 1;
		return bs;
	}
	
	private static void writeExports(SimpleTU tu) throws IOException {
		Iterator<SimpleExportable> iter = tu.sf.exportsIter();
		if (!iter.hasNext()) return;
		try (tu.expOut) {
			while (iter.hasNext()) {
				SimpleExportable se = iter.next();
				tu.expOut.write(se.toExportString());
				tu.expOut.write('\n');
			}
		}
	}
	
	public static long align(long addr, int bc) {
		if (bc > 4) bc = 8;
		if (Integer.bitCount(bc) != 1) bc = Integer.highestOneBit(bc) << 1;
		long bcm1 = bc - 1;
		if ((addr & bcm1) != 0L) {
			addr = (addr & ~bcm1) + bc;
		}
		return addr;
	}
	
	public static long align(long addr, long bc) {
		if (bc > 4L) bc = 8L;
		if (Long.bitCount(bc) != 1) bc = Long.highestOneBit(bc) << 1;
		long bcm1 = bc - 1;
		if ((addr & bcm1) != 0L) {
			addr = (addr & ~bcm1) + bc;
		}
		return addr;
	}
	
	private class SimpleSourceDependency extends SimpleDependency {
		
		private final Path dependency;
		
		public SimpleSourceDependency(String name, String runtimeDepend, Path dependency) {
			super(name, runtimeDepend);
			this.dependency = dependency;
		}
		
		@Override
		public SimpleExportable get(String name) {
			SimpleTU stu = SimpleCompiler.super.tus.get(this.dependency);
			if (stu == null) { throw new NoSuchElementException("the dependency " + this.depend + ": '" + this.dependency + "' could not be found"); }
			return stu.sf.getExport(name).changeRelative(this);
		}
		
		@Override
		public Iterator<SimpleExportable> getAll() {
			SimpleTU stu = SimpleCompiler.super.tus.get(this.dependency);
			if (stu == null) { throw new NoSuchElementException("the dependency " + this.depend + ": '" + this.dependency + "' could not be found"); }
			return stu.sf.exportsIter();
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
	}
	
	public static class SimpleSymbolDependency extends SimpleDependency {
		
		private final Map<String, SimpleExportable> imported;
		
		public SimpleSymbolDependency(String name, String runtimeDepend, Map<String, SimpleExportable> imported) {
			super(name, runtimeDepend);
			this.imported = Collections.unmodifiableMap(imported);
		}
		
		private static SimpleSymbolDependency create(String name, String run, Path path, Charset cs) {
			try (BufferedReader r = Files.newBufferedReader(path, cs)) {
				SimpleSymbolDependency dep = new SimpleSymbolDependency(name, run, new HashMap<>());
				dep.imported.putAll(SimpleExportable.readExports(dep, r));
				return dep;
			} catch (IOException e) {
				throw new IOError(e);
			}
			
		}
		
		@Override
		public SimpleExportable get(String name) {
			SimpleExportable se = this.imported.get(name);
			if (se == null) { throw new NoSuchElementException(name); }
			return se;
		}
		
		@Override
		public int hashCode() {
			return super.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}
		
		@Override
		public Iterator<SimpleExportable> getAll() {
			return this.imported.values().iterator();
		}
		
	}
	
	public class SimpleTU extends TranslationUnit implements TriFunction<String, String, String, SimpleDependency> {
		
		private List<Command>           commands = new ArrayList<>();
		private final Map<String, Long> labels   = new HashMap<>();
		private final SimpleFile        sf       = new SimpleFile(this);
		
		private long   outOfMem;
		private long   depLoad;
		private long   pos;
		private Writer expOut;
		
		public SimpleTU(Path source, File target) {
			super(source, target);
			this.commands.add(new CompilerCommandCommand(CompilerCommand.notAlign));
		}
		
		@Override
		public SimpleDependency apply(String name, String compileDepend, String runtimeDepend) {
			Path   path = SimpleCompiler.this.srcRoot.resolve(compileDepend);
			String dep;
			if (runtimeDepend != null) {
				dep = runtimeDepend;
			} else {
				dep = SimpleDependency.runtimeName(compileDepend);
			}
			SimpleDependency res = null;
			if (Files.exists(path)) {
				res = new SimpleSourceDependency(name, dep, path.normalize());
			} else {
				for (Path p : SimpleCompiler.this.lockups) {
					if (Files.exists(p)) {
						res = SimpleSymbolDependency.create(name, dep, path, SimpleCompiler.this.cs);
						break;
					}
				}
				if (res == null) { throw new NoSuchElementException("could not find the dependency " + name + "; '" + compileDepend + "'"); }
			}
			return res;
		}
		
	}
	
	// this method is used by ANTLR, can't be done in the SimpleConstant class
	// because the SimpleValue class is not known there
	/**
	 * creates a new {@link SimpleConstant}
	 * 
	 * @param name   the name of the constant
	 * @param val    the value of the constant
	 * @param export the export flag of the constant
	 * 
	 * @return the newly created constant
	 */
	public static SimpleConstant createConstant(String name, SimpleValue val, boolean export) {
		if (!(val instanceof SimpleValueConst c)) { throw new IllegalArgumentException("a constant needs a constant value (val: " + val + ')'); }
		return new SimpleConstant(name, c.getNumber(), export);
	}
	
	public static SimpleType createArray(SimpleType t, SimpleValue val) {
		if (val == null) { return new SimpleTypeArray(t, -1L); }
		if (!(val instanceof SimpleValueConst c)) {
			throw new IllegalArgumentException("the length of an array needs to be constant (val: " + val + ')');
		}
		return new SimpleTypeArray(t, c.getNumber());
	}
	
}
