package de.hechler.patrick.code.simple.interpreter.memory;

import java.nio.ByteBuffer;

public interface MemoryManager extends AutoCloseable {
	
	/**
	 * mark the allocated pages as executable
	 */
	static final int FLAG_EXECUTABLE   = 0x00000001;
	/**
	 * let the allocated pages grow downwards if the page below was accessed and accessed page is not yet allocated free
	 */
	static final int FLAG_GROW_DOWN    = 0x00000002;
	static final int FLAG_READ_ONLY    = 0x00000004;
	static final int FLAG_CUSTOM       = 0x00000008;
	/**
	 * the {@code requestedAddress} must be used, if this is not possible the allocation fails
	 * <p>
	 * note that the allocation can not success if {@code requestedAddress} is not set (set to {@code 0}) or if
	 * {@code requestedAddress} is not aligned to the {@link #pageSize() page-size}
	 */
	static final int FLAG_MUST_REQUEST = 0x00000010;
	static final int ALL_FLAGS         = FLAG_EXECUTABLE | FLAG_GROW_DOWN | FLAG_MUST_REQUEST | FLAG_READ_ONLY;
	
	// information about MemoryManger
	
	int pageShift();
	
	default long pageSize() {
		return 1L << pageShift();
	}
	
	// allocate/free
	
	long allocate(long minSize, long requestedAddress, int flags);
	
	void free(long page, long minSize);
	
	// information about page
	
	int flags(long address);
	
	// set
	
	void _privilegedSetRO(long address, ByteBuffer buf);
	
	void set(long address, ByteBuffer buf);
	
	void set64(long address, long value);
	
	void set32(long address, int value);
	
	void set16(long address, int value);
	
	void set8(long address, int value);
	
	// get
	
	void get(long address, ByteBuffer buf);
	
	long get64(long address);
	
	int get32(long address);
	
	int get16(long address);
	
	int get8(long address);
	
	// copy
	
	void copy(long from, long to, long len);
	
	void move(long from, long to, long len);
	
	void fill(long from, long len, int bVal);
	
}
