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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.ng200.openolympus.cerberus.exceptions.CompilerError;
import org.ng200.openolympus.cerberus.util.ExceptionalProducer;
import org.ng200.openolympus.cerberus.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolutionResultBuilder implements Serializable {
	public static SolutionResultBuilder copyOf(
			final SolutionResultBuilder builder) {
		final SolutionResultBuilder result = new SolutionResultBuilder();
		result.timeUsed = builder.timeUsed;
		result.cpuTime = builder.cpuTime;
		result.peakMemory = builder.peakMemory;
		result.syscall = builder.syscall;

		result.stages.putAll(builder.stages);
		result.errorMessages.putAll(builder.errorMessages);

		result.failed = builder.failed;
		result.resultType = builder.resultType;
		result.checkingResult = builder.checkingResult;

		return result;
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 78123193148217137L;

	private static final Logger logger = LoggerFactory
			.getLogger(SolutionResultBuilder.class);

	private long timeUsed = -1;
	private long cpuTime = -1;
	private long peakMemory = -1;
	private long syscall = -1;

	private final Map<SolutionCheckingStage, Boolean> stages = new HashMap<>();
	private final Map<SolutionCheckingStage, String> errorMessages = new HashMap<>();

	private boolean failed = false;
	private SolutionResult.Result resultType = null;
	private AnswerCheckResult checkingResult;
	private BigDecimal score = BigDecimal.ZERO;

	public SolutionResult build() {
		if (this.resultType == SolutionResult.Result.SECURITY_VIOLATION) {
			return new SolutionResult(this.syscall, this.stages,
					this.errorMessages, this.checkingResult);
		}
		return new SolutionResult(this.resultType, this.cpuTime, this.timeUsed,
				this.peakMemory, this.getScore(), this.stages,
				this.errorMessages, this.checkingResult);
	}

	public SolutionResultBuilder checkingStage(
			final ExceptionalProducer<AnswerCheckResult> lambda) {
		if (this.failed) {
			return this;
		}
		try {
			this.checkingResult = lambda.run();
			switch (this.checkingResult.getCheckingResultType()) {
			case OK:
				this.setStatus(SolutionResult.Result.OK);
				this.succeed(SolutionCheckingStage.ANSWER);
				return this;
			case PRESENTATION_ERROR:
				this.setStatus(SolutionResult.Result.PRESENTATION_ERROR);
				this.fail(SolutionCheckingStage.ANSWER,
						"#answers.presentationError");
				return this;
			case WRONG_ANSWER:
				this.setStatus(SolutionResult.Result.WRONG_ANSWER);
				this.fail(SolutionCheckingStage.ANSWER, "#answers.wrongAnswer");
				return this;
			}
		} catch (final Exception e) {
			SolutionResultBuilder.logger.error("Internal error: {}", e);
			this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
			this.fail(SolutionCheckingStage.ANSWER, Exceptions.toString(e));
			return this;
		}
		return this;
	}

	public SolutionResultBuilder compileStage(
			final ExceptionalProducer<CompilerResult> lambda) {
		if (this.failed) {
			return this;
		}
		try {
			final CompilerResult result = lambda.run();
			switch (result.getResultType()) {
			case COMPILE_ERROR:
				this.setStatus(SolutionResult.Result.COMPILE_ERROR);
				this.fail(SolutionCheckingStage.COMPILATION, result.getError()
						.getErrors());
				return this;
			case INTERNAL_ERROR:
				this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
				this.fail(SolutionCheckingStage.COMPILATION, "#errors.unknown");
				return this;
			case OK:
				this.succeed(SolutionCheckingStage.COMPILATION);
				return this;
			}
		} catch (final Exception e) {
			e.printStackTrace();
			SolutionResultBuilder.logger.error("Internal error: {}", e);
			this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
			this.fail(SolutionCheckingStage.COMPILATION, Exceptions.toString(e));
			return this;
		}
		return this;
	}

	public SolutionResultBuilder fail(
			final SolutionCheckingStage checkingStage, final String errorMessage) {
		SolutionResultBuilder.logger.error("Solution failed on stage {}: {}",
				checkingStage, errorMessage);
		this.failed = true;
		this.errorMessages.put(checkingStage, errorMessage);
		this.stages.put(checkingStage, false);
		return this;
	}

	public SolutionResultBuilder fail(
			final SolutionCheckingStage checkingStage,
			final String errorMessage, final boolean internalError) {
		if (internalError) {
			this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
		}
		return this.fail(checkingStage, errorMessage);
	}

	public BigDecimal getScore() {
		if (this.failed) {
			return BigDecimal.ZERO;
		}
		return this.score;
	}

	public SolutionResultBuilder runtimeStage(
			final ExceptionalProducer<ExecutionResult> lambda) {
		if (this.failed) {
			return this;
		}
		try {
			final ExecutionResult result = lambda.run();
			this.cpuTime = result.getCpuTime();
			this.timeUsed = result.getRealTime();
			this.peakMemory = result.getMemoryPeak();
			this.syscall = result.getSyscall();

			switch (result.getResultType()) {
			case ABNORMAL_TERMINATION:
				this.setStatus(SolutionResult.Result.RUNTIME_ERROR);
				this.fail(SolutionCheckingStage.RUNTIME,
						"#errors.abnormalTermination");
				return this;
			case INCORRECT_SECURITY_CONFIG:
				this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
				this.fail(SolutionCheckingStage.RUNTIME,
						"#errors.securityConfig");
				return this;
			case INTERNAL_ERROR:
				this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
				this.fail(SolutionCheckingStage.RUNTIME,
						"#errors.internalError", true);
				return this;
			case MEMORY_LIMIT:
				this.setStatus(SolutionResult.Result.MEMORY_LIMIT);
				this.fail(SolutionCheckingStage.RUNTIME, "#errors.memoryLimit");
				return this;
			case OK:
				this.succeed(SolutionCheckingStage.RUNTIME);
				return this;
			case OUTPUT_LIMIT:
				this.setStatus(SolutionResult.Result.OUTPUT_LIMIT);
				this.fail(SolutionCheckingStage.RUNTIME, "#errors.outputLimit");
				return this;
			case RUNTIME_ERROR:
				this.setStatus(SolutionResult.Result.RUNTIME_ERROR);
				this.fail(SolutionCheckingStage.RUNTIME, "#errors.runtimeError");
				return this;
			case SECURITY_VIOLATION:
				this.setStatus(SolutionResult.Result.SECURITY_VIOLATION);
				this.fail(SolutionCheckingStage.RUNTIME,
						"#errors.securityViolation");
				return this;
			case TIME_LIMIT:
				this.setStatus(SolutionResult.Result.TIME_LIMIT);
				this.fail(SolutionCheckingStage.RUNTIME, "#errors.timeLimit");
				return this;
			}
		} catch (final Exception e) {
			SolutionResultBuilder.logger.error("Internal error: {}", e);
			this.setStatus(SolutionResult.Result.INTERNAL_ERROR);
			this.fail(SolutionCheckingStage.RUNTIME, Exceptions.toString(e));
			return this;
		}
		return this;
	}

	public void setScore(final BigDecimal score) {
		this.score = score;
	}

	private void setStatus(final SolutionResult.Result resultType) {
		this.resultType = resultType;
	}

	public SolutionResultBuilder stage(final SolutionCheckingStage stage,
			final StageLambda lambda) {
		if (this.failed) {
			return this;
		}
		try {
			String errorMessage;
			if ((errorMessage = lambda.run()) == null) {
				this.succeed(stage);
			} else {
				this.fail(stage, errorMessage);
			}
		} catch (final CompilerError e) {
			this.fail(stage, e.getErrors());
		} catch (final Exception e) {
			SolutionResultBuilder.logger.error("Internal error: {}", e);
			this.fail(stage, Exceptions.toString(e));
			SolutionResultBuilder.logger.error(
					"Error while executing stage {}: {}", stage.name(), e);
		}
		return this;
	}

	public SolutionResultBuilder succeed(
			final SolutionCheckingStage checkingStage) {
		if (!this.stages.containsKey(checkingStage)) {
			this.stages.put(checkingStage, true);
		}
		return this;
	}

	public SolutionResultBuilder withCpuTime(final long cpuTime) {
		this.cpuTime = cpuTime;
		return this;
	}

	public SolutionResultBuilder withPeakMemory(final long peakMemory) {
		this.peakMemory = peakMemory;
		return this;
	}

	public SolutionResultBuilder withTimeUsed(final long timeUsed) {
		this.timeUsed = timeUsed;
		return this;
	}
}
