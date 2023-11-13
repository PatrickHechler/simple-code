package de.hechler.patrick.codesprachen.simple.compile.objects.cmd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.error.CompileError;
import de.hechler.patrick.codesprachen.simple.compile.error.ErrorContext;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.SimpleVariable;
import de.hechler.patrick.codesprachen.simple.compile.objects.simplefile.scope.SimpleScope;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.FuncType;
import de.hechler.patrick.codesprachen.simple.compile.objects.types.SimpleType;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.CastVal;
import de.hechler.patrick.codesprachen.simple.compile.objects.value.SimpleValue;

public class FuncCallCmd extends SimpleCommand {
	
	public final SimpleValue       func;
	public final List<SimpleValue> results;
	public final List<SimpleValue> arguments;
	
	private FuncCallCmd(SimpleScope parent, SimpleValue func, List<SimpleValue> results, List<SimpleValue> arguments) {
		super(parent);
		this.func = func;
		this.results = Collections.unmodifiableList(results);
		this.arguments = Collections.unmodifiableList(arguments);
	}
	
	public static FuncCallCmd create(SimpleScope parent, SimpleValue func, List<SimpleValue> results,
		List<SimpleValue> arguments, ErrorContext ctx) {
		SimpleType type = func.type();
		if ( !( type instanceof FuncType ft ) || ( ft.flags() & FuncType.FLAG_FUNC_ADDRESS ) == 0 ) {
			throw new CompileError(ctx, "I need something of type function address to call it!");
		}
		List<SimpleValue> res = convert(results, ft.resMembers(), ctx);
		List<SimpleValue> args = convert(arguments, ft.argMembers(), ctx);
		return new FuncCallCmd(parent, func, res, args);
	}
	
	private static List<SimpleValue> convert(List<SimpleValue> results, List<SimpleVariable> resMembers,
		ErrorContext ctx) {
		List<SimpleValue> res = new ArrayList<>(resMembers.size());
		if ( resMembers.size() != results.size() ) {
			throw new CompileError(ctx, "wrong number of function result assignments!");
		}
		for (int i = 0; i < resMembers.size(); i++) {
			SimpleType targetType = resMembers.get(i).type();
			results.get(i).type().checkCastable(targetType, ctx, false);
			res.add(CastVal.create(results.get(i), targetType, ctx));
		}
		return res;
	}
	
	@Override
	public SimpleValue directNameValueOrNull(String name, ErrorContext ctx) {
		return null;
	}
	
	@Override
	public void toString(StringBuilder append, StringBuilder indent) {
		append.append(this.func).append(" <");
		append(append, this.results);
		append.append("> <-- (");
		append(append, this.results);
		append.append(");");
	}
	
	private static void append(StringBuilder append, List<SimpleValue> list) {
		if ( list.isEmpty() ) return;
		append.append(list.get(0));
		for (int i = 0, s = list.size(); i < s; i++) {
			append.append(", ").append(list.get(i));
		}
	}
	
}
