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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.ExecutionResult;
import org.ng200.openolympus.cerberus.SecurityElevationCommandConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * An executor that executes native applications using OpenOlympus Watchdog.
 * 
 * @author Nick Guletskii
 *
 */
public abstract class OpenOlympusWatchdogExecutor implements Executor {

	private static final Logger logger = LoggerFactory
			.getLogger(OpenOlympusWatchdogExecutor.class);
	private static boolean alreadyEnsuredUserExists;

	private static String callNativeId(boolean group) throws IOException {
		OpenOlympusWatchdogExecutor.ensureUserAndGroupExists();

		final CommandLine commandLine = new CommandLine("id");
		commandLine.addArgument(group ? "-g" : "-u");
		commandLine.addArgument("olympuswatchdogchild");

		final DefaultExecutor executor = new DefaultExecutor();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		executor.setStreamHandler(new PumpStreamHandler(out));

		executor.setWatchdog(new ExecuteWatchdog(1000));

		try {
			executor.execute(commandLine);

			return out.toString(StandardCharsets.UTF_8.name());
		} catch (final ExecuteException e) {
			throw new ExecuteException(
					"Couldn't find user/group id of the olympuswatchdogchild user/group: does it even exist?",
					e.getExitValue(), e);
		}
	}

	private static void ensureUserAndGroupExists() throws IOException {
		if (alreadyEnsuredUserExists)
			return;
		final CommandLine commandLine = new CommandLine(
				SecurityElevationCommandConfiguration
						.getPriviligeEscalationExecutableName());
		commandLine.addArgument("useradd");
		commandLine.addArgument("-U");
		commandLine.addArgument("-M"); // Don't create home directory
		commandLine.addArgument("-s");
		commandLine.addArgument("/bin/false");
		commandLine.addArgument("olympuswatchdogchild");

		final DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValues(new int[] {
											0, /* Added user */
											9
				/* User already exists */
		});

		executor.setWatchdog(new ExecuteWatchdog(1000));

		try {
			executor.execute(commandLine);
			alreadyEnsuredUserExists = true;
		} catch (final ExecuteException e) {
			throw new ExecuteException(
					"Couldn't find user/group id of the olympuswatchdogchild user/group: does it even exist?",
					e.getExitValue(), e);
		}
	}

	/**
	 * @return The group id for the group to execute the executable as.
	 * @throws ExecuteException
	 * @throws IOException
	 */
	protected static String getGroupId() throws ExecuteException, IOException {
		if (OpenOlympusWatchdogExecutor.groupId == null) {
			OpenOlympusWatchdogExecutor.groupId = OpenOlympusWatchdogExecutor
					.callNativeId(true);
		}
		return OpenOlympusWatchdogExecutor.groupId;
	}

	/**
	 * @return The user id for the user to execute the executable as.
	 * @throws ExecuteException
	 * @throws IOException
	 */
	protected static String getUserId() throws ExecuteException, IOException {
		if (OpenOlympusWatchdogExecutor.userId == null) {
			OpenOlympusWatchdogExecutor.userId = OpenOlympusWatchdogExecutor
					.callNativeId(false);
		}
		return OpenOlympusWatchdogExecutor.userId;
	}

	private static String userId = null;

	private static String groupId = null;

	public OpenOlympusWatchdogExecutor() {
		super();
	}

	/**
	 * Parses the verdict file generated by OpenOlympus Watchdog.
	 * 
	 * @param verdictFile
	 *            - the verdict file to read
	 * @return The result of execution.
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws NumberFormatException
	 */
	protected ExecutionResult readOlrunnerVerdict(final Path verdictFile)
			throws IOException, IllegalStateException, NumberFormatException {
		final String text = FileAccess.readUTF8String(verdictFile).trim();

		logger.info("Watchdog verdict file contents: {}", text);

		final Pattern pattern = Pattern
				.compile("([A-Za-z_]+)\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)");

		final Matcher matcher = pattern.matcher(text);
		if (!matcher.matches()) {
			throw new IllegalStateException(
					"The verdict file doesn't contain a vaildly formatted string! Got this: \""
							+ text + "\"");
		}
		final String resultTypeString = matcher.group(1);
		final long realTime = Long.valueOf(matcher.group(2));
		final long cpuTime = Long.valueOf(matcher.group(3));
		final long memory = Long.valueOf(matcher.group(4));

		switch (resultTypeString) {
		case "OK":
		case "MEMORY_LIMIT":
		case "OUTPUT_LIMIT":
		case "TIME_LIMIT":
		case "RUNTIME_ERROR":
		case "INTERNAL_ERROR":
		case "ABNORMAL_TERMINATION":
		case "SECURITY_VIOLATION":
			return new ExecutionResult(
					ExecutionResult.ExecutionResultType
							.valueOf(resultTypeString),
					realTime, cpuTime, memory, -1);
		default:
			return new ExecutionResult(
					ExecutionResult.ExecutionResultType
							.valueOf(resultTypeString),
					-1, -1, -1, -1);
		}
	}

	/**
	 * Sets up limits for a command line
	 * 
	 * @param commandLine
	 *            to add arguments to
	 * @throws ExecuteException
	 * @throws IOException
	 */
	protected void setUpOlrunnerLimits(final CommandLine commandLine)
			throws ExecuteException, IOException {
		commandLine.addArgument(MessageFormat.format("--memorylimit={0}",
				Long.toString(this.getMemoryLimit())));

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