package de.hechler.patrick.codesprachen.simple.compile.parser;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.InputStream;

import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleDependency;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
class SimpleSourceFileParserChecker {
	
	@Check
	void checkParser() {
		InputStream in = SimpleSourceFileParser.class.getResourceAsStream("/programs/hello-world.ssf");
		SimpleSourceFileParser ssp = new SimpleSourceFileParser(in, "hello-world.ssf", null);
		SimpleDependency sf = ssp.parse("/bin");
		assertEquals("""
			typedef ubyte char;
			func puts <unum wrote, unum errno> <-- (ubyte# string) {
			  wrote <-- 0;
			  errno <-- 1;
			}
			func main <ubyte exitnum> <-- (num argc, char## argv) {
			  unum wrote;
			  unum errno;
			  puts <wrote, errno> <-- ("hello world\n");
			  exitnum <-- errno != 0;
			}
			""", sf.toString());
	}
	
}
