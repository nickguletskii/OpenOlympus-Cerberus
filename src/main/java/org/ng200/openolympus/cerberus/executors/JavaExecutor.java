/**
 * The MIT License
 * Copyright (c) 2014-2016 Nick Guletskii
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.ExecutionResult;
import org.ng200.openolympus.cerberus.ExecutionResult.ExecutionResultType;
import org.ng200.openolympus.cerberus.SecurityElevationCommandConfiguration;
import org.ng200.openolympus.cerberus.SolutionJudge;
import org.ng200.openolympus.cerberus.util.Lists;
import org.ng200.openolympus.cerberus.util.TemporaryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An executor that executes JVM executables
 * 
 * @author Nick Guletskii
 *
 */
public class JavaExecutor extends OpenOlympusWatchdogExecutor implements
		Executor {

	private transient TemporaryStorage storage;
	private long memoryLimit = 0;
	private long cpuLimit = 0;
	private long timeLimit = 0;
	private long diskLimit = 0;
	private InputStream inputStream = null;
	private OutputStream errorStream = null;
	private OutputStream outputStream = null;

	private static final Logger logger = LoggerFactory
			.getLogger(SandboxedExecutor.class);
	private List<String> writeFiles;
	private List<String> readFiles = new ArrayList<String>();

	public JavaExecutor() {
		// Serialization constructor
	}

	public TemporaryStorage getStorage() {
		return storage;
	}

	public void setStorage(TemporaryStorage storage) {
		this.storage = storage;
	}

	public List<String> getWriteFiles() {
		return writeFiles;
	}

	public void setWriteFiles(List<String> writeFiles) {
		this.writeFiles = writeFiles;
	}

	public List<String> getReadFiles() {
		return readFiles;
	}

	public void setReadFiles(List<String> readFiles) {
		this.readFiles = readFiles;
	}

	public JavaExecutor(final SolutionJudge holder,
			final List<String> writeFiles) throws IOException {
		this.writeFiles = writeFiles;
		this.storage = new TemporaryStorage(holder);
		this.storage.getPath().resolve("chroot").toFile().mkdirs();
	}

	private void buildPolicy(final Path chrootRoot, final Path policyFile)
			throws IOException {
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("grant {\n");
		stringBuilder
				.append(this.readFiles
						.stream()
						.map(f -> MessageFormat
								.format("  permission java.io.FilePermission \"{0}\", \"read\";\n",
										chrootRoot.resolve(f)))
						.collect(Collectors.joining("\n")));
		stringBuilder.append("\n");
		stringBuilder
				.append(this.writeFiles
						.stream()
						.map(f -> MessageFormat
								.format("  permission java.io.FilePermission \"{0}\", \"write\";\n",
										chrootRoot.resolve(f)))
						.collect(Collectors.joining("\n")));
		stringBuilder.append("\n};");
		final String policyString = stringBuilder.toString();

		FileAccess.writeUTF8StringToFile(policyFile, policyString);
	}

	@Override
	public void close() throws IOException {
		this.storage.close();
	}

	@Override
	public ExecutionResult execute(final Path program) throws IOException {

		final Path chrootRoot = this.storage.getPath().resolve("chroot");

		final Path chrootedProgram = chrootRoot.resolve(program.getFileName()
				.toString());

		FileAccess.createDirectories(chrootedProgram);
		FileAccess.copyDirectory(program, chrootedProgram,
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES);

		final Path outOfMemoryFile = chrootRoot.resolve("outOfMemory");

		final Path policyFile = this.storage.getPath().resolve("olymp.policy");

		try (Stream<Path> paths = FileAccess.walkPaths(storage.getPath())) {
			paths.forEach(path -> {
				try {
					Files.setPosixFilePermissions(
							path,
							new HashSet<PosixFilePermission>(Lists.from(
									PosixFilePermission.OWNER_EXECUTE,
									PosixFilePermission.OWNER_READ,
									PosixFilePermission.OWNER_WRITE,
									PosixFilePermission.GROUP_EXECUTE,
									PosixFilePermission.GROUP_READ,
									PosixFilePermission.GROUP_WRITE,
									PosixFilePermission.OTHERS_EXECUTE,
									PosixFilePermission.OTHERS_READ)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		this.buildPolicy(chrootRoot, policyFile);

		final CommandLine commandLine = new CommandLine(
				SecurityElevationCommandConfiguration
						.getPriviligeEscalationExecutableName());
		commandLine.addArgument("olympus_watchdog");

		this.setUpOlrunnerLimits(commandLine);

		commandLine.addArgument("--security=0");
		commandLine.addArgument("--jail=/");

		commandLine.addArgument("--");

		commandLine.addArgument("/usr/bin/java");

		commandLine.addArgument("-classpath");
		commandLine.addArgument(chrootedProgram.toAbsolutePath().toString());
		commandLine.addArgument("-Djava.security.manager");
		commandLine.addArgument("-Djava.security.policy="
				+ policyFile.toAbsolutePath().toString());

		commandLine.addArgument("-Xmx" + this.getMemoryLimit());
		commandLine.addArgument("-Xms" + this.getMemoryLimit());

		commandLine.addArgument(MessageFormat.format(
				"-XX:OnOutOfMemoryError=touch {0}; echo \"\" > {0}",
				outOfMemoryFile.toAbsolutePath().toString()), false);

		commandLine.addArgument("Main");

		final DefaultExecutor executor = new DefaultExecutor();

		executor.setWatchdog(new ExecuteWatchdog(20000)); // 20 seconds for the
		// sandbox to
		// complete
		executor.setWorkingDirectory(chrootRoot.toFile());

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
				.readOlrunnerVerdict(chrootRoot.resolve("verdict.txt"));

		if (FileAccess.exists(outOfMemoryFile)) {
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
	public void getFile(final String name, final Path destination)
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
	public void provideFile(final Path file) throws IOException {
		JavaExecutor.logger.info("Providing file {}", file);
		FileAccess.copy(
				file,
				this.storage.getPath().resolve("chroot")
						.resolve(file.getFileName()),
				StandardCopyOption.COPY_ATTRIBUTES);
		this.readFiles.add(file.getFileName().toString());
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
	protected void setUpOlrunnerLimits(final CommandLine commandLine)
			throws ExecuteException, IOException {

		commandLine.addArgument(MessageFormat.format("--memorylimit={0}",
				Long.toString(-1)));

		commandLine.addArgument(MessageFormat.format("--cpulimit={0}",
				Long.toString(this.getCpuLimit())));

		commandLine.addArgument(MessageFormat.format("--timelimit={0}",
				Long.toString(this.getTimeLimit())));

		commandLine.addArgument(MessageFormat.format("--disklimit={0}",
				Long.toString(this.getDiskLimit())));

		commandLine.addArgument(MessageFormat.format("--gid={0}",
				OpenOlympusWatchdogExecutor.getGroupId()));

		commandLine.addArgument(MessageFormat.format("--uid={0}",
				OpenOlympusWatchdogExecutor.getUserId()));
	}
}
