package de.hechler.patrick.code.simple.interpreter.java;

import static de.hechler.patrick.code.simple.parser.objects.types.NativeType.UBYTE;
import static de.hechler.patrick.code.simple.parser.objects.types.NativeType.UNUM;
import static java.util.List.of;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import de.hechler.patrick.code.simple.interpreter.SimpleInterpreter;
import de.hechler.patrick.code.simple.interpreter.memory.MemoryManager;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleExportable;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleFile;
import de.hechler.patrick.code.simple.parser.objects.simplefile.SimpleVariable;
import de.hechler.patrick.code.simple.parser.objects.types.FuncType;
import de.hechler.patrick.code.simple.parser.objects.types.PointerType;
import de.hechler.patrick.code.simple.parser.objects.types.SimpleType;
import de.hechler.patrick.code.simple.parser.objects.types.StructType;
import de.hechler.patrick.code.simple.parser.objects.value.ScalarNumericVal;

public class JavaStdLib extends JavaDependency {
	
	private static final SimpleType PNTR  =
		PointerType.create(StructType.create(List.of(), 0, ErrorContext.NO_CONTEXT), ErrorContext.NO_CONTEXT);
	private static final SimpleType BPNTR = PointerType.create(UBYTE, ErrorContext.NO_CONTEXT);
	
	private SimpleInterpreter si;
	
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
	 * <li>start address of the next page</li>
	 * </ol>
	 */
	private long firstPage;
	
	private ByteBuffer sysinBuf;
	
	public JavaStdLib() {
		super(null);
		function("exit", ft(of(), of(sv(UBYTE, "exitnum"))), (si_, args) -> {
			int eval = (int) ( (ConstantValue.ScalarValue) args.get(0) ).value();
			throw new SimpleInterpreter.ExitError(eval);
		});
		function("mem_alloc", ft(of(sv(PNTR, "addr")), of(sv(UNUM, "length"), sv(UNUM, "align"))), (si_, args) -> {// NOSONAR
			long len = ( (ConstantValue.ScalarValue) args.get(0) ).value();
			long align = ( (ConstantValue.ScalarValue) args.get(1) ).value();
			return List.of(new ConstantValue.ScalarValue(UNUM, alloc(si_, len, align)));
		});
		function("mem_realloc",
			ft(of(sv(PNTR, "new_addr")), of(sv(PNTR, "old_addr"), sv(UNUM, "new_length"), sv(UNUM, "new_align"))),
			(si_, args) -> {
				long addr = ( (ConstantValue.ScalarValue) args.get(0) ).value();
				long nlen = ( (ConstantValue.ScalarValue) args.get(1) ).value();
				long nalign = ( (ConstantValue.ScalarValue) args.get(2) ).value();
				return List.of(new ConstantValue.ScalarValue(PNTR, realloc(si_, addr, nlen, nalign)));
			});
		function("mem_free", ft(of(), of(sv(PNTR, "addr"))), (si_, args) -> {
			long addr = ( (ConstantValue.ScalarValue) args.get(0) ).value();
			free(si_, addr);
			return List.of();
		});
		function("mem_copy", ft(of(), of(sv(PNTR, "from"), sv(PNTR, "to"), sv(UNUM, "length"))), (si_, args) -> {
			long from = ( (ConstantValue.ScalarValue) args.get(0) ).value();
			long to = ( (ConstantValue.ScalarValue) args.get(1) ).value();
			long length = ( (ConstantValue.ScalarValue) args.get(2) ).value();
			si_.memManager().copy(from, to, length);
			return List.of();
		});
		function("mem_move", ft(of(), of(sv(PNTR, "from"), sv(PNTR, "to"), sv(UNUM, "length"))), (si_, args) -> {
			long from = ( (ConstantValue.ScalarValue) args.get(0) ).value();
			long to = ( (ConstantValue.ScalarValue) args.get(1) ).value();
			long length = ( (ConstantValue.ScalarValue) args.get(2) ).value();
			si_.memManager().move(from, to, length);
			return List.of();
		});
		function("writestr", ft(of(sv(UNUM, "wrote"), sv(UNUM, "errno")), of(sv(BPNTR, "string"))), (si_, args) -> {
			long addr = ( (ConstantValue.ScalarValue) args.get(0) ).value();
			long len;
			MemoryManager mem = si_.memManager();
			for (len = 0L; mem.get8(addr + len) != 0; len++);
			if ( len > Integer.MAX_VALUE ) len = Integer.MAX_VALUE;
			ByteBuffer bb = ByteBuffer.allocate((int) len);
			mem.get(addr, bb);
			String str = new String(bb.array(), StandardCharsets.UTF_8);
			System.out.print(str);
			return List.of(new ConstantValue.ScalarValue(UNUM, len), new ConstantValue.ScalarValue(UNUM, 0L));
		});
		function("readln",
			ft(of(sv(UNUM, "read_len"), sv(UNUM, "errno")), of(sv(BPNTR, "buffer"), sv(UNUM, "max_length"))),
			(si_, args) -> {
				long addr = ( (ConstantValue.ScalarValue) args.get(0) ).value();
				long maxLen = ( (ConstantValue.ScalarValue) args.get(1) ).value();
				long len = 0L;
				long errno = 0L;
				MemoryManager mem = si_.memManager();
				if ( maxLen < 0L ) {
					errno = 1L;
				}
				return readLine(addr, maxLen, len, errno, mem);
			});
		JavaDependency sys = new JavaDependency(null) {
			@Override
			public int hashCode() {
				return "std:sys".hashCode();
			}
			
			@Override
			public boolean equals(Object obj) {
				return obj == this;
			}
		};
		sys.function("pagesize", ft(of(sv(UNUM, "result")), of()),
			(si_, args) -> List.of(new ConstantValue.ScalarValue(UNUM, si_.memManager().pageSize())));
		sys.function("pageshift", ft(of(sv(UNUM, "result")), of()),
			(si_, args) -> List.of(new ConstantValue.ScalarValue(UNUM, si_.memManager().pageShift())));
		dependency(sys, "sys", ErrorContext.NO_CONTEXT);
		variable(new SimpleVariable(PNTR, "NULL", ScalarNumericVal.create(PNTR, 0L, ErrorContext.NO_CONTEXT),
			SimpleVariable.FLAG_CONSTANT | SimpleExportable.FLAG_EXPORT), ErrorContext.NO_CONTEXT);
	}
	
