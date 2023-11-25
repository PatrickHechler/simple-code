package de.hechler.patrick.code.simple.interpreter;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import de.hechler.patrick.code.simple.interpreter.SimpleInterpreter;
import de.hechler.patrick.code.simple.interpreter.memory.MemoryManager;
import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;
import de.hechler.patrick.zeugs.check.exceptions.CheckerException;

@CheckClass(singleThread = true) // do not get the std* from the other tests
public class SimpleinterpreterChecker {
	
	private PrintStream origStdout;
	private PrintStream origStderr;
	private InputStream origStdin;
	
	private ByteArrayOutputStream stdout;
	private ByteArrayOutputStream stderr;
	
	@End
	private void restoreStd() {
		System.setErr(this.origStderr);
		System.setOut(this.origStdout);
		System.setIn(this.origStdin);
		this.stderr = null;
		this.stdout = null;
	}
	
	@Start
	private void redirectStd() {
		this.origStderr = System.err;
		this.origStdout = System.out;
		this.origStdin = System.in;
		redirectStdNoSave();
	}
	
	private void redirectStdNoSave() {
		this.stdout = new ByteArrayOutputStream();
		this.stderr = new ByteArrayOutputStream();
		System.setOut(new PrintStream(this.stdout, true, StandardCharsets.UTF_8));
		System.setErr(new PrintStream(this.stderr, true, StandardCharsets.UTF_8));
		System.setIn(new ByteArrayInputStream(new byte[0]));
	}
	
	@Check
	private void helloWorldCheck() throws URISyntaxException {
		Path path = Path.of(getClass().getResource("/programs/hello-world.ssf").toURI());
		SimpleInterpreter si = new SimpleInterpreter(List.of(path.getParent()));
		path = path.getFileSystem().getPath("/hello-world.ssf");
		assertEquals(0, si.execute(path, new String[]{ "/hello-world.ssf" }));
		assertEquals(0, this.stderr.toByteArray().length);
		assertEquals("hello world\n", new String(this.stdout.toByteArray(), StandardCharsets.UTF_8));
	}
	
	public static String getStr(MemoryManager mem, long addr) {
		int len;
		for (len = 0; mem.get8(addr + len) != 0 && len < Integer.MAX_VALUE; len++);
		ByteBuffer bb = ByteBuffer.allocate(len);
		mem.get(addr, bb);
		return new String(bb.array(), StandardCharsets.UTF_8);
	}
	
	@Check
	private void parseNumCheck() throws URISyntaxException {
		singleParseNumCheck(5, "5");
		singleParseNumCheck(50, "50");
		singleParseNumCheck(0xFF & 500, "500");
		singleParseNumCheck(0xFF & 555, "555");
		singleParseNumCheck(255, "255");
		singleParseNumCheck(0, "256");
		singleParseNumCheck(100, "0100");
		singleParseNumCheck(0xFF & 2000, "000000000000000000000000002000");
		singleParseNumCheck(0, "0");
		singleParseNumCheck(0, "");
	}
	
	private void singleParseNumCheck(int exitNum, String arg1) throws URISyntaxException, CheckerException {
		Path path = Path.of(getClass().getResource("/programs/parse-num.ssf").toURI());
		SimpleInterpreter si = new SimpleInterpreter(List.of(path.getParent()));
		path = path.getFileSystem().getPath("/parse-num.ssf");
		assertEquals(exitNum, si.execute(path, new String[]{ "/parse-num.ssf", arg1 }));
		assertEquals(0, this.stderr.toByteArray().length);
		assertEquals(0, this.stdout.toByteArray().length);
	}
	
	@Check
	private void parsePringNumLECheck() throws URISyntaxException {
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "5", "5");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "23", "50");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "4F", "500");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "B2", "555");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "FF", "255");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "0", "256");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "46", "0100");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "0D", "000000000000000000000000002000");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "0", "0");
		singleParsePrintNumCheck("/parse-printLE-num.ssf", "0", "");
	}
	
	@Check
	private void parsePringNumBECheck() throws URISyntaxException {
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "5", "5");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "50", "50");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "244", "500");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "43", "555");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "255", "255");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "0", "256");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "100", "0100");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "208", "000000000000000000000000002000");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "0", "0");
		singleParsePrintNumCheck("/parse-printBE-num.ssf", "0", "");
	}
	
	private void singleParsePrintNumCheck(String fileName, String stdout, String arg1)
		throws URISyntaxException, CheckerException {
		redirectStdNoSave();
		Path path = Path.of(getClass().getResource("/programs" + fileName).toURI());
		SimpleInterpreter si = new SimpleInterpreter(List.of(path.getParent()));
		path = path.getFileSystem().getPath(fileName);
		assertEquals(0, si.execute(path, new String[]{ fileName, arg1 }));
		assertEquals(0, this.stderr.toByteArray().length);
		assertEquals(stdout, new String(this.stdout.toByteArray(), StandardCharsets.UTF_8));
	}
	
}
