package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.fail;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.hechler.patrick.codesprachen.simple.symbol.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.SimpleVariable.SimpleOffsetVariable;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePointer;
import de.hechler.patrick.codesprachen.simple.symbol.objects.types.SimpleTypePrimitive;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
@SuppressWarnings({ "javadoc", "static-method" })
public class SimpleExportsChecker {
	
	Random rnd;
	
	@Start
	private void start() {
		this.rnd = new Random(0x687A64L);
	}
	
	@End
	private void end() {
		this.rnd = null;
	}
	
	@Check(disabled = true)
	private void constant() {
		fail("not yet done");
	}
	
	@Check(disabled = true)
	private void func() {
		fail("not yet done");
	}
	
	@Check(disabled = true)
	private void struct() {
		fail("not yet done");
	}
	
	@Check
	private void variable() {
		SimpleOffsetVariable sv = sv(SimpleType.NUM, "variable_name", true);
		SimpleFile sf = new SimpleFile(null);
		sv.init(0x7FFFFFFFFFFFFFFFL & this.rnd.nextLong(), sf);
		checkExport(sv, null, sv.type);
		sv = sv(SimpleType.UBYTE, "variable_name", true);
		sv.init(0x7FFFFFFFFFFFFFFFL & this.rnd.nextLong(), sf);
		checkExport(sv, null, sv.type);
		sv = sv(new SimpleFuncType(svs(), svs()), "otherVariableName", true);
		sv.init(0x7FFFFFFFFFFFFFFFL & this.rnd.nextLong(), sf);
		checkExport(sv, null, sv.type);
		sv = sv(new SimpleFuncType(svs(new SimpleOffsetVariable(SimpleType.NUM, "arg1", false)), svs()), "otherVariableName", true);
		sv.init(0x7FFFFFFFFFFFFFFFL & this.rnd.nextLong(), sf);
		checkExport(sv, null, sv.type);
		sv = sv(new SimpleFuncType(svs(), svs(new SimpleOffsetVariable(SimpleType.NUM, "arg1", false))), "otherVariableName", true);
		sv.init(0x7FFFFFFFFFFFFFFFL & this.rnd.nextLong(), sf);
		checkExport(sv, null, sv.type);
		sv = sv(new SimpleFuncType(svs(sv(new SimpleTypePointer(new SimpleStructType("my_EMPTY_structure", true, svs())), "arg1", false)),
			svs(new SimpleOffsetVariable(SimpleType.NUM, "res1", false))), "otherVariableName", true);
		sv.init(0x7FFFFFFFFFFFFFFFL & this.rnd.nextLong(), sf);
		checkExport(sv, null, sv.type);
	}
	
	private static SimpleOffsetVariable sv(SimpleType type, String name, boolean export) {
		return new SimpleOffsetVariable(type, name, export);
	}
	
	private static List<SimpleOffsetVariable> svs(SimpleOffsetVariable... result) {
		return Arrays.asList(result);
	}
	
	private void checkExport(SimpleExportable se, SimpleType[] types2, SimpleType... types1) {
		StringBuilder b = new StringBuilder();
		export(b, types1);
		b.append(se.toExportString()).append('\n');
		if (types2 != null) {
			export(b, types2);
		}
		try {
			Map<String, SimpleExportable> exports = SimpleExportable.readExports(null, new StringReader(b.toString()));
			SimpleExportable              imp     = exports.get(se.name());
			assertEquals(se, imp);
			assertEquals(se.toString(), imp.toString());
			assertEquals(se.toExportString(), imp.toExportString());
		} catch (Throwable t) {
			System.out.print("check export failed:\n" + b);
			throw t;
		}
	}
	
	private void export(StringBuilder b, SimpleType... types) {
		export(b, (Object[]) types);
	}
	
	private void export(StringBuilder b, SimpleOffsetVariable... types) {
		export(b, (Object[]) types);
	}
	
	private void export(StringBuilder b, Object... types) {
		for (int i = 0; i < types.length; i++) {
			SimpleType t = getType(types[i]);
			if (t instanceof SimpleTypePrimitive) {
				// NOP
			} else if (t instanceof SimpleTypePointer pt) {
				export(b, pt.target);
			} else if (t instanceof SimpleFuncType ft) {
				export(b, ft.arguments);
				export(b, ft.results);
			} else if (t instanceof SimpleStructType st) {
				export(b, st.members);
				b.append(st.toExportString()).append('\n');
			} else {
				fail("unknown type: " + t.getClass());
			}
		}
	}
	
	private static SimpleType getType(Object obj) throws InternalError {
		if (obj instanceof SimpleType) {
			return (SimpleType) obj;
		} else if (obj instanceof SimpleOffsetVariable) {
			return ((SimpleOffsetVariable) obj).type;
		} else {
			throw new InternalError("unknown class: " + obj.getClass());
		}
	}
	
}
