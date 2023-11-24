package de.hechler.patrick.codesprachen.simple.interpreter.memory;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongSupplier;
import java.util.stream.LongStream;


public class MemoryManagerImpl implements MemoryManager {
	
	private static final ValueLayout.OfLong  INT64;
	private static final ValueLayout.OfInt   INT32;
	private static final ValueLayout.OfShort INT16;
	private static final ValueLayout.OfByte  INT8;
	
	private static final ValueLayout.OfLong  MA_INT64;
	private static final ValueLayout.OfInt   MA_INT32;
	private static final ValueLayout.OfShort MA_INT16;
	
	static {
		if ( ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ) {
			INT64 = ValueLayout.JAVA_LONG;
			INT32 = ValueLayout.JAVA_INT;
			INT16 = ValueLayout.JAVA_SHORT;
			INT8 = ValueLayout.JAVA_BYTE;
			
			MA_INT64 = ValueLayout.JAVA_LONG_UNALIGNED;
			MA_INT32 = ValueLayout.JAVA_INT_UNALIGNED;
			MA_INT16 = ValueLayout.JAVA_SHORT_UNALIGNED;
		} else {
			INT64 = ValueLayout.JAVA_LONG.withOrder(ByteOrder.LITTLE_ENDIAN);
			INT32 = ValueLayout.JAVA_INT.withOrder(ByteOrder.LITTLE_ENDIAN);
			INT16 = ValueLayout.JAVA_SHORT.withOrder(ByteOrder.LITTLE_ENDIAN);
			INT8 = ValueLayout.JAVA_BYTE.withOrder(ByteOrder.LITTLE_ENDIAN);
			
			MA_INT64 = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
			MA_INT32 = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
			MA_INT16 = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
		}
	}
	
	private final int          pageShift;
	private final LongSupplier rnd;
	private final Arena        arena;
	private Object[]           pages;
	private ListEntry          free;
	private int                allocatedPages;
	
	private static final int MAX_ARR_SIZE = 1 << 30;
	
	public static final int MIN_PAGE_SHIFT = 5;
	public static final int MAX_PAGE_SHIFT = 62;
	
	public static final long MIN_PAGE_SIZE = 1L << MIN_PAGE_SHIFT;
	public static final long MAX_PAGE_SIZE = 1L << MAX_PAGE_SHIFT;
	
	public MemoryManagerImpl() {
		this(defSup());
	}
	
	public MemoryManagerImpl(LongSupplier rnd) {
		this(clacDefPageShift(), rnd);
	}
	
	public MemoryManagerImpl(int pageShift) {
		this(pageShift, defSup());
	}
	
	private static LongSupplier defSup() {
		Random rnd = new Random();
		// by default only use the first 62 bits:
		// this prevents requested pages with the GROW_DOWN flag set
		// to hit randomly allocated pages when they are in the high address space
		// theoretical the pages are unsigned but the negative pages may be used for something else
		return () -> rnd.nextLong() & 0x3FFFFFFFFFFFFFFFL;
	}
	
	public MemoryManagerImpl(int pageShift, LongSupplier rnd) {
		if ( pageShift < MIN_PAGE_SHIFT || pageShift > MAX_PAGE_SHIFT ) {
			throw new IllegalArgumentException("pageShift is invalid: " + pageShift);
		}
		long pageSize = 1L << pageShift;
		long totalMem = Runtime.getRuntime().totalMemory();
		if ( totalMem <= pageSize ) {
			throw new IllegalArgumentException("there is not even enough memory for a single page! pageSize: " + pageSize + " totalMemory: " + totalMem);
		}
		this.pageShift = pageShift;
		this.rnd = Objects.requireNonNull(rnd, "random");
		this.arena = Arena.ofConfined();
		this.pages = new Object[16];
	}
	
