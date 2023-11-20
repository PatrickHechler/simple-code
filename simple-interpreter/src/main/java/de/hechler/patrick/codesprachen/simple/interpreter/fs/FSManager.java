package de.hechler.patrick.codesprachen.simple.interpreter.fs;

import java.io.Closeable;
import java.io.IOException;

import de.hechler.patrick.codesprachen.simple.interpreter.memory.MemoryManager;

public interface FSManager extends Closeable {
	
	static final int LOAD_FLAG_EXE          = MemoryManager.FLAG_EXECUTABLE;
	static final int LOAD_FLAG_MUST_REQUEST = MemoryManager.FLAG_MUST_REQUEST;
	static final int LOAD_FLAG_READ_ONLY    = MemoryManager.FLAG_READ_ONLY;
	static final int ALL_LOAD_FLAGS         = LOAD_FLAG_EXE | LOAD_FLAG_MUST_REQUEST | LOAD_FLAG_READ_ONLY;
	
	LoadFileResult loadFile(String path, long address, int flags) throws IOException;
	
	// the flag LOAD_FLAG_READ_ONLY is implied here
	long mapROFile(String path, long offset, long length, long address, int flags) throws IOException;
	
	void unloadMap(long address);
	
	record LoadFileResult(long address, long fileSize) {}
	
}
