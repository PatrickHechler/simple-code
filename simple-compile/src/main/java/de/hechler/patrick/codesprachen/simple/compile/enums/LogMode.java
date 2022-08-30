package de.hechler.patrick.codesprachen.simple.compile.enums;

import de.hechler.patrick.codesprachen.simple.compile.utils.ConsoleColors;

public enum LogMode {
	
	/** same as functions */
	all,
	
	functions,
	
	files,
	
	compileSteps,
	
	finishMsg,
	
	nothing,
	
	;
	
	public static boolean enableColor = false;
	
	private static String reset() {
		return enableColor ? ConsoleColors.RESET : "";
	}
	
	public void log(LogMode need, String prefix, String name, String postfix) {
		if (ordinal() > need.ordinal()) {
			return;
		}
		String color = need.color();
		System.out.println("[" + color + need + reset() + "]: " + prefix + color + name + reset() + postfix);
	}
	
	private String color() {
		if ( !enableColor) {
			return "";
		}
		switch (this) {
		case functions:
			return ConsoleColors.YELLOW_BOLD;
		case files:
			return ConsoleColors.PURPLE_BOLD;
		case compileSteps:
			return ConsoleColors.CYAN_BOLD;
		case finishMsg:
			return ConsoleColors.GREEN_BOLD;
		case all:
		case nothing:
			throw new InternalError("this log mode does not has a color! (" + name() + ")");
		}
		throw new InternalError("unknown log mode: " + name());
	}
	
	@Override
	public String toString() {
		switch (this) {
		case all:
			return "all";
		case functions:
			return "func";
		case files:
			return "file";
		case compileSteps:
			return "compile-step";
		case finishMsg:
			return "finish";
		case nothing:
			return "nothing";
		}
		throw new InternalError("unknown log mode: " + name());
	}
	
}