	private static int clacDefPageShift() {
		try {
			MemoryManagerImpl.class.getClassLoader();
			Field tu = Class.forName("sun.misc.Unsafe", true, ClassLoader.getSystemClassLoader()).getDeclaredField("theUnsafe");
			tu.setAccessible(true); // NOSONAR
			Object obj = tu.get(null);
			Method pageSize = obj.getClass().getDeclaredMethod("pageSize");
			if ( pageSize.getReturnType() == Integer.TYPE ) {
				int ps = ( (Integer) pageSize.invoke(obj) ).intValue();
				int psM1 = ps - 1;
				if ( ps != 0 && ( psM1 & ps ) == 0 ) {
					return Integer.bitCount(psM1);
				}
			}
		} catch (@SuppressWarnings("unused") ClassNotFoundException | NoSuchMethodException | NoSuchFieldException | SecurityException
				| IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			System.err.println("can not get the native page size, use 1024");
		}
		return 10;
	}
	
	@Override
	public int pageShift() {
		return this.pageShift;
	}
	
	@Override
	public long allocate(long minSize, long requestedAddress, int flags) {
		if ( ( flags & ~ALL_FLAGS ) != 0 ) {
			throw new IllegalArgumentException("invalid flags value: 0x" + Integer.toHexString(flags));
		}
		if ( minSize <= 0 ) {
			throw new IllegalArgumentException("minSize is zero or negative: " + minSize);
		}
		return allocImpl(minSize, requestedAddress, flags).address;
	}
	
	private Page allocImpl(long minSize, long requestedAddress, int flags) { // NOSONAR
		long pageSize = 1L << this.pageShift;
		long maxPageIndex = pageSize - 1L;
		long allocCount = minSize >>> this.pageShift;
		if ( ( minSize & maxPageIndex ) != 0 ) {
			allocCount++;
		}
		Object[] ps = this.pages;
		int psLen = ps.length;
		if ( allocCount > psLen - ( psLen >>> 2 ) - this.allocatedPages ) {
			if ( ps.length == MAX_ARR_SIZE ) {
				System.err.println("[WARN]: well you allocated really many pages");
			} else {
				Object[] nps = new Object[psLen << 1];
				for (Object obj : ps) { // NOSONAR
					if ( obj == null ) continue;
					if ( obj instanceof Page p ) {
						put0(nps, p);
						continue;
					}
					ListEntry l = (ListEntry) obj;
					put0(nps, l.page);
					l = l.next;
					put0(nps, l.page);
					for (l = l.next; l != null; l = l.next) {
						put0(nps, l.page);
					}
				}
				this.pages = nps;
			}
		}
		ListEntry f = this.free;
		if ( f == null ) {
			f = new ListEntry();
			f.page = new Page(this.arena.allocate(pageSize, pageSize));
			this.free = f;// later cleared
		}
		loop: for (long remain = allocCount; --remain >= 0; f = f.next) {// NOSONAR
			f.page.seg.fill((byte) 0);
			if ( f.next == null && remain > 0 ) {
				while ( true ) {
					ListEntry n = new ListEntry();
					MemorySegment seg = this.arena.allocate(pageSize, pageSize);
					seg.fill((byte) 0); // the javadoc does not specify the content
					n.page = new Page(seg);
					f.next = n;
					f = n;
					if ( --remain < 0 ) {// NOSONAR // the inner loop replaces the outer loop, if the if lets it
						break loop;
					}
				}
			}
		}
		f = this.free;
		if ( ( flags & FLAG_MUST_REQUEST ) == 0 ) {
			if ( requestedAddress == 0 ) {
				requestedAddress = this.rnd.getAsLong() & ~maxPageIndex;
				if ( requestedAddress == 0 ) {
					requestedAddress = pageSize;
				}
			} else {
				if ( ( requestedAddress & maxPageIndex ) != 0L ) {
					requestedAddress = ( requestedAddress & ~maxPageIndex ) + pageSize;
					if ( requestedAddress == 0 ) {
						requestedAddress = pageSize;
					}
				}
			}
			// if you manage to allocate 2^64 - 2*pageSize this really is an endless loop
			tryRequestedAddress: while ( true ) { // NOSONAR
				if ( requestedAddress <= 0L && requestedAddress + minSize >= 0L ) {
					requestedAddress = pageSize;
				}
				final Page firstPage = f.page;
				for (long remain = allocCount, addr = requestedAddress; --remain >= 0; f = f.next, addr += pageSize) {
					Page p = f.page;
					p.address = addr;
					p.flags = flags;
					if ( !putIfAbsent(ps, p) ) {
						for (ListEntry sf = this.free; sf != f; sf = sf.next) {
							remove0(ps, sf.page);
						}
						requestedAddress = addr + pageSize;
						continue tryRequestedAddress;
					}
				}
				this.free = f;
				return firstPage;
			}
		} else if ( requestedAddress == 0 || ( requestedAddress & maxPageIndex ) != 0 ) {
			throw new IllegalArgumentException(
					"the flag FLAG_MUST_REQUEST is set but requestedAddress is invalid: requestedAddress=" + requestedAddress + " pageSize=" + pageSize);
		} else {
			flags &= ~FLAG_MUST_REQUEST;
			final Page firstPage = f.page;
			for (long remain = allocCount, addr = requestedAddress; --remain >= 0; f = f.next, addr += pageSize) {
				Page p = f.page;
				p.address = addr;
				p.flags = flags;
				if ( !putIfAbsent(ps, p) ) {
					for (ListEntry sf = this.free; sf != f; sf = sf.next) {
						remove0(ps, sf.page);
					}
					throw new IllegalStateException("the flag FLAG_MUST_REQUEST is set but requestedAddress is already used");
				}
			}
			this.free = f;
			return firstPage;
		}
	}
	
