package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.NativeType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class IfCmd extends SimpleCommand {
	
	public final SimpleValue   condition;
	public final SimpleCommand trueCmd;
	public final SimpleCommand falseCmd;
	
	private IfCmd(SimpleScope parent, SimpleValue condition, SimpleCommand trueCmd, SimpleCommand falseCmd) {
		super(parent);
		this.condition = condition;
		this.trueCmd = trueCmd;
		this.falseCmd = falseCmd;
	}
	
	public static IfCmd create(SimpleScope parent, SimpleValue condition, SimpleCommand trueCmd, SimpleCommand falseCmd,
		ErrorContext ctx) {
		condition.type().checkCastable(NativeType.UNUM, ctx, false);
		return new IfCmd(parent, condition, trueCmd, falseCmd);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("while (").append(this.condition).append(") ");
		trueCmd.toString(append, indent);
		if ( this.falseCmd != null ) {
			append.append('\n').append(indent);
			falseCmd.toString(append, indent);
		}
	}
	
}
