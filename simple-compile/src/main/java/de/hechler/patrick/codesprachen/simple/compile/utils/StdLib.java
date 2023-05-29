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
package de.hechler.patrick.codesprachen.simple.compile.utils;

import static java.util.List.of;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import de.hechler.patrick.codesprachen.primitive.assemble.enums.Commands;
import de.hechler.patrick.codesprachen.primitive.core.objects.PrimitiveConstant;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmConstants;
import de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines;
import de.hechler.patrick.codesprachen.simple.compile.objects.SimpleFile.SimpleDependency;
import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunctionSymbol;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

@SuppressWarnings({ "javadoc", "unused" })
public class StdLib {
	
	private static final String STRUCT_BIG_NUM_NAME = "bigNum";
	private static final long   COMPARE_FLAGS       = PrimAsmPreDefines.STATUS_GREATER | PrimAsmPreDefines.STATUS_EQUAL | PrimAsmPreDefines.STATUS_LOWER;
	
	public abstract static class StdLibFunc extends SimpleFunctionSymbol {
		
		public StdLibFunc(String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> results) {
			super(true, name, args, results);
		}
		
		@Override
		public SimpleExportable changeRelative(Object relative) {
			throw new AssertionError("change relative called on a std lib function");
		}
		
	}
	
	public static class StdLibFuncCmd extends StdLibFunc {
		
		public final Commands command;
		
		public StdLibFuncCmd(Commands cmd, String name, SimpleOffsetVariable arg, SimpleOffsetVariable result) {
			super(name, of(arg), of(result));
			if (cmd.params != 1) throw new AssertionError();
			this.command = cmd;
		}
		
		public StdLibFuncCmd(Commands cmd, String name, SimpleOffsetVariable arg, SimpleOffsetVariable arg2, SimpleOffsetVariable result) {
			super(name, of(arg, arg2), of(result));
			if (cmd.params != 2) throw new AssertionError();
			this.command = cmd;
		}
		
		public StdLibFuncCmd(Commands cmd, String name, SimpleOffsetVariable arg, SimpleOffsetVariable arg2, SimpleOffsetVariable result,
				SimpleOffsetVariable result2) {
			super(name, of(arg, arg2), of(result));
			if (cmd.params != 2) throw new AssertionError();
			this.command = cmd;
		}
		
	}
	
	public static class StdLibIntFunc extends StdLibFunc {
		
		public final long intnum;
		
		public StdLibIntFunc(long intnum, String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> results) {
			super(name, args, results);
			this.intnum = intnum;
		}
		
	}
	
	public static class StdLibIntFunc2 extends StdLibIntFunc {
		
		public final long flags;
		
		public StdLibIntFunc2(long intnum, String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> results, long statusFlags) {
			super(intnum, name, args, results);
			if (statusFlags != COMPARE_FLAGS) throw new AssertionError(Long.toHexString(statusFlags));
			this.flags = statusFlags;
		}
		
	}
	
	private StdLib() {}
	
	private static final SimpleType FPNUM  = SimpleType.FPNUM;
	private static final SimpleType NUM    = SimpleType.NUM;
	private static final SimpleType UNUM   = SimpleType.UNUM;
	private static final SimpleType DWORD  = SimpleType.DWORD;
	private static final SimpleType UDWORD = SimpleType.UDWORD;
	private static final SimpleType WORD   = SimpleType.WORD;
	private static final SimpleType UWORD  = SimpleType.UWORD;
	private static final SimpleType BYTE   = SimpleType.BYTE;
	private static final SimpleType UBYTE  = SimpleType.UBYTE;
	private static final SimpleType CHAR   = SimpleType.UBYTE;
	
	public static final Map<String, SimpleConstant>       ALL_CONSTANTS = Collections.unmodifiableMap(allConsts());
	public static final Map<String, SimpleOffsetVariable> ALL_VARS      = Collections.unmodifiableMap(allVars());
	public static final Map<String, SimpleStructType>     ALL_STRUCTS   = Collections.unmodifiableMap(allStructs());
	public static final Map<String, StdLibFunc>           ALL_FUNCS     = Collections.unmodifiableMap(allFuncs());
	
