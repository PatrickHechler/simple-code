package de.hechler.patrick.codesprachen.simple.compile.interfaces;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypeArray;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.types.SimpleTypePointer;

public interface SimpleExportable {
	
	static final char E_VAR_START  = 'V';
	static final char E_FUNC_START = 'F';
	static final char E_NAME_START = ':';
	
	static final char E_VAR_START_TYPE = '~';
	
	static final char E_T_FUNC_START            = '(';
	static final char E_T_FUNC_ARGS_RESULTS_SEP = '>';
	static final char E_T_FUNC_END              = ')';
	
	
	static final char E_T_STRUCT_START    = '{';
	static final char E_T_STRUCT_NAME_END = ':';
	static final char E_T_STRUCT_END      = '}';
	
	static final char E_T_POINTER     = '*';
	static final char E_T_ARRAY       = '[';
	static final char E_T_EMPTY_ARRAY = ']';
	
	static final char E_T_FPNUM  = 'f';
	static final char E_T_UNUM   = 'N';
	static final char E_T_NUM    = 'n';
	static final char E_T_UDWORD = 'D';
	static final char E_T_DWORD  = 'd';
	static final char E_T_UWORD  = 'W';
	static final char E_T_WORD   = 'w';
	static final char E_T_UBYTE  = 'B';
	static final char E_T_BYTE   = 'b';
	
	String toExportString();
	
	public static SimpleExportable fromExport(String str) {
		try {
			Map <String, UnfinishedType> structures = new HashMap <>();
			SimpleExportable export;
			char[] chars = str.toCharArray();
			int i = 0;
			switch (chars[i ++ ]) {
			case E_VAR_START: {
				int len;
				for (len = 0; chars[i + len] != E_NAME_START; len ++ );
				String hexAddress = new String(chars, i, len);
				i += len + 1;
				for (len = 0; chars[i + len] != E_VAR_START_TYPE; len ++ );
				String name = new String(chars, i, len);
				i += len + 1;
				List <SimpleVariable> varType = new ArrayList <>();
				i = readType(chars, i, varType, name.concat("_"), structures);
				if (i < chars.length) {
					throw new IllegalArgumentException("this is not an exported string! (str: '" + str + "')");
				}
				export = new SimpleVariable(Long.parseLong(hexAddress, 16), varType.get(0).type, name);
			}
			case E_FUNC_START: {
				int len;
				for (len = 0; chars[i + len] != E_NAME_START; len ++ );
				String hexAddress = new String(chars, i, len);
				i += len + 1;
				for (len = 0; chars[i + len] != E_T_FUNC_START; len ++ );
				String name = new String(chars, i, len);
				i += len + 1;
				if (chars[i ++ ] != E_T_FUNC_START) {
					throw new IllegalArgumentException("this is not an exported string! (str: '" + str + "')");
				}
				List <SimpleVariable> args = new ArrayList <>();
				while (chars[i] != E_T_FUNC_ARGS_RESULTS_SEP) {
					i = readType(chars, i, args, "arg", structures);
				}
				List <SimpleVariable> results = new ArrayList <>();
				while (chars[i] != E_T_FUNC_END) {
					i = readType(chars, i, results, "res", structures);
				}
				if (i < chars.length) {
					throw new IllegalArgumentException("this is not an exported string! (str: '" + str + "')");
				}
				finishList(structures, args);
				finishList(structures, results);
				export = new SimpleFunction(Long.parseLong(hexAddress, 16), name, args, results);
				break;
			}
			default:
				throw new IllegalArgumentException("this is not an exported string! (str: '" + str + "')");
			}
			return export;
		} catch (IndexOutOfBoundsException ioobe) {
			throw new IllegalArgumentException("this is not an exported string! (str: '" + str + "')", ioobe);
		}
	}
	
