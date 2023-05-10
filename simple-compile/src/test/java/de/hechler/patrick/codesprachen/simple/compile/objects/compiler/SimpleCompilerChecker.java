package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import static de.hechler.patrick.zeugs.check.Assert.assertEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertTrue;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

import de.hechler.patrick.zeugs.check.anotations.Check;
import de.hechler.patrick.zeugs.check.anotations.CheckClass;
import de.hechler.patrick.zeugs.check.anotations.End;
import de.hechler.patrick.zeugs.check.anotations.MethodParam;
import de.hechler.patrick.zeugs.check.anotations.Start;
import de.hechler.patrick.zeugs.check.exceptions.CheckerException;
import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

@CheckClass(disableSuper = true)
@SuppressWarnings("javadoc")
public class SimpleCompilerChecker {
	
	private FSProvider patrFsProv;
	
	@Start
	protected void init() throws IOException {
		Files.createDirectories(Path.of("./testout/"));
	}
	
	@Start
	protected void start(@MethodParam Method met) throws NoSuchProviderException {
		System.out.println("check now " + met.getName());
		this.patrFsProv = FSProvider.ofName(FSProvider.PATR_FS_PROVIDER_NAME);
	}
	
	@End
	protected void end(@MethodParam Method met) {
		System.out.println("finished " + met.getName() + " check");
		this.patrFsProv.loadedFS().forEach(fs -> {
			try {
				fs.close();
			} catch (IOException e) {
				throw new IOError(e);
			}
		});
	}
	
	private static final byte[] EMPTY_BARR = new byte[0];
	private static final String SRC_RES    = "/de/hechler/patrick/codesprachen/simple/compile/programs";
	
	private static final String ADD_PMF = "/add";
	private static final String ADD_PFS = "./testout/add.pfs";
	private static final String ADD_RES = "/de/hechler/patrick/codesprachen/simple/compile/programs/add.ssc";
	
	private static final String ADD2_PMF = "/add2";
	private static final String ADD2_PFS = "./testout/add2.pfs";
	private static final String ADD2_RES = "/de/hechler/patrick/codesprachen/simple/compile/programs/add2.ssc";
	
	private static final String HW_PMF = "/hello-world";
	private static final String HW_PFS = "./testout/hello-world.pfs";
	private static final String HW_RES = "/de/hechler/patrick/codesprachen/simple/compile/programs/hello-world.ssc";
	
