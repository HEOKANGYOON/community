package com.kangyoon.community.global.config.function;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

public class MatchAgainstFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        BasicType<Boolean> booleanType = functionContributions.getTypeConfiguration()
                .getBasicTypeRegistry()
                .resolve(StandardBasicTypes.BOOLEAN);


        functionContributions.getFunctionRegistry().registerPattern(
                "match_against",
                "MATCH(?1) AGAINST(?2 IN BOOLEAN MODE)",
                booleanType
        );

        functionContributions.getFunctionRegistry().registerPattern(
                "match_against_multi",
                "MATCH(?1, ?2) AGAINST(?3 IN BOOLEAN MODE)",
                booleanType
        );
    }
}
