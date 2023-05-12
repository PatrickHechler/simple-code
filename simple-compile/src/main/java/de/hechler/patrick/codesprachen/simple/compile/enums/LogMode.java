//This file is part of the Simple Code Project
//DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//Copyright (C) 2023  Patrick Hechler
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
