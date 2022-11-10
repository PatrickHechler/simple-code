package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.nio.file.Path;


public class TranslationUnit {
	
	public final Path source;
	public final Path target;
	
	public TranslationUnit(Path source, Path target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public final int hashCode() {
		return source.hashCode() ^ target.hashCode();
	}
	
	@Override
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (obj == this) {
			return true;
		} else if (obj.getClass() != this.getClass()) {
			return false;
		}
		TranslationUnit tu = (TranslationUnit) obj;
		return this.source.equals(tu.source) && this.target.equals(tu.target);
	}
	
}
