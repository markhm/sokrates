package nl.obren.sokrates.sourcecode.lang.tsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TSqlAnalyzerTest {
    TSqlAnalyzer analyzer;

    @Before
    public void init() {
        analyzer = new TSqlAnalyzer();
    }

    @Test
    public void cleanForLinesOfCodeCalculations() {

        SourceFile sourceFile = new SourceFile(new File("test_lines.tsql"), TSqlExamples.CONTENT_1);

        // System.out.println(sourceFile.getLines());

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        // System.out.println(cleanedContent.getCleanedContent());
        assertEquals(TSqlExamples.CONTENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    @Ignore
    public void cleanForDuplicationCalculations() {

        SourceFile sourceFile = new SourceFile(new File("test_duplicate.tsql"), TSqlExamples.CONTENT_2);

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(TSqlExamples.CONTENT_2_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    @Ignore
    public void extractUnits1() {
        TSqlAnalyzer analyzer = new TSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units1.tsql"), TSqlExamples.CONTENT_3);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(1, unitInfos.size());
        assertEquals("create_email_address", unitInfos.get(0).getShortName());
        assertEquals(14, unitInfos.get(0).getLinesOfCode());
        assertEquals(2, unitInfos.get(0).getMcCabeIndex());
        assertEquals(4, unitInfos.get(0).getNumberOfParameters());
    }

    @Test
    @Ignore
    public void extractUnits2() {
        TSqlAnalyzer analyzer = new TSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units2.tsql"), TSqlExamples.CONTENT_4);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(2, unitInfos.size());
        assertEquals("print_aa", unitInfos.get(1).getShortName());
        assertEquals(8, unitInfos.get(1).getLinesOfCode());
        assertEquals(2, unitInfos.get(1).getMcCabeIndex());
        assertEquals(1, unitInfos.get(1).getNumberOfParameters());
    }

    @Test
    @Ignore
    public void extractUnitsFromPackage() {
        TSqlAnalyzer analyzer = new TSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units3.tsql"), TSqlExamples.CONTENT_9);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(3, unitInfos.size());
        assertEquals("addCustomer", unitInfos.get(0).getShortName());
        assertEquals("delCustomer", unitInfos.get(1).getShortName());
        assertEquals("listCustomer", unitInfos.get(2).getShortName());
        assertEquals(10, unitInfos.get(0).getLinesOfCode());
        assertEquals(1, unitInfos.get(0).getMcCabeIndex());
        assertEquals(5, unitInfos.get(0).getNumberOfParameters());
        assertEquals(5, unitInfos.get(1).getLinesOfCode());
        assertEquals(1, unitInfos.get(1).getMcCabeIndex());
        assertEquals(1, unitInfos.get(1).getNumberOfParameters());
        assertEquals(14, unitInfos.get(2).getLinesOfCode());
        assertEquals(2, unitInfos.get(2).getMcCabeIndex());
        assertEquals(0, unitInfos.get(2).getNumberOfParameters());
    }

    @Test
    @Ignore
    public void extractUnitsWithoutParameters() {
        TSqlAnalyzer analyzer = new TSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units4.tsql"), TSqlExamples.CONTENT_10);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(1, unitInfos.size());
        assertEquals("get_p1100_date", unitInfos.get(0).getShortName());
        assertEquals(4, unitInfos.get(0).getLinesOfCode());
        assertEquals(1, unitInfos.get(0).getMcCabeIndex());
        assertEquals(0, unitInfos.get(0).getNumberOfParameters());
    }

}
