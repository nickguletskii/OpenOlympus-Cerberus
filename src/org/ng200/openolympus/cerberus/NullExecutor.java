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
package org.ng200.openolympus.cerberus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.ng200.openolympus.cerberus.executors.Executor;
import org.ng200.openolympus.cerberus.executors.OlrunnerExecutor;

public class NullExecutor extends OlrunnerExecutor {

	@Override
	public void close() throws IOException {
	}

	@Override
	public ExecutionResult execute(final File program) throws IOException {
		return null;
	}

	@Override
	public long getCpuLimit() {
		return 0;
	}

	@Override
	public long getDiskLimit() {
		return 0;
	}

	@Override
	public OutputStream getErrorStream() {
		return null;
	}

	@Override
	public void getFile(final String name, final File destination)
			throws IOException {

	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public long getMemoryLimit() {
		return 0;
	}

	@Override
	public OutputStream getOutputStream() {
		return null;
	}

	@Override
	public long getTimeLimit() {
		return 0;
	}

	@Override
	public void provideFile(final File file) throws IOException {

	}

	@Override
	public Executor setCpuLimit(final long cpuLimit) {
		return null;
	}

	@Override
	public Executor setDiskLimit(final long diskLimit) {
		return null;
	}

	@Override
	public Executor setErrorStream(final OutputStream errorStream) {
		return null;
	}

	@Override
	public Executor setInputStream(final InputStream inputStream) {
		return null;
	}

	@Override
	public Executor setMemoryLimit(final long memoryLimit) {
		return null;
	}

	@Override
	public Executor setOutputStream(final OutputStream outputStream) {
		return null;
	}

	@Override
	public Executor setTimeLimit(final long timeLimit) {
		return null;
	}

}
