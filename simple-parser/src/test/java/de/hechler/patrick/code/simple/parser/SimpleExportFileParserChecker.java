package de.hechler.patrick.code.simple.parser;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.InputStream;

import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleTypedef;
import de.hechler.patrick.code.simple.parser.objects.types.NativeType;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
class SimpleExportFileParserChecker {
	
	@Check
	void checkParser() {
		InputStream in = SimpleExportFileParser.class.getResourceAsStream("/programs/some-funcs.sexp");
		SimpleExportFileParser sep = new SimpleExportFileParser(in, "some-funcs.sexp", null);
		SimpleFile sf = new SimpleFile("/src", "/bin");
		sf.typedef(new SimpleTypedef("char", 0, NativeType.UBYTE), ErrorContext.NO_CONTEXT);
		sep.parse(sf);
		assertEquals("""
			typedef ubyte char;
			typedef struct {
			  (ubyte)[16] bytes;
			} uuid;
			typedef struct {
			  struct {
			    (ubyte)[16] bytes;
			  } uuid;
			  unum name_length;
			  (ubyte)# name;
			} my_custom_type;
			func add <num result> <-- (num a, num b);
			func deref <num value> <-- ((num)# addr);
			func addu <unum result> <-- (unum a, unum b);
			func derefu <unum value> <-- ((unum)# addr);
			""", sf.toString());
	}
	
}
