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
package org.ng200.openolympus.cerberus.compilers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ng200.openolympus.cerberus.exceptions.CompilationException;

/**
 * Provides a common API to different compilers
 * 
 * @author Nick Guletskii
 *
 */
public interface Compiler {
	public void addArgument(String argument);

	/**
	 * Compiles a set of files.
	 * 
	 * @param inputFiles
	 *            Files to compile.
	 * @param outputFile
	 *            Output file
	 * @throws CompilationException
	 *             If the compiler threw an error or the input files have
	 *             compilation errors.
	 * @throws IOException
	 */
	public default void compile(final List<Path> inputFiles,
			final Path outputFile) throws CompilationException, IOException {
		this.compile(inputFiles, outputFile, new HashMap<String, Object>());
	}

	/**
	 * Compiles a set of files.
	 * 
	 * @param inputFiles
	 *            Files to compile.
	 * @param outputFile
	 *            Output file
	 * @param additionalParameters
	 *            Additional parameters to pass to the compiler
	 * @throws CompilationException
	 *             If the compiler threw an error or the input files have
	 *             compilation errors.
	 * @throws IOException
	 */
	public void compile(List<Path> inputFiles, Path outputFile,
			Map<String, Object> additionalParameters)
					throws CompilationException, IOException;
}
