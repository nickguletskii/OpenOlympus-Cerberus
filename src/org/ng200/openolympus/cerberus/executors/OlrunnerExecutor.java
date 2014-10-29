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
package org.ng200.openolympus.cerberus.executors;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.ng200.openolympus.cerberus.ExecutionResult;

public abstract class OlrunnerExecutor implements Executor {

	public OlrunnerExecutor() {
		super();
	}

	protected ExecutionResult readOlrunnerVerdict(final File verdictFile)
			throws IOException, IllegalStateException, NumberFormatException {
		final String text = FileUtils.readFileToString(verdictFile).trim();
		
		/*
		 * Capture one of the following: $1 $1($2) $1($2,$3,$4)
		 */

		final Pattern pattern = Pattern
				.compile("([A-Za-z_]+)(?:\\((\\d+)(?:,\\s*(\\d+),\\s*(\\d+))?\\))?");

		final Matcher matcher = pattern.matcher(text);
		if (!matcher.matches()) {
			throw new IllegalStateException(
					"The verdict file doesn't contain a vaildly formatted string!");
		}
		final String resultTypeString = matcher.group(1);

		switch (resultTypeString) {
		case "OK":
		case "MEMORY_LIMIT":
		case "OUTPUT_LIMIT":
		case "TIME_LIMIT":
		case "RUNTIME_ERROR":
		case "ABNORMAL_TERMINATION":
			return new ExecutionResult(
					ExecutionResult.ExecutionResultType
							.valueOf(resultTypeString),
					Long.valueOf(matcher.group(2)), Long.valueOf(matcher
							.group(3)), Long.valueOf(matcher.group(4)), -1);
		case "SECURITY_VIOLATION":
			return new ExecutionResult(
					ExecutionResult.ExecutionResultType.SECURITY_VIOLATION, -1,
					-1, -1, Long.valueOf(matcher.group(2)));
		default:
			return new ExecutionResult(
					ExecutionResult.ExecutionResultType
							.valueOf(resultTypeString),
					-1, -1, -1, -1);
		}
	}

	protected void setUpOlrunnerLimits(final CommandLine commandLine) {
		commandLine.addArgument(MessageFormat.format("--memorylimit={0}",
				Long.toString(this.getMemoryLimit())));

		commandLine.addArgument(MessageFormat.format("--cpulimit={0}",
				Long.toString(this.getCpuLimit())));

		commandLine.addArgument(MessageFormat.format("--timelimit={0}",
				Long.toString(this.getTimeLimit())));

		commandLine.addArgument(MessageFormat.format("--disklimit={0}",
				Long.toString(this.getDiskLimit())));
	}

}