	private List<ConstantValue> readLine(long addr, long maxLen, long len, long errno, MemoryManager mem) {
		ByteBuffer bb = this.sysinBuf;
		byte[] buf;
		if ( bb == null ) {
			bb = ByteBuffer.allocate(128);
			this.sysinBuf = bb;
			buf = bb.array();
		} else if ( bb.hasRemaining() ) {
			buf = bb.array();
			int limit = bb.limit();
			int cpy = (int) Math.max(bb.remaining(), maxLen - len);
			bb.limit(bb.position() + cpy);
			mem.set(addr, bb);
			bb.limit(limit);
			len += cpy;
			addr += cpy;
		} else buf = bb.array();
		try {
			while ( len < maxLen ) {
				int r = System.in.available();
				if ( r == 0 ) {
					int b = System.in.read();
					if ( b == -1 ) {
						break;
					}
					mem.set8(addr++, b);
					len++;
					if ( b == '\n' ) break;
					if ( len == maxLen ) break;
				}
				r = (int) Math.max(maxLen - len, r);
				r = Math.max(buf.length, r);
				r = System.in.read(buf, 0, r);
				bb.position(0);
				for (int i = 0; i < buf.length; i++) {
					if ( buf[i] == '\n' ) {
						bb.limit(i);
						mem.set(addr, bb);
						bb.position(i);
						bb.limit(r);
						return readResult(len, errno);
					}
				}
				bb.limit(r);
				mem.set(addr, bb);
				addr += r;
			}
		} catch (ClosedChannelException e) {
			errno = 2L;
			System.err.println(e);
		} catch (IOException e) {
			errno = 3L;
			System.err.println(e);
		}
		return readResult(len, errno);
	}
	
