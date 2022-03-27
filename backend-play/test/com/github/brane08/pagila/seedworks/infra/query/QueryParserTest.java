package com.github.brane08.pagila.seedworks.infra.query;

import com.github.brane08.pagila.film.entities.Film;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class QueryParserTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBuildQuery() {
		QueryParser parser = new QueryParser(null, Film.class, "name==some;size=lt=20;age=gt=25");
		parser.buildQuery();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBuildQueryWithException() {
		QueryParser parser = new QueryParser(null, Film.class, "name==some;size=lt=20,age=gt=25");
		parser.buildQuery();
	}
}