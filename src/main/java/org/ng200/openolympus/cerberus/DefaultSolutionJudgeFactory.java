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
package org.ng200.openolympus.cerberus;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

import org.ng200.openolympus.SharedTemporaryStorageFactory;

/**
 *
 * Default solution judge factory, creates {@link DefaultSolutionJudge}.
 * 
 * Properties:
 * <ul>
 * <li>maximumScorePerTest - maximum score per test.</li>
 * <li>consoleIO - boolean value which determines whether to use standard
 * input/output or use file-based input/output. If false, input.txt and
 * output.txt will be used.</li>
 * </ul>
 * 
 * @author Nick Guletskii
 */
public class DefaultSolutionJudgeFactory implements SolutionJudgeFactory {

	/**
	 *
	 */
	private static final long serialVersionUID = -4480926810325700795L;

	@Override
	public SolutionJudge createJudge(final Properties properties,
			final SharedTemporaryStorageFactory sharedTemporaryStorageFactory) {
		try {
			return new DefaultSolutionJudge(
					Boolean.valueOf(properties.getProperty("consoleIO")),
					"input.txt",
					"output.txt",
					"US-ASCII", sharedTemporaryStorageFactory);
		} catch (final IOException e) {
			throw new RuntimeException("Couldn't create solution judge: ", e);
		}
	}

	@Override
	public BigDecimal getMaximumScoreForTest(final String testPath,
			final Properties properties) {
		return new BigDecimal(properties.getProperty("maximumScorePerTest"));
	}

}