	private static List<ConstantValue> readResult(long len, long errno) {
		return List.of(new ConstantValue.ScalarValue(UNUM, len), new ConstantValue.ScalarValue(UNUM, errno));
	}
	
	private void init(SimpleInterpreter si) {
		if ( si == null ) {
			throw new NullPointerException("si");
		}
		if ( this.si == si ) {
			return;
		} else if ( this.si != null ) {
			throw new IllegalStateException("already initilized");
		}
		this.si = si;
		final MemoryManager mem = this.si.memManager();
		final long bs = mem.pageSize();
		if ( bs < 32L ) {
			throw new IllegalArgumentException("page size too small");
		}
		final int offSize = offSize(bs);
		this.firstPage = mem.allocate(bs, 0L, 0);
		final long blockEndOff = bs - 8L;
		final long blockStartOff = blockEndOff - offSize * 3;
		final long blockEndAddr = this.firstPage + blockEndOff;
		btSetOffset(mem, blockEndAddr - offSize, offSize, blockStartOff);
		btSetOffset(mem, blockEndAddr - ( offSize << 1 ), offSize, 8L);
	}
	
	private static SimpleVariable sv(SimpleType t, String name) {
		return new SimpleVariable(t, name, null, 0);
	}
	
	private static FuncType ft(List<SimpleVariable> res, List<SimpleVariable> args) {
		return FuncType.create(res, args, FuncType.FLAG_FUNC_ADDRESS | SimpleExportable.FLAG_EXPORT,
			ErrorContext.NO_CONTEXT);
	}
	
	public long alloc(SimpleInterpreter si, final long len, final long align) {
		init(si);
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
		final MemoryManager mem = this.si.memManager();
		final long pageSize = mem.pageSize();
		final int offSize = offSize(pageSize);
		if ( pageSize - 8L - offSize <= len ) {
			return largeAlloc(len, align);
		}
		long blockAddr = this.firstPage;
		while ( true ) {
			final long tableEndAddr = blockAddr + pageSize - 8L;
			final long tableStartOffset = btGetOffset(mem, tableEndAddr - offSize, offSize);
			final long tableStartAddr = blockAddr + tableStartOffset;
			long lastEntryEnd = btGetOffset(mem, tableEndAddr - ( offSize << 1 ), offSize);
			if ( tableStartOffset - lastEntryEnd < offSize ) {
				blockAddr = nextPage(mem, pageSize, offSize, blockAddr);
				continue;
			}
			long resultAddress =
				allocInPage(len, align, mem, blockAddr, offSize, tableEndAddr, tableStartOffset, tableStartAddr);
			if ( resultAddress != 0L ) return resultAddress;
			blockAddr = nextPage(mem, pageSize, offSize, blockAddr);
		}
	}
	