	public static final SimpleDependency DEP = new SimpleDependency("std", null, true) {
		
		private final Map<String, SimpleExportable> map = init();
		
		private Map<String, SimpleExportable> init() {
			Map<String, SimpleExportable> res = new HashMap<>();
			res.putAll(ALL_FUNCS);
			for (SimpleConstant sc : ALL_CONSTANTS.values()) {
				if (res.put(sc.name(), sc) != null) {
					throw new AssertionError("multiple values in StdLib with same name: " + sc.name());
				}
			}
			for (SimpleOffsetVariable sc : ALL_VARS.values()) {
				if (res.put(sc.name(), sc) != null) {
					throw new AssertionError("multiple values in StdLib with same name: " + sc.name());
				}
			}
			for (SimpleStructType sc : ALL_STRUCTS.values()) {
				if (res.put(sc.name(), sc) != null) {
					throw new AssertionError("multiple values in StdLib with same name: " + sc.name());
				}
			}
			for (SimpleExportable se : res.values()) {
				switch (se) {
				case @SuppressWarnings("preview") SimpleOffsetVariable sov -> sov.init(-2L, this);
				case @SuppressWarnings("preview") SimpleConstant sc -> {/**/}
				case @SuppressWarnings("preview") SimpleStructType sst -> {/**/}
				case @SuppressWarnings("preview") StdLibFunc slf -> slf.init(-2L, this);
				default -> throw new AssertionError("unknown type: " + se.getClass().getName());
				}
			}
			return res;
		}
		
		@Override
		public SimpleExportable get(String name) {
			SimpleExportable val = this.map.get(name);
			if (val != null) return val;
			throw new NoSuchElementException("there is no export with the name '" + name + "' in the std dependency");
		}
		
		@Override
		public Iterator<SimpleExportable> getAll() {
			return this.map.values().iterator();
		}
		
		@Override
		public SimpleExportable changeRelative(Object relative) {
			throw new AssertionError("change relative used on StdLib");
		}
		
	};
	
	private static Map<String, StdLibFunc> allFuncs() {
		Map<String, StdLibFunc> res    = allInts();
		SimpleStructType        bigNum = ALL_STRUCTS.get(STRUCT_BIG_NUM_NAME);
		res.put("bigAdd", new StdLibFuncCmd(Commands.CMD_BADD, "bigAdd", sv(bigNum, 0, "valA"), sv(bigNum, 0, "valB"), sv(bigNum, 0, "res")));
		res.put("bigSub", new StdLibFuncCmd(Commands.CMD_BSUB, "bigSub", sv(bigNum, 0, "valA"), sv(bigNum, 0, "valB"), sv(bigNum, 0, "res")));
		res.put("bigMul", new StdLibFuncCmd(Commands.CMD_BMUL, "bigMul", sv(bigNum, 0, "valA"), sv(bigNum, 0, "valB"), sv(bigNum, 0, "res")));
		res.put("bigDiv", new StdLibFuncCmd(Commands.CMD_BDIV, "bigDiv", sv(bigNum, 0, "valA"), sv(bigNum, 0, "valB"), sv(bigNum, 0, "res"), sv(bigNum, 0, "mod")));
		res.put("bigNeg", new StdLibFuncCmd(Commands.CMD_BNEG, "bigNeg", sv(bigNum, 0, "val"), sv(bigNum, 0, "res")));
		return res;
	}
	
