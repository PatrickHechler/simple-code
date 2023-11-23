package de.hechler.patrick.codesprachen.simple.interpreter;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.Start;

@CheckClass
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
	private void redirectStdOutErr() {
		this.origStderr = System.err;
		this.origStdout = System.out;
		this.origStdin = System.in;
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
		si.execute(path, new String[] { "/hello-world.ssf" });
		assertEquals(0, this.stderr.toByteArray().length);
		assertEquals("hello world\n", new String(this.stdout.toByteArray(), StandardCharsets.UTF_8));
	}
	
}