	private Page get(final long addr) {
		final Object[] ps = this.pages;
		final int len = ps.length;
		final int hash = hash(addr, len);
		Object o = ps[hash];
		if ( o == null ) {
			Page p = getNoAlloc(addr + ( 1L << this.pageShift ));
			if ( ( p.flags & FLAG_GROW_DOWN ) == 0 ) {
				throw new IllegalArgumentException(noPageMsg(addr));
			}
			return allocImpl(1L, addr, p.flags | FLAG_MUST_REQUEST);
		}
		if ( o instanceof Page p ) {
			if ( p.address == addr ) {
				return p;
			}
			p = getNoAlloc(addr + ( 1L << this.pageShift ));
			if ( ( p.flags & FLAG_GROW_DOWN ) == 0 ) {
				throw new IllegalArgumentException(noPageMsg(addr));
			}
			return allocImpl(1L, addr, p.flags | FLAG_MUST_REQUEST);
		}
		ListEntry old = (ListEntry) o;
		while ( true ) {
			if ( old.page.address == addr ) {
				return old.page;
			}
			old = old.next;
			if ( old == null ) {
				Page p = getNoAlloc(addr + ( 1L << this.pageShift ));
				if ( ( p.flags & FLAG_GROW_DOWN ) == 0 ) {
					throw new IllegalArgumentException(noPageMsg(addr));
				}
				return allocImpl(1L, addr, p.flags | FLAG_MUST_REQUEST);
			}
		}
	}
	
	private Page getNoAlloc(final long addr) {
		final Object[] ps = this.pages;
		final int len = ps.length;
		final int hash = hash(addr, len);
		Object o = ps[hash];
		if ( o == null ) {
			throw new IllegalArgumentException(noPageMsg(addr - ( 1L << this.pageShift )));
		}
		if ( o instanceof Page p ) {
			if ( p.address == addr ) {
				return p;
			}
			throw new IllegalArgumentException(noPageMsg(addr - ( 1L << this.pageShift )));
		}
		ListEntry old = (ListEntry) o;
		while ( true ) {
			if ( old.page.address == addr ) {
				return old.page;
			}
			old = old.next;
			if ( old == null ) {
				throw new IllegalArgumentException(noPageMsg(addr - ( 1L << this.pageShift )));
			}
		}
	}
	
