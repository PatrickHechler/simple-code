package de.hechler.patrick.codesprachen.simple.compile.objects.antl;

import java.util.Collections;
import java.util.List;

import de.hechler.patrick.codesprachen.simple.compile.objects.antl.commands.SimpleCommandBlock;

public class SimpleFunction {
	
	public final boolean               export;
	public final boolean               main;
	public final String                name;
	public final List <SimpleVariable> arguments;
	public final List <SimpleVariable> results;
	public final SimpleCommandBlock    body;
	
	public SimpleFunction(boolean export, boolean main, String name, List <SimpleVariable> args, List <SimpleVariable> results, SimpleCommandBlock cmd) {
		this.export = export;
		this.main = main;
		this.name = name;
		this.arguments = Collections.unmodifiableList(args);
		this.results = Collections.unmodifiableList(results == null ? Collections.emptyList() : results);
		this.body = cmd;
	}
	
}
