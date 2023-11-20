package de.hechler.patrick.codesprachen.simple.interpreter.java;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import de.hechler.patrick.codesprachen.simple.interpreter.SimpleInterpreter;
import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManager;

public class JavaStdLib extends JavaDependency {
	
	private final SimpleInterpreter si;
	
	/**
	 * <ol>
	 * <li>allocated blocks</li>
	 * <li>allocation table
	 * <ol>
	 * <li>table entries (ordered by offset)
	 * <ol>
	 * <li>start offset</li>
	 * <li>end offset</li>
	 * </ol>
	 * </li>
	 * <li>offset of allocation table</li>
	 * </ol>
	 * </li>
	 * <li>next page</li>
	 * </ol>
	 */
	private final long firstPage;
	
	public JavaStdLib(SimpleInterpreter si) {
		super(null);
		this.si = si;
		final MemoryManager mem = this.si.memManager();
		final long bs = blockSize(mem);
		final int ws = offSize(bs);
		this.firstPage = mem.allocate(bs, 0L, 0);
		final long blockEndOff = bs - 8L;
		final long blockStartOff = blockEndOff - ws;
		final long blockEndAddr = this.firstPage + blockEndOff;
		btSetOffset(mem, blockEndAddr - ws, ws, blockStartOff);
//		btSetOffset(mem, blockEndAddr - ( ws << 1 ), ws, 0); // the entire block is filled with zero after allocation
	}
	
	public long alloc(final long len, final long align) {
		if ( len <= 0L ) {
			throw new IllegalArgumentException("len <= 0: " + len);
		}
		if ( align <= 0L ) {
			throw new IllegalArgumentException("align <= 0: " + align);
		}
		if ( align == 1L ) {
			return alloc(len);
		}
		final long alignM1 = align - 1L;
		if ( ( align & ( alignM1 ) ) != 0L ) {
			throw new IllegalArgumentException("align is no power of two: " + align);
		}
		final MemoryManager mem = this.si.memManager();
		final long bs = blockSize(mem);
		final int ws = offSize(bs);
		if ( bs - 8L - ws <= len ) {
			if ( align > 8 || align > mem.pageSize() ) {
				// TODO
			}
			long addr = mem.allocate(len + 8L, 0L, MemoryManager.FLAG_CUSTOM);
			mem.set64(addr, len);
			return addr + 8L;
		}
		long blockAddr = this.firstPage;
		while ( true ) {
			final long tableEndAddr = blockAddr + bs - 8L;
			final long tableStartOffset = btGetOffset(mem, tableEndAddr - ws, ws);
			final long tableStartAddr = blockAddr + tableStartOffset;
			if ( tableEndAddr != tableStartAddr ) {
				long lastEntryEnd = btGetOffset(mem, tableEndAddr - ( ws << 1 ), ws);
				if ( tableStartOffset - lastEntryEnd < ws ) {
					blockAddr = nextPage(mem, bs, ws, blockAddr, tableEndAddr);
					continue;
				}
			}
			long addr = tableEndAddr - ws;
			while ( true ) {
				long startAddr = blockAddr + btGetOffset(mem, addr, ws);
				addr -= ws;
				long prevEndAddr;
				if ( addr < tableStartAddr ) {
					prevEndAddr = blockAddr;
				} else {
					prevEndAddr = btGetOffset(mem, addr, ws);
				}
				if ( ( prevEndAddr & alignM1 ) != 0 ) {
					prevEndAddr = ( prevEndAddr & ~alignM1 ) + align;
				}
				long free = startAddr - prevEndAddr;
				if ( free >= len ) {
					long nextHalfFree = ( free - len ) >>> 1;
					long resultAddr = ( prevEndAddr + nextHalfFree ) & ~alignM1;
					mem.move(tableStartAddr, tableStartOffset - ( ws << 1 ), addr - tableStartAddr);
					addr -= ws;
					btSetOffset(mem, addr, ws, resultAddr - blockAddr + len);
					addr -= ws;
					btSetOffset(mem, addr, ws, resultAddr - blockAddr);
					btSetOffset(mem, tableEndAddr - ws, ws, tableStartAddr - ws);
					return resultAddr;
				}
				if ( addr < tableStartAddr ) {
					break;
				}
				addr -= ws;
			}
			if ( align > mem.pageSize() ) {
				blockAddr = nextPage(mem, bs, ws, blockAddr, tableEndAddr, align);
			} else {
				blockAddr = nextPage(mem, bs, ws, blockAddr, tableEndAddr);
			}
		}
	}
	
