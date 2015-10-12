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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.ng200.openolympus.cerberus.util.TemporaryStorage;

/**
 * Class responsible for cleaning up local objects such as
 * {@link TemporaryStorage}
 * 
 * @author Nick Guletskii
 *
 */
public class Janitor {
	/**
	 * Clean up all local resources associated with a judge
	 * 
	 * @param judge
	 */
	public static void cleanUp(final SolutionJudge judge) {
		Janitor.cleanupSteps.forEach((step) -> step.accept(judge));
	}

	/**
	 * Registers a step to run when disposing of a local instance of a judge.
	 * 
	 * @param cleanupStep
	 */
	public static void registerCleanupStep(
			final Consumer<SolutionJudge> cleanupStep) {
		Janitor.cleanupSteps.add(cleanupStep);
	}

	private static List<Consumer<SolutionJudge>> cleanupSteps = Collections
			.synchronizedList(new ArrayList<Consumer<SolutionJudge>>());
}
