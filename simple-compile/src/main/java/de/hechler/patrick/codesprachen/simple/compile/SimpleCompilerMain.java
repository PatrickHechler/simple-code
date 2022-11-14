package de.hechler.patrick.codesprachen.simple.compile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import de.hechler.patrick.codesprachen.primitive.assemble.exceptions.AssembleError;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.DefMultiCompiler;
import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.MultiCompiler;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor.Bool;

public class SimpleCompilerMain {

	public static final Logger LOGGER = Logger.getLogger("simple-compile");

	private static MultiCompiler compiler;
	private static Path src;
	private static Path bin;

	public static void main(String[] args) {
		setup(args);
		try {
			compileRecursive(src);
			compiler.compile();
		} catch (Throwable t) {
			System.err.println("erron while compiling: " + t);
			t.printStackTrace();
			if (t instanceof AssembleError) {
				AssembleError ae = (AssembleError) t;
				System.err.println("line:          " + ae.line);
				System.err.println("posInLine:     " + ae.posInLine);
				System.err.println("length:        " + ae.length);
				System.err.println("charPos:       " + ae.charPos);
			}
			if (t instanceof ParseCancellationException) {
				ParseCancellationException pce = (ParseCancellationException) t;
				Throwable c = pce.getCause();
				if (c instanceof RecognitionException) {
					RecognitionException ime = (RecognitionException) c;
					System.err.println("got:           " + ime.getOffendingToken());
					System.err.println("line:          " + ime.getOffendingToken().getLine());
					IntervalSet toks = ime.getExpectedTokens();
					System.err.println("expected:      " + toks);
					for (int i : toks.toArray()) {
						System.err.println("                 " + i + " : " + SimpleGrammarLexer.ruleNames[i]);
					}
					System.err.println("context:       " + ime.getCtx());
					System.err.println("context.class: " + ime.getCtx().getClass().getSimpleName());
				}
			}
//			closePFS();
			System.exit(1);
		}
//		closePFS();
		LOGGER.info("compiled successful " + src.toString());
	}

	private static void compileRecursive(Path src) throws IOException {
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(src)) {
			for (Path sub : dir) {
				Path target = bin.resolve(sub.relativize(bin));
				if (Files.isDirectory(sub)) {
					Files.createDirectory(target);

				} else {
					LOGGER.info("compile " + sub + " to " + target);
					compiler.addTranslationUnit(sub, target);
				}
			}
		}
	}

//	private static void closePFS() {
//		try {
////			pfs.close();
//		} catch (Throwable e) {
//			if (e instanceof ThreadDeath) {
//				throw e;
//			}
//			LOGGER.warning("could not close the file system: " + e);
//			System.exit(2);
//		}
//	}

	private static void setup(String[] args) {
		List<Path> lookupPaths = new ArrayList<>();
		Path bin = null;
		boolean recreateOutput = false;
		Charset cs = null;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--help":
			case "--?":
				help();
				System.exit(0);
			case "--lookup":
				if (++i >= args.length) {
					crash("not enugh args", i - 1, args);
				}
				lookupPaths.add(Paths.get(args[i]));
				break;
			case "--src":
				if (src != null) {
					crash("doubled option", i, args);
				}
				if (++i >= args.length) {
					crash("not enugh args", i - 1, args);
				}
				src = Paths.get(args[i]);
				break;
			case "--recreate-out":
				if (recreateOutput) {
					crash("doubled option", i, args);
				}
				recreateOutput = true;
				break;
			default:
				crash("unknown argument: '" + args[i] + '\'', i, args);
			}
		}
		if (src == null) {
			crash("src folder not set", -1, args);
		}
		if (lookupPaths.size() == 0) {
			lookupPaths.add(src);
		}
		Path[] lookups = lookupPaths.toArray(new Path[lookupPaths.size()]);
		if (cs == null) {
			cs = StandardCharsets.UTF_8;
		}
		if (bin == null) {
			bin = Paths.get("./out.pfs");
		}
		try {
			if (recreateOutput) {
				Files.deleteIfExists(bin);
			}
			BlockAccessor ba;
			Bool formatt = new Bool();
			ba = SeekablePathBlockAccessor.create(bin, 1 << 14, false, formatt);
//			pfs = new PatrFileSysImpl(ba); // TODO pfs for java
//			if (formatt.value) {
//				pfs.format(1L << 16, 1 << 14);
//			}
//			PatrFolder root = pfs.getRoot();
			compiler = new DefMultiCompiler(cs, src, lookups);
		} catch (IOException e) {
			crash("could not open the output file system: " + e.toString(), -1, args);
		}
	}

	private static void help() {
		System.out.print(//
				/*		*/"Options:\n"//
						+ "    --help\n"//
						+ "    --?"//
						+ "        to print this message and exit\n"//
						+ "    --lookup [FOLDER]\n"//
						+ "        to add a lookup folder\n"//
						+ "    --src [FOLDER]\n"//
						+ "        to set the source folder\n"//
						+ "    --recreate-out\n"//
						+ "        to delete the output file if it\n"//
						+ "        exist already at the start\n");
	}

	private static void crash(String msg, int index, String[] args) {
		System.err.println(msg);
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				if (i == index) {
					System.err.print("error occurred here --> ");
				}
				System.err.println("[" + i + "]: " + args[i]);
			}
		}
		System.exit(1);
	}

}
