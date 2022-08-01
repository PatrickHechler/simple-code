package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertInstanceOf;
import static de.hechler.patrick.zeugs.check.Assert.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SimpleExportable;
import de.hechler.patrick.codesprachen.simple.compile.objects.commands.SimpleCommandBlock;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleFuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleStructType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;
import de.hechler.patrick.zeugs.check.exceptions.CheckerException;

@CheckClass
public class SimpleExportsChecker {

	Random rnd;

	@Start
	private void start() {
		rnd = new Random(0x687A64L);
	}

	@End
	private void end() {
		rnd = null;
	}

	@Check
	private void variable() {
		SimpleFile file = new SimpleFile(new Path[0], StandardCharsets.UTF_8);
		SimpleType t = SimpleType.NUM;
		checkVar(file, t);
		t = SimpleType.BYTE;
		checkVar(file, t);
		t = new SimpleStructType("struct_type", Arrays.asList());
		checkVar(file, t);
		t = new SimpleStructType("struct_type2", Arrays.asList(sv(SimpleType.BYTE, "mem")));
		checkVar(file, t);
		t = new SimpleStructType("struct_type2",
				Arrays.asList(sv(SimpleType.BYTE, "mem"), sv(SimpleType.DWORD, "mem2")));
		checkVar(file, t);
		t = new SimpleStructType("struct_type2", Arrays.asList(sv(SimpleType.BYTE, "mem"),
				sv(new SimpleFuncType(Arrays.asList(), Arrays.asList()), "funcmem"), sv(SimpleType.DWORD, "mem2")));
		checkVar(file, t);
		t = new SimpleStructType("struct_type2",
				Arrays.asList(sv(SimpleType.BYTE, "mem"),
						sv(new SimpleFuncType(Arrays.asList(sv(SimpleType.FPNUM, "fp")), Arrays.asList()), "funcmem"),
						sv(SimpleType.DWORD, "mem2")));
		checkVar(file, t);
		t = new SimpleStructType(
				"struct_type2", Arrays
						.asList(sv(SimpleType.BYTE, "mem"),
								sv(new SimpleFuncType(Arrays.asList(sv(SimpleType.FPNUM, "fp")),
										Arrays.asList(sv(SimpleType.DWORD, "dw"))), "funcmem"),
								sv(SimpleType.DWORD, "mem2")));
		checkVar(file, t);
		t = new SimpleStructType("struct_type2",
				Arrays.asList(sv(SimpleType.BYTE, "mem"),
						sv(new SimpleFuncType(Arrays.asList(sv(SimpleType.FPNUM, "fp"), sv(SimpleType.BYTE, "b")),
								Arrays.asList(sv(SimpleType.DWORD, "dw"))), "funcmem"),
						sv(SimpleType.DWORD, "mem2")));
		checkVar(file, t);
	}

	private void checkVar(SimpleFile f, SimpleType t) {
		SimpleVariable vari = sv(t, "var_name");
		vari.addr = rnd.nextLong();
		String exportString = vari.toExportString();
		SimpleExportable impVar = SimpleExportable.fromExport(exportString);
		assertVar(vari, impVar);
	}

	private void assertVar(SimpleVariable sv, SimpleExportable impVar) {
		assertInstanceOf(SimpleVariable.class, impVar);
		SimpleVariable iv = (SimpleVariable) impVar;
		assertEquals(sv.addr, iv.addr);
		assertEquals(sv.name, iv.name);
		assertEquals(sv.type, iv.type);
	}

