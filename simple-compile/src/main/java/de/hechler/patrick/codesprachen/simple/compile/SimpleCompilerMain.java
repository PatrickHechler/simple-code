package de.hechler.patrick.codesprachen.simple.compile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchProviderException;
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
import de.hechler.patrick.zeugs.pfs.FSProvider;
import de.hechler.patrick.zeugs.pfs.interfaces.FS;
import de.hechler.patrick.zeugs.pfs.interfaces.FSElement;
import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder;
import de.hechler.patrick.zeugs.pfs.interfaces.Folder.FolderIter;
import de.hechler.patrick.zeugs.pfs.opts.JavaFSOptions;
import de.hechler.patrick.zeugs.pfs.opts.PatrFSOptions;

public class SimpleCompilerMain {
	
	private static final String NOT_ENUGH_ARGS_MSG = "not enugh args";
	
	private static final String DOUBLED_OPTION_MSG = "doubled option";
	
	public static final Logger LOGGER = Logger.getLogger("simple-compile");
	
	private static FS            pfs;
	private static MultiCompiler compiler;
	private static Path          src;
	private static Folder        bin;
	private static boolean       force;
	
	public static void main(String[] args) {
		setup(args);
		try {
			if (force) {
				deleteChilds(bin);
			}
			compileRecursive(src, bin);
			compiler.compile();
		} catch (Throwable t) {
			System.err.println("erron while compiling: " + t);
			t.printStackTrace();
			if (t instanceof AssembleError ae) {
				System.err.println("line:          " + ae.line);
				System.err.println("posInLine:     " + ae.posInLine);
				System.err.println("length:        " + ae.length);
				System.err.println("charPos:       " + ae.charPos);
			}
			if (t instanceof ParseCancellationException pce) {
				Throwable c = pce.getCause();
				if (c instanceof RecognitionException ime) {
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
			closePFS();
			System.exit(1);
		}
		String binStr = bin.toString();
		closePFS();
		if (LOGGER.isLoggable(Level.INFO)) {
			System.out.println("compiled successfully " + src.toString() + " to " + binStr);
		}
	}
	
	private static void deleteChilds(Folder bin) throws IOException {
		try (FolderIter iter = bin.iter(true)) {
			FSElement e = iter.next();
			if (e.isFolder()) {
				deleteChilds(e.getFolder());
			}
			iter.delete();
		}
	}
	
	private static void compileRecursive(Path src, Folder bin) throws IOException {
		for (Path sub : Files.newDirectoryStream(src)) {
			String name = sub.getFileName().toString();
			if (Files.isDirectory(sub)) {
				Folder target = bin.createFolder(name);
				compileRecursive(sub, target);
			} else {
				File out = bin.createFile(name);
				if (LOGGER.isLoggable(Level.INFO)) {
					System.out.println("register: '" + sub + "' will be compiled to '" + out + '\'');
				}
				compiler.addTranslationUnit(sub, out);
			}
		}
	}
	
	private static void closePFS() {
		try {
			pfs.close();
		} catch (Throwable e) {
			if (e instanceof ThreadDeath td) {
				throw td;
			}
			System.err.println("could not close the file system:");
			e.printStackTrace();
			System.exit(2);
		}
	}
	
	private static void setup(String[] args) {
		List<Path> lookupPaths    = new ArrayList<>();
		boolean    recreateOutput = false;
		Charset    cs             = null;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--help", "--?" -> argHelp();
			case "--lookup" -> argLookup(args, lookupPaths, ++i);
			case "--src" -> argSrc(args, ++i);
			case "--recreate-out", "--force" -> recreateOutput = argForce(args, recreateOutput, i);
			case "--charset" -> cs = argCs(args, cs, ++i);
			case "--lfs", "--rfs" -> argLfs(args, ++i);
			case "--pfs" -> argPfs(args, i += 2);
			default -> crash("unknown argument: '" + args[i] + '\'', i, args);
			}
		}
		if (src == null) {
			crash("src folder not set", -1, args);
		}
		if (lookupPaths.isEmpty()) {
			lookupPaths.add(src);
		}
		Path[] lookups = lookupPaths.toArray(new Path[lookupPaths.size()]);
		if (cs == null) {
			cs = StandardCharsets.UTF_8;
		}
		force = recreateOutput;
		compiler = new DefMultiCompiler(cs, src, lookups);
	}
	
	private static void argHelp() throws InternalError {
		help();
		System.exit(0);
	}
	
	private static void argLookup(String[] args, List<Path> lookupPaths, int i) {
		if (i >= args.length) {
			crash(NOT_ENUGH_ARGS_MSG, i - 1, args);
		}
		lookupPaths.add(Paths.get(args[i]));
	}
	
	private static void argSrc(String[] args, int i) {
		if (src != null) {
			crash(DOUBLED_OPTION_MSG, i - 1, args);
		}
		if (i >= args.length) {
			crash(NOT_ENUGH_ARGS_MSG, i - 1, args);
		}
		src = Paths.get(args[i]);
	}
	
	private static boolean argForce(String[] args, boolean recreateOutput, int i) {
		if (recreateOutput) {
			crash(DOUBLED_OPTION_MSG, i, args);
		}
		recreateOutput = true;
		return recreateOutput;
	}
	
	private static Charset argCs(String[] args, Charset cs, int i) {
		if (cs != null) {
			crash(DOUBLED_OPTION_MSG, i - 1, args);
		}
		if (i >= args.length) {
			crash(NOT_ENUGH_ARGS_MSG, i - 1, args);
		}
		cs = Charset.forName(args[i]);
		return cs;
	}
	
	private static void argLfs(String[] args, int i) {
		if (pfs != null) {
			crash(DOUBLED_OPTION_MSG, i - 1, args);
		}
		if (i >= args.length) {
			crash(NOT_ENUGH_ARGS_MSG, i - 1, args);
		}
		try {
			pfs = FSProvider.ofName(FSProvider.JAVA_FS_PROVIDER_NAME).loadFS(new JavaFSOptions(Path.of(args[i])));
			bin = pfs.folder("/");
		} catch (NoSuchProviderException | IOException e) {
			crash(e.toString(), i, args);
		}
	}
	
	private static void argPfs(String[] args, int i) {
		if (pfs != null) {
			crash(DOUBLED_OPTION_MSG, i - 2, args);
		}
		if (i >= args.length) {
			crash(NOT_ENUGH_ARGS_MSG, i - 2, args);
		}
		try {
			pfs = FSProvider.ofName(FSProvider.PATR_FS_PROVIDER_NAME).loadFS(new PatrFSOptions(args[i - 1]));
			bin = pfs.folder(args[i]);
		} catch (NoSuchProviderException | IOException e) {
			crash(e.toString(), i, args);
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
						+ "    --force\n"//
						+ "        to delete the output file if it\n"//
						+ "        exist already at the start\n"//
						+ "    --rfs <PATH>\n"//
						+ "    --lfs <PATH>\n"//
						+ "        to use the real file system\n"//
						+ "    --pfs <PATH> <PATH>\n"//
						+ "        to set the patr file system path\n"//
						+ "        the first path specifies the file system file\n"//
						+ "        the second path specifies the output folder\n"//
		);
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
