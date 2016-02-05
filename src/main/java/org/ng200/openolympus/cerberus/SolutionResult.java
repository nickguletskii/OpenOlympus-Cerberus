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
package org.ng200.openolympus.cerberus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

public class SolutionResult implements Serializable {
	public static enum Result {
		/**
		 * The solution passed the test.
		 */
		OK("solution.result.ok"),

		/**
		 * The solution has exceeded the time limit, be it CPU time or real
		 * time.
		 */
		TIME_LIMIT("solution.result.timeLimit"),

		/**
		 * The solution attempted to allocate more memory than it is allowed to.
		 */
		MEMORY_LIMIT("solution.result.memoryLimit"),

		/**
		 * The executable exceeded the limit on bytes written to disk.
		 */
		OUTPUT_LIMIT("solution.result.outputLimit"),

		/**
		 * Runtime error - the executable exited incorrectly.
		 */
		RUNTIME_ERROR("solution.result.runtimeError"),

		/**
		 * Internal error - a testing system component has failed.
		 */
		INTERNAL_ERROR("solution.result.internalError"),

		/**
		 * The executable attempted to break out of the sandbox or circumvent
		 * security measures otherwise.
		 */
		SECURITY_VIOLATION("solution.result.securityViolation"),

		/**
		 * The solution couldn't be compiled.
		 */
		COMPILE_ERROR("solution.result.compileError"),

		/**
		 * The solution's output isn't properly formatted.
		 */
		PRESENTATION_ERROR("solution.result.presentationError"),

		/**
		 * The solution generated an answer that was rejected by the judge as it
		 * is incorrect.
		 */
		WRONG_ANSWER("solution.result.wrongAnswer"),

		/**
		 * The solution is being tested.
		 */
		WAITING("solution.result.waiting");

		private String translationKey;

		private Result(final String translationKey) {
			this.translationKey = translationKey;
		}

		@Override
		public String toString() {
			return this.translationKey;
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -1021785296863034669L;;

	private Result result;

	private long cpuTime;

	private long realTime;

	private long memoryPeak;

	private long unauthorisedSyscall;

	private BigDecimal score;

	private Map<SolutionCheckingStage, String> errorMessages;

	public SolutionResult() {
		// Serialization constructor
	}

	public void setResult(Result result) {
		this.result = result;
	}

	public void setCpuTime(long cpuTime) {
		this.cpuTime = cpuTime;
	}

	public void setRealTime(long realTime) {
		this.realTime = realTime;
	}

	public void setMemoryPeak(long memoryPeak) {
		this.memoryPeak = memoryPeak;
	}

	public void setUnauthorisedSyscall(long unauthorisedSyscall) {
		this.unauthorisedSyscall = unauthorisedSyscall;
	}

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
