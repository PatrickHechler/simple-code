package de.hechler.patrick.codesprachen.simple.parser;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import de.hechler.patrick.codesprachen.simple.parser.SimpleSourceFileParser;
import de.hechler.patrick.codesprachen.simple.parser.objects.simplefile.SimpleDependency;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;

@CheckClass
class SimpleSourceFileParserChecker {
	
	private static final byte[] HELLO_WORLD = "hello world\n\0".getBytes(StandardCharsets.UTF_8);
	
	@Check
	void checkParser() {
		InputStream in = SimpleSourceFileParser.class.getResourceAsStream("/programs/hello-world.ssf");
		SimpleSourceFileParser ssp = new SimpleSourceFileParser(in, "hello-world.ssf", null);
		SimpleDependency sf = ssp.parse("/bin");
		assertEquals("""
			typedef ubyte char;
			func puts <unum wrote, unum errno> <-- ((ubyte)# string) {
			    wrote <-- 0;
			    errno <-- 1;
			}
			func main <ubyte exitnum> <-- (unum argc, ((ubyte)#)# argv) {
			    unum wrote;
			    unum err;
			    puts <wrote, err> <-- ((((ubyte)#) DataVal [value=%s, type=(ubyte)[%d]] ));
			    exitnum <-- (err != 0);
			}
			""".formatted(toCSVHexBytes(HELLO_WORLD), HELLO_WORLD.length), sf.toString());
	}
	
	private static String toCSVHexBytes(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 4 - 2);
		sb.append(hex(( data[0] >> 4 ) & 0xF));
		sb.append(hex(data[0] & 0xF));
		for (int i = 1; i < data.length; i++) {
			byte b = data[i];
			sb.append(", ");
			sb.append(hex(( b >> 4 ) & 0xF));
			sb.append(hex(b & 0xF));
		}
		return sb.toString();
	}
	
	private static char hex(int n) {
		if ( n < 10 ) return (char) ( '0' + n );
		return (char) ( 'a' - 10 + n );
	}
	
}