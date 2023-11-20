package de.hechler.patrick.codesprachen.simple.interpreter.fs;

import java.io.IOError;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManager;

public class FSManagerImpl implements FSManager {
	
	private static final int MAX_BUF_SIZE = 1 << 30;
	
	private final Path                 root;
	private final Path                 cwd;
	private final FileSystem           fs;
	private final MemoryManager        mm;
	private final Map<FileMap,FileMap> maps = new ConcurrentHashMap<>();
	private final Thread               deamon;
	
	private SoftReference<ByteBuffer> buf;
	
	public FSManagerImpl(Path root, MemoryManager mm) {
		root = root.toAbsolutePath();
		this.root = root;
		this.cwd = root;
		this.fs = root.getFileSystem();
		this.mm = Objects.requireNonNull(mm, "memory manager");
		try {
			final WatchService ws = this.fs.newWatchService();
			this.deamon = Thread.ofVirtual().start(() -> {
				deamon(ws);
			});
		} catch ( IOException e ) {
			throw new IOError(e);
		}
	}
	
	private void deamon(final WatchService ws) {
		try {
			while ( true ) {
				WatchKey wk = ws.take();
				if ( this.maps.isEmpty() ) {
					continue;
				}
				for (WatchEvent<?> event : wk.pollEvents()) {
					Object context = event.context();
					if ( !( context instanceof Path p ) ) {
						System.err.println("unknown watcher context: "
							+ ( context == null ? "null" : context.getClass().getName() ) + " : " + context);
						continue;
					}
					for (Iterator<FileMap> iter = this.maps.values().iterator(); iter.hasNext();) {
						FileMap fm = iter.next();
						try {
							if ( !Files.isSameFile(p, fm.p) ) {
								continue;
							}
							updateFileMap(fm);
						} catch ( @SuppressWarnings("unused") IOException err ) {
							if ( !Files.exists(fm.p) ) {
								iter.remove();
							}
						}
					}
				}
			}
		} catch ( @SuppressWarnings("unused") InterruptedException finish ) {}
	}
	
	@Override
	public LoadFileResult loadFile(String path, long address, int flags) throws IOException {
		if ( !this.deamon.isAlive() ) {
			throw new ClosedFileSystemException();
		}
		if ( ( flags & ALL_LOAD_FLAGS ) != flags ) {
			throw new IllegalArgumentException("invalid flags: " + Integer.toHexString(flags));
		}
		Path p = fs.getPath(path);
		if ( p.isAbsolute() ) {
			Path r = p.relativize(p.getRoot());
			p = this.root.resolve(r);
		} else {
			p = this.cwd.resolve(p);
		}
		try ( SeekableByteChannel c = Files.newByteChannel(p, StandardOpenOption.READ) ) {
			final long len = c.size();
			if ( len == 0L ) {
				address = this.mm.allocate(1L, address, flags);
				return new LoadFileResult(address, 0L);
			}
			address = this.mm.allocate(len, address, flags);
			long addr = address;
			ByteBuffer bb = buf();
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
	
	@Override
	public long mapROFile(String path, long offset, long length, long requestedAddress, int flags) throws IOException {
		if ( !this.deamon.isAlive() ) {
			throw new ClosedFileSystemException();
		}
		if ( ( flags & ALL_LOAD_FLAGS ) != flags ) {
			throw new IllegalArgumentException("invalid flags: " + Integer.toHexString(flags));
		}
		flags |= LOAD_FLAG_READ_ONLY;
		if ( length <= 0L ) {
			throw new IllegalAccessError("maxLength <= 0: " + length);
		}
		Path p = fs.getPath(path);
		if ( p.isAbsolute() ) {
			Path r = p.relativize(p.getRoot());
			p = this.root.resolve(r);
		} else {
			p = this.cwd.resolve(p);
		}
		Path rp = p.toRealPath();
		requestedAddress = this.mm._fsAllocateMap(length, requestedAddress, flags);
		try {
			FileMap fm = new FileMap(requestedAddress, offset, length, rp);
			updateFileMap(fm);
			this.maps.put(fm, fm);
		} catch ( Throwable t ) {
			this.mm._fsFreeMap(requestedAddress, length);
			throw t;
		}
		return requestedAddress;
	}
	
	private void updateFileMap(FileMap fm) {
		try ( SeekableByteChannel c = Files.newByteChannel(fm.p, StandardOpenOption.READ) ) {
			c.position(fm.offset);
			long remain = fm.length;
			long addr = fm.address;
			ByteBuffer bb = buf();
			while ( true ) {
				int reat = c.read(bb);
				if ( reat == -1 ) break;
				bb.position(0);
				bb.limit(reat);
				this.mm._privilegedSetRO(addr, bb);
				bb.position(0);
				final int cap = bb.capacity();
				bb.limit(cap);
				addr += reat;
				remain -= reat;
				if ( remain < cap ) {
					if ( remain == 0 ) {
						this.mm.fill(addr, reat, 0);
						return;
					}
					bb.limit((int) remain);
				}
			}
		} catch ( IOException e ) {
			System.err.println("[FSManagerImpl]: could not update the file map at address 0x"
				+ Long.toHexString(fm.address) + " of the file " + fm.p);
			e.printStackTrace();
		}
	}
	
	@Override
	public void unloadMap(long address) {
		if ( !this.deamon.isAlive() ) {
			throw new ClosedFileSystemException();
		}
		FileMap fm = this.maps.get(new FileMap(address, 0L, 0L, null));
		this.mm._fsFreeMap(fm.address, fm.length);
	}
	
	private ByteBuffer buf() {
		SoftReference<ByteBuffer> b = this.buf;
		if ( b != null ) {
			ByteBuffer res = b.get();
			if ( res != null ) return res;
		}
		ByteBuffer n = ByteBuffer.allocateDirect((int) Math.min(mm.pageSize(), MAX_BUF_SIZE));
		this.buf = new SoftReference<>(n);
		return n;
	}
	
	record FileMap(long address, long offset, long length, Path p) {
		
		@Override
		public int hashCode() {
			return (int) ( this.address ^ ( this.address >>> 32 ) );
		}
		
		@Override
		public boolean equals(Object obj) {
			if ( this == obj ) { return true; }
			if ( !( obj instanceof FileMap ) ) { return false; }
			FileMap other = (FileMap) obj;
			return address == other.address;
		}
		
	}
	
	@Override
	public void close() throws IOException {
		this.deamon.interrupt();
		for (Iterator<FileMap> iter = this.maps.values().iterator(); iter.hasNext();) {
			FileMap fm = iter.next();
			iter.remove();
			this.mm._fsFreeMap(fm.address, fm.length);
		}
	}
	
}
