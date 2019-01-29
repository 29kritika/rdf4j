/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.planNodes.LoggingNode;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Håvard Ottestad
 */
@RunWith(Parameterized.class)
abstract public class AbstractShaclTest {

	private static final List<String> testCasePaths = Arrays.asList(
		"test-cases/datatype/simple",
		"test-cases/minLength/simple",
		"test-cases/maxLength/simple",
		"test-cases/pattern/simple",
		"test-cases/languageIn/simple",
		"test-cases/nodeKind/simple",
		"test-cases/minCount/simple",
		"test-cases/maxCount/simple",
		"test-cases/or/inheritance",
		"test-cases/or/inheritance-deep",
		"test-cases/or/inheritance-deep-minCountMaxCount",
		"test-cases/or/inheritanceNodeShape",
		"test-cases/or/datatype",
		"test-cases/or/minCountMaxCount",
		"test-cases/or/maxCount",
		"test-cases/or/minCount",
		"test-cases/or/nodeKindMinLength",
		"test-cases/or/implicitAnd",
		"test-cases/or/datatypeDifferentPaths"

	);

	final String testCasePath;
	final String path;
	final ExpectedResult expectedResult;

	public AbstractShaclTest(String testCasePath, String path, ExpectedResult expectedResult) {
		this.testCasePath = testCasePath;
		this.path = path;
		this.expectedResult = expectedResult;
		LoggingNode.loggingEnabled = true;
	}

	@Parameterized.Parameters(name = "{2} - {1}")
	public static Collection<Object[]> data() {

		return getTestsToRun();
	}


	private static List<String> findTestCases(String testCase, String baseCase) {

		List<String> ret = new ArrayList<>();

		for (int i = 0; i < 1000; i++) {
			String path = testCase + "/" + baseCase + "/case" + i;
			InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(path);
			if (resourceAsStream != null) {
				ret.add(path);
				try {
					resourceAsStream.close();
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		return ret;

	}

	private static Collection<Object[]> getTestsToRun() {
		List<Object[]> ret = new ArrayList<>();

		for (String testCasePath : testCasePaths) {
			for (ExpectedResult baseCase : ExpectedResult.values()) {
				findTestCases(testCasePath, baseCase.name()).forEach(path -> {
					Object[] temp = { testCasePath, path, baseCase };
					ret.add(temp);

				});
			}
		}

		return ret;
	}

	static void runTestCase(String shaclPath, String dataPath, ExpectedResult expectedResult) throws Exception {

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		String shaclFile = shaclPath + "shacl.ttl";
		System.out.println(shaclFile);
		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		shaclSail.setLogValidationPlans(true);
		SailRepository shaclRepository = new SailRepository(shaclSail);
		shaclRepository.initialize();
		Utils.loadShapeData(shaclRepository, shaclFile);

		boolean exception = false;
		boolean ran = false;

		for (int j = 0; j < 100; j++) {

			String name = dataPath + "query" + j + ".rq";
			try (InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(name)) {
				if (resourceAsStream == null) {
					continue;
				}

				ran = true;
				System.out.println(name);

				try (SailRepositoryConnection connection = shaclRepository.getConnection()) {
					connection.begin(IsolationLevels.SNAPSHOT);
					String query = IOUtil.readString(resourceAsStream);
					connection.prepareUpdate(query).execute();
					connection.commit();
				} catch (RepositoryException sailException) {
					exception = true;
					System.out.println(sailException.getMessage());

					System.out.println("\n############################################");
					System.out.println("\tValidation Report\n");
					ShaclSailValidationException cause = (ShaclSailValidationException) sailException.getCause();
					Model validationReport = cause.validationReportAsModel();
					Rio.write(validationReport, System.out, RDFFormat.TURTLE);
					System.out.println("\n############################################");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		shaclSail.shutDown();

		if (ran) {
			if (expectedResult == ExpectedResult.valid) {
				assertFalse("Expected transaction to succeed", exception);
			} else {
				assertTrue("Expected transaction to fail", exception);
			}
		}

	}

	static void runTestCaseSingleTransaction(String shaclPath, String dataPath, ExpectedResult expectedResult)
		throws Exception
	{

		if (!dataPath.endsWith("/")) {
			dataPath = dataPath + "/";
		}

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		ShaclSail shaclSail = new ShaclSail(new MemoryStore());
		SailRepository shaclRepository = new SailRepository(shaclSail);
		shaclRepository.initialize();
		Utils.loadShapeData(shaclRepository, shaclPath + "shacl.ttl");

		boolean exception = false;
		boolean ran = false;

		try (SailRepositoryConnection shaclSailConnection = shaclRepository.getConnection()) {
			shaclSailConnection.begin(IsolationLevels.SNAPSHOT);

			for (int j = 0; j < 100; j++) {

				String name = dataPath + "query" + j + ".rq";
				InputStream resourceAsStream = AbstractShaclTest.class.getClassLoader().getResourceAsStream(name);
				if (resourceAsStream == null) {
					continue;
				}

				ran = true;
				System.out.println(name);

				try {
					String query = IOUtil.readString(resourceAsStream);
					shaclSailConnection.prepareUpdate(query).execute();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			try {
				shaclSailConnection.commit();

			} catch (RepositoryException sailException) {
				exception = true;
				System.out.println(sailException.getMessage());
			}
		}
		if (ran) {
			if (expectedResult == ExpectedResult.valid) {
				assertFalse(exception);
			}
			else {
				assertTrue(exception);
			}
		}

	}

	String getShaclPath() {
		String shaclPath = testCasePath;

		if (!shaclPath.endsWith("/")) {
			shaclPath = shaclPath + "/";
		}

		return shaclPath + "shacl.ttl";
	}

	enum ExpectedResult {
		valid,
		invalid
	}

}