	private long largeAlloc(final long len, final long align) {
		final MemoryManager mem = this.si.memManager();
		long blockEnd = mem.get64(this.firstPage);
		if ( blockEnd == 0L ) {
			final long nextPageSize = largePageSize(len);
			final int nextOffSize = offSize(nextPageSize);
			blockEnd = createNextPage(mem, nextPageSize, nextOffSize);
		}
		while ( true ) {
			final long pageAddr = mem.get64(blockEnd - 8L);
			final long pageSize = blockEnd - pageAddr;
			final int offSize = offSize(pageSize);
			final long tableEndAddr = pageAddr + pageSize - 16L;
			final long tableStartOffset = btGetOffset(mem, tableEndAddr - offSize, offSize);
			final long tableStartAddr = pageAddr + tableStartOffset;
			if ( tableEndAddr != tableStartAddr ) {
				long lastEntryEnd = btGetOffset(mem, tableEndAddr - ( offSize << 1 ), offSize);
				if ( tableStartOffset - lastEntryEnd < offSize ) {
					final long nextPageSize = largePageSize(len);
					final int nextOffSize = offSize(nextPageSize);
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
		long addr = tableEndAddr - offSize;
		while ( true ) {
			final long startAddr = pageAddr + btGetOffset(mem, addr, offSize);
			addr -= offSize;
			long prevEndAddr;
			if ( addr < tableStartAddr ) {
				prevEndAddr = pageAddr;
			} else {
				prevEndAddr = pageAddr + btGetOffset(mem, addr, offSize);
			}
			if ( ( prevEndAddr & alignM1 ) != 0 ) {
				prevEndAddr = ( prevEndAddr & ~alignM1 ) + align;
			}
			final long free = startAddr - prevEndAddr;
			if ( free >= len ) {
				long nextHalfFree = ( free - len ) >>> 1;
				long resultAddr = ( prevEndAddr + nextHalfFree ) & ~alignM1;
				mem.get8(tableStartAddr);
				mem.get8(tableEndAddr);
				mem.move(tableStartAddr, tableStartAddr - ( offSize << 1 ), addr + offSize - tableStartAddr);
				btSetOffset(mem, addr, offSize, resultAddr - pageAddr + len);
				btSetOffset(mem, addr - offSize, offSize, resultAddr - pageAddr);
				btSetOffset(mem, tableEndAddr - offSize, offSize, tableStartOffset - offSize);
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
			result = mem.allocate(pageSize, 0L, 0);
			nextEndOff = pageSize - 8L;
		}
		final long nextStartOff = nextEndOff - offSize;
		final long nextStartAddr = result + nextStartOff;
		btSetOffset(mem, nextStartAddr, offSize, nextStartOff);
		return result;
	}
	
	public long realloc(SimpleInterpreter si, final long oldAddr, final long newLength, final long newAlign) {
		init(si);
		if ( newLength <= 0L ) {
			if ( newLength != 0L ) {
				throw new IllegalArgumentException("newLength < 0: " + newLength);
			}
			free(si, oldAddr);
			return 0L;
		}
		if ( newAlign <= 0L ) {
			throw new IllegalArgumentException("align <= 0: " + newAlign);
		}
		final long alignM1 = newAlign - 1L;
		if ( ( newAlign & ( alignM1 ) ) != 0L ) {
			throw new IllegalArgumentException("align is no power of two: " + newAlign);
		}
		final MemoryManager mem = this.si.memManager();
		final long pageSize = mem.pageSize();
		final long pageAddr = oldAddr & ~pageSize;
		if ( ( mem.flags(pageAddr) & MemoryManager.FLAG_CUSTOM ) != 0 ) {
			return reallocLarge(oldAddr, newLength, newAlign);
		}
		final int offSize = offSize(pageSize);
		return reallocImpl(mem, oldAddr, newLength, newAlign, pageAddr, pageAddr + pageSize - 8L - offSize, offSize);
	}
	
	private long reallocLarge(long oldAddr, long newLength, long newAlign) {
		final MemoryManager mem = this.si.memManager();
		long pageEnd = mem.get64(this.firstPage);
		while ( pageEnd != 0L ) {
			long pageStart = mem.get64(pageEnd - 8L);
			if ( oldAddr >= pageStart && oldAddr < pageEnd ) {
				int offSize = offSize(pageEnd - pageStart);
				return reallocImpl(mem, oldAddr, newLength, newAlign, pageStart, pageEnd - 16L - offSize, offSize);
			}
			pageEnd = mem.get64(pageEnd - 16L);
		}
		throw new IllegalStateException(noAllocMsg(oldAddr));
	}
	
	private long reallocImpl(MemoryManager mem, final long oldAddr, final long newLength, final long newAlign,
		final long pageAddr, final long tableEndStartAddr, final int offSize) {
		final long tableStartAddr = pageAddr + btGetOffset(mem, tableEndStartAddr, offSize);
		final int dOffSize = offSize << 1;
		final long mask = ~( dOffSize - 1 );
		long low = 0; // the address may not be aligned for the mask
		long high = tableEndStartAddr - tableStartAddr;
		if ( ( high & mask ) != high ) {
			throw new IllegalStateException("corrupt allocation table");
		}
		final long oldOff = oldAddr - pageAddr;
		while ( low <= high ) {
			final long mid = ( ( low + high ) >>> 1 ) & mask;
			final long val = btGetOffset(mem, tableStartAddr + mid, offSize);
			if ( val < oldOff ) {
				low = mid + dOffSize;
			} else if ( val > oldOff ) {
				high = mid - dOffSize;
			} else {
				final long newAlignM1 = newAlign - 1L;
				long beforeEndOff = mid == 0L ? 0L : btGetOffset(mem, tableStartAddr + mid - offSize, offSize);
				if ( ( beforeEndOff & newAlignM1 ) != 0L ) {
					beforeEndOff = ( beforeEndOff & ~newAlignM1 ) + newAlign;
				}
				final long afterStartOff = btGetOffset(mem, tableStartAddr + mid + dOffSize, offSize);
				final long maxFree = afterStartOff - beforeEndOff;
				final long oldLength = oldAddr - pageAddr - btGetOffset(mem, tableStartAddr + offSize, offSize);
				if ( maxFree >= newLength ) {
					final long newBeforeFree = ( ( maxFree - newLength ) >>> 1 ) & ~newAlignM1;
					final long resultOff = beforeEndOff + newBeforeFree;
					btSetOffset(mem, tableStartAddr, offSize, resultOff);
					btSetOffset(mem, tableStartAddr + offSize, offSize, resultOff + newLength);
					final long result = pageAddr + resultOff;
					if ( result != oldAddr ) {
						mem.move(oldAddr, result, Math.min(newLength, oldLength));
					}
					return result;
				}
				long result = alloc(this.si, newLength, newAlign);
				mem.copy(oldAddr, result, Math.min(newLength, oldLength));
				free(this.si, oldAddr);
				return result;
			}
		}
		throw new IllegalStateException(noAllocMsg(oldAddr));
	}
	
	private static String noAllocMsg(final long oldAddr) {
		return "no allocated memory block starts at 0x" + Long.toHexString(oldAddr);
	}
	
	public void free(SimpleInterpreter si, long addr) {
		init(si);
		final MemoryManager mem = this.si.memManager();
		final long pageSize = mem.pageSize();
		final long pageAddr = addr & ~pageSize;
		if ( ( mem.flags(pageAddr) & MemoryManager.FLAG_CUSTOM ) != 0 ) {
			freeLarge(addr);
			return;
		}
		final int offSize = offSize(pageSize);
		if ( freeImpl(mem, addr, pageAddr, pageAddr + pageSize - 8L - offSize, offSize) ) {
			long page = mem.get64(this.firstPage);
			if ( page == pageAddr ) {
				throw new IllegalStateException("coruppted memory allocation table");
			}
			while ( true ) {
				long next = mem.get64(page + pageSize - 8L);
				if ( next == pageAddr ) {
					long nextNext = mem.get64(next + pageSize - 8L);
					mem.set64(addr + pageSize - 8L, nextNext);
					mem.free(next, pageSize);
					return;
				}
			}
		}
	}
	
	private void freeLarge(long addr) {
		final MemoryManager mem = this.si.memManager();
		long pageEnd = mem.get64(this.firstPage);
		long prevPageEnd = 0L;
		while ( pageEnd != 0L ) {
			long pageStart = mem.get64(pageEnd - 8L);
			if ( addr >= pageStart && addr < pageEnd ) {
				int offSize = offSize(pageEnd - pageStart);
				if ( freeImpl(mem, addr, pageStart, pageEnd - 16L - offSize, offSize) ) {
					long nextPageEnd = mem.get64(pageEnd - 16L);
					mem.set64(prevPageEnd - 16L, nextPageEnd);
					mem.free(pageStart, pageEnd - pageStart);
				}
				return;
			}
			prevPageEnd = pageEnd;
			pageEnd = mem.get64(pageEnd - 16L);
		}
		throw new IllegalStateException(noAllocMsg(addr));
	}
	
	private static boolean freeImpl(MemoryManager mem, final long freeAddr, final long pageAddr,
		final long tableEndStartAddr, final int offSize) {
		final long tableStartAddr = pageAddr + btGetOffset(mem, tableEndStartAddr, offSize);
		final int dOffSize = offSize << 1;
		final long mask = ~( dOffSize - 1 );
		long low = 0; // the address may not be aligned for the mask
		long high = tableEndStartAddr - tableStartAddr;
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
				return tableStartAddr + offSize >= tableEndStartAddr;
			}
		}
		throw new IllegalStateException(noAllocMsg(freeAddr));
	}
	
	// used for debugging
	@SuppressWarnings("unused")
	private static String metadataToString(final MemoryManager mem, final long pageAddr, final long pageSize) {
		StringBuilder sb = new StringBuilder().append('\n');
		final int offSize = offSize(pageSize);
		final boolean largePage = pageSize != mem.pageSize();
		sb.append("page: 0x").append(Long.toHexString(pageAddr)).append(" : ").append(Long.toUnsignedString(pageAddr))
			.append('\n');
		sb.append("  size: 0x").append(Long.toHexString(pageSize)).append(" : ").append(pageSize).append('\n');
		sb.append("  large page: ").append(largePage ? "yes" : "no").append('\n');
		long off = pageSize - ( largePage ? 16L : 8L );
		final long tableEndAddr = pageAddr + off;
		final long tableStartOff = btGetOffset(mem, tableEndAddr - offSize, offSize);
		sb.append("  tableStart: ").append(tableStartOff).append(" : 0x").append(Long.toHexString(tableStartOff)) // NOSONAR
			.append(", addr: ").append(Long.toUnsignedString(pageAddr + tableStartOff)).append(" : 0x")
			.append(Long.toHexString(pageAddr + tableStartOff)).append('\n');
		int i = -1;
		for (off -= offSize * 3; off >= tableStartOff; off -= offSize << 1, i--) {
			long startOff = btGetOffset(mem, pageAddr + off, offSize);
			long endOff = btGetOffset(mem, pageAddr + off + offSize, offSize);
			sb.append("  tableEndAddr[").append(i).append("] : (page + ").append(off).append(" : 0x")
				.append(Long.toHexString(off)).append("): ").append(startOff).append("..").append(endOff).append(" 0x")
				.append(Long.toHexString(startOff)).append("..0x").append(Long.toHexString(endOff)).append(", addr: ")
				.append(Long.toUnsignedString(pageAddr + startOff)).append("..")
				.append(Long.toUnsignedString(pageAddr + endOff)).append(" 0x")
				.append(Long.toHexString(pageAddr + startOff)).append("..0x")
				.append(Long.toHexString(pageAddr + endOff)).append('\n');
		}
		return sb.toString();
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
		SimpleFile sl = si.stdlib();
		MemoryManager mem = si.memManager();
		long len = ( args.length + 1L ) << 3;
		long addr = alloc(si, sl, len);
		long a = addr;
		for (int i = 0; i < args.length; i++, a += 8L) {
			byte[] bytes = args[i].getBytes(StandardCharsets.UTF_8);
			ByteBuffer buf = ByteBuffer.wrap(bytes);
			long argAdr = alloc(si, sl, bytes.length + 1L);
			mem.set(argAdr, buf);
			mem.set8(argAdr + bytes.length, 0);
			mem.set64(a, argAdr);
		}
		mem.set64(a, 0L);
		return addr;
	}
	
	private static long alloc(SimpleInterpreter si, SimpleFile sl, long len) {
		if ( sl instanceof JavaStdLib jsl ) return jsl.alloc(si, len, 1L);
		ConstantValue.ScalarValue param0 = new ConstantValue.ScalarValue(UNUM, len);
		ConstantValue.ScalarValue param1 = new ConstantValue.ScalarValue(UNUM, 1L);
		List<ConstantValue> params = List.of(param0, param1);
		ConstantValue result = si.execute(sl, "mem_alloc", params).get(0);
		return ( (ConstantValue.ScalarValue) result ).value();
	}
	
	@Override
	public int hashCode() {
		return "std".hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}
	
}
