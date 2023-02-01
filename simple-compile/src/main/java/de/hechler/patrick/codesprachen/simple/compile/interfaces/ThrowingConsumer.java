package de.hechler.patrick.codesprachen.simple.compile.interfaces;

public interface ThrowingConsumer<V, T extends Throwable> {
	
	void accept(V value) throws T;
	
}
