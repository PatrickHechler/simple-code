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

import java.io.IOError;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.hechler.patrick.zeugs.pfs.interfaces.File;
import de.hechler.patrick.zeugs.pfs.interfaces.WriteStream;


public class CopyCompiler extends PerFileCompiler {
	
	/** {@inheritDoc} */
	@Override
	protected void compile(Path source, File target) {
		try (SeekableByteChannel read = Files.newByteChannel(source, StandardOpenOption.READ);
				WriteStream write = target.openWrite();
				Arena ses = Arena.openConfined()) {
			MemorySegment mem  = ses.allocate(1024L);
			ByteBuffer    bbuf = mem.asByteBuffer();
			while (true) {
				bbuf.position(0);
				int r = read.read(bbuf);
				if (r == -1) break;
				long w;
				if (r == mem.byteSize()) {
					w = write.write(mem);
				} else {
					w = write.write(mem.asSlice(0L, r));
				}
				while (w < r) {
					w += write.write(mem.asSlice(w, r - w));
				}
			}
		} catch (IOException e) {
			throw new IOError(e);
		}
	}
	
}
