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
package org.ng200.openolympus.cerberus.verifiers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.AnswerCheckResult;
import org.ng200.openolympus.cerberus.VerifierResult;

public class WhitespaceTokenizedVerifier {

	private final File file;

	private final Pattern removeWhitespaceBeforeEOL = Pattern.compile("\\s+$");
	private final Pattern removeDuplicateWhitespace = Pattern.compile("(\\s)+");

	public WhitespaceTokenizedVerifier(final File file) {
		this.file = file;
	}

	public VerifierResult isAnswerCorrect(final byte[] userByteArray,
			final Charset charset) throws IOException {

		String userAnswer = new String(userByteArray, charset);
		String properAnswer = null;
		userAnswer = Stream
				.of(userAnswer.split("\n"))
				.map((line) -> this.removeWhitespaceBeforeEOL.matcher(line)
						.replaceAll(""))
						.map((line) -> this.removeDuplicateWhitespace.matcher(line)
								.replaceAll(" ")).collect(Collectors.joining("\n"));

		try (BufferedReader properAnswerReader = FileAccess.newBufferedReader(
				this.file.toPath(), charset)) {
			properAnswer = properAnswerReader
					.lines()
					.map((line) -> this.removeWhitespaceBeforeEOL.matcher(line)
							.replaceAll(""))
							.map((line) -> this.removeDuplicateWhitespace.matcher(line)
									.replaceAll(" ")).collect(Collectors.joining("\n"));
		}

		userAnswer = this.removeWhitespaceBeforeEOL.matcher(userAnswer)
				.replaceAll("");
		properAnswer = this.removeWhitespaceBeforeEOL.matcher(properAnswer)
				.replaceAll("");

		if (!userAnswer.equals(properAnswer)) {
			return new VerifierResult(
					AnswerCheckResult.CheckingResultType.WRONG_ANSWER,
					"verifier.tokens.mismatch");
		}
		return new VerifierResult(AnswerCheckResult.CheckingResultType.OK,
				"verifier.tokens.match");
	}
}
