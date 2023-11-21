package de.hechler.patrick.codesprachen.simple.interpreter.java;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import de.hechler.patrick.codesprachen.simple.interpreter.SimpleInterpreter;
import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManager;

public class JavaStdLib extends JavaDependency {
	
	private final SimpleInterpreter si;
	
	/**
	 * <ol>
	 * <li>end address of first large page
	 * <ol>
	 * <li>large data block</li>
	 * <li>large data block</li>
	 * <li>table entries (like on normal pages, but with a larger page size)</li>
	 * <li>end address of the next large page</li>
	 * <li>start address of this large page</li>
	 * </ol>
	 * </li>
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
		final long          bs  = mem.pageSize();
		if ( bs < 32L ) {
			throw new IllegalArgumentException("page size too small");
		}
		final int ws = offSize(bs);
		this.firstPage = mem.allocate(bs, 0L, 0);
		final long blockEndOff   = bs - 8L;
		final long blockStartOff = blockEndOff - ws;
		final long blockEndAddr  = this.firstPage + blockEndOff;
		btSetOffset(mem, blockEndAddr - ws, ws, blockStartOff);
		btSetOffset(mem, blockEndAddr - ( ws << 1 ), ws, 8L);
	}
	
	public long alloc(final long len, final long align) {
		if ( len <= 0L ) {
			throw new IllegalArgumentException("len <= 0: " + len);
		}
		if ( align <= 0L ) {
			throw new IllegalArgumentException("align <= 0: " + align);
		}
		final long alignM1 = align - 1L;
		if ( ( align & ( alignM1 ) ) != 0L ) {
			throw new IllegalArgumentException("align is no power of two: " + align);
		}
		final MemoryManager mem      = this.si.memManager();
		final long          pageSize = mem.pageSize();
		final int           offSize  = offSize(pageSize);
		if ( pageSize - 8L - offSize <= len ) {
			return largeAlloc(len, align);
		}
		long blockAddr = this.firstPage;
		while ( true ) {
			final long tableEndAddr     = blockAddr + pageSize - 8L;
			final long tableStartOffset = btGetOffset(mem, tableEndAddr - offSize, offSize);
			final long tableStartAddr   = blockAddr + tableStartOffset;
			if ( tableEndAddr != tableStartAddr ) {
				long lastEntryEnd = btGetOffset(mem, tableEndAddr - ( offSize << 1 ), offSize);
				if ( tableStartOffset - lastEntryEnd < offSize ) {
					blockAddr = nextPage(mem, pageSize, offSize, blockAddr);
					continue;
				}
			}
			long resultAddress =
				allocInPage(len, align, mem, blockAddr, offSize, tableEndAddr, tableStartOffset, tableStartAddr);
			if ( resultAddress != 0L ) return resultAddress;
			blockAddr = nextPage(mem, pageSize, offSize, blockAddr);
		}
	}
	
	private long largeAlloc(final long len, final long align) {
		final MemoryManager mem      = this.si.memManager();
		long                blockEnd = mem.get64(this.firstPage);
		if ( blockEnd == 0L ) {
			final long nextPageSize = largePageSize(len);
			final int  nextOffSize  = offSize(nextPageSize);
			blockEnd = createNextPage(mem, nextPageSize, nextOffSize);
		}
		while ( true ) {
			final long pageAddr         = mem.get64(blockEnd - 8L);
			final long pageSize         = blockEnd - pageAddr;
			final int  offSize          = offSize(pageSize);
			final long tableEndAddr     = pageAddr + pageSize - 16L;
			final long tableStartOffset = btGetOffset(mem, tableEndAddr - offSize, offSize);
			final long tableStartAddr   = pageAddr + tableStartOffset;
			if ( tableEndAddr != tableStartAddr ) {
				long lastEntryEnd = btGetOffset(mem, tableEndAddr - ( offSize << 1 ), offSize);
				if ( tableStartOffset - lastEntryEnd < offSize ) {
					final long nextPageSize = largePageSize(len);
					final int  nextOffSize  = offSize(nextPageSize);
					blockEnd = nextPage(mem, nextPageSize, nextOffSize, pageAddr);
					continue;
				}
			}
			long resultAddress =
				allocInPage(len, align, mem, pageAddr, offSize, tableEndAddr, tableStartOffset, tableStartAddr);
			if ( resultAddress != 0L ) return resultAddress;
			blockEnd = nextPage(mem, pageSize, offSize, pageAddr);
		}
	}
	
	private static long largePageSize(final long len) {
		final long nextPageSize = Long.highestOneBit(len) << 1L;
		if ( nextPageSize < 0L ) {
			throw new IllegalArgumentException("len >= 2^62: " + len);
		}
		return nextPageSize;
	}
	
	private static long allocInPage(final long len, final long align, final MemoryManager mem, long pageAddr,
		final int offSize, final long tableEndAddr, final long tableStartOffset, final long tableStartAddr) {
		final long alignM1 = align - 1L;
		long       addr    = tableEndAddr - offSize;
		while ( true ) {
			long startAddr = pageAddr + btGetOffset(mem, addr, offSize);
			addr -= offSize;
			long prevEndAddr;
			if ( addr < tableStartAddr ) {
				prevEndAddr = pageAddr;
			} else {
				prevEndAddr = btGetOffset(mem, addr, offSize);
			}
			if ( ( prevEndAddr & alignM1 ) != 0 ) {
				prevEndAddr = ( prevEndAddr & ~alignM1 ) + align;
			}
			long free = startAddr - prevEndAddr;
			if ( free >= len ) {
				long nextHalfFree = ( free - len ) >>> 1;
				long resultAddr   = ( prevEndAddr + nextHalfFree ) & ~alignM1;
				mem.move(tableStartAddr, tableStartOffset - ( offSize << 1 ), addr - tableStartAddr);
				addr -= offSize;
				btSetOffset(mem, addr, offSize, resultAddr - pageAddr + len);
				addr -= offSize;
				btSetOffset(mem, addr, offSize, resultAddr - pageAddr);
				btSetOffset(mem, tableEndAddr - offSize, offSize, tableStartAddr - offSize);
				return resultAddr;
			}
			if ( addr < tableStartAddr ) {
				break;
			}
			addr -= offSize;
		}
		return 0L;
	}
	
	private static long nextPage(final MemoryManager mem, final long pageSize, final int offSize, long pageAddr) {
		long next = mem.get64(pageAddr + pageSize - 8L);
		if ( next != 0L ) return next;
		return createNextPage(mem, pageSize, offSize);
	}
	
	private static long createNextPage(final MemoryManager mem, final long pageSize, final int offSize) {
		final long result;
		final long nextEndOff;
		if ( pageSize != mem.pageSize() ) {
			long pageAddr = mem.allocate(pageSize, 0L, MemoryManager.FLAG_CUSTOM);
			result = pageAddr + pageSize;
			mem.set64(result - 8L, pageAddr);
			nextEndOff = pageSize - 16L;
		} else {
			result     = mem.allocate(pageSize, 0L, 0);
			nextEndOff = pageSize - 8L;
		}
		final long nextStartOff  = nextEndOff - offSize;
		final long nextStartAddr = result + nextStartOff;
		btSetOffset(mem, nextStartAddr, offSize, nextStartOff);
		return result;
	}
	
	public void free(long addr) {
		final MemoryManager mem      = this.si.memManager();
		final long          pageSize = mem.pageSize();
		final long          pageAddr = addr & ~pageSize;
		if ( ( mem.flags(pageAddr) & MemoryManager.FLAG_CUSTOM ) != 0 ) {
			freeLarge(addr);
			return;
		}
		final int offSize = offSize(pageSize);
		freeImpl(mem, addr, pageAddr, pageAddr + pageSize - 8L - offSize, offSize);
	}
	
	private void freeLarge(long addr) {
		final MemoryManager mem     = this.si.memManager();
		long                pageEnd = mem.get64(this.firstPage);
		while ( pageEnd != 0L ) {
			long pageStart = mem.get64(pageEnd - 8L);
			if ( addr >= pageStart && addr < pageEnd ) {
				int offSize = offSize(pageEnd - pageStart);
				freeImpl(mem, addr, pageStart, pageEnd - 16L - offSize, offSize);
				return;
			}
		}
		throw new IllegalStateException("no allocated memory block starts at 0x" + Long.toHexString(addr));
	}
	
	private static void freeImpl(MemoryManager mem, final long freeAddr, final long pageAddr,
		final long tableEndStartAddr, final int offSize) {
		final long tableStartAddr = pageAddr + btGetOffset(mem, tableEndStartAddr, offSize);
		final int  dOffSize       = offSize << 1;
		final long mask           = ~( dOffSize - 1 );
		long       low            = 0; // the address may not be aligned for the mask
		long       high           = tableEndStartAddr - tableStartAddr;
		if ( ( high & mask ) != high ) {
			throw new IllegalStateException("corrupt allocation table");
		}
		final long freeOff = freeAddr - pageAddr;
		while ( low <= high ) {
			final long mid = ( ( low + high ) >>> 1 ) & mask;
			final long val = btGetOffset(mem, tableStartAddr + mid, offSize);
			if ( val < freeOff ) {
				low = mid + dOffSize;
			} else if ( val > freeOff ) {
				high = mid - dOffSize;
			} else {
				mem.move(tableStartAddr, tableStartAddr + dOffSize, mid);
				btSetOffset(mem, tableEndStartAddr - offSize, dOffSize, tableStartAddr - pageAddr + offSize);
				return;
			}
		}
		throw new IllegalStateException("no allocated memory block starts at 0x" + Long.toHexString(freeAddr));
	}
	
	private static long btGetOffset(MemoryManager mem, long addr, int offSize) {
		switch ( offSize ) {
		case 1:
			return mem.get8(addr);
		case 2:
			return mem.get16(addr);
		case 4:
			return mem.get32(addr);
		default:
			return mem.get64(addr);
		}
	}
	
	private static void btSetOffset(MemoryManager mem, long addr, int offSize, long val) {
		switch ( offSize ) {
		case 1 -> mem.set8(addr, (int) val);
		case 2 -> mem.set16(addr, (int) val);
		case 4 -> mem.set32(addr, (int) val);
		default -> mem.set64(addr, val);
		}
	}
	
	private static int offSize(long blockSize) {
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
	
	public static long allocArgs(SimpleInterpreter si, String[] args) {
		JavaStdLib    jsl  = (JavaStdLib) si.stdlib();
		MemoryManager mem  = si.memManager();
		long          len  = ( args.length + 1L ) << 3;
		long          addr = jsl.alloc(len, 1L);
		long          a    = addr;
		for (int i = 0; i < args.length; i++, a += 8) {
			byte[]     bytes  = args[i].getBytes(StandardCharsets.UTF_8);
			ByteBuffer buf    = ByteBuffer.wrap(bytes);
			long       argAdr = jsl.alloc(bytes.length + 1L, 1L);
			mem.set(argAdr, buf);
			mem.set8(argAdr + bytes.length, 0);
		}
		mem.set64(a, 0L);
		return addr;
	}
	
}
