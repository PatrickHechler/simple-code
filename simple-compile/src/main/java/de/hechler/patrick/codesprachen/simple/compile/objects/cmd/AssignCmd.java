package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.VariableVal;

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
		if ( sv.name().equals(name) ) {
			return VariableVal.create(sv, ctx);
		}
		return null;
	}
	
}
