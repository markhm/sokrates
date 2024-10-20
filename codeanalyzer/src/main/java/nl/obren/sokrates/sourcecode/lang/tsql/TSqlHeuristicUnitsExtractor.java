package nl.obren.sokrates.sourcecode.lang.tsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TSqlHeuristicUnitsExtractor {

    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        List<UnitInfo> units = new ArrayList<>();
        CleanedContent cleanedContent = getCleanContent(sourceFile);
        List<String> normalLines = sourceFile.getLines();
        List<String> lines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex).trim();

            // Check for unit signatures and ensure both types are captured
            if (line.startsWith("CREATE ") || line.startsWith("create ")) {
                String blockName = getName(line);
                if (blockName.isEmpty()) {
                    continue; // Skip if block name cannot be determined
                }

                int endOfUnitBodyIndex = getEndOfUnitBodyIndex(lines, lineIndex, blockName);
                if (endOfUnitBodyIndex > lineIndex) { // Changed from >= to > to avoid counting empty units
                    StringBuilder body = new StringBuilder();

                    // Collect lines of the unit body
                    for (int bodyIndex = cleanedContent.getFileLineIndexes().get(lineIndex);
                         bodyIndex <= cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex);
                         bodyIndex++) {
                        body.append(normalLines.get(bodyIndex)).append("\n");
                    }

                    // Create UnitInfo instance
                    UnitInfo unit = new UnitInfo();
                    unit.setSourceFile(sourceFile);
                    unit.setLinesOfCode((endOfUnitBodyIndex - lineIndex) + 1);
                    unit.setCleanedBody(body.toString());
                    unit.setBody(body.toString());
                    unit.setStartLine(cleanedContent.getFileLineIndexes().get(lineIndex) + 1);
                    unit.setEndLine(cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex) + 1);
                    unit.setMcCabeIndex(getMcCabeIndex(body.toString()));
                    unit.setShortName(blockName);
                    unit.setNumberOfParameters(getNumberOfParameters(body.toString()));

                    // Add unit to the list
                    units.add(unit);

                    // Advance lineIndex to the end of the current unit
                    lineIndex = endOfUnitBodyIndex;
                }
            }
        }

        return units;
    }

