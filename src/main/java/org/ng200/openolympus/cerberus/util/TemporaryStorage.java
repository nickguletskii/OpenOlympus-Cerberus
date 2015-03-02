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
package org.ng200.openolympus.cerberus.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.Janitor;
import org.ng200.openolympus.cerberus.SolutionJudge;

public class TemporaryStorage implements AutoCloseable {

	public static void cleanUp(final SolutionJudge judge) {
		TemporaryStorage.storages.computeIfAbsent(judge,
				(key) -> new ArrayList<>()).forEach(
				(storage) -> {
					try {
						storage.close();
					} catch (final Exception e) {
						throw new RuntimeException(
								"Couldn't clean up temporary storage: ", e);
					}
				});
		TemporaryStorage.storages.remove(judge);
	}

	private static void register(final SolutionJudge holder,
			final TemporaryStorage temporaryStorage) {
		TemporaryStorage.storages.computeIfAbsent(holder,
				(key) -> new ArrayList<>()).add(temporaryStorage);
	}

	static {
		Janitor.registerCleanupStep((judge) -> TemporaryStorage.cleanUp(judge));
	}

	private Path directory;

	public Path getDirectory() {
		return directory;
	}

	public void setDirectory(Path directory) {
		this.directory = directory;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public static ConcurrentMap<SolutionJudge, List<TemporaryStorage>> getStorages() {
		return storages;
	}

	public static void setStorages(
			ConcurrentMap<SolutionJudge, List<TemporaryStorage>> storages) {
		TemporaryStorage.storages = storages;
	}

	private boolean closed;

	private static final Path RAMDISK_ROOT = FileSystems.getDefault().getPath(
			"/tmp/ramdisk");

	private static ConcurrentMap<SolutionJudge, List<TemporaryStorage>> storages = new ConcurrentHashMap<>();

	public TemporaryStorage() {
		// Serialization constructor
	}

	public TemporaryStorage(final SolutionJudge holder) throws IOException {
		TemporaryStorage.register(holder, this);
		if (FileAccess.exists(TemporaryStorage.RAMDISK_ROOT)) {
			this.directory = FileAccess.createTempDirectory(
					TemporaryStorage.RAMDISK_ROOT, "cerberus");
		} else {
			this.directory = FileAccess.createTempDirectory("cerberus");
		}
	}

	private void assertNotClosed() {
		if (this.closed) {
			throw new IllegalStateException(
					"Temporary storage is already closed.");
		}
	}

	@Override
	public synchronized void close() throws IOException {
		FileAccess.deleteDirectoryByWalking(this.directory);
		this.closed = true;
	}

	public synchronized Path getPath() {
		this.assertNotClosed();
		return this.directory;
	}
}
