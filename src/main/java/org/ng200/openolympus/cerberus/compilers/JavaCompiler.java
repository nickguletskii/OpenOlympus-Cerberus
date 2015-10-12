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
package org.ng200.openolympus.cerberus.compilers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.exceptions.CompilationException;
import org.ng200.openolympus.cerberus.exceptions.CompilerError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * An implementation of a {@link Compiler} which calls the Java compiler.
 * 
 * @author Nick Guletskii
 *
 */
public class JavaCompiler implements Compiler {
	private static final Logger logger = LoggerFactory
			.getLogger(JavaCompiler.class);
	private List<String> arguments = new ArrayList<>();

	public JavaCompiler() {
	}

	@Override
	public void addArgument(final String argument) {
		this.arguments.add(argument);
	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	/* (non-Javadoc)
	 * @see org.ng200.openolympus.cerberus.compilers.Compiler#compile(java.util.List, java.nio.file.Path, java.util.Map)
	 */
	@Override
	public void compile(final List<Path> inputFiles, final Path outputFile,
			final Map<String, Object> additionalParameters)
					throws CompilationException, IOException {

		FileAccess.createDirectories(outputFile);

		final CommandLine commandLine = new CommandLine("javac");
		commandLine.setSubstitutionMap(additionalParameters);

		this.arguments.forEach((arg) -> commandLine.addArgument(arg));

		commandLine.addArgument("-d");
		commandLine.addArgument(outputFile.toAbsolutePath().toString());

		commandLine.addArgument("-nowarn"); // Prohibit warnings because they
		// screw
		// up error detection

		inputFiles.forEach((file) -> commandLine.addArguments(MessageFormat
				.format("\"{0}\"", file.toAbsolutePath().toString()))); // Add
		// input
		// files

		JavaCompiler.logger.info("Running javac with arguments: {}",
				Arrays.asList(commandLine.getArguments()));

		final DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValues(new int[] {
											0,
											1
		});

		final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
		executor.setStreamHandler(
				new PumpStreamHandler(null, errorStream, null));

		executor.setWatchdog(new ExecuteWatchdog(20000));// 20 seconds to
		// compile
		int result;
		try {
			result = executor.execute(commandLine);
		} catch (final IOException e) {
			JavaCompiler.logger.error("Could not execute javac: {}", e);
			throw new CompilationException("Could not execute javac", e);
		}

		switch (result) {
		case 0:
			return;
		case 1:
			try {
				String errorString = errorStream.toString("UTF-8");
				final Pattern pattern = Pattern.compile(
						"^("
								+ inputFiles
										.stream()
										.map(file -> Pattern.quote(file
												.toAbsolutePath().toString()))
										.collect(Collectors.joining("|"))
								+ "):",
						Pattern.MULTILINE);
				errorString = pattern.matcher(errorString).replaceAll("");

				JavaCompiler.logger.debug("Compilation error: {}", errorString);

				throw new CompilerError("javac.wrote.stderr", errorString);
			} catch (final UnsupportedEncodingException e) {
				throw new CompilationException(
						"Unsupported encoding! The compiler should output UTF-8!",
						e);
			}
		}
	}
}
