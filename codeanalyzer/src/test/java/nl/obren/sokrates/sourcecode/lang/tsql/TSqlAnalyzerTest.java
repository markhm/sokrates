/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.tsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TSqlAnalyzerTest {

    private TSqlAnalyzer analyzer;

    @Before
    public void init() {
        analyzer = new TSqlAnalyzer();
    }

    private SourceFile srcFile(String content) {
        return srcFile("test.tsql", content);
    }

    private SourceFile srcFile(String name, String content) {
        return new SourceFile(new File(name), content);
    }

    @Test
    public void cleanForLinesOfCodeCalculations_removesCommentsAndEmptyLines() {
        CleanedContent cleaned = analyzer.cleanForLinesOfCodeCalculations(srcFile(TSqlExamples.CONTENT_LOC));
        assertEquals(TSqlExamples.CONTENT_LOC_CLEANED, cleaned.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations_trimsWhitespaceAndEmptiesStrings() {
        CleanedContent cleaned = analyzer.cleanForDuplicationCalculations(srcFile(TSqlExamples.CONTENT_DUP));
        assertEquals(TSqlExamples.CONTENT_DUP_CLEANED, cleaned.getCleanedContent());
    }

    @Test
    public void extractUnits_simpleProcedureWithIfElse() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.SIMPLE_PROCEDURE));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.foo", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        // McCabe: base 1 + IF(1). ELSE is not a decision point on its own.
        assertEquals(2, unit.getMcCabeIndex());
        assertEquals(8, unit.getLinesOfCode());
        assertEquals(1, unit.getStartLine());
        assertEquals(8, unit.getEndLine());
    }

    @Test
    public void extractUnits_whileAndCase() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.WHILE_AND_CASE_PROCEDURE));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.bar", unit.getShortName());
        assertEquals(0, unit.getNumberOfParameters());
        // McCabe: 1 + WHILE(1) + CASE(1) + WHEN(2) = 5.
        assertEquals(5, unit.getMcCabeIndex());
        assertEquals(10, unit.getLinesOfCode());
    }

    @Test
    public void extractUnits_tryCatchDoesNotTerminateEarly() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.TRY_CATCH_PROCEDURE));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.baz", unit.getShortName());
        // Without the TRY/CATCH exclusion the unit would close at 'END TRY' (line 6) instead of the
        // outer 'END' (line 10).
        assertEquals(10, unit.getLinesOfCode());
        // McCabe: 1 + BEGIN CATCH(1) = 2.
        assertEquals(2, unit.getMcCabeIndex());
    }

    @Test
    public void extractUnits_parenthesisedParameterList() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.PAREN_PARAMS_PROCEDURE));
        assertEquals(1, units.size());
        assertEquals(2, units.get(0).getNumberOfParameters());
        assertEquals("dbo.p1", units.get(0).getShortName());
    }

    @Test
    public void extractUnits_barewordParameterList() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.BAREWORD_PARAMS_PROCEDURE));
        assertEquals(1, units.size());
        assertEquals(2, units.get(0).getNumberOfParameters());
        assertEquals("dbo.p2", units.get(0).getShortName());
    }

    @Test
    public void extractUnits_inlineTableValuedFunction() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.INLINE_TVF));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.f1", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        assertEquals(1, unit.getMcCabeIndex());
        // Whole definition fits on one line.
        assertEquals(1, unit.getLinesOfCode());
        assertEquals(1, unit.getStartLine());
        assertEquals(1, unit.getEndLine());
    }

    @Test
    public void extractUnits_twoProceduresSeparatedByGo() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.TWO_PROCS_WITH_GO));
        assertEquals(2, units.size());
        assertEquals("dbo.p1", units.get(0).getShortName());
        assertEquals("dbo.p2", units.get(1).getShortName());
        assertEquals(5, units.get(0).getLinesOfCode());
        assertEquals(5, units.get(1).getLinesOfCode());
        // Second unit starts after the GO and any intermediate blank lines.
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(5, units.get(0).getEndLine());
        assertEquals(7, units.get(1).getStartLine());
        assertEquals(11, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_createOrAlter() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.CREATE_OR_ALTER));
        assertEquals(1, units.size());
        assertEquals("dbo.p3", units.get(0).getShortName());
        assertEquals(1, units.get(0).getNumberOfParameters());
    }

    @Test
    public void extractUnits_bracketedName() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.BRACKETED_NAME));
        assertEquals(1, units.size());
        assertEquals("[dbo].[weird name]", units.get(0).getShortName());
    }

    @Test
    public void computeMcCabeIndex_directWordBoundaryBehaviour() {
        TSqlHeuristicUnitsExtractor extractor = new TSqlHeuristicUnitsExtractor();
        // identifiers that merely *contain* the keyword substring must NOT count (word boundary).
        assertEquals(1, extractor.computeMcCabeIndex("SET @ANDY = 1; SET @IFACE = 2; SET @ORDER = 3;"));
        // Each genuine keyword contributes +1. Base 1 + IF + AND + OR = 4.
        assertEquals(4, extractor.computeMcCabeIndex("IF @x = 1 AND @y = 2 OR @z = 3 SELECT 1"));
    }
}
