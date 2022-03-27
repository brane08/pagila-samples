package com.github.brane08.pagila.seedworks.query;

public interface QueryParserFactory {

    <R> QueryParser<R> buildParser(String queryPart, Class<R> resultClass);
}
