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

import java.math.BigDecimal;
import java.util.Map;

public class SolutionResult {
	public static enum Result {
		OK, TIME_LIMIT, MEMORY_LIMIT, OUTPUT_LIMIT, RUNTIME_ERROR, INTERNAL_ERROR, SECURITY_VIOLATION, COMPILE_ERROR, PRESENTATION_ERROR, WRONG_ANSWER
	};

	private final Result result;

	private final long cpuTime;

	private final long realTime;

	private final long memoryPeak;

	private final long unauthorisedSyscall;

	private BigDecimal score;

	private Map<SolutionCheckingStage, String> errorMessages;

	protected SolutionResult(final long syscall,
			final Map<SolutionCheckingStage, Boolean> stages,
			final Map<SolutionCheckingStage, String> errorMessages,
			final AnswerCheckResult checkingResult) {
		this.errorMessages = errorMessages;
		this.result = Result.SECURITY_VIOLATION;
		this.cpuTime = -1;
		this.realTime = -1;
		this.memoryPeak = -1;
		this.unauthorisedSyscall = syscall;
		this.score = BigDecimal.ZERO;
	}

	protected SolutionResult(final Result result, final long cpuTime,
			final long realTime, final long memoryPeak, final BigDecimal score,
			final Map<SolutionCheckingStage, Boolean> stages,
			final Map<SolutionCheckingStage, String> errorMessages,
			final AnswerCheckResult checkingResult) {
		this.result = result;
		this.cpuTime = cpuTime;
		this.realTime = realTime;
		this.memoryPeak = memoryPeak;
		this.score = score;
		this.errorMessages = errorMessages;
		this.unauthorisedSyscall = -1;
	}

	public long getCpuTime() {
		return this.cpuTime;
	}

	public Map<SolutionCheckingStage, String> getErrorMessages() {
		return this.errorMessages;
	}

	public long getMemoryPeak() {
		return this.memoryPeak;
	}

	public long getRealTime() {
		return this.realTime;
	}

	public Result getResult() {
		return this.result;
	}

	public BigDecimal getScore() {
		return this.score;
	}

	public long getUnauthorisedSyscall() {
		return this.unauthorisedSyscall;
	}

	public void setErrorMessages(
			final Map<SolutionCheckingStage, String> errorMessages) {
		this.errorMessages = errorMessages;
	}

	public void setScore(final BigDecimal score) {
		this.score = score;
	}

	@Override
	public String toString() {
		return String
				.format("SolutionResult [result=%s, cpuTime=%s, realTime=%s, memoryPeak=%s, unauthorisedSyscall=%s]",
						this.result, this.cpuTime, this.realTime,
						this.memoryPeak, this.unauthorisedSyscall);
	}

}
