package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class WhileCmd extends SimpleCommand {
	
	public final SimpleValue condition;
	public final SimpleCommand loop;
	
	private WhileCmd(SimpleScope parent, SimpleValue condition, SimpleCommand loop) {
		super(parent);
		this.condition = condition;
		this.loop = loop;
	}
	
	public static WhileCmd create(SimpleScope parent, SimpleValue condition, SimpleCommand loop, ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx, false);
		return new WhileCmd(parent, condition, loop);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("while (").append(this.condition).append(") ");
		loop.toString(append, indent);
	}
	
}