	@Check
	private void checkAdd() throws IOException, InterruptedException, URISyntaxException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(ADD_PFS, true, 4096L, 1024))) {
			System.out.println("opened fs, compile now");
			SimpleCompiler compiler = new SimpleCompiler(StandardCharsets.UTF_8, Path.of(getClass().getResource(SRC_RES).toURI()), new Path[0]);
			fs.stream(ADD_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			try (File file = fs.file(ADD_PMF)) {
				file.flag(FSElement.FLAG_EXECUTABLE, 0);
				compiler.addTranslationUnit(Path.of(getClass().getResource(ADD_RES).toURI()), file);
				compiler.compile();
			}
			try (File file = fs.file(ADD_PMF)) {
				System.out.println("finished compile, close now fs, binary-length: " + file.length());
			}
		}
		System.out.println("execute now the program");
		execute(ADD_PFS, ADD_PMF, 9, EMPTY_BARR, EMPTY_BARR, EMPTY_BARR);
	}
	
	@Check(disabled = true)
	private void checkAdd2() throws IOException, URISyntaxException, InterruptedException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(ADD2_PFS, true, 4096L, 1024))) {
			System.out.println("opened fs, compile now");
			SimpleCompiler compiler = new SimpleCompiler(StandardCharsets.UTF_8, Path.of(getClass().getResource(SRC_RES).toURI()), new Path[0]);
			fs.stream(ADD2_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			File file = fs.file(ADD2_PMF);
			file.flag(FSElement.FLAG_EXECUTABLE, 0);
			compiler.addTranslationUnit(Path.of(getClass().getResource(ADD2_RES).toURI()), file);
			compiler.compile();
			System.out.println("finished compile, close now fs");
		}
		System.out.println("execute now the program");
		execute(ADD2_PFS, ADD2_PMF, 0, EMPTY_BARR, "5 + 4 = 9".getBytes(StandardCharsets.UTF_8), EMPTY_BARR);
	}
	
	@Check(disabled = true)
	private void checkHelloWorld() throws IOException, URISyntaxException, InterruptedException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(HW_PFS, true, 4096L, 1024))) {
			System.out.println("opened fs, compile now");
			SimpleCompiler compiler = new SimpleCompiler(StandardCharsets.UTF_8, Path.of(getClass().getResource(SRC_RES).toURI()), new Path[0]);
			fs.stream(HW_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			File file = fs.file(HW_PMF);
			file.flag(FSElement.FLAG_EXECUTABLE, 0);
			compiler.addTranslationUnit(Path.of(getClass().getResource(HW_RES).toURI()), file);
			compiler.compile();
			System.out.println("finished compile, close now fs");
		}
		System.out.println("execute now the program");
		execute(HW_PFS, HW_PMF, 0, EMPTY_BARR, "hello world\n".getBytes(StandardCharsets.UTF_8), EMPTY_BARR);
	}
	
	protected void execute(String pfsFile, String pmfFile, int exitCode, byte[] stdin, byte[] stdout, byte[] stderr, String... programArgs)
		throws IOException, InterruptedException {
		Runtime  r    = Runtime.getRuntime();
		String[] args = new String[] { "pvm", "--pfs=" + pfsFile, "--pmf=" + pmfFile };
		if (programArgs.length != 0) {
			int olen = args.length;
			args = Arrays.copyOf(args, olen + programArgs.length);
			System.arraycopy(programArgs, 0, args, olen, programArgs.length);
		}
		System.out.println("args: " + Arrays.toString(args));
		Process process = r.exec(args);
		System.out.println("started process pid: " + process.pid() + "   " + process);
		if (stdin.length > 0) { process.getOutputStream().write(stdin); }
		TwoBools b1 = new TwoBools(), b2 = new TwoBools();
		Thread   t  = Thread.ofVirtual().unstarted(() -> check(() -> process.isAlive(), process.getErrorStream(), stderr, b1));
		t.setName("check stderr");
		t.start();
		t = Thread.ofVirtual().unstarted(() -> check(() -> process.isAlive(), process.getInputStream(), stdout, b2));
		t.setName("check stdout");
		t.start();
		assertEquals(exitCode, process.waitFor());
		checkResult(b1);
		checkResult(b2);
	}
	
	protected void checkResult(TwoBools res) throws InterruptedException, CheckerException {
		while (!res.finish) {
			synchronized (res) {
				if (res.finish) { break; }
				res.wait(1000L);
			}
		}
		assertTrue(res.result);
	}
	
	protected void check(BooleanSupplier cond, InputStream stream, byte[] value, TwoBools b) {
		System.err.println(logStart() + "start");
		try {
			b.result = false;
			byte[] other = new byte[value.length];
			for (int i = 0; i < value.length;) {
				try {
					int reat = stream.read(other, i, other.length - i);
					if (reat == -1) {
						if (cond.getAsBoolean()) {
							sleep();
							continue;
						}
						throw new IOException("reached EOF too early");
					}
					System.err.print(logStart() + "little read: ");
					System.err.write(other, i, reat);
					System.err.println("\nreat: " + reat);
					i += reat;
				} catch (IOException e) {
					throw new IOError(e);
				}
			}
			if (other.length != 0) {
				System.err.print(logStart() + "read: ");
				try {
					System.err.write(other);
				} catch (@SuppressWarnings("unused") IOException e) {
				}
				System.err.println();
			}
			b.result = Arrays.equals(value, other);
			try {
				while (stream.available() > 0 || cond.getAsBoolean()) {
					if (stream.available() == 0) {
						sleep();
						continue;
					}
					b.result = false;
					other    = new byte[stream.available()];
					int r = stream.read(other, 0, other.length);
					System.err.println(logStart() + "additioannly read: " + new String(other, 0, r));
				}
			} catch (IOException e) {
				throw new IOError(e);
			}
		} finally {
			b.finish = true;
			synchronized (b) {
				b.notifyAll();
			}
			System.err.println(logStart() + "finish");
		}
	}
	
	private static void sleep() {
		try {
			Thread.sleep(0L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static String logStart() {
		return "[" + Thread.currentThread().getName() + "]: ";
	}
	
	private static final class TwoBools {
		
		volatile boolean finish;
		volatile boolean result;
		
	}
	
}
