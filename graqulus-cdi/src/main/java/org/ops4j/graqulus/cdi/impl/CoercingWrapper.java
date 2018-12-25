package org.ops4j.graqulus.cdi.impl;

import org.ops4j.graqulus.cdi.api.Serializer;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;

@SuppressWarnings("unchecked")
public class CoercingWrapper<S, T> implements Coercing<S, T> {

    private Serializer<S, T> serializer;

    public CoercingWrapper(Serializer<S, T> serializer) {
        this.serializer = serializer;
    }

    @Override
    public T serialize(Object dataFetcherResult) throws CoercingSerializeException {
        return serializer.serialize((S) dataFetcherResult);
    }

    @Override
    public S parseValue(Object input) throws CoercingParseValueException {
        return serializer.deserialize((T) input);
    }

    @Override
    public S parseLiteral(Object input) throws CoercingParseLiteralException {
        if (input instanceof StringValue) {
            StringValue stringValue = (StringValue) input;
            return serializer.deserializeNonNull((T) stringValue.getValue());
        }
        throw new IllegalArgumentException("Cannot parse " + input.toString());
    }
}
