package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import static java.util.List.of;
import static de.hechler.patrick.codesprachen.primitive.core.utils.PrimAsmPreDefines.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunctionSymbol;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;

@SuppressWarnings({"javadoc", "unused"})
public class StdLib {
	
	public static class StdLibFunc extends SimpleFunctionSymbol {
		
		public final long intnum;
		
		public StdLibFunc(long intnum, String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> results) {
			super(true, name, args, results);
			this.intnum = intnum;
		}
		
	}
	
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
	
	public static final Map<String, StdLibFunc> ALL_INTERRUPTS = Collections.unmodifiableMap(allInts());
	
	// GENERATED-CODE-START
	// this code-block is automatic generated, do not modify
	private static Map<String, StdLibFunc> allInts() {
		Map<String, StdLibFunc> res = new HashMap<>();
		res.put("INT_ERROR_ILLEGAL_INTERRUPT", slf(0, "errorIllegalInterrupt", of(sv(NUM, 0, "intnum")), of()));
		res.put("INT_ERROR_UNKNOWN_COMMAND", slf(1, "errorUnknownCommand", of(), of()));
		res.put("INT_ERROR_ILLEGAL_MEMORY", slf(2, "errorIllegalMemory", of(), of()));
		res.put("INT_ERROR_ARITHMETIC_ERROR", slf(3, "errorArithmeticError", of(), of()));
		res.put("INT_EXIT", slf(4, "exit", of(sv(NUM, 0, "exitnum")), of()));
		res.put("INT_MEMORY_ALLOC", slf(5, "memoryAlloc", of(sv(UNUM, 0, "len")), of(sv(UBYTE, 1, "mem"))));
		res.put("INT_MEMORY_REALLOC", slf(6, "memoryRealloc", of(sv(UBYTE, 1, "old_mem"), sv(UNUM, 0, "new_len")), of(sv(NUM, 0, "ignored0"), sv(UBYTE, 1, "new_mem"))));
		res.put("INT_MEMORY_FREE", slf(7, "memoryFree", of(sv(UBYTE, 1, "mem")), of()));
		res.put("INT_OPEN_STREAM", slf(8, "openStream", of(sv(CHAR, 1, "file_name"), sv(UNUM, 0, "mode")), of(sv(NUM, 0, "id"))));
		res.put("INT_STREAM_WRITE", slf(9, "streamWrite", of(sv(NUM, 0, "id"), sv(UNUM, 0, "len"), sv(UBYTE, 1, "data")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "wrote"))));
		res.put("INT_STREAM_READ", slf(10, "streamRead", of(sv(NUM, 0, "id"), sv(UNUM, 0, "len"), sv(UBYTE, 1, "data")), of(sv(NUM, 0, "ignored0"), sv(UNUM, 0, "read"))));
		res.put("INT_STREAM_CLOSE", slf(11, "streamClose", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "success"))));
		res.put("INT_STREAM_FILE_GET_POS", slf(12, "streamFileGetPos", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "pos"))));
		res.put("INT_STREAM_FILE_SET_POS", slf(13, "streamFileSetPos", of(sv(NUM, 0, "id"), sv(UNUM, 0, "pos")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "success"))));
		res.put("INT_STREAM_FILE_ADD_POS", slf(14, "streamFileAddPos", of(sv(NUM, 0, "id"), sv(NUM, 0, "add")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "pos"))));
		res.put("INT_STREAM_FILE_SEEK_EOF", slf(15, "streamFileSeekEof", of(sv(NUM, 0, "id")), of(sv(NUM, 0, "ignored0"), sv(NUM, 0, "pos"))));
		res.put("INT_OPEN_FILE", slf(16, "openFile", of(sv(CHAR, 1, "file")), of(sv(NUM, 0, "id"))));
		res.put("INT_OPEN_FOLDER", slf(17, "openFolder", of(), of()));
		res.put("INT_OPEN_PIPE", slf(18, "openPipe", of(), of()));
		res.put("INT_OPEN_ELEMENT", slf(19, "openElement", of(), of()));
		res.put("INT_ELEMENT_OPEN_PARENT", slf(20, "elementOpenParent", of(), of()));
		res.put("INT_ELEMENT_GET_CREATE", slf(21, "elementGetCreate", of(), of()));
		res.put("INT_ELEMENT_GET_LAST_MOD", slf(22, "elementGetLastMod", of(), of()));
		res.put("INT_ELEMENT_SET_CREATE", slf(23, "elementSetCreate", of(), of()));
		res.put("INT_ELEMENT_SET_LAST_MOD", slf(24, "elementSetLastMod", of(), of()));
		res.put("INT_ELEMENT_DELETE", slf(25, "elementDelete", of(), of()));
		res.put("INT_ELEMENT_MOVE", slf(26, "elementMove", of(), of()));
		res.put("INT_ELEMENT_GET_NAME", slf(27, "elementGetName", of(), of()));
		res.put("INT_ELEMENT_GET_FLAGS", slf(28, "elementGetFlags", of(), of()));
		res.put("INT_ELEMENT_MODIFY_FLAGS", slf(29, "elementModifyFlags", of(), of()));
		res.put("INT_FOLDER_CHILD_COUNT", slf(30, "folderChildCount", of(), of()));
		res.put("INT_FOLDER_OPEN_CHILD_OF_NAME", slf(31, "folderOpenChildOfName", of(), of()));
		res.put("INT_FOLDER_OPEN_CHILD_FOLDER_OF_NAME", slf(32, "folderOpenChildFolderOfName", of(), of()));
		res.put("INT_FOLDER_OPEN_CHILD_FILE_OF_NAME", slf(33, "folderOpenChildFileOfName", of(), of()));
		res.put("INT_FOLDER_OPEN_CHILD_PIPE_OF_NAME", slf(34, "folderOpenChildPipeOfName", of(), of()));
		res.put("INT_FOLDER_CREATE_CHILD_FOLDER", slf(35, "folderCreateChildFolder", of(), of()));
		res.put("INT_FOLDER_CREATE_CHILD_FILE", slf(36, "folderCreateChildFile", of(), of()));
		res.put("INT_FOLDER_CREATE_CHILD_PIPE", slf(37, "folderCreateChildPipe", of(), of()));
		res.put("INT_FOLDER_OPEN_ITER", slf(38, "folderOpenIter", of(), of()));
		res.put("INT_FILE_LENGTH", slf(39, "fileLength", of(), of()));
		res.put("INT_FILE_TRUNCATE", slf(40, "fileTruncate", of(), of()));
		res.put("INT_HANDLE_OPEN_STREAM", slf(41, "handleOpenStream", of(), of()));
		res.put("INT_PIPE_LENGTH", slf(42, "pipeLength", of(), of()));
		res.put("INT_TIME_GET", slf(43, "timeGet", of(), of()));
		res.put("INT_TIME_RES", slf(44, "timeRes", of(), of()));
		res.put("INT_TIME_SLEEP", slf(45, "timeSleep", of(), of()));
		res.put("INT_TIME_WAIT", slf(46, "timeWait", of(), of()));
		res.put("INT_RND_OPEN", slf(47, "rndOpen", of(), of()));
		res.put("INT_RND_NUM", slf(48, "rndNum", of(), of()));
		res.put("INT_MEM_CMP", slf(49, "memCmp", of(), of()));
		res.put("INT_MEM_CPY", slf(50, "memCpy", of(), of()));
		res.put("INT_MEM_MOV", slf(51, "memMov", of(), of()));
		res.put("INT_MEM_BSET", slf(52, "memBset", of(), of()));
		res.put("INT_STR_LEN", slf(53, "strLen", of(), of()));
		res.put("INT_STR_CMP", slf(54, "strCmp", of(), of()));
		res.put("INT_STR_FROM_NUM", slf(55, "strFromNum", of(), of()));
		res.put("INT_STR_FROM_FPNUM", slf(56, "strFromFpnum", of(), of()));
		res.put("INT_STR_TO_NUM", slf(57, "strToNum", of(), of()));
		res.put("INT_STR_TO_FPNUM", slf(58, "strToFpnum", of(), of()));
		res.put("INT_STR_TO_U16STR", slf(59, "strToU16str", of(), of()));
		res.put("INT_STR_TO_U32STR", slf(60, "strToU32str", of(), of()));
		res.put("INT_STR_FROM_U16STR", slf(61, "strFromU16str", of(), of()));
		res.put("INT_STR_FROM_U32STR", slf(62, "strFromU32str", of(), of()));
		res.put("INT_STR_FORMAT", slf(63, "strFormat", of(), of()));
		res.put("INT_LOAD_FILE", slf(64, "loadFile", of(), of()));
		res.put("INT_LOAD_LIB", slf(65, "loadLib", of(), of()));
		res.put("INT_UNLOAD_LIB", slf(66, "unloadLib", of(), of()));
		return res;
}
	
	// here is the end of the automatic generated code-block
	// GENERATED-CODE-END
	
	
	private static StdLibFunc slf(long intnum, String name, List<SimpleOffsetVariable> args, List<SimpleOffsetVariable> ress) {
		return new StdLibFunc(intnum, name, args, ress);
	}
	
	private static SimpleOffsetVariable sv(SimpleType type, int deapth, String name) {
		if (deapth < 0) throw new AssertionError();
		while (deapth > 0) type = new SimpleTypePointer(type);
		return new SimpleVariable.SimpleOffsetVariable(type, name);
	}
	
}
