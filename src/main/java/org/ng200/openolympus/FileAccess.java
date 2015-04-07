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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class FileAccess {

	public static <T> T actOnChildren(Path path,
			Function<Stream<Path>, T> toApply) throws IOException {
		try (Stream<Path> children = Files.list(path)) {
			return toApply.apply(children);
		}
	}

	public static Path copy(final Path source, final Path target,
			final CopyOption... copyOptions) throws IOException {
		return Files.copy(source, target, copyOptions);
	}

	public static void copyDirectory(final Path from, final Path to,
			CopyOption... copyOptions) throws IOException {
		try (Stream<Path> files = Files.walk(from)) {
			for (final Path file : files.collect(Collectors.toList())) {
				final Path target = to.resolve(from.relativize(file));
				Files.createDirectories(target.getParent());
				Files.copy(file, target, copyOptions);
			}
		}
	}

	public static void createDirectories(final Path dir,
			final FileAttribute<?>... attrs) throws IOException {
		Files.createDirectories(dir, attrs);
	}

	public static void createFile(final Path file,
			final FileAttribute<?>... attrs) throws IOException {
		Files.createFile(file, attrs);
	}

	public static Path createTempDirectory(Path dir, String key,
			final FileAttribute<?>... attrs) throws IOException {
		return Files.createTempDirectory(dir, key, attrs);
	}

	public static Path createTempDirectory(final String string,
			final FileAttribute<?>... attrs) throws IOException {
		return Files.createTempDirectory(string, attrs);
	}

	public static void delete(final Path path) throws IOException {
		Files.delete(path);
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

	public static boolean exists(Path path, LinkOption... options) {
		return Files.exists(path, options);
	}

	public static boolean isExecutable(final Path path) {
		return Files.isExecutable(path);
	}

	public static boolean isFile(Path path, LinkOption... options) {
		return Files.isRegularFile(path, options);
	}

	public static InputStream newBufferedInputStream(Path inputFile)
			throws IOException {
		return new BufferedInputStream(Files.newInputStream(inputFile));
	}

	public static OutputStream newBufferedOutputStream(Path outputFile)
			throws IOException {
		return new BufferedOutputStream(Files.newOutputStream(outputFile));
	}

	public static BufferedReader newBufferedReader(final Path path)
			throws IOException {
		return Files.newBufferedReader(path, Charset.forName("UTF-8"));
	}

	public static BufferedReader newBufferedReader(final Path path,
			final Charset charset) throws IOException {
		return Files.newBufferedReader(path, charset);
	}

	public static byte[] readAllBytes(final Path path) throws IOException {
		return FileUtils.readFileToByteArray(path.toFile());
	}

	public static String readUTF8String(Path verdictFile) throws IOException {
		return new String(Files.readAllBytes(verdictFile),
				StandardCharsets.UTF_8);
	}

	public static void rsync(final Path from, final Path to) throws IOException {
		final CommandLine commandLine = new CommandLine("/usr/bin/rsync");
		commandLine.addArgument("-r");
		commandLine.addArgument("--ignore-errors");
		commandLine.addArgument(from.toAbsolutePath().toString());
		commandLine.addArgument(to.toAbsolutePath().toString());
		final DefaultExecutor executor = new DefaultExecutor();

		executor.setWatchdog(new ExecuteWatchdog(20000)); // 20 seconds for the
		// sandbox to
		// complete
		final ByteArrayOutputStream outAndErr = new ByteArrayOutputStream();
		executor.setStreamHandler(new PumpStreamHandler(outAndErr));
		executor.setExitValues(new int[] {
			0
		});
		try {
			executor.execute(commandLine);
		} catch (final ExecuteException e) {
			throw new IOException("Rsync failed:\n" + outAndErr.toString(), e);
		}
	}

	public static void walkFileTree(final Path directory,
			final FileVisitor<Path> visitor) throws IOException {
		Files.walkFileTree(directory, visitor);
	}

	public static Stream<Path> walkPaths(final Path base) throws IOException {
		return Files.walk(base);
	}

	public static void writeUTF8StringToFile(Path file, String string,
			OpenOption... options) throws IOException {
		Files.write(file, string.getBytes(StandardCharsets.UTF_8), options);
	}

}
