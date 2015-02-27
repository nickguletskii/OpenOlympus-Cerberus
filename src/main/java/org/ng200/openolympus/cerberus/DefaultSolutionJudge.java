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
package org.ng200.openolympus.cerberus;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.SharedTemporaryStorage;
import org.ng200.openolympus.SharedTemporaryStorageFactory;
import org.ng200.openolympus.cerberus.compilers.Compiler;
import org.ng200.openolympus.cerberus.compilers.FPCCompiler;
import org.ng200.openolympus.cerberus.compilers.GNUCompiler;
import org.ng200.openolympus.cerberus.compilers.JavaCompiler;
import org.ng200.openolympus.cerberus.exceptions.CompilationException;
import org.ng200.openolympus.cerberus.exceptions.CompilerError;
import org.ng200.openolympus.cerberus.executors.JavaExecutor;
import org.ng200.openolympus.cerberus.executors.OpenOlympusWatchdogExecutor;
import org.ng200.openolympus.cerberus.executors.SandboxedExecutor;
import org.ng200.openolympus.cerberus.util.Lists;
import org.ng200.openolympus.cerberus.util.TemporaryStorage;
import org.ng200.openolympus.cerberus.verifiers.FileExistsVerifier;
import org.ng200.openolympus.cerberus.verifiers.WhitespaceTokenizedVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSolutionJudge extends SolutionJudge {
	private static enum ProgramLanguage {
		CPP, FPC, JAVA
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -6077008331283504808L;

	private static final Logger logger = LoggerFactory
			.getLogger(DefaultSolutionJudge.class);

	private transient TemporaryStorage storage;
	private final SharedTemporaryStorage sharedStorage;
	private final String inputFileName;
	private final String outputFileName;
	private final Path program;
	private final boolean consoleInput;
	private final String charset;
	private final SolutionResultBuilder baseResultBuilder = new SolutionResultBuilder();

	private Boolean compiled = false;

	private ProgramLanguage programLanguage = null;

	public DefaultSolutionJudge(final String inputFileName,
			final String outputFileName, final boolean consoleInput,
			final String charset,
			final SharedTemporaryStorageFactory storageFactory)
					throws IOException {
		this.inputFileName = inputFileName;
		this.outputFileName = outputFileName;
		this.consoleInput = consoleInput;
		this.charset = charset;
		this.sharedStorage = storageFactory.createSharedTemporaryStorage();
		this.program = this.sharedStorage.getPath().resolve("program");
	}

	private void checkAnswer(final SolutionResultBuilder resultBuilder,
			final Path outputFile, final byte[] bytes,
			final BigDecimal maximumScore) {
		resultBuilder.checkingStage(
				() -> {
					return new WhitespaceTokenizedVerifier(outputFile)
					.isAnswerCorrect(bytes,
							Charset.forName(this.charset));
				}).checkingStage(
						() -> {
							resultBuilder.setScore(maximumScore);
							return new VerifierResult(
									AnswerCheckResult.CheckingResultType.OK,
									"Successful judgement.");
						});
	}

	private void checkAnswerFile(final SolutionResultBuilder resultBuilder,
			final BigDecimal maximumScore, final Path outputFile,
			final Path userOutputFile) throws IOException {
		this.checkAnswer(resultBuilder, outputFile,
				FileAccess.readAllBytes(userOutputFile), maximumScore);
	}

	@Override
	public void closeLocal() throws IOException {
		this.getStorage().close();
	}

	@Override
	public void closeShared() throws IOException {
		this.sharedStorage.close();
	}

	@Override
	public void compile(final List<Path> sources, Properties properties) {
		synchronized (this.compiled) {
			this.baseResultBuilder
			.compileStage(() -> {
				if (sources.size() != 1) {
					throw new IllegalArgumentException(
							"DefaultSolutionJudge only supports one source file!");
				}
				final Path sourceFile = sources.get(0);

				if (sourceFile.getFileName().toString()
						.endsWith(".cpp")) {
					final Path temporaryCopy = this.sharedStorage
							.getPath().resolve("main.cpp");

					FileAccess.copy(sources.get(0), temporaryCopy);

					this.programLanguage = ProgramLanguage.CPP;
					return this.compileCpp(temporaryCopy);
				} else if (sourceFile.getFileName().toString()
						.endsWith(".pas")) {
					final Path temporaryCopy = this.sharedStorage
							.getPath().resolve("main.pas");

					FileAccess.copy(sources.get(0), temporaryCopy);

					this.programLanguage = ProgramLanguage.FPC;
					return this.compileFpc(temporaryCopy);
				} else if (sourceFile.getFileName().toString()
						.endsWith(".java")) {
					final Path temporaryCopy = this.sharedStorage
							.getPath().resolve("Main.java");

					FileAccess.copy(sources.get(0), temporaryCopy);

					this.programLanguage = ProgramLanguage.JAVA;
					return this.compileJava(temporaryCopy);
				} else {
					return new CompilerResult(
							CompilerResult.CompileResultType.COMPILE_ERROR,
							new CompilerError("Unknown file type",
									"Please check the file type."));
				}
			});
		}
	}

	private CompilerResult compile(final Path sourceFile,
			final Compiler compiler) throws CompilationException, IOException {
		assert FileAccess.exists(sourceFile);
		CompilerResult result;
		try {
			compiler.compile(Lists.from(sourceFile), this.program,
					new HashMap<String, Object>());

			result = new CompilerResult(CompilerResult.CompileResultType.OK);

			this.compiled = true;
		} catch (final CompilerError error) {
			result = new CompilerResult(
					CompilerResult.CompileResultType.COMPILE_ERROR, error);
		}
		return result;
	}

	private CompilerResult compileCpp(final Path sourceFile)
			throws CompilationException, IOException {
		final GNUCompiler compiler = new GNUCompiler();
		compiler.addArgument("-O2");
		return this.compile(sourceFile, compiler);
	}

	private CompilerResult compileFpc(final Path sourceFile)
			throws CompilationException, IOException {
		final FPCCompiler compiler = new FPCCompiler();
		compiler.addArgument("-O2");
		return this.compile(sourceFile, compiler);
	}

	private CompilerResult compileJava(final Path sourceFile)
			throws CompilationException, IOException {
		return this.compile(sourceFile, new JavaCompiler());
	}

	private void executeWithConsoleInput(final List<Path> testFiles,
			final SolutionResultBuilder resultBuilder,
			final boolean checkAnswer, final BigDecimal maximumScore,
			final Properties properties) {

		final Path outputFile = testFiles
				.stream()
				.filter((file) -> file.getFileName().toString()
						.equals(this.outputFileName))
						.findAny()
						.orElseThrow(
								() -> new IllegalArgumentException(
										"Output file is not supplied"));

		final Path inputFile = testFiles
				.stream()
				.filter((file) -> file.getFileName().toString()
						.equals(this.inputFileName))
						.findAny()
						.orElseThrow(
								() -> new IllegalArgumentException(
										"Input file is not supplied"));

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteArrayOutputStream err = new ByteArrayOutputStream();

		try (InputStream in = FileAccess.newBufferedInputStream(inputFile);
				OpenOlympusWatchdogExecutor executor = this.getExecutor()) {
			resultBuilder.runtimeStage(() -> {
				executor.setCpuLimit(
						Long.valueOf(properties.getProperty("cpuTimeLimit")))
						.setTimeLimit(
								Long.valueOf(properties
										.getProperty("realTimeLimit")))
										.setMemoryLimit(
												Long.valueOf(properties
														.getProperty("memoryLimit")))
														.setDiskLimit(
																Long.valueOf(properties
																		.getProperty("diskLimit")));

				executor.setOutputStream(out).setErrorStream(err)
				.setInputStream(in);
				return executor.execute(this.program);
			});
			if (checkAnswer) {
				this.checkAnswer(resultBuilder, outputFile, out.toByteArray(),
						maximumScore);
			}
		} catch (final IOException e) {
			throw new RuntimeException(
					"Couldn't execute and check user's solution: ", e);
		}

	}

	private void executeWithFiles(final List<Path> testFiles,
			final SolutionResultBuilder resultBuilder,
			final boolean checkAnswer, final BigDecimal maximumScore,
			final Properties properties) {
		final Path outputFile = testFiles
				.stream()
				.filter((file) -> file.getFileName().toString()
						.equals(this.outputFileName))
						.findAny()
						.orElseThrow(
								() -> new IllegalArgumentException(
										"Output file is not supplied"));
		Path userOutputFile;
		try {
			userOutputFile = this.getStorage().getPath()
					.resolve(this.outputFileName + ".user");
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		try (OpenOlympusWatchdogExecutor executor = this.getExecutor()) {

			resultBuilder.runtimeStage(() -> {
				executor.setCpuLimit(
						Long.valueOf(properties.getProperty("cpuTimeLimit")))
						.setTimeLimit(
								Long.valueOf(properties
										.getProperty("realTimeLimit")))
										.setMemoryLimit(
												Long.valueOf(properties
														.getProperty("memoryLimit")))
														.setDiskLimit(
																Long.valueOf(properties
																		.getProperty("diskLimit")));

				executor.setOutputStream(null).setErrorStream(null)
				.setInputStream(null);

				executor.provideFile(testFiles
						.stream()
						.filter((file) -> file.getFileName().toString()
								.equals(this.inputFileName))
								.findAny()
								.orElseThrow(
										() -> new IllegalArgumentException(
												"Input file is not supplied")));
				final ExecutionResult result = executor.execute(this.program);
				return result;
			});

			resultBuilder
			.checkingStage(
					() -> FileExistsVerifier
					.noFileNotFoundException(() -> executor
							.getFile(this.outputFileName,
									userOutputFile)))
									.checkingStage(
											() -> FileExistsVerifier.fileExists(userOutputFile));
			if (checkAnswer) {
				this.checkAnswerFile(resultBuilder, maximumScore, outputFile,
						userOutputFile);
			}
		} catch (final IOException e) {
			throw new RuntimeException(
					"Couldn't execute and check user's solution: ", e);
		}
	}

	public SolutionResult getCurrentStatus() {
		return this.baseResultBuilder.build();
	}

	private OpenOlympusWatchdogExecutor getExecutor() throws IOException {
		if (this.programLanguage == null) {
			throw new IllegalStateException(
					"Unknown file type: should've failed during compilation.");
		}
		switch (this.programLanguage) {
		case CPP:
		case FPC:
			return new SandboxedExecutor(this);
		case JAVA:
			if (this.consoleInput) {
				return new JavaExecutor(this, Lists.from());
			} else {
				return new JavaExecutor(this, Lists.from(this.outputFileName));
			}
		}
		return null;
	}

	@Override
	public Collection<String> getOutputFiles() {
		return Lists.from(this.outputFileName);
	}

	private synchronized TemporaryStorage getStorage() throws IOException {
		if (this.storage == null) {
			this.storage = new TemporaryStorage(this);
		}
		return this.storage;
	}

	@Override
	public boolean isCompiled() {
		synchronized (this.compiled) {
			return this.compiled;
		}
	}

	@Override
	public SolutionResult run(final List<Path> testFiles,
			final boolean checkAnswer, final BigDecimal maximumScore,
			final Properties properties) {
		if (!this.baseResultBuilder.getShouldContinue()) {
			return this.baseResultBuilder.build();
		}

		final SolutionResultBuilder resultBuilder = SolutionResultBuilder
				.copyOf(this.baseResultBuilder);
		if (this.consoleInput) {
			this.executeWithConsoleInput(testFiles, resultBuilder, checkAnswer,
					maximumScore, properties);
		} else {
			this.executeWithFiles(testFiles, resultBuilder, checkAnswer,
					maximumScore, properties);
		}
		return resultBuilder.build();
	}

}
