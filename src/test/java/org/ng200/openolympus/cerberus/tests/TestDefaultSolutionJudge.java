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
import org.ng200.openolympus.cerberus.SolutionResult;
import org.ng200.openolympus.cerberus.SolutionResult.Result;
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
			throws IOException {
		final SharedTemporaryStorageFactory storageFactory = new SharedTemporaryStorageFactory(
				FileSystems.getDefault().getPath("/tmp/"));
		final DefaultSolutionJudge judge = new DefaultSolutionJudge(
				"input.txt", "output.txt", true, "US-ASCII", storageFactory);
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

			this.testOnTest(judge, properties, storage, "", "hello world",
					Result.OK);
			this.testOnTest(judge, properties, storage, "", "foo",
					Result.WRONG_ANSWER);
		} finally {
			Janitor.cleanUp(judge);
		}
	}

	private void testCompileError(String fileName) throws IOException {
		final SharedTemporaryStorageFactory storageFactory = new SharedTemporaryStorageFactory(
				FileSystems.getDefault().getPath("/tmp/"));
		final DefaultSolutionJudge judge = new DefaultSolutionJudge(
				"input.txt", "output.txt", true, "US-ASCII", storageFactory);
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

	private void testOnTest(DefaultSolutionJudge judge, Properties properties,
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
