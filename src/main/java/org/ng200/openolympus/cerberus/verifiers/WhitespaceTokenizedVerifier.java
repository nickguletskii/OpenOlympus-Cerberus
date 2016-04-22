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
package org.ng200.openolympus.cerberus.verifiers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.AnswerCheckResult;
import org.ng200.openolympus.cerberus.VerifierResult;

import com.google.common.collect.Iterators;

/**
 * @author Nick Guletskii
 *
 */
public class WhitespaceTokenizedVerifier {

	private Path file;

	private final Pattern removeWhitespaceBeforeEOL = Pattern.compile("\\s+$");
	private final Pattern removeDuplicateWhitespace = Pattern
			.compile("(\\s)\\1");

	private boolean deduplicateWhitespace;

	private boolean stripWhitespaceBeforeEOL;

	public WhitespaceTokenizedVerifier() {
		// Serialization constructor
	}

	public Path getFile() {
		return file;
	}

	public void setFile(Path file) {
		this.file = file;
	}

	public boolean isDeduplicateWhitespace() {
		return deduplicateWhitespace;
	}

	public void setDeduplicateWhitespace(boolean deduplicateWhitespace) {
		this.deduplicateWhitespace = deduplicateWhitespace;
	}

	public boolean isStripWhitespaceBeforeEOL() {
		return stripWhitespaceBeforeEOL;
	}

	public void setStripWhitespaceBeforeEOL(boolean stripWhitespaceBeforeEOL) {
		this.stripWhitespaceBeforeEOL = stripWhitespaceBeforeEOL;
	}

	public WhitespaceTokenizedVerifier(final Path file,
			boolean deduplicateWhitespace, boolean stripWhitespaceBeforeEOL) {
		this.file = file;
		this.deduplicateWhitespace = deduplicateWhitespace;
		this.stripWhitespaceBeforeEOL = stripWhitespaceBeforeEOL;
	}

	public Stream<String> applyTransformations(Stream<String> str) {
		if (this.stripWhitespaceBeforeEOL)
			str = str.map((line) -> this.removeWhitespaceBeforeEOL.matcher(line)
					.replaceAll(""));
		if (this.deduplicateWhitespace)
			str = str.map((line) -> this.removeDuplicateWhitespace.matcher(line)
					.replaceAll(" "));
		return str;
	}

	public VerifierResult isAnswerCorrect(final BufferedReader bufferedReader,
			final Charset charset) throws IOException {

		Stream<String> properAnswer = null;
		Stream<String> userAnswer = applyTransformations(
				bufferedReader.lines());

		try (BufferedReader properAnswerReader = FileAccess.newBufferedReader(
				this.file, charset)) {
			properAnswer = applyTransformations(properAnswerReader.lines());

			if (!Iterators.elementsEqual(userAnswer.iterator(),
					properAnswer.iterator())) {
				return new VerifierResult(
						AnswerCheckResult.CheckingResultType.WRONG_ANSWER,
						"verifier.tokens.mismatch");
			}
		}
		return new VerifierResult(AnswerCheckResult.CheckingResultType.OK,
				"verifier.tokens.match");
	}
}
