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
package de.hechler.patrick.codesprachen.simple.compile.utils;

/**
 * Here are a list of colors in a Java class with public static fields
 * 
 * <p>
 * 
 * <h1>Usage</h1>
 * 
 * System.out.println(ConsoleColors.RED + "RED COLORED" + ConsoleColors.RESET + " NORMAL");
 * 
 * <p>
 * 
 * <h1>Note</h1>
 * 
 * Don't forget to use the RESET after printing as the effect will remain if it's not cleared
 */
public class ConsoleColors {
	
	// Reset
	public static final String RESET = "\033[0m"; // Text Reset
	
	// Regular Colors
	public static final String BLACK  = "\033[0;30m"; // BLACK
	public static final String RED    = "\033[0;31m"; // RED
	public static final String GREEN  = "\033[0;32m"; // GREEN
	public static final String YELLOW = "\033[0;33m"; // YELLOW
	public static final String BLUE   = "\033[0;34m"; // BLUE
	public static final String PURPLE = "\033[0;35m"; // PURPLE
	public static final String CYAN   = "\033[0;36m"; // CYAN
	public static final String WHITE  = "\033[0;37m"; // WHITE
	
	// Bold
	public static final String BLACK_BOLD  = "\033[1;30m"; // BLACK
	public static final String RED_BOLD    = "\033[1;31m"; // RED
	public static final String GREEN_BOLD  = "\033[1;32m"; // GREEN
	public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
	public static final String BLUE_BOLD   = "\033[1;34m"; // BLUE
	public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
	public static final String CYAN_BOLD   = "\033[1;36m"; // CYAN
	public static final String WHITE_BOLD  = "\033[1;37m"; // WHITE
	
	// Underline
	public static final String BLACK_UNDERLINED  = "\033[4;30m"; // BLACK
	public static final String RED_UNDERLINED    = "\033[4;31m"; // RED
	public static final String GREEN_UNDERLINED  = "\033[4;32m"; // GREEN
	public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
	public static final String BLUE_UNDERLINED   = "\033[4;34m"; // BLUE
	public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
	public static final String CYAN_UNDERLINED   = "\033[4;36m"; // CYAN
	public static final String WHITE_UNDERLINED  = "\033[4;37m"; // WHITE
	
	// Background
	public static final String BLACK_BACKGROUND  = "\033[40m"; // BLACK
	public static final String RED_BACKGROUND    = "\033[41m"; // RED
	public static final String GREEN_BACKGROUND  = "\033[42m"; // GREEN
	public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
	public static final String BLUE_BACKGROUND   = "\033[44m"; // BLUE
	public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
	public static final String CYAN_BACKGROUND   = "\033[46m"; // CYAN
	public static final String WHITE_BACKGROUND  = "\033[47m"; // WHITE
	
	// High Intensity
	public static final String BLACK_BRIGHT  = "\033[0;90m"; // BLACK
	public static final String RED_BRIGHT    = "\033[0;91m"; // RED
	public static final String GREEN_BRIGHT  = "\033[0;92m"; // GREEN
	public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
	public static final String BLUE_BRIGHT   = "\033[0;94m"; // BLUE
	public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
	public static final String CYAN_BRIGHT   = "\033[0;96m"; // CYAN
	public static final String WHITE_BRIGHT  = "\033[0;97m"; // WHITE
	
	// Bold High Intensity
	public static final String BLACK_BOLD_BRIGHT  = "\033[1;90m"; // BLACK
	public static final String RED_BOLD_BRIGHT    = "\033[1;91m"; // RED
	public static final String GREEN_BOLD_BRIGHT  = "\033[1;92m"; // GREEN
	public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m"; // YELLOW
	public static final String BLUE_BOLD_BRIGHT   = "\033[1;94m"; // BLUE
	public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m"; // PURPLE
	public static final String CYAN_BOLD_BRIGHT   = "\033[1;96m"; // CYAN
	public static final String WHITE_BOLD_BRIGHT  = "\033[1;97m"; // WHITE
	
	// High Intensity backgrounds
	public static final String BLACK_BACKGROUND_BRIGHT  = "\033[0;100m"; // BLACK
	public static final String RED_BACKGROUND_BRIGHT    = "\033[0;101m"; // RED
	public static final String GREEN_BACKGROUND_BRIGHT  = "\033[0;102m"; // GREEN
	public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m"; // YELLOW
	public static final String BLUE_BACKGROUND_BRIGHT   = "\033[0;104m"; // BLUE
	public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
	public static final String CYAN_BACKGROUND_BRIGHT   = "\033[0;106m"; // CYAN
	public static final String WHITE_BACKGROUND_BRIGHT  = "\033[0;107m"; // WHITE
	
}
