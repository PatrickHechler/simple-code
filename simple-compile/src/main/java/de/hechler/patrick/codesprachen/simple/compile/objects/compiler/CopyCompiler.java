package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.IOError;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;


public class CopyCompiler extends PerFileCompiler {
	
	@Override
	protected void compile(Path source, File target) {
		try (SeekableByteChannel read = Files.newByteChannel(source, StandardOpenOption.READ);
				WriteStream write = target.openWrite();
				MemorySession ses = MemorySession.openConfined()) {
			MemorySegment mem  = ses.allocate(1024L);
			ByteBuffer    bbuf = mem.asByteBuffer();
			while (true) {
				bbuf.position(0);
				int r = read.read(bbuf);
				if (r == -1) break;
				long w;
				if (r == mem.byteSize()) {
					w = write.write(mem);
				} else {
					w = write.write(mem.asSlice(0L, r));
				}
				while (w < r) {
					w += write.write(mem.asSlice(w, r - w));
				}
			}
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
}
