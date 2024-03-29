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
package de.hechler.patrick.code.simple.parser.objects.simplefile;

import de.hechler.patrick.code.simple.parser.objects.simplefile.scope.SimpleScope;

public abstract class SimpleDependency implements SimpleScope, SimpleExportable<SimpleDependency> {
	
	public final String sourceFile;
	public final String binaryTarget;
	
	public SimpleDependency(String sourceFile, String binaryTarget) {// NOSONAR
		this.sourceFile = sourceFile;
		this.binaryTarget = binaryTarget;
	}
	
	@Override
	public SimpleDependency replace(SimpleDependency other) {
		if ( other.sourceFile.equals(other.sourceFile) ) {
			if ( other.binaryTarget == null ) {
				return this;
			}
			if ( this.binaryTarget == null ) {
				return other;
			}
			if ( other.binaryTarget.equals(this.binaryTarget) ) {
				return other;
			}
		}
		return null;
	}
	
}
