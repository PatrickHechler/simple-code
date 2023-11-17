package de.hechler.patrick.codesprachen.simple.compile.parser;

import java.io.InputStream;

import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
class SimpleExportFileParserChecker {
	
	@Check
	void checkParser() {
		InputStream in = SimpleExportFileParser.class.getResourceAsStream("/programs/some-funcs.sexp");
		SimpleExportFileParser sep = new SimpleExportFileParser(in, "some-funcs.sexp", null);
		SimpleDependency sf = sep.parse("/bin", false);
		System.out.println(sf);
	}
	
}
