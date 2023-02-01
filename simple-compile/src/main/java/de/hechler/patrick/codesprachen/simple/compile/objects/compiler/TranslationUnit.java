package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.File;


public class TranslationUnit {
	
	public final Path source;
	public final File target;
	
	public TranslationUnit(Path source, File target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public final int hashCode() {
		return source.hashCode() * 31 + target.hashCode();
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
		return this.source.equals(tu.source) && this.target.equals((Object) tu.target);
	}
	
}
