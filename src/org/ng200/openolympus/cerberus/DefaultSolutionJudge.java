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
package org.ng200.openolympus.cerberus;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
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
import org.ng200.openolympus.cerberus.executors.OlrunnerExecutor;
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
	private File program;
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
		this.program = new File(this.sharedStorage.getDirectory(), "program");
	}

	private void checkAnswer(final SolutionResultBuilder resultBuilder,
			final File outputFile, final byte[] bytes,
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
			final BigDecimal maximumScore, final File outputFile,
			final File userOutputFile) throws IOException {
		this.checkAnswer(resultBuilder, outputFile,
				FileUtils.readFileToByteArray(userOutputFile), maximumScore);
	}

	@Override
	public void closeLocal() throws IOException {
		this.getStorage().close();
	}

	@Override
	public void closeShared() throws IOException {
		this.sharedStorage.close();
	}

	private CompilerResult compile(final File sourceFile,
			final Compiler compiler) throws CompilationException, IOException {
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

	@Override
	public void compile(final List<File> sources) {
		synchronized (this.compiled) {
			this.baseResultBuilder
					.compileStage(() -> {
						if (sources.size() != 1) {
							throw new IllegalArgumentException(
									"DefaultSolutionJudge only supports one source file!");
						}
						File sourceFile = sources.get(0);

						if (sourceFile.getName().endsWith(".cpp")) {
							File temporaryCopy = sharedStorage.getPath()
									.resolve("main.cpp").toFile();

							FileAccess.copy(sources.get(0), temporaryCopy);

							this.programLanguage = ProgramLanguage.CPP;
							return this.compileCpp(temporaryCopy);
						} else if (sourceFile.getName().endsWith(".pas")) {
							File temporaryCopy = sharedStorage.getPath()
									.resolve("main.pas").toFile();

							FileAccess.copy(sources.get(0), temporaryCopy);

							this.programLanguage = ProgramLanguage.FPC;
							return this.compileFpc(temporaryCopy);
						} else if (sourceFile.getName().endsWith(".java")) {
							File temporaryCopy = sharedStorage.getPath()
									.resolve("Main.java").toFile();

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

	private CompilerResult compileCpp(final File sourceFile)
			throws CompilationException, IOException {
		final GNUCompiler compiler = new GNUCompiler();
		compiler.addArgument("-O2");
		return this.compile(sourceFile, compiler);
	}

	private CompilerResult compileFpc(final File sourceFile)
			throws CompilationException, IOException {
		final FPCCompiler compiler = new FPCCompiler();
		compiler.addArgument("-O2");
		return this.compile(sourceFile, compiler);
	}

	private CompilerResult compileJava(final File sourceFile)
			throws CompilationException, IOException {
		return this.compile(sourceFile, new JavaCompiler());
	}

	private void executeWithConsoleInput(final List<File> testFiles,
			final SolutionResultBuilder resultBuilder,
			final boolean checkAnswer, final BigDecimal maximumScore,
			final Properties properties) {

		final File outputFile = testFiles
				.stream()
				.filter((file) -> file.getName().equals(this.outputFileName))
				.findAny()
				.orElseThrow(
						() -> new IllegalArgumentException(
								"Output file is not supplied"));

		final File inputFile = testFiles
				.stream()
				.filter((file) -> file.getName().equals(this.inputFileName))
				.findAny()
				.orElseThrow(
						() -> new IllegalArgumentException(
								"Input file is not supplied"));

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteArrayOutputStream err = new ByteArrayOutputStream();

		try (InputStream in = new BufferedInputStream(new FileInputStream(
				inputFile)); OlrunnerExecutor executor = this.getExecutor()) {
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

	private void executeWithFiles(final List<File> testFiles,
			final SolutionResultBuilder resultBuilder,
			final boolean checkAnswer, final BigDecimal maximumScore,
			final Properties properties) {
		final File outputFile = testFiles
				.stream()
				.filter((file) -> file.getName().equals(this.outputFileName))
				.findAny()
				.orElseThrow(
						() -> new IllegalArgumentException(
								"Output file is not supplied"));
		File userOutputFile;
		try {
			userOutputFile = this.getStorage().getPath()
					.resolve(this.outputFileName + ".user").toFile();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		try (OlrunnerExecutor executor = this.getExecutor()) {

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
						.filter((file) -> file.getName().equals(
								this.inputFileName))
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

	private OlrunnerExecutor getExecutor() throws IOException {
		if (this.programLanguage == null) {
			return new NullExecutor();
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
	public SolutionResult run(final List<File> testFiles,
			final boolean checkAnswer, final BigDecimal maximumScore,
			final Properties properties) {
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
