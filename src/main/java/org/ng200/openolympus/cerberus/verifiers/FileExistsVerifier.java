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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.cerberus.AnswerCheckResult;
import org.ng200.openolympus.cerberus.VerifierResult;
import org.ng200.openolympus.cerberus.util.ExceptionalRunnable;

/**
 * 
 * Verifies that the file exists.
 * 
 * @author Nick Guletskii
 *
 */
public class FileExistsVerifier {

	/**
	 * Checks if the file exists.
	 * 
	 * @param userOutputFile
	 * @return
	 */
	public static AnswerCheckResult fileExists(final Path userOutputFile) {
		if (FileAccess.exists(userOutputFile)
				&& FileAccess.isFile(userOutputFile)) {
			return new VerifierResult(AnswerCheckResult.CheckingResultType.OK,
					"#verifier.file.exists");
		}
		return new VerifierResult(
				AnswerCheckResult.CheckingResultType.PRESENTATION_ERROR,
				"#verifier.file.doesntexist");
	}

	/**
	 * Runs the runnable and checks that it didn't throw a
	 * {@link NoSuchFileException} or a {@FileNotFoundException}.
	 * 
	 * @param run
	 * @return
	 * @throws IOException
	 */
	public static AnswerCheckResult noFileNotFoundException(
			final ExceptionalRunnable<IOException> run) throws IOException {
		try {
			run.run();
		} catch (NoSuchFileException | FileNotFoundException e) {
			return new VerifierResult(
					AnswerCheckResult.CheckingResultType.PRESENTATION_ERROR,
					"#verifier.file.doesntexist");
		}
		return new VerifierResult(AnswerCheckResult.CheckingResultType.OK,
				"#verifier.file.exists");
	}

}
