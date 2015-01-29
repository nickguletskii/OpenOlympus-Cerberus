/**
 * The MIT License
 * Copyright (c) 2014-2015 Nick Guletskii
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.ng200.openolympus;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

public class SharedTemporaryStorage implements AutoCloseable, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -1569837982555606657L;
	private final File directory;

	public SharedTemporaryStorage(final Path storagePath) throws IOException {
		final Path temporaryStoragePath = storagePath.resolve("tmp");
		FileAccess.createDirectories(temporaryStoragePath);
		this.directory = Files.createTempDirectory(temporaryStoragePath,
				"cerberus").toFile();
		FileAccess.createDirectories(directory.toPath());
		if (!directory.exists())
			throw new IOException("Couldn't create temporary shared directory!");
	}

	@Override
	public void close() throws IOException {
		FileAccess.deleteDirectoryByWalking(this.directory);
	}

	public File getDirectory() {
		return this.directory;
	}

	public Path getPath() {
		return this.directory.toPath();
	}
}
