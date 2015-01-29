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
package org.ng200.openolympus.cerberus.executors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.ExecutionResult;
import org.ng200.openolympus.cerberus.ExecutionResult.ExecutionResultType;
import org.ng200.openolympus.cerberus.SolutionJudge;
import org.ng200.openolympus.cerberus.util.TemporaryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaExecutor extends OlrunnerExecutor implements Executor {

	private final TemporaryStorage storage;
	private long memoryLimit = 0;
	private long cpuLimit = 0;
	private long timeLimit = 0;
	private long diskLimit = 0;
	private InputStream inputStream = null;
	private OutputStream errorStream = null;
	private OutputStream outputStream = null;

	private static final Logger logger = LoggerFactory
			.getLogger(SandboxedExecutor.class);
	private final List<String> writeFiles;
	private final List<String> readFiles = new ArrayList<String>();

	public JavaExecutor(final SolutionJudge holder,
			final List<String> writeFiles) throws IOException {
		this.writeFiles = writeFiles;
		this.storage = new TemporaryStorage(holder);
		this.storage.getPath().resolve("chroot").toFile().mkdirs();
	}

	private void buildPolicy(final File chrootRoot, final File policyFile)
			throws IOException {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("grant {\n");
		stringBuilder
		.append(this.readFiles
				.stream()
				.map(f -> MessageFormat
						.format("  permission java.io.FilePermission \"{0}\", \"read\";\n",
								new File(chrootRoot, f)
						.getAbsolutePath()))
						.collect(Collectors.joining("\n")));
		stringBuilder.append("\n");
		stringBuilder
		.append(this.writeFiles
				.stream()
				.map(f -> MessageFormat
						.format("  permission java.io.FilePermission \"{0}\", \"write\";\n",
								new File(chrootRoot, f)
						.getAbsolutePath()))
						.collect(Collectors.joining("\n")));
		stringBuilder.append("\n};");
		final String policyString = stringBuilder.toString();

		FileUtils.writeStringToFile(policyFile, policyString);
	}

	@Override
	public void close() throws IOException {
		this.storage.close();
	}

	@Override
	public ExecutionResult execute(final File program) throws IOException {

		final File chrootRoot = new File(this.storage.getDirectory(), "chroot");

		final File chrootedProgram = new File(chrootRoot, program.getName());
		FileUtils.copyDirectory(program, chrootedProgram);
		final File outOfMemoryFile = new File(chrootRoot, "outOfMemory");

		final File policyFile = new File(this.storage.getDirectory(),
				"olymp.policy");

		this.buildPolicy(chrootRoot, policyFile);

		final CommandLine commandLine = new CommandLine("olrunner");

		this.setUpOlrunnerLimits(commandLine);

		commandLine.addArgument("--security=0");
		commandLine.addArgument("--jail=/");

		commandLine.addArgument("--");

		commandLine.addArgument("/usr/bin/java");

		commandLine.addArgument("-cp");
		commandLine.addArgument(chrootedProgram.getAbsolutePath());
		commandLine.addArgument("-Djava.security.manager");
		commandLine.addArgument("-Djava.security.policy="
				+ policyFile.getAbsolutePath());

		commandLine.addArgument("-Xmx" + this.getMemoryLimit());
		commandLine.addArgument("-Xms" + this.getMemoryLimit());

		commandLine.addArgument(MessageFormat.format(
				"-XX:OnOutOfMemoryError=touch {0}; echo \"\" > {0}",
				outOfMemoryFile.getAbsolutePath()), false);

		commandLine.addArgument("Main");

		final DefaultExecutor executor = new DefaultExecutor();

		executor.setWatchdog(new ExecuteWatchdog(20000)); // 20 seconds for the
		// sandbox to
		// complete
		executor.setWorkingDirectory(chrootRoot);

		executor.setStreamHandler(new PumpStreamHandler(this.outputStream,
				this.errorStream, this.inputStream));
		try {
			executor.execute(commandLine);
		} catch (final IOException e) {
			if (!e.getMessage().toLowerCase().equals("stream closed")) {
				throw e;
			}
		}
		final ExecutionResult readOlrunnerVerdict = this
				.readOlrunnerVerdict(new File(chrootRoot, "verdict.txt"));

		if (outOfMemoryFile.exists()) {
			readOlrunnerVerdict.setResultType(ExecutionResultType.MEMORY_LIMIT);
		}

		readOlrunnerVerdict.setMemoryPeak(this.getMemoryLimit());

		return readOlrunnerVerdict;
	}

	@Override
	public long getCpuLimit() {
		return this.cpuLimit;
	}

	@Override
	public long getDiskLimit() {
		return this.diskLimit;
	}

	@Override
	public OutputStream getErrorStream() {
		return this.errorStream;
	}

	@Override
	public void getFile(final String name, final File destination)
			throws IOException {
		FileAccess.copy(this.storage.getPath().resolve("chroot").resolve(name),
				destination, StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES);
	}

	@Override
	public InputStream getInputStream() {
		return this.inputStream;
	}

	@Override
	public long getMemoryLimit() {
		return this.memoryLimit;
	}

	@Override
	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	@Override
	public long getTimeLimit() {
		return this.timeLimit;
	}

	@Override
	public void provideFile(final File file) throws IOException {
		JavaExecutor.logger.info("Providing file {}", file);
		FileAccess.copy(file,
				this.storage.getPath().resolve("chroot")
				.resolve(file.getName()),
				StandardCopyOption.COPY_ATTRIBUTES);
		this.readFiles.add(file.getName());
	}

	@Override
	public Executor setCpuLimit(final long cpuLimit) {
		this.cpuLimit = cpuLimit;
		return this;
	}

	@Override
	public Executor setDiskLimit(final long diskLimit) {
		this.diskLimit = diskLimit;
		return this;
	}

	@Override
	public Executor setErrorStream(final OutputStream errorStream) {
		this.errorStream = errorStream;
		return this;
	}

	@Override
	public Executor setInputStream(final InputStream inputStream) {
		this.inputStream = inputStream;
		return this;
	}

	@Override
	public Executor setMemoryLimit(final long memoryLimit) {
		this.memoryLimit = memoryLimit;
		return this;
	}

	@Override
	public Executor setOutputStream(final OutputStream outputStream) {
		this.outputStream = outputStream;
		return this;
	}

	@Override
	public Executor setTimeLimit(final long timeLimit) {
		this.timeLimit = timeLimit;
		return this;
	}

	@Override
	protected void setUpOlrunnerLimits(final CommandLine commandLine) {
		commandLine.addArgument(MessageFormat.format("--memorylimit={0}",
				Long.toString(Long.MAX_VALUE)));

		commandLine.addArgument(MessageFormat.format("--cpulimit={0}",
				Long.toString(this.getCpuLimit())));

		commandLine.addArgument(MessageFormat.format("--timelimit={0}",
				Long.toString(this.getTimeLimit())));

		commandLine.addArgument(MessageFormat.format("--disklimit={0}",
				Long.toString(this.getDiskLimit())));
	}
}