	private Page getWWA(final long addr) {
		Page p = get(addr);
		if ( ( p.flags & FLAG_READ_ONLY ) != 0 ) {
			throw new IllegalStateException("the page 0x" + Long.toHexString(addr) + " is read only");
		}
		return p;
	}
	
	private static String noPageMsg(final long addr) {
		return "the page 0x" + Long.toHexString(addr) + " could not be found";
	}
	
	private boolean putIfAbsent(Object[] ps, Page p) {
		final long addr = p.address;
		final int hash = hash(p.address, ps.length);
		Object o = ps[hash];
		if ( o == null ) {
			ps[hash] = p;
			return true;
		}
		if ( o instanceof Page op ) {
			ListEntry no = new ListEntry();
			no.page = op;
			o = no;
		}
		ListEntry old = (ListEntry) o;
		while ( true ) {
			if ( old.page.address == addr ) {
				return false;
			}
			old = old.next;
			if ( old == null ) break;
		}
		ListEntry e = new ListEntry();
		e.page = p;
		e.next = (ListEntry) o;
		ps[hash] = e;
		return true;
	}
	
	private void put0(Object[] ps, Page p) {
		final int len = ps.length;
		final int hash = hash(p.address, len);
		Object o = ps[hash];
		if ( o == null ) {
			ps[hash] = p;
		}
		ListEntry e = new ListEntry();
		e.page = p;
		e.next = (ListEntry) o;
		ps[hash] = e;
	}
	
	private int hash(long addr, final int len) {
		return (int) ( addr >>> this.pageShift ) & ( len - 1 );
	}
	
	@Override
	public void free(long addr, long minSize) {
		final long pageSize = 1L << this.pageShift;
		if ( ( addr & ( pageSize - 1 ) ) != 0L ) {
			throw new IllegalArgumentException("the given address is not page aligned");
		}
		if ( minSize <= 0 ) {
			throw new IllegalArgumentException("minSize <= 0");
		}
		long count = count(minSize, pageSize);
		for (; --count >= 0; addr += pageSize) {
			Object[] ps = this.pages;
			final int len = ps.length;
			final int hash = hash(addr, len);
			Object o = ps[hash];
			if ( o instanceof Page p ) {
				if ( p.address == addr ) {
					ps[hash] = null;
					ListEntry e = new ListEntry();
					e.page = p;
					e.next = this.free;
					this.free = e;
				} else {
					throw new IllegalArgumentException("the address is not allocated!");
				}
			} else if ( o == null ) {
				throw new IllegalArgumentException("the address is not allocated!");
			}
			ListEntry e = (ListEntry) o;
			if ( e.page.address == addr ) {
				e = e.next;
				if ( e.next == null ) {
					ps[hash] = e.page;
				} else {
					ps[hash] = e;
				}
				
			}
			for (ListEntry n = e.next;; e = n) {
				if ( n.page.address == addr ) {
					e.next = n.next;
					n.next = this.free;
					this.free = n;
					break;
				}
			}
		}
	}
	
	private long count(long minSize, final long pageSize) {
		return ( minSize & ( pageSize - 1 ) ) != 0L ? ( minSize >> this.pageShift ) + 1 : minSize >> this.pageShift;
	}
	
	private void remove0(Object[] ps, Page p) {
		final int len = ps.length;
		final int hash = hash(p.address, len);
		Object o = ps[hash];
		if ( o instanceof Page ) {
			ps[hash] = null;
		}
		ListEntry e = (ListEntry) o;
		if ( e.page == p ) {
			e = e.next;
			if ( e.next == null ) {
				ps[hash] = e.page;
				return;
			}
			ps[hash] = e;
			return;
		}
		for (ListEntry n = e.next;; e = n) {
			if ( n.page == p ) {
				e.next = n.next;
				return;
			}
		}
	}
	
	@Override
	public int flags(long address) {
		return get(address).flags;
	}
	
