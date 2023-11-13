package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public abstract class SimpleCommand implements SimpleScope {
	
	protected final SimpleScope parent;
	
	public SimpleCommand(SimpleScope parent) {
		this.parent = parent;
	}
	
	public abstract SimpleValue directNameValueOrNull(String name, ErrorContext ctx);
	
	@Override
	public SimpleValue nameValueOrNull(String name, ErrorContext ctx) {
		SimpleValue value = directNameValueOrNull(name, ctx);
		if ( value != null ) return value;
		return parent.nameValueOrNull(name, ctx);
	}
	
	@Override
	public Object nameTypeOrDepOrFuncOrNull(String typedefName, ErrorContext ctx) {
		return parent.nameTypeOrDepOrFuncOrNull(typedefName, ctx);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, new StringBuilder());
		return sb.toString();
	}
	
	public abstract void toString(StringBuilder append, StringBuilder indent);
	
}
