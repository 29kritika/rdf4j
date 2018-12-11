/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.memory;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RDFSchemaRepositoryConnectionTest;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.inferencer.fc.SchemaCachingRDFSInferencer;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Ignore;
import org.junit.Test;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class SchemaCachingRDFSInferencerRDFSchemaMemoryRepositoryConnectionTest
		extends RDFSchemaRepositoryConnectionTest
{

	public SchemaCachingRDFSInferencerRDFSchemaMemoryRepositoryConnectionTest(
			IsolationLevel level)
	{
		super(level);
	}

	@Override
	protected Repository createRepository() {
		return new SailRepository(new SchemaCachingRDFSInferencer(new MemoryStore(), true));
	}


	@Ignore
	@Test
	@Override
	public void testDefaultContext()
		throws Exception
	{
		// ignore
	}

	@Ignore
	@Test
	@Override
	public void testDefaultInsertContext()
		throws Exception
	{
		// ignore
	}

	@Ignore
	@Test
	@Override
	public void testExclusiveNullContext()
		throws Exception
	{
		// ignore
	}


	@Override
	@Test
	@Ignore
	public void testQueryDefaultGraph()
		throws Exception
	{
		// ignore
	}


	@Override
	@Test
	@Ignore
	public void testDeleteDefaultGraph()
		throws Exception
	{
		// ignore
	}

	@Override
	@Test
	@Ignore
	public void testContextStatementsNotDuplicated()
		throws Exception
	{
		// ignore
	}

	@Override
	@Test
	@Ignore
	public void testContextStatementsNotDuplicated2()
		throws Exception
	{
		// ignore
	}


	@Test
	public void testContextTbox()
	{

//		Man subClassOf Human g1
//		Human subClassOf Animal g2
//	-> Man subClassOf Animal ??

		IRI man = vf.createIRI("http://example.org/Man");
		IRI human = vf.createIRI("http://example.org/Human");
		IRI animal = vf.createIRI("http://example.org/Animal");
		IRI bob = vf.createIRI("http://example.org/bob");

		IRI graph1 = vf.createIRI("http://example.org/graph1");
		IRI graph2 = vf.createIRI("http://example.org/graph2");
		IRI graph3 = vf.createIRI("http://example.org/graph3");


		testCon.add(man, RDFS.SUBCLASSOF, human, graph1);
		testCon.add(human, RDFS.SUBCLASSOF, animal, graph2);
		testCon.add(bob, RDF.TYPE, man, graph3);


		/*
		The SchemaCachingRDFSInferencer correctly adds inferred A-box statements to the correct graph,
		but does not add inferred T-box statements to the correct graph.
		 */


		System.out.println("-----------");
		try (Stream<Statement> stream = Iterations.stream(testCon.getStatements(man, RDFS.SUBCLASSOF, null,true))) {
			stream.forEach(System.out::println);
		}
		System.out.println("-----------");
		try (Stream<Statement> stream = Iterations.stream(testCon.getStatements(bob, RDF.TYPE, null,true))) {
			stream
				.peek(statement -> assertEquals(statement.getContext(), graph3))
				.forEach(System.out::println);
		}

		System.out.println("-----------");


	}


}