//    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
//
//        List<UnitInfo> units = new ArrayList<>();
//
//        CleanedContent cleanedContent = getCleanContent(sourceFile);
//
//        List<String> normalLines = sourceFile.getLines();
//        List<String> lines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());
//
//        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
//            String line = lines.get(lineIndex).trim();
//            if (isUnitSignature(line)) {
//                String blockName = "";
//                if (line.trim().startsWith("CREATE ") || line.trim().startsWith("create ")) {
//                    blockName = getName(line);
//                }
//
//                int endOfUnitBodyIndex = getEndOfUnitBodyIndex(lines, lineIndex, blockName);
//
//                if (endOfUnitBodyIndex >= lineIndex) {
//                    StringBuilder body = new StringBuilder();
//                    for (int bodyIndex = cleanedContent.getFileLineIndexes().get(lineIndex);
//                         bodyIndex <= cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex);
//                         bodyIndex++) {
//                        body.append(normalLines.get(bodyIndex)).append("\n");
//                    }
//                    UnitInfo unit = new UnitInfo();
//                    unit.setSourceFile(sourceFile);
//                    unit.setLinesOfCode((endOfUnitBodyIndex - lineIndex) + 1);
//                    unit.setCleanedBody(body.toString());
//                    unit.setBody(body.toString());
//                    unit.setStartLine(cleanedContent.getFileLineIndexes().get(lineIndex) + 1);
//                    unit.setEndLine(cleanedContent.getFileLineIndexes().get(endOfUnitBodyIndex) + 1);
//                    unit.setMcCabeIndex(getMcCabeIndex(body.toString()));
//                    unit.setShortName(blockName.isEmpty() ? "AnonymousBlock" : blockName);
//                    unit.setNumberOfParameters(getNumberOfParameters(body.toString()));
//                    lineIndex = endOfUnitBodyIndex;
//                    units.add(unit);
//                }
//            }
//        }
//
//        return units;
//    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        CleanedContent normallyCleanedContent = LanguageAnalyzerFactory.getInstance()
                .getLanguageAnalyzer(sourceFile)
                .cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));

        return normallyCleanedContent;
    }

    protected String extraCleanContent(String content) {
        String cleanedContent = emptyStrings(content);
        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);
        return cleanedContent;
    }

    private String emptyStrings(String cleanedContent) {
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        return cleanedContent;
    }

    private String getName(String line) {
        String name = "";
        String strippedLine = line.trim();
        List<String> literals = Arrays.asList("CREATE ", "PROCEDURE ", "FUNCTION ", "VIEW ", "TRIGGER ", " IS", " AS");

        for (String literal : literals) {
            strippedLine = strippedLine.replace(literal, " ");
            strippedLine = strippedLine.replace(literal.toLowerCase(), " ");
        }

        strippedLine = strippedLine.trim().replace("\"", "").replace("'", "");

        if (strippedLine.contains("(")) {
            strippedLine = strippedLine.substring(0, strippedLine.indexOf("(")).trim();
        }

        name = strippedLine.trim();
        return name;
    }

    private int getNumberOfParameters(String body) {
        // Remove newlines and trim whitespace
        String bodyForSearch = body.replace("\n", " ").trim();

        // Find the start of the parameters after the procedure name
        int asIndex = bodyForSearch.indexOf(" AS ");
        if (asIndex > 0) {
            // Extract everything before the " AS "
            String paramsSection = bodyForSearch.substring(0, asIndex).trim();

            // Find the last occurrence of "CREATE PROCEDURE" to get to the parameters
            int procedureIndex = paramsSection.lastIndexOf("CREATE PROCEDURE");
            if (procedureIndex >= 0) {
                // Extract the parameter section
                String paramString = paramsSection.substring(procedureIndex + "CREATE PROCEDURE".length()).trim();

                // If the parameter string is not empty
                if (StringUtils.isNotBlank(paramString)) {
                    // Split the parameters by comma, trimming each for excess whitespace
                    String[] params = paramString.split("\\s*,\\s*");
                    return params.length; // Return the count of parameters
                } else {
                    return 0; // No parameters found
                }
            }
        }
        return 0; // No "AS" found
    }

    private int getMcCabeIndex(String body) {
        String bodyForSearch = " " + body.replace("\n", " ");
        bodyForSearch = bodyForSearch.replace("(", " (");

        // Updated list of actual decision points
        List<String> mcCabeIndexLiterals = Arrays.asList(
                " IF ", " ELSE ", " WHILE ", " CASE ", " WHEN ", " LOOP ", " AND ", " OR "
        );

        int mcCabeIndex = 1;

        for (String literal : mcCabeIndexLiterals) {
            mcCabeIndex += StringUtils.countMatches(bodyForSearch, literal);
            mcCabeIndex += StringUtils.countMatches(bodyForSearch, literal.toLowerCase());
        }

        return mcCabeIndex;
    }

    public int getEndOfUnitBodyIndex(List<String> lines, int startLineIndex, String blockName) {
        int index = startLineIndex + 1;
        for (String line : lines.subList(startLineIndex + 1, lines.size())) {
            if (!line.trim().isEmpty()) {
                if ((line.trim().startsWith("END") || line.trim().startsWith("end")) && line.contains(blockName)) {
                    return index;
                }
            }
            index++;
        }
        return lines.size() - 1;
    }

    protected boolean isUnitSignature(String line) {
        return line.trim().startsWith("CREATE ") || line.trim().startsWith("create ")
                || line.trim().startsWith("BEGIN ") || line.trim().startsWith("begin ")
                || line.trim().startsWith("PROCEDURE ") || line.trim().startsWith("procedure ")
                || line.trim().startsWith("FUNCTION ") || line.trim().startsWith("function ");
    }
}
