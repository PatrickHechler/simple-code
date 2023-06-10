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
package de.hechler.patrick.codesprachen.simple.compile.objects.compiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import de.hechler.patrick.codesprachen.simple.compile.interfaces.SCompiler;
import de.hechler.patrick.zeugs.pfs.interfaces.File;

@SuppressWarnings("javadoc")
public abstract class MultiCompiler implements SCompiler {

	private final Set<SCompiler> comps = new HashSet<>();

	public void addTranslationUnit(Path source, File target) throws IOException {
		SCompiler c = findCompiler(source);
		c.addTranslationUnit(source, target);
		comps.add(c);
	}

	public void compile() throws IOException {
		for (SCompiler c : comps) {
			c.compile();
		}
	}

	protected abstract SCompiler findCompiler(Path source) throws IOException;

}
