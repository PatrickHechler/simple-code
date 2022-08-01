package de.hechler.patrick.codesprachen.simple.compile.objects;

import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_LOCK;
import static de.hechler.patrick.pfs.utils.PatrFileSysConstants.NO_TIME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

import de.hechler.patrick.codesprachen.primitive.assemble.objects.PrimitiveAssembler;
import de.hechler.patrick.codesprachen.simple.compile.enums.FileType;
import de.hechler.patrick.pfs.exception.ElementLockedException;
import de.hechler.patrick.pfs.interfaces.PatrFile;
import de.hechler.patrick.pfs.interfaces.PatrFileSysElement;
import de.hechler.patrick.pfs.interfaces.PatrFolder;

public class MultiCompiler {
	
	public static final String SIMPLE_SOURCE_CODE_END    = "s";
	public static final String PRIMITIVE_SOURCE_CODE_END = "psc";
	public static final String SIMPLE_SYMBOL_FILE_END    = "sf";
	public static final String PRIMITIVE_SYMBOL_FILE_END = "psf";
	public static final String SIMPLE_SOURCE_CODE        = "." + SIMPLE_SOURCE_CODE_END;
	public static final String PRIMITIVE_SOURCE_CODE     = "." + PRIMITIVE_SOURCE_CODE_END;
	public static final String SIMPLE_SYMBOL_FILE        = "." + SIMPLE_SYMBOL_FILE_END;
	public static final String PRIMITIVE_SYMBOL_FILE     = "." + PRIMITIVE_SYMBOL_FILE_END;
	
	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
	private static final boolean DEFAULT_FORCE   = false;
	
	private static final Function <Path, FileType> DEFAULT_FILE_MODES = p -> {
		String name = p.getFileName().toString();
		switch (name.substring(name.lastIndexOf('.') + 1)) {
		case PRIMITIVE_SOURCE_CODE_END:
			return FileType.primitiveCode;
		case SIMPLE_SOURCE_CODE_END:
			return FileType.simpleCode;
		case PRIMITIVE_SYMBOL_FILE_END:
		case SIMPLE_SYMBOL_FILE_END:
			return FileType.ignore;
		default:
			return FileType.copy;
		}
	};
	
	private static final int MAX_BUFFER = 4096;
	
	private final PatrFolder                bin;
	private final Function <Path, FileType> fileModes;
	private final boolean                   force;
	private final Charset                   cs;
	
	private SimpleCompiler compiler;
	private Path[]         lockups;
	
	public MultiCompiler(PatrFolder bin) {
		this(bin, DEFAULT_FILE_MODES, DEFAULT_FORCE, DEFAULT_CHARSET);
	}
	
	public MultiCompiler(PatrFolder bin, boolean force) {
		this(bin, DEFAULT_FILE_MODES, force, DEFAULT_CHARSET);
	}
	
	public MultiCompiler(PatrFolder bin, Function <Path, FileType> fileModes) {
		this(bin, fileModes, DEFAULT_FORCE, DEFAULT_CHARSET);
	}
	
	public MultiCompiler(PatrFolder bin, Function <Path, FileType> fileModes, boolean force, Charset cs) {
		this.bin = bin;
		this.fileModes = fileModes;
		this.force = force;
		this.cs = cs;
	}
	
	public synchronized void compile(Path src, Path... lockups) throws IOException {
		if (src == null) {
			throw new NullPointerException("src is null!");
		}
		if ( !Files.exists(src)) {
			throw new IllegalArgumentException("src Path does not exist!");
		}
		this.lockups = lockups;
		if (Files.isRegularFile(src)) {
			compileFile(null, src, bin);
		} else if (Files.isDirectory(src)) {
			compileFolder(src, src, bin);
		} else {
			throw new IllegalStateException("src is no file and no folder!");
		}
		compiler.compile();
	}
	
