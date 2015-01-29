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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.ExecutionResult;
import org.ng200.openolympus.cerberus.SolutionJudge;
import org.ng200.openolympus.cerberus.util.TemporaryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SandboxedExecutor extends OlrunnerExecutor implements Executor {
	public static final Path CHROOT_TEMPLATE_PATH = FileSystems.getDefault()
			.getPath("/usr/chroot");
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

	public SandboxedExecutor(final SolutionJudge holder) throws IOException {
		this.storage = new TemporaryStorage(holder);
		SandboxedExecutor.logger.info("Chroot template path: {}",
				SandboxedExecutor.CHROOT_TEMPLATE_PATH.toAbsolutePath()
						.toString());
		FileAccess.rsync(SandboxedExecutor.CHROOT_TEMPLATE_PATH.toFile(),
				this.storage.getPath().toFile());
	}

	@Override
	public void close() throws IOException {
		this.storage.close();
	}

	@Override
	public ExecutionResult execute(final File program) throws IOException {
		SandboxedExecutor.logger.info("Copying program into jail");
		final Path chrootedProgram = this.storage.getPath().resolve("chroot")
				.resolve(program.getName());
		chrootedProgram.getParent().toFile().mkdirs();
		FileAccess.copy(program, chrootedProgram,
				StandardCopyOption.COPY_ATTRIBUTES);

		final CommandLine commandLine = new CommandLine("sudo");
		commandLine.addArgument("olympus_watchdog");

		this.setUpOlrunnerLimits(commandLine);

		commandLine.addArgument(MessageFormat.format("--jail={0}", this.storage
				.getPath().resolve("chroot").toAbsolutePath().toString()));

		commandLine.addArgument("--");
		commandLine.addArgument("/"
				+ this.storage.getPath().resolve("chroot")
						.relativize(chrootedProgram).toString());

		final DefaultExecutor executor = new DefaultExecutor();

		executor.setExitValue(0);

		executor.setWatchdog(new ExecuteWatchdog(60000)); // 60 seconds for the
		// sandbox to
		// complete

		executor.setWorkingDirectory(this.storage.getDirectory());

		executor.setStreamHandler(new PumpStreamHandler(outputStream,
				errorStream, this.inputStream));

		try {
			executor.execute(commandLine);
		} catch (final IOException e) {
			if (!e.getMessage().toLowerCase().equals("stream closed")) {
				throw e;
			}
		}

		return this.readOlrunnerVerdict(new File(this.storage.getDirectory(),
				"verdict.txt"));
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
				destination, StandardCopyOption.REPLACE_EXISTING);
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
		SandboxedExecutor.logger.info("Providing file {}", file);
		FileAccess.copy(file,
				this.storage.getPath().resolve("chroot")
						.resolve(file.getName()));
	}

	@Override
	public SandboxedExecutor setCpuLimit(final long cpuLimit) {
		this.cpuLimit = cpuLimit;
		return this;
	}

	@Override
	public SandboxedExecutor setDiskLimit(final long diskLimit) {
		this.diskLimit = diskLimit;
		return this;
	}

	@Override
	public SandboxedExecutor setErrorStream(final OutputStream errorStream) {
		this.errorStream = errorStream;
		return this;
	}

	@Override
	public SandboxedExecutor setInputStream(final InputStream inputStream) {
		this.inputStream = inputStream;
		return this;
	}

	@Override
	public SandboxedExecutor setMemoryLimit(final long memoryLimit) {
		this.memoryLimit = memoryLimit;
		return this;
	}

	@Override
	public SandboxedExecutor setOutputStream(final OutputStream outputStream) {
		this.outputStream = outputStream;
		return this;
	}

	@Override
	public SandboxedExecutor setTimeLimit(final long timeLimit) {
		this.timeLimit = timeLimit;
		return this;
	}
}