	// GENERATED-CODE-START
	// this code-block is automatic generated, do not modify
	private static Map<String, StdLibFunc> allInts() {
		Map<String, StdLibFunc> res = new HashMap<>();
		res.put("errorIllegalInterrupt", slf(0, "errorIllegalInterrupt", of(sv(NUM, 0, "intnum")), of()));
		res.put("errorUnknownCommand", slf(1, "errorUnknownCommand", of(), of()));
		res.put("errorIllegalMemory", slf(2, "errorIllegalMemory", of(), of()));
		res.put("errorArithmeticError", slf(3, "errorArithmeticError", of(), of()));
		res.put("exit", slf(4, "exit", of(sv(UBYTE, 0, "exitnum")), of()));
		res.put("memoryAlloc", slf(5, "memoryAlloc", of(sv(UNUM, 0, "len")), of(sv(UBYTE, 1, "mem"))));
		res.put("memoryRealloc", slf(6, "memoryRealloc", of(sv(UBYTE, 1, "oldMem"), sv(UNUM, 0, "newLen")), of(sv(NUM, 0, "ignored0"), sv(UBYTE, 1, "newMem"))));
		res.put("memoryFree", slf(7, "memoryFree", of(sv(UBYTE, 1, "mem")), of()));
		res.put("streamOpen", slf(8, "streamOpen", of(sv(CHAR, 1, "fileName"), sv(UNUM, 0, "flags")), of(sv(NUM, 0, "id"))));
		res.put("streamWrite",
				slf(9, "streamWrite", of(sv(NUM, 0, "id"), sv(UNUM, 0, "len"), sv(UBYTE, 1, "data")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "wrote"))));
		res.put("streamRead",
				slf(10, "streamRead", of(sv(NUM, 0, "id"), sv(UNUM, 0, "len"), sv(UBYTE, 1, "data")), of(sv(NUM, 0, "ignored0"), sv(UNUM, 0, "read"))));
		res.put("streamClose", slf(11, "streamClose", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "success"))));
		res.put("streamFileGetPos", slf(12, "streamFileGetPos", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "pos"))));
		res.put("streamFileSetPos", slf(13, "streamFileSetPos", of(sv(NUM, 0, "id"), sv(UNUM, 0, "pos")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("streamFileAddPos", slf(14, "streamFileAddPos", of(sv(NUM, 0, "id"), sv(NUM, 0, "add")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "pos"))));
		res.put("streamFileSeekEof", slf(15, "streamFileSeekEof", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "pos"))));
		res.put("streamFile", slf(16, "streamFile", of(sv(CHAR, 1, "file")), of(sv(NUM, 0, "id"))));
		res.put("streamFolder", slf(17, "streamFolder", of(sv(CHAR, 1, "folder")), of(sv(NUM, 0, "id"))));
		res.put("streamPipe", slf(18, "streamPipe", of(sv(CHAR, 1, "pipe")), of(sv(NUM, 0, "id"))));
		res.put("streamElement", slf(19, "streamElement", of(sv(CHAR, 1, "element")), of(sv(NUM, 0, "id"))));
		res.put("elementOpenParent", slf(20, "elementOpenParent", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "parent_id"))));
		res.put("elementGetCreate", slf(21, "elementGetCreate", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "date"))));
		res.put("elementGetLastMod", slf(22, "elementGetLastMod", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "date"))));
		res.put("elementSetCreate", slf(23, "elementSetCreate", of(sv(NUM, 0, "id"), sv(NUM, 0, "date")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("elementSetLastMod", slf(24, "elementSetLastMod", of(sv(NUM, 0, "id"), sv(NUM, 0, "date")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("elementDelete", slf(25, "elementDelete", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("elementMove",
				slf(26, "elementMove", of(sv(NUM, 0, "id"), sv(CHAR, 1, "newName"), sv(NUM, 0, "newParentId")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("elementGetName", slf(27, "elementGetName", of(sv(NUM, 0, "id"), sv(CHAR, 1, "buffer")), of(sv(NUM, 0, "ignored0"), sv(CHAR, 1, "name"))));
		res.put("elementGetFlags", slf(28, "elementGetFlags", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "flags"))));
		res.put("elementModifyFlags", slf(29, "elementModifyFlags", of(sv(NUM, 0, "id"), sv(UDWORD, 0, "addFlags"), sv(UDWORD, 0, "remFlags")),
				of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("folderChildCount", slf(30, "folderChildCount", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childCount"))));
		res.put("folderOpenChildOfName",
				slf(31, "folderOpenChildOfName", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderOpenChildFolderOfName",
				slf(32, "folderOpenChildFolderOfName", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderOpenChildFileOfName",
				slf(33, "folderOpenChildFileOfName", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderOpenChildPipeOfName",
				slf(34, "folderOpenChildPipeOfName", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderCreateChildFolder",
				slf(35, "folderCreateChildFolder", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderCreateChildFile",
				slf(36, "folderCreateChildFile", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderCreateChildPipe",
				slf(37, "folderCreateChildPipe", of(sv(NUM, 0, "id"), sv(CHAR, 1, "name")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "childId"))));
		res.put("folderOpenIter", slf(38, "folderOpenIter", of(sv(NUM, 0, "id"), sv(NUM, 0, "showHidden")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "iterId"))));
		res.put("fileLength", slf(39, "fileLength", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "len"))));
		res.put("fileTruncate", slf(40, "fileTruncate", of(sv(NUM, 0, "id"), sv(NUM, 0, "len")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("handleOpenStream", slf(41, "handleOpenStream", of(sv(NUM, 0, "id"), sv(UDWORD, 0, "flags")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "streamId"))));
		res.put("pipeLength", slf(42, "pipeLength", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "len"))));
		res.put("timeGet", slf(43, "timeGet", of(), of(sv(NUM, 0, "secs"), sv(NUM, 0, "nanos"), sv(NUM, 0, "success"))));
		res.put("timeRes", slf(44, "timeRes", of(), of()));
		res.put("timeSleep",
				slf(45, "timeSleep", of(sv(NUM, 0, "secs"), sv(NUM, 0, "nanos")), of(sv(NUM, 0, "remainSecs"), sv(NUM, 0, "remainNanos"), sv(NUM, 0, "success"))));
		res.put("timeWait",
				slf(46, "timeWait", of(sv(NUM, 0, "secs"), sv(NUM, 0, "nanos")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "ignored1"), sv(NUM, 0, "success"))));
		res.put("rndOpen", slf(47, "rndOpen", of(), of(sv(NUM, 0, "id"))));
		res.put("rndNum", slf(48, "rndNum", of(), of(sv(NUM, 0, "rnd"))));
		res.put("memCmp", slf(49, "memCmp", of(sv(UBYTE, 1, "memA"), sv(UBYTE, 1, "memB"), sv(NUM, 0, "len")), of(sv(0x7L, "cmpRes")), 0x7L));
		res.put("memCpy", slf(50, "memCpy", of(sv(UBYTE, 1, "dstMem"), sv(UBYTE, 1, "srcMem"), sv(NUM, 0, "len")), of()));
		res.put("memMov", slf(51, "memMov", of(sv(UBYTE, 1, "srcMem"), sv(UBYTE, 1, "dstMem"), sv(NUM, 0, "len")), of()));
		res.put("memBset", slf(52, "memBset", of(sv(UBYTE, 1, "mem"), sv(UBYTE, 0, "val"), sv(NUM, 0, "len")), of()));
		res.put("strLen", slf(53, "strLen", of(sv(CHAR, 1, "str")), of(sv(NUM, 0, "len"))));
		res.put("strCmp", slf(54, "strCmp", of(sv(CHAR, 1, "strA"), sv(CHAR, 1, "strB")), of(sv(0x7L, "cmpRes")), 0x7L));
		res.put("strFromNum", slf(55, "strFromNum", of(sv(NUM, 0, "val"), sv(CHAR, 1, "buf"), sv(NUM, 0, "base"), sv(UNUM, 0, "bufLen")),
				of(sv(NUM, 0, "strLen"), sv(CHAR, 1, "str"), sv(NUM, 0, "ignored0"), sv(NUM, 0, "newBufLen"))));
		res.put("strFromFpnum", slf(56, "strFromFpnum", of(sv(FPNUM, 0, "val"), sv(CHAR, 1, "buf"), sv(UNUM, 0, "bufLen")),
				of(sv(NUM, 0, "strLen"), sv(CHAR, 1, "str"), sv(NUM, 0, "newBufLen"))));
		res.put("strToNum", slf(57, "strToNum", of(sv(CHAR, 1, "str"), sv(NUM, 0, "base")), of(sv(NUM, 0, "val"), sv(NUM, 0, "success"))));
		res.put("strToFpnum", slf(58, "strToFpnum", of(sv(CHAR, 1, "str")), of(sv(FPNUM, 0, "val"), sv(NUM, 0, "success"))));
		res.put("strToU16str", slf(59, "strToU16str", of(sv(CHAR, 1, "u8str"), sv(UWORD, 1, "u16str"), sv(NUM, 0, "bufLen")),
				of(sv(CHAR, 1, "u8strUnconcStart"), sv(UWORD, 1, "u16strUnconvStart"), sv(NUM, 0, "remBufLen"), sv(NUM, 0, "remU8Len"))));
		res.put("strToU32str", slf(60, "strToU32str", of(sv(CHAR, 1, "u8str"), sv(UDWORD, 1, "u32str"), sv(NUM, 0, "bufLen")),
				of(sv(CHAR, 1, "u8strUnconcStart"), sv(UDWORD, 1, "u32strUnconvStart"), sv(NUM, 0, "remBufLen"), sv(NUM, 0, "remU8Len"))));
		res.put("strFromU16str", slf(61, "strFromU16str", of(sv(UWORD, 1, "u16str"), sv(CHAR, 1, "u8str"), sv(NUM, 0, "bufLen")),
				of(sv(UWORD, 1, "u16strUnconcStart"), sv(CHAR, 1, "u8strUnconvStart"), sv(NUM, 0, "remBufLen"), sv(NUM, 0, "remU8Len"))));
		res.put("strFromU32str", slf(62, "strFromU32str", of(sv(UWORD, 1, "u32str"), sv(CHAR, 1, "u8str"), sv(NUM, 0, "bufLen")),
				of(sv(UWORD, 1, "u32strUnconcStart"), sv(CHAR, 1, "u8strUnconvStart"), sv(NUM, 0, "remBufLen"), sv(NUM, 0, "remU8Len"))));
		res.put("strFormat",
				slf(63, "strFormat", of(sv(CHAR, 1, "frmtStr"), sv(CHAR, 1, "outStr"), sv(NUM, 0, "bufLen"), sv(NUM, 1, "args")), of(sv(NUM, 0, "outLen"))));
		res.put("loadFile", slf(64, "loadFile", of(sv(CHAR, 1, "file")), of(sv(UBYTE, 1, "data"), sv(NUM, 0, "len"))));
		res.put("loadLib", slf(65, "loadLib", of(sv(CHAR, 1, "file")), of(sv(UBYTE, 1, "data"), sv(NUM, 0, "len"), sv(NUM, 0, "loaded"))));
		res.put("unloadLib", slf(66, "unloadLib", of(sv(UBYTE, 1, "data")), of()));
		return res;
	}
	
	// here is the end of the automatic generated code-block
	// GENERATED-CODE-END
	
	private static Map<String, SimpleConstant> allConsts() {
		Map<String, SimpleConstant> res = new HashMap<>();
		for (PrimitiveConstant pc : PrimAsmConstants.START_CONSTANTS.values()) {
			if (pc.name().startsWith("INT_")) continue;
			String name;
			if (pc.name().startsWith("STD_")) {
				name = name(pc.name().substring(4));
			} else {
				name = name(pc.name());
			}
			res.put(name, new SimpleConstant(name, pc.value(), true));
		}
		return res;
	}
	
	private static Map<String, SimpleStructType> allStructs() {
		Map<String, SimpleStructType> res = new HashMap<>();
		res.put(STRUCT_BIG_NUM_NAME, new SimpleStructType(STRUCT_BIG_NUM_NAME, true,
				of(sv(UNUM, 0, "lowbits"), sv(NUM, 0, "highbits"))));
		return res;
	}
	
	private static Map<String, SimpleOffsetVariable> allVars() {
		Map<String, SimpleOffsetVariable> res = new HashMap<>();
		res.put("errno", sv(NUM, 0, "errno"));
		return res;
	}
	
	private static String name(String name) {
		StringBuilder b = new StringBuilder(name.length());
		for (int i = 0; i < name.length();) {
			int ni = name.indexOf('_', i);
			if (i == 0) i--;
			else b.append(name.charAt(i));
			if (ni == -1) ni = name.length();
			String sub = name.substring(i + 1, ni);
			b.append(sub.toLowerCase());
			i = ni + 1;
		}
		return b.toString();
	}
	
	private static StdLibIntFunc slf(long intnum, String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> ress) {
		return new StdLibIntFunc(intnum, name, args, ress);
	}
	
	private static StdLibIntFunc slf(long intnum, String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> ress, long flags) {
		return new StdLibIntFunc2(intnum, name, args, ress, flags);
	}
	
	private static SimpleOffsetVariable sv(SimpleType type, int deapth, String name) {
		if (deapth < 0) throw new AssertionError();
		while (deapth-- > 0) type = new SimpleTypePointer(type);
		return new SimpleVariable.SimpleOffsetVariable(type, name);
	}
	
	private static SimpleOffsetVariable sv(long statusFlags, String name) {
		if (statusFlags != COMPARE_FLAGS) throw new AssertionError(Long.toHexString(statusFlags));
		return new SimpleVariable.SimpleOffsetVariable(SimpleType.NUM, name);
	}
	
}
