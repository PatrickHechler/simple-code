package de.hechler.patrick.codesprachen.simple.compile.objects;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarLexer;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser;
import de.hechler.patrick.codesprachen.simple.compile.antlr.SimpleGrammarParser.SimpleFileContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.antl.SimpleFile;
import de.hechler.patrick.pfs.interfaces.PatrFile;

public class SimpleCompiler {
	
	private final Path    srcRoot;
	private final Path[]  lockups;
	private final Charset cs;
	
	private final Map <String, CompileTarget> targets = new HashMap <>();
	private final Queue <CompileTarget>       prework = new ConcurrentLinkedQueue <>();
	
	private volatile boolean working;
	private volatile boolean stop;
	
	public SimpleCompiler(Path srcRoot, Path[] lockups, Charset cs) {
		this.srcRoot = srcRoot.normalize();
		this.lockups = new Path[lockups.length];
		for (int i = 0; i < lockups.length; i ++ ) {
			this.lockups[i] = lockups[i].normalize();
		}
		this.cs = cs;
	}
	
	public synchronized void addFile(PatrFile outFile, Path currentFile, Path exports, boolean neverExecutable) {
		assert !stop;
		CompileTarget target = new CompileTarget(currentFile, exports, outFile, neverExecutable);
		String relPath = srcRoot.relativize(currentFile.normalize()).toString();
		CompileTarget old = targets.put(relPath, target);
		prework.add(target);
		if ( !working) {
			working = true;
			Thread worker = new Thread(() -> {
				try {
					precompileQueue();
				} finally {
					working = false;
					synchronized (SimpleCompiler.this) {
						notify();
					}
				}
			}, "simple precompiler");
			worker.setPriority(Math.min(worker.getPriority() + 2, Thread.MAX_PRIORITY));
			worker.start();
		}
		assert old == null;
	}
	
	private void precompileQueue() {
		while (true) {
			CompileTarget pc = prework.poll();
			if (pc == null) return;
			try {
				try (Reader r = Files.newBufferedReader(pc.source, cs)) {
					ANTLRInputStream in = new ANTLRInputStream();
					in.load(r, 1024, 1024);
					SimpleGrammarLexer lexer = new SimpleGrammarLexer(in);
					CommonTokenStream toks = new CommonTokenStream(lexer);
					SimpleGrammarParser parser = new SimpleGrammarParser(toks);
					pc.file = new SimpleFile();
					parser.simpleFile(pc.file);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public synchronized void compile() {
		stop = true;
		while (working) {
			try {
				wait(1000L);
			} catch (InterruptedException e) {
				System.err.println("unexpected interrupt!");
				e.printStackTrace();
			}
		}
		precompileQueue();
		// TODO Auto-generated method stub
		
	}
	
	private static class CompileTarget {
		
		private final Path     source;
		private final Path     exports;
		private final PatrFile binary;
		private final boolean  neverExe;
		
		private volatile SimpleFile file;
		
		public CompileTarget(Path src, Path exp, PatrFile bin, boolean neverExe) {
			this.source = src;
			this.exports = exp;
			this.binary = bin;
			this.neverExe = neverExe;
		}
		
	}
	
}
