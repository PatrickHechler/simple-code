package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;


public class PrimitiveCompiler extends PerFileCompiler {
	
	private final Charset cs;
	private final Path[] lockups;
	
	public PrimitiveCompiler(Charset cs, Path[] lockups) {
		this.cs = cs;
		this.lockups = lockups;
	}
	
	public void compile(Path source, Path target) throws IOException {
		Path expout = target.resolveSibling(target.getFileName().toString().replaceFirst("(.*)[.]"  + DefMultiCompiler.PRIMITIVE_SOURCE_CODE_END, "$1") + ("." +  DefMultiCompiler.PRIMITIVE_SYMBOL_FILE_END));
		try (OutputStream out = Files.newOutputStream(target)) {
			try (BufferedReader reader = Files.newBufferedReader(source, cs)) {
				PrimitiveAssembler asm;
				try (OutputStream eo = Files.newOutputStream(expout, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
					try (PrintStream eop = new PrintStream(eo, false, cs)) {
						asm = new PrimitiveAssembler(out, eop, lockups, false, true);
						asm.assemble(source, reader);
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
