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
package org.ng200.openolympus.cerberus.executors;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import org.ng200.openolympus.cerberus.ExecutionResult;

public interface Executor extends Closeable {

	public abstract ExecutionResult execute(Path program) throws IOException;

	public abstract long getCpuLimit();

	public abstract long getDiskLimit();

	public abstract OutputStream getErrorStream();

	public abstract void getFile(String name, Path destination)
			throws IOException;

	public abstract InputStream getInputStream();

	public abstract long getMemoryLimit();

	public abstract OutputStream getOutputStream();

	public abstract long getTimeLimit();

	public abstract void provideFile(Path file) throws IOException;

	public abstract Executor setCpuLimit(long cpuLimit);

	public abstract Executor setDiskLimit(long diskLimit);

	public abstract Executor setErrorStream(OutputStream errorStream);

	public abstract Executor setInputStream(InputStream inputStream);

	public abstract Executor setMemoryLimit(long memoryLimit);

	public abstract Executor setOutputStream(OutputStream outputStream);

	public abstract Executor setTimeLimit(long timeLimit);

}