package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class AsmCmd extends SimpleCommand {
	
	public final List<AsmParam>  params;
	public final String          asm;
	public final List<AsmResult> results;
	
	private AsmCmd(SimpleScope parent, List<AsmParam> params, String asm, List<AsmResult> results) {
		super(parent);
		this.params = List.copyOf(params);
		this.asm = asm;
		this.results = List.copyOf(results);
	}
	
	public static AsmCmd create(SimpleScope parent, List<AsmParam> params, String asm, List<AsmResult> results,
		@SuppressWarnings( "unused" ) ErrorContext ctx) {
		return new AsmCmd(parent, params, asm, results);
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append("asm");
		for (AsmParam p : this.params) {
			append.append(" \"").append(p.target).append("\" <-- ").append(p.value);
		}
		append.append(":::").append(asm).append(">>>");
		for (AsmResult r : this.results) {
			if ( r.value == null ) append.append(' ').append(r.target).append(" <-- ?");
			else append.append(' ').append(r.target).append(" <-- \"").append(r.value).append('"');
		}
		append.append(';');
	}
	
	public record AsmParam(String target, SimpleValue value) {
		
		public static AsmParam create(String target, SimpleValue value, ErrorContext ctx) {
			value.type().checkCastable(null, ctx, false);
			return new AsmParam(target, value);
		}
		
	}
	
	public record AsmResult(SimpleValue target, String value) {
		
		public static AsmResult create(SimpleValue value, String target, ErrorContext ctx) {
			value.type().checkCastable(null, ctx, false);
			value.checkAssignable(value.type(), ctx); // the special meaning of null in only guaranteed in checkCastable
			return new AsmResult(value, target);
		}
		
	}
	
}
