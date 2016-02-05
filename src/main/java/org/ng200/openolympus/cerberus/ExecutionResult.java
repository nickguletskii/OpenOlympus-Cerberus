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

/**
 * 
 * This class encapsulates the result of an executable being executed in a
 * sandbox or otherwise.
 * 
 * @author Nick Guletskii
 *
 */
public class ExecutionResult {
	public static enum ExecutionResultType {
		/**
		 * OK
		 */
		OK,
		/**
		 * The executable attempted to break out of the sandbox or circumvent
		 * security measures otherwise.
		 */
		SECURITY_VIOLATION,
		/**
		 * Memory limit exceed - the executable attempted to allocate more
		 * memory than allowed.
		 */
		MEMORY_LIMIT,
		/**
		 * The executable exceeded the limit on bytes written to disk.
		 */
		OUTPUT_LIMIT,
		/**
		 * The executable was forcibly terminated because it didn't terminate in
		 * time, be it CPU time or real time.
		 */
		TIME_LIMIT,
		/**
		 * Runtime error - the executable exited incorrectly.
		 */
		RUNTIME_ERROR,
		/**
		 * Internal error - a testing system component has failed.
		 */
		INTERNAL_ERROR,
		/**
		 * The security system isn't properly configured.
		 */
		INCORRECT_SECURITY_CONFIG

	}

	private long memoryPeak;
	private ExecutionResultType resultType;
	private long realTime;
	private long cpuTime;
	private long syscall;

	public ExecutionResult() {
		// Serialization constructor
	}

	public ExecutionResult(final ExecutionResultType resultType,
			final long realTime, final long cpuTime, final long memoryPeak,
			final long syscall) {
		this.resultType = resultType;
		this.realTime = realTime;
		this.cpuTime = cpuTime;
		this.memoryPeak = memoryPeak;
		this.syscall = syscall;
	}

	public long getCpuTime() {
		return this.cpuTime;
	}

	public long getMemoryPeak() {
		return this.memoryPeak;
	}

	public long getRealTime() {
		return this.realTime;
	}

	public ExecutionResultType getResultType() {
		return this.resultType;
	}

	public long getSyscall() {
		return this.syscall;
	}

	public void setCpuTime(final long cpuTime) {
		this.cpuTime = cpuTime;
	}

	public void setMemoryPeak(final long memoryPeak) {
		this.memoryPeak = memoryPeak;
	}

	public void setRealTime(final long realTime) {
		this.realTime = realTime;
	}

	public void setResultType(final ExecutionResultType resultType) {
		this.resultType = resultType;
	}

	public void setSyscall(final long syscall) {
		this.syscall = syscall;
	}

}
