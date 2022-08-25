package de.hechler.patrick.codesprachen.simple.compile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.MultiCompiler;
import de.hechler.patrick.pfs.interfaces.BlockAccessor;
import de.hechler.patrick.pfs.interfaces.PatrFileSystem;
import de.hechler.patrick.pfs.interfaces.PatrFolder;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor;
import de.hechler.patrick.pfs.objects.ba.SeekablePathBlockAccessor.Bool;
import de.hechler.patrick.pfs.objects.fs.PatrFileSysImpl;

public class SimpleCompilerMain {
	
	private static PatrFileSystem pfs;
	private static MultiCompiler  compiler;
	private static Path           src;
	private static Path[]         lookups;
	
	public static void main(String[] args) {
		setup(args);
		try {
			compiler.compile(src, lookups);
		} catch (Exception e) {
			System.err.println("erron while compiling: " + e);
			e.printStackTrace();
			closePFS();
			System.exit(1);
		}
		closePFS();
		System.out.println("compiled successful " + src);
	}
	
	private static void closePFS() {
		try {
			pfs.close();
		} catch (IOException e) {
			System.err.println("could not close the file system: " + e);
			System.exit(2);
		}
	}
	
	private static void setup(String[] args) {
		List <Path> lookupPaths = new ArrayList <>();
		Path output = null;
		boolean recreateOutput = false;
		for (int i = 0; i < args.length; i ++ ) {
			switch (args[i]) {
			case "--help":
			case "--?":
				help();
				System.exit(0);
			case "--lookup":
				if ( ++ i >= args.length) {
					crash("not enugh args", i - 1, args);
				}
				lookupPaths.add(Paths.get(args[i]));
				break;
			case "--src":
				if (src != null) {
					crash("doubled option", i, args);
				}
				if ( ++ i >= args.length) {
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
		lookups = lookupPaths.toArray(new Path[lookupPaths.size()]);
		if (output == null) {
			output = Paths.get("./out.pfs");
		}
		try {
			if (recreateOutput) {
				Files.deleteIfExists(output);
			}
			BlockAccessor ba;
			Bool formatt = new Bool();
			ba = SeekablePathBlockAccessor.create(output, 1 << 14, false, formatt);
			pfs = new PatrFileSysImpl(ba);
			if (formatt.value) {
				pfs.format(1L << 16, 1 << 14);
			}
			PatrFolder root = pfs.getRoot();
			compiler = new MultiCompiler(root);
		} catch (IOException e) {
			crash("could not open the output file system: " + e.toString(), -1, args);
		}
	}
	
	private static void help() {
		System.out.print(
			/* */ "Options:\n"
				+ "    --help\n"
				+ "    --?"
				+ "        to print this message and exit\n"
				+ "    --lookup [FOLDER]\n"
				+ "        to add a lookup folder\n"
				+ "    --src [FOLDER]\n"
				+ "        to set the source folder\n"
				+ "    --recreate-out\n"
				+ "        to delete the output file if it\n"
				+ "        exist already at the start\n");
	}
	
	private static void crash(String msg, int index, String[] args) {
		System.err.println(msg);
		if (args != null) {
			for (int i = 0; i < args.length; i ++ ) {
				if (i == index) {
					System.err.print("error occurred here --> ");
				}
				System.err.println("[" + i + "]: " + args[i]);
			}
		}
		System.exit(1);
	}
	
}