	private static void finishList(Map <String, UnfinishedType> structures, List <SimpleVariable> args) {
		for (int i = 0; i < args.size(); i ++ ) {
			SimpleVariable old = args.get(i);
			SimpleType finish = finish(old.type, structures);
			if (finish != old.type) {
				args.set(i, new SimpleVariable(finish, old.name));
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends SimpleType> T finish(T type, Map <String, UnfinishedType> structures) {
		if ( !type.isPrimitive()) {
			if (type.isPointerOrArray()) {
				SimpleType target = ((SimpleTypePointer) type).target;
				SimpleType nt = finish(target, structures);
				if (nt != target) {
					if (type.isArray()) {
						int ec = ((SimpleTypeArray) type).elementCount;
						type = (T) new SimpleTypeArray(nt, ec);
					} else {
						type = (T) new SimpleTypePointer(nt);
					}
				}
			} else if (type.isFunc()) {
				SimpleFuncType func = (SimpleFuncType) type;
				finishArray(structures, func.arguments);
				finishArray(structures, func.results);
			} else if (type.isStruct()) {
				SimpleStructType struct = (SimpleStructType) type;
				finishArray(structures, struct.members);
			} else {
				throw new InternalError("unknown type (not primitive, no pointer/array, no function and no structure)! type: " + type);
			}
		}
		return type;
	}
	
	private static void finishArray(Map <String, UnfinishedType> structures, SimpleVariable[] vars) {
		for (int i = 0; i < vars.length; i ++ ) {
			SimpleType finished = finish(vars[i].type, structures);
			if (finished != vars[i].type) {
				vars[i] = new SimpleVariable(finished, vars[i].name);
			}
		}
	}
	
	private static int readType(char[] chars, int i, List <SimpleVariable> list, String nameStart, Map <String, UnfinishedType> structures) {
		String name = nameStart.concat(Integer.toString(list.size()));
		SimpleType type;
		switch (chars[i ++ ]) {
		case E_T_FUNC_START: {
			List <SimpleVariable> args = new ArrayList <>();
			String subNameStart = name.concat("_arg");
			while (chars[i] != E_T_FUNC_ARGS_RESULTS_SEP) {
				i = readType(chars, i, args, subNameStart, structures);
			}
			List <SimpleVariable> results = new ArrayList <>();
			subNameStart = name.concat("_res");
			while (chars[i] != E_T_FUNC_END) {
				i = readType(chars, i, results, subNameStart, structures);
			}
			type = new SimpleFuncType(args, results);
			break;
		}
		case E_T_STRUCT_START: {
			int len;
			for (len = 0; chars[i + len] != E_T_STRUCT_NAME_END; len ++ );
			String structname = new String(chars, i, len);
			i += len + 1;
			UnfinishedType uf = structures.get(structname);
			if (uf == null) {
				String subNameStart = structname.concat("_m");
				List <SimpleVariable> sub = new ArrayList <>();
				while (chars[i] != E_T_STRUCT_END) {
					i = readType(chars, i, sub, subNameStart, structures);
				}
				type = new SimpleStructType(structname, sub);
			} else if (uf.finishedType != null) {
				type = uf.finishedType;
			} else {
				type = uf;
			}
			break;
		}
		case E_T_POINTER: {
			i = readType(chars, i, list, nameStart, structures);
			int index = list.size() - 1;
			type = new SimpleTypePointer(list.remove(index).type);
			break;
		}
		case E_T_ARRAY: {
			int len;
			for (len = 0; chars[i + len] >= '0' && chars[i + len] <= '9'; len ++ );
			int ec = Integer.parseInt(new String(chars, i, len - i));
			i = readType(chars, i + len, list, nameStart, structures);
			int index = list.size() - 1;
			type = new SimpleTypeArray(list.remove(index).type, ec);
			break;
		}
		case E_T_EMPTY_ARRAY: {
			i = readType(chars, i, list, nameStart, structures);
			int index = list.size() - 1;
			type = new SimpleTypeArray(list.remove(index).type, -1);
			break;
		}
		case E_T_FPNUM:
			type = SimpleType.FPNUM;
			break;
		case E_T_UNUM:
			type = SimpleType.UNUM;
			break;
		case E_T_NUM:
			type = SimpleType.NUM;
			break;
		case E_T_UDWORD:
			type = SimpleType.UDWORD;
			break;
		case E_T_DWORD:
			type = SimpleType.DWORD;
			break;
		case E_T_UWORD:
			type = SimpleType.UWORD;
			break;
		case E_T_WORD:
			type = SimpleType.WORD;
			break;
		case E_T_UBYTE:
			type = SimpleType.UBYTE;
			break;
		case E_T_BYTE:
			type = SimpleType.BYTE;
			break;
		default:
			throw new IllegalArgumentException("this is not an exported string! (str: '" + new String(chars) + "')");
		}
		list.add(new SimpleVariable(type, name));
		return i;
	}
	
	public static class UnfinishedType implements SimpleType {
		
		public SimpleType finishedType = null;
		
		public UnfinishedType() {}
		
		@Override
		public boolean isPrimitive() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isPointerOrArray() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isPointer() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isArray() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isStruct() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public boolean isFunc() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int byteCount() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public void appendToExportStr(StringBuilder build, Set <String> exportedStructs) {
			throw new UnsupportedOperationException();
		}
		
		
		@Override
		public String toString() {
			return "<some-unfinished-type>";
		}
		
	}
	
}
