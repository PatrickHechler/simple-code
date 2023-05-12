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

import java.nio.file.Path;

import de.hechler.patrick.zeugs.pfs.interfaces.File;


public class TranslationUnit {
	
	public final Path source;
	public final File target;
	
	public TranslationUnit(Path source, File target) {
		this.source = source;
		this.target = target;
	}
	
	@Override
	public final int hashCode() {
		return source.hashCode() * 31 + target.hashCode();
	}
	
	@Override
	public final boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else if (obj == this) {
			return true;
		} else if (obj.getClass() != this.getClass()) {
			return false;
		}
		TranslationUnit tu = (TranslationUnit) obj;
		return this.source.equals(tu.source) && this.target.equals((Object) tu.target);
	}
	
}
