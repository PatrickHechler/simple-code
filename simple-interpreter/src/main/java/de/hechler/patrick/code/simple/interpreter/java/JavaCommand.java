package de.hechler.patrick.code.simple.interpreter.java;

import de.hechler.patrick.code.simple.parser.error.ErrorContext;
import de.hechler.patrick.code.simple.parser.objects.cmd.SimpleCommand;
import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.code.simple.parser.objects.value.SimpleValue;

public class JavaCommand extends SimpleCommand {
	
	public final JavaFunction func;
	
	public JavaCommand(SimpleScope parent, JavaFunction func) {
		super(parent);
		this.func = func;
	}
	
	@Override
	@SuppressWarnings("unused")
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, @SuppressWarnings("unused") StringBuilder indent) {
		append.append("{java code block: " + this.func + "}");
	}
	
}