	@Check
	private void func() {
		SimpleFile file = new SimpleFile(new Path[0], StandardCharsets.UTF_8);
		List<SimpleVariable> l1 = Collections.emptyList();
		List<SimpleVariable> l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"));
		checkFunction(file, l1, l2);
		l1 = Collections.emptyList();
		l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"), sv(SimpleType.FPNUM, "dword_arg2"));
		checkFunction(file, l1, l2);
		l1 = Arrays.asList(sv(SimpleType.BYTE, "byte_arg"));
		l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"), sv(SimpleType.FPNUM, "fpnum_arg2"));
		checkFunction(file, l1, l2);
		l1 = Arrays.asList(sv(SimpleType.BYTE, "byte_arg"), sv(SimpleType.WORD, "word_arg"));
		l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"),
				sv(new SimpleStructType("struct_name", Collections.emptyList()), "struct_arg"),
				sv(SimpleType.FPNUM, "fpnum_arg2"));
		checkFunction(file, l1, l2);
		l1 = Arrays.asList(sv(SimpleType.BYTE, "byte_arg"), sv(SimpleType.WORD, "word_arg"));
		l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"),
				sv(new SimpleStructType("struct_name", Arrays.asList(sv(SimpleType.BYTE, "byte_mem"))), "struct_arg2"),
				sv(SimpleType.FPNUM, "fpnum_arg2"));
		checkFunction(file, l1, l2);
		l1 = Arrays.asList(sv(SimpleType.BYTE, "byte_arg"), sv(SimpleType.WORD, "word_arg"));
		l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"), sv(new SimpleStructType("struct_name", Arrays.asList(
				sv(SimpleType.BYTE, "byte_mem"),
				sv(new SimpleFuncType(Arrays.asList(sv(SimpleType.FPNUM, "fp_number")),
						Arrays.asList(sv(SimpleType.NUM, "number"), sv(SimpleType.BYTE, "success"))), "func_mem"))),
				"struct_arg2"), sv(SimpleType.FPNUM, "fpnum_arg2"));
		checkFunction(file, l1, l2);
		l1 = Arrays.asList(sv(SimpleType.BYTE, "byte_arg"), sv(SimpleType.WORD, "word_arg"));
		l2 = Arrays.asList(sv(SimpleType.DWORD, "dword_arg"),
				sv(new SimpleStructType("struct_name", Arrays.asList(sv(SimpleType.BYTE, "byte_mem"))), "struct_arg2"),
				sv(new SimpleFuncType(Arrays.asList(sv(SimpleType.FPNUM, "fp_number")),
						Arrays.asList(sv(SimpleType.NUM, "number"), sv(SimpleType.BYTE, "success"))), "func_mem"),
				sv(SimpleType.FPNUM, "fpnum_arg2"));
		checkFunction(file, l1, l2);
	}

	private SimpleVariable sv(SimpleType t, String n) {
		return new SimpleVariable(t, n);
	}

	private void checkFunction(SimpleFile file, List<SimpleVariable> l1, List<SimpleVariable> l2)
			throws CheckerException {
		SimpleFunction f = new SimpleFunction(true, false, "func_name0", l1, l1, emptyBlock(file, l1, l1));
		f.address = 0x9BA847F75C10D719L;
		String exportString = f.toExportString();
		SimpleExportable impfunc = SimpleExportable.fromExport(exportString);
		assertFunc(f, impfunc);
		f = new SimpleFunction(true, false, "func_name1", l1, l2, emptyBlock(file, l1, l2));
		f.address = 0xE43F68A64C15D48BL;
		exportString = f.toExportString();
		impfunc = SimpleExportable.fromExport(exportString);
		assertFunc(f, impfunc);
		f = new SimpleFunction(true, false, "func_name2", l2, l1, emptyBlock(file, l2, l1));
		f.address = 0L; // happens, when there are no resources (and the first method is marked as
						// export)
		exportString = f.toExportString();
		impfunc = SimpleExportable.fromExport(exportString);
		assertFunc(f, impfunc);
		f = new SimpleFunction(true, false, "func_name3", l2, l2, emptyBlock(file, l2, l2));
		f.address = 1L;
		exportString = f.toExportString();
		impfunc = SimpleExportable.fromExport(exportString);
		assertFunc(f, impfunc);
	}

	private void assertFunc(SimpleFunction f, SimpleExportable impfunc) throws CheckerException {
		assertInstanceOf(SimpleFunction.class, impfunc);
		SimpleFunction impf = (SimpleFunction) impfunc;
		assertEquals(f, impf);
		assertEquals(f.address, impf.address);
		assertEquals(f.name, impf.name);
		assertEquals(f.type, impf.type);
		assertNotNull(impf.type.arguments);
		assertNotNull(impf.type.results);
	}

	private SimpleCommandBlock emptyBlock(SimpleFile file, List<SimpleVariable> args, List<SimpleVariable> res) {
		return new SimpleCommandBlock(file.newFuncPool(args, res), Collections.emptyList());
	}

}
