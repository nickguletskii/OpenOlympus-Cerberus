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
package org.ng200.openolympus.cerberus.executors;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.ng200.openolympus.cerberus.ExecutionResult;

/**
 * 
 * An executor is responsible for controlling and monitoring the execution of an
 * executable.
 * 
 * @author Nick Guletskii
 *
 */
/**
 * @author nick
 *
 */
public interface Executor extends Closeable {

	/**
	 * Executes and blocks until the executable has terminated.
	 * 
	 * @param executable
	 *            Path to the executable to execute
	 * @return The result of executing this executable
	 * @throws IOException
	 */
	public abstract ExecutionResult execute(Path executable) throws IOException;

	/**
	 * @return the current CPU time limit in milliseconds
	 */
	public abstract long getCpuLimit();

	/**
	 * @return the current IO limit in bytes
	 */
	public abstract long getDiskLimit();

	/**
	 * @return the error output stream the executable should write to
	 */
	public abstract OutputStream getErrorStream();

	/**
	 * Copies a file from the sandbox into the specified destination.
	 * 
	 * @param path
	 *            The path to the file inside the sandbox
	 * @param destination
	 *            The destination path to copy to
	 * @throws IOException
	 */
	public abstract void getFile(String path, Path destination)
			throws IOException;

	/**
	 * @return the input stream the executable should read from
	 */
	public abstract InputStream getInputStream();

	/**
	 * @return the memory limit in bytes
	 */
	public abstract long getMemoryLimit();

	/**
	 * @return the output stream the executable should write to.
	 */
	public abstract OutputStream getOutputStream();

	/**
	 * @return the time limit in milliseconds.
	 */
	public abstract long getTimeLimit();

	/**
	 * @param file
	 *            Copies the file into the root of the sandbox.
	 * @throws IOException
	 */
	public abstract void provideFile(Path file) throws IOException;

	/**
	 * @param cpuLimit
	 *            the CPU time limit in milliseconds
	 * @return this
	 */
	public abstract Executor setCpuLimit(long cpuLimit);

	/**
	 * @param diskLimit
	 *            the IO limit in bytes
	 * @return this
	 */
	public abstract Executor setDiskLimit(long diskLimit);

	/**
	 * Sets the error output stream the executable should write to.
	 * 
	 * @param errorStream
	 * @return this
	 */
	public abstract Executor setErrorStream(OutputStream errorStream);

	/**
	 * Sets the input stream the executable should read from.
	 * 
	 * @param inputStream
	 * @return this
	 */
	public abstract Executor setInputStream(InputStream inputStream);

	/**
	 * @param memoryLimit
	 *            the memory limit in bytes
	 * @return this
	 */
	public abstract Executor setMemoryLimit(long memoryLimit);

	/**
	 * Sets the output stream the executable should write to
	 * 
	 * @param outputStream
	 * @return this
	 */
	public abstract Executor setOutputStream(OutputStream outputStream);

	/**
	 * @param timeLimit
	 *            the real time limit in milliseconds
	 * @return this
	 */
	public abstract Executor setTimeLimit(long timeLimit);

}