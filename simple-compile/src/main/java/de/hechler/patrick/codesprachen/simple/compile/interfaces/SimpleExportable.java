package de.hechler.patrick.codesprachen.simple.compile.interfaces;

import java.io.IOError;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;

import de.hechler.patrick.codesprachen.simple.symbol.SimpleExportGrammarLexer;
import de.hechler.patrick.codesprachen.simple.symbol.SimpleExportGrammarParser;
import de.hechler.patrick.codesprachen.simple.symbol.SimpleExportGrammarParser.SimpleExportsContext;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleConstant;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleFunction;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypeArray;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePrimitive;


public interface SimpleExportable {
	
	static char UNKNOWN_SIZE_ARRAY = ']';
	static char ARRAY              = '[';
	static char POINTER            = '#';
	
	static char PRIM_FPNUM  = '.';
	static char PRIM_NUM    = '-';
	static char PRIM_UNUM   = '+';
	static char PRIM_DWORD  = '\\';
	static char PRIM_UDWORD = '/';
	static char PRIM_WORD   = '!';
	static char PRIM_UWORD  = '&';
	static char PRIM_BYTE   = '"';
	static char PRIM_UBYTE  = '\'';
	
	static char FUNC   = '*';
	static char VAR    = ';';
	static char STRUCT = '~';
	static char CONST  = '%';
	
	static char NAME_TYPE_SEP = ':';
	static char VAR_SEP       = ',';
	
	boolean isExport();
	
	String toExportString();
	
	String name();
	
	static SimpleExportable[] correctImports(Map <String, SimpleStructType> structs, List <SimpleExportable> imps) {
		int impcnt = imps.size();
		SimpleExportable[] result = new SimpleExportable[structs.size() + impcnt];
		for (int i = 0; i < impcnt; i ++ ) {
			SimpleExportable se = imps.get(i);
			/* @formatter:off if (se instanceof SimpleStructType) {
				// won't find the exported structures here
			} else // */ // @formatter:on
			if (se instanceof SimpleConstant) {
				// nothing to do
			} else if (se instanceof SimpleFunction) {
				SimpleFunction sf = (SimpleFunction) se;
				correctArray(structs, sf.type.arguments);
				correctArray(structs, sf.type.results);
			} else if (se instanceof SimpleVariable) {
				SimpleVariable sv = (SimpleVariable) se;
				SimpleType corrected = correctType(structs, sv.type);
				if (corrected != sv.type) {
					se = new SimpleVariable(sv.addr, corrected, sv.name);
				}
			} else {
				throw new InternalError("unknown exportable class: " + se.getClass().getName());
			}
			result[i] = se;
		}
		Iterator <SimpleStructType> iter = structs.values().iterator();
		for (int i = impcnt; i < result.length; i ++ ) {
			result[i] = iter.next();
		}
		assert !iter.hasNext();
		return result;
	}
	
	static SimpleType correctType(Map <String, SimpleStructType> structs, SimpleType type) {
		if (type instanceof SimpleFuncType) {
			SimpleFuncType sft = (SimpleFuncType) type;
			correctArray(structs, sft.arguments);
			correctArray(structs, sft.results);
		} else if (type instanceof SimpleTypePointer) {
			SimpleTypePointer p = (SimpleTypePointer) type;
			SimpleType corrected = correctType(structs, p.target);
			if (corrected != p.target) {
				if (p instanceof SimpleTypeArray) {
					return new SimpleTypeArray(corrected, ((SimpleTypeArray) p).elementCount);
				} else {
					return new SimpleTypePointer(corrected);
				}
			}
		} else if (type instanceof SimpleFutureStructType) {
			SimpleStructType res = structs.get( ((SimpleFutureStructType) type).name);
			if (res == null) {
				throw new NoSuchElementException("the needed structure was not exported! (name='" + ((SimpleFutureStructType) type).name + "') (exported structs: " + structs + ")");
			}
			return res;
		} else if (type instanceof SimpleStructType) {
			throw new InternalError("simple struct type is not allowed here (should possibly be a SimpleFutureStructType)");
		} else if ( ! (type instanceof SimpleTypePrimitive)) {
			throw new InternalError("unknown type class: " + type.getClass().getName());
		}
		return type;
	}
	
	static void correctArray(Map <String, SimpleStructType> structs, SimpleVariable[] results) {
		for (int i = 0; i < results.length; i ++ ) {
			SimpleVariable sv = results[i];
			SimpleType corrected = correctType(structs, sv.type);
			if (corrected != sv.type) {
				results[i] = new SimpleVariable(sv.addr, corrected, sv.name);
			}
		}
	}
	
	class SimpleFutureStructType implements SimpleType {
		
		public final String name;
		
		public SimpleFutureStructType(String name) {
			this.name = name;
		}
		
		//@formatter:off
		@Override public boolean isPrimitive() { return false; }
		@Override public boolean isPointerOrArray() { return false; }
		@Override public boolean isPointer() { return false; }
		@Override public boolean isArray() { return false; }
		@Override public boolean isStruct() { return true; }
		@Override public boolean isFunc() { return false; }
		@Override public int byteCount() { throw new UnsupportedOperationException(); }
		@Override public void appendToExportStr(StringBuilder build) { throw new UnsupportedOperationException(); }
		//@formatter:on
		
		@Override
		public String toString() {
			return "struct " + name;
		}
		
	}
	
	static void exportVars(StringBuilder build, SimpleVariable[] arr) {
		boolean first = true;
		for (SimpleVariable sv : arr) {
			if ( !first) {
				build.append(VAR_SEP);
			}
			first = false;
			build.append(sv.name);
			build.append(NAME_TYPE_SEP);
			sv.type.appendToExportStr(build);
		}
	}
	
	static Map <String, SimpleExportable> readExports(Reader r) {
		try {
			ANTLRInputStream in = new ANTLRInputStream();
			in.load(r, 1024, 1024);
			Lexer lexer = new SimpleExportGrammarLexer(in);
			CommonTokenStream toks = new CommonTokenStream(lexer);
			SimpleExportGrammarParser parser = new SimpleExportGrammarParser(toks);
			parser.setErrorHandler(new BailErrorStrategy());
			SimpleExportsContext sec = parser.simpleExports();
			SimpleExportable[] se = sec.imported;
			Map <String, SimpleExportable> result = new HashMap <>(se.length);
			for (int i = 0; i < se.length; i ++ ) {
				result.put(se[i].name(), se[i]);
			}
			return result;
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
	
}
