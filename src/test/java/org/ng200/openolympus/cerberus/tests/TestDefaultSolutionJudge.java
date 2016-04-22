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
package org.ng200.openolympus.cerberus.tests;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.ng200.openolympus.FileAccess;
import org.ng200.openolympus.SharedTemporaryStorageFactory;
import org.ng200.openolympus.cerberus.DefaultSolutionJudge;
import org.ng200.openolympus.cerberus.Janitor;
import org.ng200.openolympus.cerberus.SolutionJudge;
import org.ng200.openolympus.cerberus.SolutionResult;
import org.ng200.openolympus.cerberus.SolutionResult.Result;
import org.ng200.openolympus.cerberus.util.ExceptionalBiConsumer;
import org.ng200.openolympus.cerberus.util.Lists;
import org.ng200.openolympus.cerberus.util.TemporaryStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDefaultSolutionJudge {

	private static final Logger logger = LoggerFactory
			.getLogger(TestDefaultSolutionJudge.class);

	private Properties defaultTestProperties() {
		final Properties properties = new Properties();

		properties.put("cpuTimeLimit", "1000");
		properties.put("realTimeLimit", "1000");
		properties.put("memoryLimit", Integer.toString(256 * 1024 * 1024));
		properties.put("diskLimit", Integer.toString(256 * 1024 * 1024));
		return properties;
	}

	private void successfulTest(String filename, String contents)
			throws Exception {
		withJudgeThatCompiles(filename, contents, (judge, storage) -> {
			this.testOnTest(judge, defaultTestProperties(), storage, "",
					"hello world", Result.OK);
			this.testOnTest(judge, defaultTestProperties(), storage, "", "foo",
					Result.WRONG_ANSWER);
		});
	}

	private void runtimeErrorTest(String filename, String contents)
			throws Exception {
		withJudgeThatCompiles(filename, contents, (judge, storage) -> {
			this.testOnTest(judge, defaultTestProperties(), storage, "", "",
					Result.RUNTIME_ERROR);
		});
	}

	private void withJudgeThatCompiles(String filename, String contents,
			ExceptionalBiConsumer<SolutionJudge, TemporaryStorage> testRunnable)
			throws Exception {
		final SharedTemporaryStorageFactory storageFactory = new SharedTemporaryStorageFactory(
				FileSystems.getDefault().getPath("/tmp/"));
		final DefaultSolutionJudge judge = new DefaultSolutionJudge(
				true, "input.txt", "output.txt", "US-ASCII", storageFactory,
				true);
		try (TemporaryStorage storage = new TemporaryStorage(judge);) {

			final Path testSrc = storage.getPath().resolve(filename);
			FileAccess.writeUTF8StringToFile(testSrc, contents);

			TestDefaultSolutionJudge.logger
					.info("Telling judge to compile a test solution");

			final Properties properties = this.defaultTestProperties();

			judge.compile(Lists.from(testSrc), properties);

			TestDefaultSolutionJudge.logger
					.info("The judge said that compilation has finished.");

			Assert.assertEquals(null, judge.getCurrentStatus().getResult());

			testRunnable.accept(judge, storage);

		} finally {
			Janitor.cleanUp(judge);
		}
	}

	private void testCompileError(String fileName) throws IOException {
		final SharedTemporaryStorageFactory storageFactory = new SharedTemporaryStorageFactory(
				FileSystems.getDefault().getPath("/tmp/"));
		final DefaultSolutionJudge judge = new DefaultSolutionJudge(
				true, "input.txt", "output.txt", "US-ASCII", storageFactory,
				true);
		try (TemporaryStorage storage = new TemporaryStorage(judge);) {

			final Path testSrc = storage.getPath().resolve(fileName);
			FileAccess.writeUTF8StringToFile(testSrc, "foo");

			TestDefaultSolutionJudge.logger
					.info("Telling judge to compile a test solution");

			judge.compile(Lists.from(testSrc), new Properties());

			TestDefaultSolutionJudge.logger
					.info("The judge said that compilation has finished.");

			Assert.assertEquals(Result.COMPILE_ERROR, judge.getCurrentStatus()
					.getResult());
		} finally {
			Janitor.cleanUp(judge);
		}
	}

	@Test
	public void testDefaultSolutionJudgeOnCpp() throws Exception {
		this.successfulTest("test.cpp", new StringBuilder()

				.append("#include<iostream>\n")

				.append("int main(){\n")

				.append("std::cout<<\"hello world\"<<std::endl;\n")

				.append("}").toString());
	}

	@Test
	public void testDefaultSolutionJudgeOnCppCompilationError()
			throws Exception {
		this.testCompileError("test.cpp");
	}

	@Test
	public void testDefaultSolutionJudgeOnCppRuntimeError()
			throws Exception {
		this.runtimeErrorTest("test.cpp", new StringBuilder()

				.append("#include<iostream>\n")

				.append("#include<fstream>\n")

				.append("using namespace std;\n")

				.append("int main(){\n")

				.append("return -1;\n")

				.append("}").toString());
	}

	@Test
	public void testDefaultSolutionJudgeOnFPC() throws Exception {
		this.successfulTest("test.pas", new StringBuilder()

				.append("program Hello;\n")

				.append("begin\n")

				.append("writeln ('hello world')\n")

				.append("end.").toString());
	}

	@Test
	public void testDefaultSolutionJudgeOnFPCCompilationError()
			throws Exception {
		this.testCompileError("test.pas");
	}

	@Test
	public void testDefaultSolutionJudgeOnJava() throws Exception {
		this.successfulTest("Main.java", new StringBuilder()

				.append("public class Main {")

				.append("public static void main(String[] args) {")

				.append("System.out.println(\"hello world\");\n")

				.append("}\n}").toString());
	}

	@Test
	public void testDefaultSolutionJudgeOnJavaCompilationError()
			throws Exception {
		this.testCompileError("test.java");
	}

	private void testOnTest(SolutionJudge judge, Properties properties,
			TemporaryStorage storage, String inputString, String outputString,
			Result expectedResult) throws IOException {
		final Path input = storage.getPath().resolve("input.txt");
		final Path output = storage.getPath().resolve("output.txt");
		FileAccess.writeUTF8StringToFile(input, inputString);
		FileAccess.writeUTF8StringToFile(output, outputString);

		TestDefaultSolutionJudge.logger.info(
				"Telling judge to run a test solution, expected result {}",
				expectedResult);

		final SolutionResult result = judge.run(Lists.from(input, output),
				true, BigDecimal.ONE, properties);

		TestDefaultSolutionJudge.logger.info("Result: {}", result);

		Assert.assertEquals("Unexpected result: ", expectedResult,
				result.getResult());
	}

}
