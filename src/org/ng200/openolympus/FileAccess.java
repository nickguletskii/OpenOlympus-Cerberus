/**
 * The MIT License
 * Copyright (c) 2014 Nick Guletskii
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

public class FileAccess {
	public static Path copy(final File source, final File target,
			final CopyOption... copyOptions) throws IOException {
		return FileAccess.copy(source.toPath(), target.toPath(), copyOptions);
	}

	public static Path copy(final File source, final Path target,
			final CopyOption... copyOptions) throws IOException {
		return FileAccess.copy(source.toPath(), target, copyOptions);
	}

	public static Path copy(final Path source, final File target,
			final CopyOption... copyOptions) throws IOException {
		return FileAccess.copy(source, target.toPath(), copyOptions);
	}

	public static Path copy(final Path source, final Path target,
			final CopyOption... copyOptions) throws IOException {
		return Files.copy(source, target, copyOptions);
	}

	public static void copyDirectory(final File from, final File to)
			throws IOException {
		FileUtils.copyDirectory(from, to);
	}

	public static void createDirectories(final Path dir,
			final FileAttribute<?>... attrs) throws IOException {
		Files.createDirectories(dir, attrs);
	}

	public static void createFile(final Path file,
			final FileAttribute<?>... attrs) throws IOException {
		Files.createFile(file, attrs);
	}

	public static Path createTempDirectory(final String string,
			final FileAttribute<?>... attrs) throws IOException {
		return Files.createTempDirectory(string, attrs);
	}

	public static void delete(final File file) throws IOException {
		FileAccess.delete(file.toPath());
	}

	public static void delete(final Path path) throws IOException {
		Files.delete(path);
	}

	public static void deleteDirectory(final File dir) throws IOException {
		FileUtils.deleteDirectory(dir);
	}

	public static void deleteDirectoryByWalking(final File directory)
			throws IOException {
		FileAccess.deleteDirectoryByWalking(directory.toPath());
	}

	public static void deleteDirectoryByWalking(final Path path)
			throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		FileAccess.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(final Path dir,
					final IOException e) throws IOException {
				if (e == null) {
					FileAccess.delete(dir);
					return FileVisitResult.CONTINUE;
				} else {
					throw e;
				}
			}

			@Override
			public FileVisitResult visitFile(final Path file,
					final BasicFileAttributes attrs) throws IOException {
				FileAccess.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(final Path file,
					final IOException e) throws IOException {
				FileAccess.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});
		Files.deleteIfExists(path);
	}

	public static boolean isExecutable(final File file) {
		return FileAccess.isExecutable(file.toPath());
	}

	public static boolean isExecutable(final Path path) {
		return Files.isExecutable(path);
	}

	public static BufferedReader newBufferedReader(final Path path)
			throws IOException {
		return Files.newBufferedReader(path, Charset.forName("UTF-8"));
	}

	public static BufferedReader newBufferedReader(final Path path,
			final Charset charset) throws IOException {
		return Files.newBufferedReader(path, charset);
	}

	public static byte[] readAllBytes(final File file) throws IOException {
		return FileUtils.readFileToByteArray(file);
	}

	public static byte[] readAllBytes(final Path path) throws IOException {
		return FileUtils.readFileToByteArray(path.toFile());
	}

	public static void rsync(final File from, final File to) throws IOException {
		final CommandLine commandLine = new CommandLine("/usr/bin/rsync");
		commandLine.addArgument("-r");
		commandLine.addArgument("--ignore-errors");
		commandLine.addArgument(from.getAbsolutePath());
		commandLine.addArgument(to.getAbsolutePath());
		final DefaultExecutor executor = new DefaultExecutor();

		executor.setWatchdog(new ExecuteWatchdog(20000)); // 20 seconds for the
		// sandbox to
		// complete
		executor.setStreamHandler(new PumpStreamHandler(new OutputStream() {
			@Override
			public void write(final int b) throws IOException {
			}
		}));
		executor.setExitValues(new int[] {
		                                  0
		});
		try {
			executor.execute(commandLine);
		} catch (final ExecuteException e) {
			throw new IOException("Rsync failed", e);
		}
	}

	public static Stream<File> walkFiles(final File base) throws IOException {
		return Files.walk(base.toPath()).map(p -> p.toFile());
	}

	public static Stream<File> walkFiles(final Path base) throws IOException {
		return Files.walk(base).map(p -> p.toFile());
	}

	public static void walkFileTree(final Path directory,
			final FileVisitor<Path> visitor) throws IOException {
		Files.walkFileTree(directory, visitor);
	}

	public static Stream<Path> walkPaths(final File base) throws IOException {
		return Files.walk(base.toPath());
	}

	public static Stream<Path> walkPaths(final Path base) throws IOException {
		return Files.walk(base);
	}

	public static void writeString(final String str, final File descriptionFile)
			throws IOException {
		Files.write(descriptionFile.toPath(),
				str.getBytes(Charset.forName("UTF-8")));
	}
}
