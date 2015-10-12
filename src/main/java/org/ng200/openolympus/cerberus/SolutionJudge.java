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

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * @author nick
 *
 */
public abstract class SolutionJudge implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -2634100282330478183L;

	/**
	 * Serialisation constructor. Must exist, otherwise deserialisation is not
	 * possible.
	 */
	public SolutionJudge() {
	}

	/**
	 * This method should clean up all local resources (local temporary files,
	 * etc...).
	 * 
	 * May get called more than once.
	 * 
	 * @throws Exception
	 */
	public abstract void closeLocal() throws Exception;

	/**
	 * This method should clean up all shared resources that are held by this
	 * judge, e.g. SharedTemporaryStorage. Shared resources are resources that
	 * are available to all instances across the testing network.
	 * 
	 * May get called more than once.
	 * 
	 * @throws Exception
	 */
	public abstract void closeShared() throws Exception;

	/**
	 * 
	 * This method should mutate this judge by compiling the solution. This
	 * method must block until the compilation process is finished.
	 * 
	 * The state of the judge after this method has returned is called the
	 * "compiled" state of the judge. This state will be restored for every
	 * {@link #run(List, boolean, BigDecimal, Properties)} method invocation.
	 * 
	 * After this method completes, {@link #isCompiled()} should return true.
	 * 
	 * @param sources
	 *            List of solution files to compile.
	 * @param properties
	 *            Task configuration properties.
	 */
	public abstract void compile(List<Path> sources, Properties properties);

	/**
	 * @return true if the judge has already compiled source files.
	 */
	public abstract boolean isCompiled();

	/**
	 * Generates a verdict for a test. A new SolutionJudge is deserialised from
	 * its "compiled" state each time this method is called.
	 * 
	 * @param testFiles
	 *            Files that belong to the test the solution is currently being
	 *            tested on. These files may not reside on a real filesystem, so
	 *            conversion to java.io.File is not possible.
	 * @param checkAnswer
	 *            <b>true</b> if the testing supervisor has requested the answer
	 *            to be checked for correctness.
	 * 
	 *            <b>false</b> if the testing supervisor isn't interested in
	 *            verifying the correctness of the solution's answer.
	 * @param maximumScore
	 *            The maximum score that the solution judge can award for this
	 *            test.
	 * @param properties
	 *            Task configuration properties.
	 * @return
	 */
	public abstract SolutionResult run(List<Path> testFiles,
			boolean checkAnswer, BigDecimal maximumScore,
			Properties properties);
}
