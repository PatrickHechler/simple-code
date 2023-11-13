package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class StructFuncCallCmd extends SimpleCommand {
	
	public final SimpleValue func;
	public final SimpleValue fstruct;
	
	private StructFuncCallCmd(SimpleScope parent, SimpleValue func, SimpleValue fstruct) {
		super(parent);
		this.func = func;
		this.fstruct = fstruct;
	}
	
	public static StructFuncCallCmd create(SimpleScope parent, SimpleValue func, SimpleValue fstruct,
		ErrorContext ctx) {
		if ( !( func.type() instanceof FuncType ft ) || ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new CompileError(ctx, "I need something of type function address to call it!");
		}
		if ( !ft.isInvokableBy(fstruct.type()) ) {
			throw new CompileError(ctx,
				"the type of the function structure does not match the type of the function: function type: " + ft
					+ " function structure type: " + fstruct.type());
		}
		return new StructFuncCallCmd(parent, func, fstruct);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("call ").append(this.func).append(' ').append(this.fstruct).append(';');
	}
	
}
