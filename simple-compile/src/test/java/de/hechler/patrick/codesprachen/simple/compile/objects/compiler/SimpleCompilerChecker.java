// This file is part of the Simple Code Project
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
// Copyright (C) 2023 Patrick Hechler
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <https://www.gnu.org/licenses/>.
package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import static de.hechler.patrick.zeugs.check.Assert.assertArrayEquals;
import static de.hechler.patrick.zeugs.check.Assert.assertEquals;

import java.io.IOError;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Set;

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
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.misc.ElementType;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.StreamOpenOptions;

@CheckClass(disableSuper = true)
@SuppressWarnings("javadoc")
public class SimpleCompilerChecker {
	
	private static final boolean DELETE_STD_FILES = true;
	
	private FSProvider patrFsProv;
	
	@Start(onlyOnce = true)
	@SuppressWarnings("static-method")
	protected void init() throws IOException {
		Files.createDirectories(Path.of("./testout/"));
	}
	
	@Start
	protected void start(@MethodParam Method met) throws NoSuchProviderException {
		System.err.println("check now " + met.getName());
		this.patrFsProv = FSProvider.ofName(FSProvider.PATR_FS_PROVIDER_NAME);
	}
	
	@End
	protected void end(@MethodParam Method met) {
		System.err.println("finished " + met.getName() + " check");
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
	
	private static final String HW_PMF_PARENT_NAME = "bin";
	private static final String HW_PMF             = "/bin/bin";
	private static final String HW_PFS             = "./testout/hello-world.pfs";
	private static final String HW_RES             = "/de/hechler/patrick/codesprachen/simple/compile/programs/hello-world.ssc";
	
	private static final String ECHO_PMF = "/bin";
	private static final String ECHO_PFS = "./testout/echo.pfs";
	private static final String ECHO_RES = "/de/hechler/patrick/codesprachen/simple/compile/programs/echo.ssc";
	
	@Check
	private void checkAdd() throws IOException, InterruptedException, URISyntaxException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(ADD_PFS, true, 4096L, 1024))) {
			System.err.println("opened fs, compile now");
			Charset        cs       = StandardCharsets.UTF_8;
			Class<?>       cls      = getClass();
			URL            res      = cls.getResource(SRC_RES);
			URI            uri      = res.toURI();
			Path           p        = Path.of(uri);
			Path[]         ps       = new Path[0];
			SimpleCompiler compiler = new SimpleCompiler(cs, p, ps);
			fs.stream(ADD_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			try (File file = fs.file(ADD_PMF)) {
				file.flag(FSElement.FLAG_EXECUTABLE, 0);
				compiler.addTranslationUnit(Path.of(cls.getResource(ADD_RES).toURI()), file);
				compiler.compile();
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		System.err.println("execute now the program");
		execute(ADD_PFS, ADD_PMF, 90, EMPTY_BARR, EMPTY_BARR, EMPTY_BARR);
	}
	
	@Check
	private void checkAdd2() throws IOException, URISyntaxException, InterruptedException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(ADD2_PFS, true, 4096L, 1024))) {
			System.err.println("opened fs, compile now");
			SimpleCompiler compiler = new SimpleCompiler(StandardCharsets.UTF_8, Path.of(getClass().getResource(SRC_RES).toURI()), new Path[0]);
			fs.stream(ADD2_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			File file = fs.file(ADD2_PMF);
			file.flag(FSElement.FLAG_EXECUTABLE, 0);
			compiler.addTranslationUnit(Path.of(getClass().getResource(ADD2_RES).toURI()), file);
			compiler.compile();
			System.err.println("finished compile, close now fs");
		}
		System.err.println("execute now the program");
		execute(ADD2_PFS, ADD2_PMF, 0, EMPTY_BARR, "5 + 4 = 9\n".getBytes(StandardCharsets.UTF_8), EMPTY_BARR);
	}
	
	@Check
	private void checkHelloWorld() throws IOException, URISyntaxException, InterruptedException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(HW_PFS, true, 4096L, 1024))) {
			System.err.println("opened fs, compile now");
			SimpleCompiler compiler = new SimpleCompiler(StandardCharsets.UTF_8, Path.of(getClass().getResource(SRC_RES).toURI()), new Path[0]);
			try (Folder root = fs.folder("/")) {
				root.createFolder(HW_PMF_PARENT_NAME).close();
			}
			fs.stream(HW_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			File file = fs.file(HW_PMF);
			file.flag(FSElement.FLAG_EXECUTABLE, 0);
			compiler.addTranslationUnit(Path.of(getClass().getResource(HW_RES).toURI()), file);
			compiler.compile();
			try (File f = fs.file(HW_PMF)) {
				System.err.println("finished compile, close now fs file length: " + f.length());
			}
		}
		Files.setPosixFilePermissions(Path.of(HW_PFS), Set.of(                                                        //
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,       //
			PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,       //
			PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE     //
		));
		System.err.println("execute now the program");
		exec("hello-world-", 0, EMPTY_BARR, "hello world\n".getBytes(StandardCharsets.UTF_8), EMPTY_BARR, HW_PFS);
	}
	
	@Check
	private void checkEcho() throws IOException, URISyntaxException, InterruptedException {
		try (FS fs = this.patrFsProv.loadFS(new PatrFSOptions(ECHO_PFS, true, 4096L, 1024))) {
			System.err.println("opened fs, compile now");
			SimpleCompiler compiler = new SimpleCompiler(StandardCharsets.UTF_8, Path.of(getClass().getResource(SRC_RES).toURI()), new Path[0]);
			fs.stream(ECHO_PMF, new StreamOpenOptions(false, true, false, ElementType.FILE, true, true)).close();
			File file = fs.file(ECHO_PMF);
			file.flag(FSElement.FLAG_EXECUTABLE, 0);
			compiler.addTranslationUnit(Path.of(getClass().getResource(ECHO_RES).toURI()), file);
			compiler.compile();
			try (File f = fs.file(ECHO_PMF)) {
				System.err.println("finished compile, close now fs file length: " + f.length());
			}
		}
		Files.setPosixFilePermissions(Path.of(ECHO_PFS), Set.of(                                                      //
			PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE,       //
			PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,       //
			PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE     //
		));
		System.err.println("execute now the program");
		exec("echo-text-", 0, EMPTY_BARR, "echo text\n".getBytes(), EMPTY_BARR, ECHO_PFS, "echo", "text");
		exec("echo-text-1arg-", 0, EMPTY_BARR, "echo text\n".getBytes(), EMPTY_BARR, "pvm", ECHO_PFS, "echo text");
		exec("echo-long-text-", 0, EMPTY_BARR,
			"this is a large echo text, containing many characters and arguments, which will/should be printed to stdout by the echo program\n"
				.getBytes(StandardCharsets.UTF_8),
			EMPTY_BARR, ECHO_PFS, ECHO_PFS, "this", "is", "a", "large", "echo", "text,", "containing", "many", "characters", "and", "arguments,", "which",
			"will/should", "be", "printed", "to", "stdout", "by", "the", "echo", "program");
	}
	
	protected void execute(String pfsFile, String pmfFile, int exitCode, byte[] stdin, byte[] stdout, byte[] stderr, String... programArgs)
		throws IOException, InterruptedException {
		String[] args = new String[] { "pvm", "--pfs=" + pfsFile, "--pmf=" + pmfFile };
		if (programArgs.length != 0) {
			int olen = args.length;
			args = Arrays.copyOf(args, olen + programArgs.length);
			System.arraycopy(programArgs, 0, args, olen, programArgs.length);
		}
		exec(pmfFile.substring(1) + '-', exitCode, stdin, stdout, stderr, args);
	}
	
	private static void exec(String name, int exitCode, byte[] stdin, byte[] stdout, byte[] stderr, String... args)
		throws IOException, CheckerException, InterruptedException {
		System.err.println("args: " + Arrays.toString(args));
		Path sin  = Files.createTempFile(name, ".in");
		Path sout = Files.createTempFile(name, ".out");
		Path serr = Files.createTempFile(name, ".err");
		try {
			Files.write(sin, stdin);
			ProcessBuilder builder = new ProcessBuilder(args);
			builder.redirectInput(Redirect.from(sin.toFile()));
			builder.redirectOutput(Redirect.to(sout.toFile()));
			builder.redirectError(Redirect.to(serr.toFile()));
			Process process = builder.start();
			System.err.println("started process pid: " + process.pid() + "   " + process);
			assertEquals(exitCode, process.waitFor());
			assertArrayEquals(stdout, Files.readAllBytes(sout));
			assertArrayEquals(stderr, Files.readAllBytes(serr));
		} finally {
			if (DELETE_STD_FILES) {
				Files.delete(sin);
				Files.delete(sout);
				Files.delete(serr);
			} else {
				System.out.println(sin);
				System.out.println(sout);
				System.out.println(serr);
			}
		}
	}
	
}
