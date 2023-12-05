package de.hechler.patrick.code.simple.parser.objects.simplefile;

public interface SimpleExportable<M extends SimpleExportable<M>> {
	
	int FLAG_EXPORT      = 0x0001;
	int FLAG_FROM_ME_DEP = 0x0002;
	
	M replace(M other);
	
}