	private static void set(final long pageSize, MemorySegment page, MemorySegment next, long off, int val) {
		if ( off >= pageSize ) {
			next.set(INT8, off - pageSize, (byte) val);
		} else {
			page.set(INT8, off, (byte) val);
		}
	}
	
	private static int get(final long pageSize, MemorySegment page, MemorySegment next, long off) {
		if ( off >= pageSize ) {
			return 0xFF & next.get(INT8, off - pageSize);
		}
		return 0xFF & page.get(INT8, off);
	}
	
	@Override
	public void _privilegedSetRO(long address, ByteBuffer buf) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final int remain = buf.remaining();
		final long endAddr = address + remain;
		final long endPageAddr = endAddr & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		Page page = get(pageAddr);
		assert ( page.flags & FLAG_READ_ONLY ) != 0;
		MemorySegment pageSeg = page.seg;
		if ( endPageAddr == pageAddr ) {
			MemorySegment seg = MemorySegment.ofBuffer(buf);
			int pos = buf.position();
			MemorySegment.copy(seg, INT8, pos, pageSeg, INT8, offset, remain);
			buf.position(pos + remain);
			return;
		}
		for (long a = pageAddr + pageSize; a <= endAddr; a += pageSize) {
			assert ( get(a).flags & FLAG_READ_ONLY ) != 0;
		}
		setImpl(buf, pageSize, pageSizeM1, pageAddr, remain, endPageAddr, offset, pageSeg);
	}
	
	@Override
	public void fill(long address, long len, int bVal) {
		if ( len <= 0 ) {
			throw new IllegalArgumentException("len <= 0: " + len);
		}
		assert ( bVal & 0xFF ) == bVal; // NOSONAR
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long endAddr = address + len;
		final long endPageAddr = endAddr & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = getWWA(pageAddr).seg;
		final byte val = (byte) bVal;
		if ( endPageAddr == pageAddr ) {
			if ( len == pageSize ) { // offset == 0 is implied by that together with the one page check
				page.fill(val);
			} else {
				page.asSlice(offset, len).fill(val);
			}
			return;
		}
		for (long a = pageAddr + pageSize; a <= endPageAddr; a += pageSize) {
			getWWA(a);
		}
		if ( offset == 0 ) {
			page.fill(val);
		} else {
			page.asSlice(offset, pageSize - offset).fill(val);
		}
		for (long a = pageAddr + pageSize; a < endPageAddr; a += pageSize) {
			page = getWWA(a).seg;
			page.fill(val);
		}
		page = getWWA(endPageAddr).seg;
		len = ( len - offset ) & pageSizeM1;
		if ( len == 0 ) {
			page.fill(val);
		} else {
			page.asSlice(0, len).fill(val);
		}
		
	}
	
	@Override
	public void set(long address, ByteBuffer buf) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final int remain = buf.remaining();
		final long endAddr = address + remain;
		final long endPageAddr = endAddr & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = getWWA(pageAddr).seg;
		if ( endPageAddr == pageAddr ) {
			MemorySegment seg = MemorySegment.ofBuffer(buf);
			int pos = buf.position();
			MemorySegment.copy(seg, INT8, pos, page, INT8, offset, remain);
			buf.position(pos + remain);
			return;
		}
		for (long a = pageAddr + pageSize; a <= endAddr; a += pageSize) {
			getWWA(a);
		}
		setImpl(buf, pageSize, pageSizeM1, pageAddr, remain, endPageAddr, offset, page);
	}
	
	private void setImpl(ByteBuffer buf, final long pageSize, final long pageSizeM1, final long pageAddr, final int remain, final long endPageAddr,
			final long offset, MemorySegment page) {
		MemorySegment seg = MemorySegment.ofBuffer(buf);
		int pos = buf.position();
		MemorySegment.copy(seg, INT8, pos, page, INT8, offset, pageSize);
		pos += pageSize - offset;
		for (long a = pageAddr + pageSize; a < endPageAddr; a += pageSize) {
			page = get(a).seg;
			MemorySegment.copy(seg, INT8, pos, page, INT8, 0, pageSize);
			pos += pageSize;
		}
		page = get(endPageAddr).seg;
		MemorySegment.copy(seg, INT8, pos, page, INT8, 0, ( remain + offset ) & pageSizeM1);
		pos += pageSize;
		buf.position(pos);
	}
	
	@Override
	public void set64(long address, long value) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = getWWA(pageAddr).seg;
		if ( ( offset & 7 ) == 0 ) {
			page.set(INT64, offset, value);
		} else if ( offset + 7 >= pageSize ) {
			MemorySegment next = getWWA(pageAddr + pageSize).seg;
			page.set(INT8, offset, (byte) value);
			int low = (int) value;
			int high = (int) ( value >>> 32 );
			set(pageSize, page, next, offset + 1, low >>> 8);
			set(pageSize, page, next, offset + 2, low >>> 16);
			set(pageSize, page, next, offset + 3, low >>> 24);
			set(pageSize, page, next, offset + 4, high);
			set(pageSize, page, next, offset + 5, high >>> 8);
			set(pageSize, page, next, offset + 6, high >>> 16);
			set(pageSize, page, next, offset + 7, high >>> 24);
		} else {
			page.set(MA_INT64, offset, value);
		}
	}
	
	@Override
	public void set32(long address, int value) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = getWWA(pageAddr).seg;
		if ( ( offset & 3 ) == 0 ) {
			page.set(INT32, offset, value);
		} else if ( offset + 3 >= pageSize ) {
			MemorySegment next = getWWA(pageAddr + pageSize).seg;
			page.set(INT8, offset, (byte) value);
			set(pageSize, page, next, offset + 1, value >>> 8);
			set(pageSize, page, next, offset + 2, value >>> 16);
			set(pageSize, page, next, offset + 3, value >>> 24);
		} else {
			page.set(MA_INT32, offset, value);
		}
	}
	
	@Override
	public void set16(long address, int value) {
		assert ( value & 0xFFFF ) == value : value; // NOSONAR
		final long pageSizeM1 = ( 1L << this.pageShift ) - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = getWWA(pageAddr).seg;
		if ( ( offset & 1 ) == 0 ) {
			page.set(INT16, offset, (short) value);
		} else if ( offset == pageSizeM1 ) {
			MemorySegment next = getWWA(pageAddr + ( 1L << this.pageShift )).seg;
			page.set(INT8, offset, (byte) value);
			next.set(INT8, 0, (byte) ( value >>> 8 ));
		} else {
			page.set(MA_INT16, offset, (short) value);
		}
	}
	
	@Override
	public void set8(long address, int value) {
		assert ( value & 0xFF ) == value; // NOSONAR
		final long pageSizeM1 = ( 1L << this.pageShift ) - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = getWWA(pageAddr).seg;
		page.set(INT8, offset, (byte) value);
	}
	
	@Override
	public void get(long address, ByteBuffer buf) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final int remain = buf.remaining();
		final long endAddr = address + remain;
		final long endPageAddr = endAddr & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = get(pageAddr).seg;
		if ( endPageAddr == pageAddr ) {
			MemorySegment seg = MemorySegment.ofBuffer(buf);
			int pos = buf.position();
			MemorySegment.copy(page, INT8, offset, seg, INT8, pos, remain);
			buf.position(pos + remain);
			return;
		}
		for (long a = pageAddr + pageSize; a <= endAddr; a += pageSize) {
			get(a);
		}
		MemorySegment seg = MemorySegment.ofBuffer(buf);
		int pos = buf.position();
		MemorySegment.copy(page, INT8, offset, seg, INT8, pos, pageSize);
		pos += pageSize - offset;
		for (long a = pageAddr + pageSize; a < endPageAddr; a += pageSize) {
			page = get(a).seg;
			MemorySegment.copy(seg, INT8, pos, page, INT8, 0, pageSize);
			pos += pageSize;
		}
		page = get(endPageAddr).seg;
		MemorySegment.copy(page, INT8, 0, seg, INT8, pos, ( remain + offset ) & pageSizeM1);
		pos += pageSize;
		buf.position(pos);
	}
	
	@Override
	public long get64(long address) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = get(pageAddr).seg;
		if ( ( offset & 1 ) == 0 ) {
			return page.get(INT64, offset);
		} else if ( offset + 3 >= pageSize ) {
			MemorySegment next = get(pageAddr + pageSize).seg;
			long value = 0xFFL & page.get(INT8, offset);
			value |= ( (long) get(pageSize, page, next, offset + 1) ) << 8;
			value |= ( (long) get(pageSize, page, next, offset + 2) ) << 16;
			value |= ( (long) get(pageSize, page, next, offset + 3) ) << 24;
			value |= ( (long) get(pageSize, page, next, offset + 4) ) << 32;
			value |= ( (long) get(pageSize, page, next, offset + 5) ) << 40;
			value |= ( (long) get(pageSize, page, next, offset + 6) ) << 48;
			value |= ( (long) get(pageSize, page, next, offset + 7) ) << 56;
			return value;
		} else {
			return page.get(MA_INT64, offset);
		}
	}
	
	@Override
	public int get32(long address) {
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = get(pageAddr).seg;
		if ( ( offset & 1 ) == 0 ) {
			return page.get(INT32, offset);
		} else if ( offset + 3 >= pageSize ) {
			MemorySegment next = get(pageAddr + pageSize).seg;
			int value = 0xFF & page.get(INT8, offset);
			value |= get(pageSize, page, next, offset + 1) << 8;
			value |= get(pageSize, page, next, offset + 2) << 16;
			value |= get(pageSize, page, next, offset + 3) << 24;
			return value;
		} else {
			return page.get(MA_INT32, offset);
		}
	}
	
	@Override
	public int get16(long address) {
		final long pageSizeM1 = ( 1L << this.pageShift ) - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = get(pageAddr).seg;
		if ( ( offset & 1 ) == 0 ) {
			return 0xFFFF & page.get(INT16, offset);
		} else if ( offset == pageSizeM1 ) {
			MemorySegment next = get(pageAddr + ( 1L << this.pageShift )).seg;
			int value = 0xFF & page.get(INT8, offset);
			value |= ( 0xFF & next.get(INT8, 0) ) << 8;
			return value;
		} else {
			return 0xFFFF & page.get(MA_INT16, offset);
		}
	}
	
	@Override
	public int get8(long address) {
		final long pageSizeM1 = ( 1L << this.pageShift ) - 1;
		final long pageAddr = address & ~pageSizeM1;
		final long offset = address & pageSizeM1;
		MemorySegment page = get(pageAddr).seg;
		return 0xFF & page.get(INT8, offset);
	}
	
	@Override
	public void copy(long src, long dst, long len) {
		if ( len <= 0 ) {
			if ( len == 0 ) return;
			throw new IllegalArgumentException("len < 0");
		}
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		long sPageAddr = src & ~pageSizeM1;
		long sOffset = dst & pageSizeM1;
		long dPageAddr = dst & ~pageSizeM1;
		long dOffset = dst & pageSizeM1;
		MemorySegment sSeg = get(sPageAddr).seg;
		MemorySegment dSeg = getWWA(dPageAddr).seg;
		for (long s = sPageAddr + pageSize, d = dPageAddr + pageSize; s <= src + len; s += pageSize, d += pageSize) {
			get(s);
			getWWA(d);
		}
		while ( true ) {// NOSONAR
			if ( sOffset > dOffset ) {
				long cpy = pageSize - sOffset;
				if ( cpy > len ) cpy = len;
				MemorySegment.copy(sSeg, INT8, sOffset, dSeg, INT8, dOffset, cpy);
				len -= cpy;
				if ( len == 0L ) {
					break;
				}
				sOffset = 0;
				sPageAddr += pageSize;
				dOffset += cpy;
				sSeg = get(sPageAddr).seg;
			} else if ( sOffset < dOffset ) {
				long cpy = pageSize - dOffset;
				if ( cpy > len ) cpy = len;
				MemorySegment.copy(sSeg, INT8, sOffset, dSeg, INT8, dOffset, cpy);
				len -= cpy;
				if ( len == 0L ) {
					break;
				}
				dOffset = 0;
				dPageAddr += pageSize;
				sOffset += cpy;
				dSeg = getWWA(dPageAddr).seg;
			} else {
				long cpy = pageSize - dOffset;
				if ( cpy > len ) cpy = len;
				MemorySegment.copy(sSeg, INT8, sOffset, dSeg, INT8, dOffset, cpy);
				len -= cpy;
				if ( len == 0L ) {
					break;
				}
				sOffset = 0;
				dOffset = 0;
				sPageAddr += pageSize;
				dPageAddr += pageSize;
				sSeg = get(sPageAddr).seg;
				dSeg = getWWA(dPageAddr).seg;
			}
		}
	}
	
	@Override
	public void move(long src, long dst, long len) {
		if ( len <= 0 ) {
			if ( len == 0 ) return;
			throw new IllegalArgumentException("len < 0: " + len);
		}
		long us = src ^ Long.MIN_VALUE;
		long ud = dst ^ Long.MIN_VALUE;
		if ( us < ud || ( ( dst + len ) ^ Long.MIN_VALUE ) <= us ) {
			copy(src, dst, len);
		}
		src += len;
		dst += len;
		final long pageSize = 1L << this.pageShift;
		final long pageSizeM1 = pageSize - 1;
		long sPageAddr = src & ~pageSizeM1;
		long sOffset = dst & pageSizeM1;
		long dPageAddr = dst & ~pageSizeM1;
		long dOffset = dst & pageSizeM1;
		MemorySegment sSeg = get(sPageAddr).seg;
		MemorySegment dSeg = getWWA(dPageAddr).seg;
		for (long s = sPageAddr - pageSize, d = dPageAddr - pageSize; s >= src - len; s -= pageSize, d -= pageSize) {
			get(s);
			getWWA(d);
		}
		while ( len > 0 ) {
			if ( sOffset < dOffset ) {
				long cpy = sOffset;
				if ( cpy > len ) cpy = len;
				MemorySegment.copy(sSeg, INT8, sOffset, dSeg, INT8, dOffset, cpy);
				len -= cpy;
				if ( len == 0L ) {
					break;
				}
				sOffset = 0;
				sPageAddr -= pageSize;
				sSeg = get(sPageAddr).seg;
				dOffset -= cpy;
			} else if ( sOffset > dOffset ) {
				long cpy = dOffset;
				if ( cpy > len ) cpy = len;
				MemorySegment.copy(sSeg, INT8, sOffset, dSeg, INT8, dOffset, cpy);
				len -= cpy;
				if ( len == 0L ) {
					break;
				}
				dOffset = 0;
				dPageAddr -= pageSize;
				dSeg = getWWA(dPageAddr).seg;
				sOffset -= cpy;
			} else {
				long cpy = dOffset;
				if ( cpy > len ) cpy = len;
				MemorySegment.copy(sSeg, INT8, sOffset, dSeg, INT8, dOffset, cpy);
				len -= cpy;
				if ( len == 0L ) {
					break;
				}
				sOffset = 0;
				dOffset = 0;
				sPageAddr -= pageSize;
				dPageAddr -= pageSize;
				sSeg = get(sPageAddr).seg;
				dSeg = getWWA(dPageAddr).seg;
			}
		}
	}
	
	@Override
	public void close() {
		this.arena.close();
	}
	
	static final class ListEntry {
		
		ListEntry next;
		Page      page;
		
	}
	
	static final class Page {
		
		final MemorySegment seg;
		
		long address;
		int  flags;
		
		public Page(MemorySegment seg) {
			this.seg = seg;
		}
		
	}
	
}
