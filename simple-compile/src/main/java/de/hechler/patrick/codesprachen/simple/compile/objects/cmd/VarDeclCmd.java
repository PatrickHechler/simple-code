package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.VariableVal;

public class VarDeclCmd extends SimpleCommand {
	
	public final SimpleVariable sv;
	
	private VarDeclCmd(SimpleScope parent, SimpleVariable sv) {
		super(parent);
		this.sv = sv;
	}
	
	public static VarDeclCmd create(SimpleScope parent, SimpleVariable sv, ErrorContext ctx) {
		if ( ( sv.flags() & SimpleVariable.FLAG_EXPORT ) != 0 ) {
			throw new AssertionError(ctx);
		}
		return new VarDeclCmd(parent, sv);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		if ( sv.name().equals(name) ) {
			return VariableVal.create(sv, ctx);
		}
		return null;
	}
	
}