	private void compileFolder(Path srcRoot, Path currentSrcFolder, PatrFolder currentBinFolder) throws IOException {
		try (DirectoryStream <Path> stream = Files.newDirectoryStream(srcRoot)) {
			for (Path srcCurrent : stream) {
				if (Files.isRegularFile(srcCurrent)) {
					compileFile(srcRoot, srcCurrent, currentBinFolder);
				} else if (Files.isDirectory(srcCurrent)) {
					PatrFolder binCurrent;
					String name = srcCurrent.getFileName().toString();
					try {
						binCurrent = currentBinFolder.getElement(name, NO_LOCK).getFolder();
					} catch (NoSuchFileException e) {
						binCurrent = currentBinFolder.addFolder(name, NO_LOCK);
					} catch (IllegalStateException e) {
						if ( !force) {
							throw e;
						}
						currentBinFolder.getElement(name, NO_LOCK).delete(NO_LOCK, NO_LOCK);
						binCurrent = currentBinFolder.addFolder(name, NO_LOCK);
					}
					compileFolder(srcRoot, srcCurrent, binCurrent);
				} else {
					throw new IllegalStateException("src is no file and no folder!");
				}
			}
		}
	}
	
	private void compileFile(Path srcRoot, Path currentFile, PatrFolder currentFolder) throws IOException {
		String name = currentFile.getFileName().toString();
		if (force) {
			try {
				PatrFileSysElement element = currentFolder.getElement(name, NO_LOCK);
				if (element.isFile() || element.isLink()) {
					element.delete(NO_TIME, NO_LOCK);
				} else {
					element.getFolder().deepDelete(e -> NO_LOCK);
				}
			} catch (NoSuchFileException ignore) {}
		}
		PatrFile outFile = currentFolder.addFile(name, NO_LOCK);
		switch (this.fileModes.apply(currentFile)) {
		case copy:
			copy(outFile, currentFile, false);
			break;
		case copyExecutable:
			copy(outFile, currentFile, false);
			break;
		case ignore:
			break;
		case primitiveCode:
			assemble(outFile, currentFile, expOut(currentFile, false), false, lockups);
			break;
		case primitiveCodeExecutable:
			assemble(outFile, currentFile, expOut(currentFile, false), true, lockups);
			break;
		case simpleCode:
			compiler.addFile(outFile, currentFile, expOut(currentFile, true), false);
			break;
		case simpleCodeNonExecutable:
			compiler.addFile(outFile, currentFile, expOut(currentFile, true), true);
			break;
		default:
			throw new InternalError("unknown file mode!");
		}
	}
	
	private Path expOut(Path currentFile, boolean simpleCode) {
		String name = currentFile.getFileName().toString();
		int lastDot = name.lastIndexOf('.');
		String end = name.substring(lastDot, name.length());
		if (end.equals(simpleCode ? SIMPLE_SOURCE_CODE : PRIMITIVE_SOURCE_CODE)) {
			name = name.substring(0, lastDot);
		}
		Path result = currentFile.resolveSibling(name + (simpleCode ? SIMPLE_SYMBOL_FILE : PRIMITIVE_SYMBOL_FILE));
		if ( !force && Files.exists(result)) {
			return null;
		}
		return result;
	}
	
	private void assemble(PatrFile outFile, Path currentFile, Path expout, boolean executable, Path[] lockups) throws IOException {
		try (OutputStream out = outFile.openOutput(true, NO_LOCK)) {
			try (BufferedReader reader = Files.newBufferedReader(currentFile, cs)) {
				PrimitiveAssembler asm;
				if (expout != null) {
					try (OutputStream eo = Files.newOutputStream(expout, StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
						try (PrintStream eop = new PrintStream(eo, false, cs)) {
							asm = new PrimitiveAssembler(out, eop, lockups, false, true);
							asm.assemble(currentFile, reader);
							eop.flush();
						}
					}
					if (Files.size(expout) == 0) {
						Files.delete(expout);
					}
				} else {
					asm = new PrimitiveAssembler(out, null, lockups, false, true);
					asm.assemble(currentFile, reader);
				}
				outFile.setExecutable(executable, NO_LOCK);
			}
		}
	}
	
	private void copy(PatrFile outFile, Path currentFile, boolean executable) throws IOException {
		try (InputStream in = Files.newInputStream(currentFile)) {
			byte[] buffer = new byte[(int) Math.min(Files.size(currentFile), MAX_BUFFER)];
			outFile.withLock(() -> executeCopy(outFile, executable, in, buffer));
		}
	}
	
	private void executeCopy(PatrFile outFile, boolean executable, InputStream in, byte[] buffer) throws IOException, ElementLockedException {
		try (OutputStream out = outFile.openOutput(true, NO_LOCK)) {
			while (true) {
				int r = in.read(buffer, 0, buffer.length);
				if (r == -1) break;
				out.write(buffer, 0, r);
			}
		}
		outFile.setExecutable(executable, NO_LOCK);
	}
	
}
