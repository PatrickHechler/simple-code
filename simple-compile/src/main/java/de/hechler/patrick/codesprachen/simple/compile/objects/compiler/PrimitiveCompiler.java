package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;

import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintStream;


public class PrimitiveCompiler extends PerFileCompiler {
	
	private final Charset cs;
	
	public PrimitiveCompiler(Charset cs) {
		this.cs = cs;
	}
	
	public void compile(Path source, Path target) {
		Path expout = target.resolveSibling(target.getFileName().toString().replaceFirst("(.*)[.]"  + DefMultiCompiler.PRIMITIVE_SOURCE_CODE_END, "$1") + ("." +  + DefMultiCompiler.PRIMITIVE_SYMBOL_FILE_END));
		try (OutputStream out = Files.newOutputStream(target)) {
			try (BufferedReader reader = Files.newBufferedReader(source, cs)) {
				PrimitiveAssembler asm;
				try (OutputStream eo = Files.newOutputStream(expout, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					try (PrintStream eop = new PrintStream(eo, false, cs)) {
						asm = new PrimitiveAssembler(out, eop, lockups, false, true);
						asm.assemble(currentFile, reader);
						eop.flush();
					}
				}
				if (Files.size(expout) == 0) {
					Files.delete(expout);
				}
			}
		}
	}
	
}
