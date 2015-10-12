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
import java.util.Properties;

import org.ng200.openolympus.SharedTemporaryStorageFactory;

/**
 * 
 * A solution judge factory is the core component of the OpenOlympus Cerberus
 * testing platform. It is primarily responsible for creating a
 * {@link SolutionJudge}, but it is also used for customising some properties of
 * the judge such as maximum score per test.
 * 
 * @author Nick Guletskii
 *
 */
public interface SolutionJudgeFactory extends Serializable {

	/**
	 * @param properties
	 *            Task configuration properties
	 * @param storageFactory
	 *            The shared temporary storage factory.
	 *            {@see SharedTemporaryStorageFactory}
	 * @return
	 */
	public SolutionJudge createJudge(Properties properties,
			SharedTemporaryStorageFactory storageFactory);

	public BigDecimal getMaximumScoreForTest(String testPath,
			Properties properties);

}
