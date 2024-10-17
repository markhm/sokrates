#!/bin/sh

mvn -pl codeanalyzer -Dtest=nl.obren.sokrates.sourcecode.lang.tsql.TSqlAnalyzerTest,nl.obren.sokrates.sourcecode.lang.tsql.TSqlHeuristicDependenciesExtractorTest test
