package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class AssignCmd extends SimpleCommand {
	
	public final SimpleValue target;
	public final SimpleValue value;
	
	private AssignCmd(SimpleScope parent, SimpleValue target, SimpleValue value) {
		super(parent);
		this.target = target;
		this.value = value;
	}
	
	public static AssignCmd create(SimpleScope parent, SimpleValue target, SimpleValue value, ErrorContext ctx) {
		target.checkAssignable(value.type(), ctx);
		return new AssignCmd(parent, target, value);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append(this.target).append(" <-- ").append(this.value).append(';');
	}
	
}
