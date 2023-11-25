package de.hechler.patrick.code.simple.interpreter.fs;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

import de.hechler.patrick.code.simple.interpreter.memory.MemoryManager;

public class FSManagerImpl implements FSManager {
	
	private static final int MAX_BUF_SIZE = 1 << 30;
	
	private final Path          root;
	private final Path          cwd;
	private final FileSystem    fs;
	private final MemoryManager mm;
	private boolean             closed;
	
	private SoftReference<ByteBuffer> buf;
	
	public FSManagerImpl(Path root, MemoryManager mm) {
		root      = root.toAbsolutePath();
		this.root = root;
		this.cwd  = root;
		this.fs   = root.getFileSystem();
		this.mm   = Objects.requireNonNull(mm, "memory manager");
	}
	
	@Override
	public LoadFileResult loadFile(String path, long address, int flags) throws IOException {
		if ( this.closed ) {
			throw new ClosedFileSystemException();
		}
		if ( ( flags & ALL_LOAD_FLAGS ) != flags ) {
			throw new IllegalArgumentException("invalid flags: " + Integer.toHexString(flags));
		}
		Path p = this.fs.getPath(path);
		if ( p.isAbsolute() ) {
			Path r = p.relativize(p.getRoot());
			p = this.root.resolve(r);
		} else {
			p = this.cwd.resolve(p);
		}
		try (SeekableByteChannel c = Files.newByteChannel(p, StandardOpenOption.READ)) {
			final long len = c.size();
			if ( len == 0L ) {
				address = this.mm.allocate(1L, address, flags);
				return new LoadFileResult(address, 0L);
			}
			address = this.mm.allocate(len, address, flags);
			long       addr = address;
			ByteBuffer bb   = buf();
			while ( true ) {
				int reat = c.read(bb);
				if ( reat == -1 ) break;
				bb.position(0);
				bb.limit(reat);
				if ( ( flags & LOAD_FLAG_READ_ONLY ) != 0 ) {
					this.mm._privilegedSetRO(addr, bb);
				} else {
					this.mm.set(addr, bb);
				}
				addr += reat;
				bb.position(0);
				bb.limit(bb.capacity());
			}
			return new LoadFileResult(address, len);
		}
	}
	
	private ByteBuffer buf() {
		SoftReference<ByteBuffer> b = this.buf;
		if ( b != null ) {
			ByteBuffer res = b.get();
			if ( res != null ) return res;
		}
		ByteBuffer n = ByteBuffer.allocateDirect((int) Math.min(this.mm.pageSize(), MAX_BUF_SIZE));
		this.buf = new SoftReference<>(n);
		return n;
	}
	
	@Override
	public void close() throws IOException {
		this.closed = true;
	}
	
}
