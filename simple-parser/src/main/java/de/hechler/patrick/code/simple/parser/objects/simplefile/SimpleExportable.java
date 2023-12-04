package de.hechler.patrick.code.simple.parser.objects.simplefile;

import de.hechler.patrick.code.simple.parser.error.CompileError;
import de.hechler.patrick.code.simple.parser.error.ErrorContext;

public interface SimpleExportable< M extends SimpleExportable<M> > {
	
	int FLAG_EXPORT         = 0x0001;
	int FLAG_FROM_ME_DEP = 0x0002;
	
	M replace(M other);
	
	static void notCompatile(String name, String type, ErrorContext ctx) {
		throw new CompileError(ctx, "there is already a something (a " + type + ") with the name " + name);
	}
	
}
