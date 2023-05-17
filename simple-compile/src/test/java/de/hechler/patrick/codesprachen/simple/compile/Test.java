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
package de.hechler.patrick.codesprachen.simple.compile;

import de.hechler.patrick.codesprachen.simple.compile.objects.compiler.SimpleCompiler;
import de.hechler.patrick.zeugs.check.objects.BigCheckResult;
import de.hechler.patrick.zeugs.check.objects.BigChecker;

@SuppressWarnings("javadoc")
public class Test {
	
	public void testname() throws Exception {
		main(new String[0]);
	}
	
	public static void main(String[] args) {
		System.out.println(System.getProperty("java.version"));
		SimpleCompiler.nop();
		BigCheckResult check = BigChecker.tryCheckAll(true, Test.class.getPackage(), Test.class.getClassLoader());
		check.print();
		if (check.wentUnexpected()) {
			System.out.println("unexpected result, detailed print below:");
			check.detailedPrint(System.out);
			throw new Error(check.toString());
		}
		System.out.println("finish: successful");
	}
	
}