	public long alloc(final long len) {
		if ( len <= 0L ) {
			throw new IllegalArgumentException("len <= 0: " + len);
		}
		final MemoryManager mem = this.si.memManager();
		final long bs = blockSize(mem);
		final int ws = offSize(bs);
		if ( bs - 8L - ws <= len ) {
			long addr = mem.allocate(len + 8L, 0L, MemoryManager.FLAG_CUSTOM);
			mem.set64(addr, len + 8L);
			return addr + 8L;
		}
		long blockAddr = this.firstPage;
		while ( true ) {
			final long tableEndAddr = blockAddr + bs - 8L;
			final long tableStartOffset = btGetOffset(mem, tableEndAddr - ws, ws);
			final long tableStartAddr = blockAddr + tableStartOffset;
			if ( tableEndAddr != tableStartAddr ) {
				long lastEntryEnd = btGetOffset(mem, tableEndAddr - ( ws << 1 ), ws);
				if ( tableStartOffset - lastEntryEnd < ws ) {
					blockAddr = nextPage(mem, bs, ws, blockAddr, tableEndAddr);
					continue;
				}
			}
			long addr = tableEndAddr - ws;
			while ( true ) {
				long startOff = btGetOffset(mem, addr, ws);
				addr -= ws;
				long prevEndOff;
				if ( addr < tableStartAddr ) {
					prevEndOff = 0;
				} else {
					prevEndOff = btGetOffset(mem, addr, ws);
				}
				long free = startOff - prevEndOff;
				if ( free >= len ) {
					long nextHalfFree = ( free - len ) >>> 1;
					long resultOffset = prevEndOff + nextHalfFree;
					mem.move(tableStartAddr, tableStartOffset - ( ws << 1 ), addr - tableStartAddr);
					addr -= ws;
					btSetOffset(mem, addr, ws, resultOffset + len);
					addr -= ws;
					btSetOffset(mem, addr, ws, resultOffset);
					btSetOffset(mem, tableEndAddr - ws, ws, tableStartAddr - ws);
					return blockAddr + resultOffset;
				}
				if ( addr < tableStartAddr ) {
					break;
				}
				addr -= ws;
			}
			blockAddr = nextPage(mem, bs, ws, blockAddr, tableEndAddr);
		}
	}
	
	private long nextPage(final MemoryManager mem, final long bs, final int ws, long blockAddr,
		final long tableEndAddr) {
		long next = mem.get64(tableEndAddr);
		if ( next == 0 ) {
			next = mem.allocate(bs, 0L, 0);
			final long nextEndOff = bs - 8L;
			final long nextStartOff = nextEndOff - ws;
			final long nextEndAddr = blockAddr + nextEndOff;
			btSetOffset(mem, nextEndAddr - ws, ws, nextStartOff);
		}
		return next;
	}
	
	// force alignment of more than page align, when a new page is allocated
	private long nextPage(final MemoryManager mem, final long bs, final int ws, long blockAddr, final long tableEndAddr,
		final long align) {
		assert mem.pageSize() < align && mem.pageSize() <= bs;
		long next = mem.get64(tableEndAddr);
		if ( next == 0 ) {
			next = mem.allocate(align + bs, 0L, 0);
			final long alignM1 = align - 1L;
			if ( ( next & alignM1 ) == 0 ) {
				mem.free(next + bs, align);
			} else {
				long oldNext = next;
				next = ( next & alignM1 ) + align;
				mem.free(oldNext, next - oldNext);
				long nextEnd = next + bs;
				long freeEnd = oldNext + align + bs;
				long freeLen = freeEnd - nextEnd;
				if ( freeLen != 0 ) {
					mem.free(nextEnd, freeLen);
				}
			}
			final long nextEndOff = bs - 8L;
			final long nextStartOff = nextEndOff - ws;
			final long nextEndAddr = blockAddr + nextEndOff;
			btSetOffset(mem, nextEndAddr - ws, ws, nextStartOff);
		}
		return next;
	}
	
	public void free(long addr) {
		final MemoryManager mem = this.si.memManager();
		final long bs = blockSize(mem);
		final int ws = offSize(bs);
		final long blockAddr = addr & ~bs;
		if ( blockAddr == addr - 8L && ( mem.flags(blockAddr) & MemoryManager.FLAG_CUSTOM ) != 0 ) {
			long len = mem.get64(blockAddr);
			mem.free(blockAddr, len);
			return;
		} else if ( ( mem.flags(blockAddr) & MemoryManager.FLAG_CUSTOM ) != 0 ) {
			// TODO
		}
		// TODO
	}
	
	private static long btGetOffset(MemoryManager mem, long startAddr, int ws) {
		return switch ( ws ) {
		case 1 -> mem.get8(startAddr);
		case 2 -> mem.get16(startAddr);
		case 4 -> mem.get32(startAddr);
		default -> mem.get64(startAddr);
		};
	}
	
	private static void btSetOffset(MemoryManager mem, long startAddr, int ws, long val) {
		switch ( ws ) {
		case 1 -> mem.set8(startAddr, (int) val);
		case 2 -> mem.set16(startAddr, (int) val);
		case 4 -> mem.set32(startAddr, (int) val);
		default -> mem.set64(startAddr, val);
		};
	}
	
	private int offSize(long blockSize) {
		if ( blockSize <= ( 1L << 16 ) ) {
			if ( blockSize <= ( 1L << 8 ) ) {
				return 1;
			}
			return 2;
		}
		if ( blockSize <= ( 1L << 32 ) ) {
			return 4;
		}
		return 8;
	}
	
	private static long blockSize(MemoryManager mem) {
		long ps = mem.pageSize();
		if ( ps >= 256 ) return ps;
		return 256;
	}
	
	public static long allocArgs(SimpleInterpreter si, String[] args) {
		JavaStdLib jsl = (JavaStdLib) si.stdlib();
		MemoryManager mem = si.memManager();
		long len = ( args.length + 1L ) << 3;
		long addr = jsl.alloc(len);
		long a = addr;
		for (int i = 0; i < args.length; i++, a += 8) {
			byte[] bytes = args[i].getBytes(StandardCharsets.UTF_8);
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			long argAdr = jsl.alloc(bytes.length + 1L);
			mem.set(argAdr, buf);
			mem.set8(argAdr + bytes.length, 0);
		}
		mem.set64(a, 0L);
		return addr;
	}
	
}
