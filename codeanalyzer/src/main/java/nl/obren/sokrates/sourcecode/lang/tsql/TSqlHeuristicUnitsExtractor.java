/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.tsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TSqlHeuristicUnitsExtractor {

    private static final Pattern UNIT_SIGNATURE = Pattern.compile(
            "^\\s*(CREATE(?:\\s+OR\\s+ALTER)?|ALTER)\\s+(PROCEDURE|PROC|FUNCTION|TRIGGER)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NAME_AFTER_KEYWORD = Pattern.compile(
            "^\\s*(?:CREATE(?:\\s+OR\\s+ALTER)?|ALTER)\\s+(?:PROCEDURE|PROC|FUNCTION|TRIGGER)\\s+" +
                    "((?:\\[[^\\]]+\\]|\"[^\"]+\"|\\w+)(?:\\s*\\.\\s*(?:\\[[^\\]]+\\]|\"[^\"]+\"|\\w+))?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern GO_BATCH = Pattern.compile("^\\s*GO(\\s+\\d+)?\\s*$", Pattern.CASE_INSENSITIVE);

    // Standalone BEGIN that opens a procedural block (not BEGIN TRAN/TRY/CATCH/DIALOG/CONVERSATION).
    private static final Pattern BEGIN_OPEN = Pattern.compile(
            "\\bBEGIN\\b(?!\\s+(?:TRAN|TRANSACTION|TRY|CATCH|DIALOG|CONVERSATION))",
            Pattern.CASE_INSENSITIVE);

    // CASE opens a nested scope that must be closed by END before the outer BEGIN/END can close.
    private static final Pattern CASE_OPEN = Pattern.compile("\\bCASE\\b", Pattern.CASE_INSENSITIVE);

    // Standalone END that closes a procedural block or CASE expression
    // (not END TRY/CATCH/TRAN/TRANSACTION/DIALOG/CONVERSATION).
    private static final Pattern END_CLOSE = Pattern.compile(
            "\\bEND\\b(?!\\s+(?:TRAN|TRANSACTION|TRY|CATCH|DIALOG|CONVERSATION))",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern AS_KEYWORD = Pattern.compile("\\bAS\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RETURNS_TABLE = Pattern.compile("\\bRETURNS\\s+TABLE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RETURN_OPEN = Pattern.compile("\\bRETURN\\s*\\(", Pattern.CASE_INSENSITIVE);

    private static final Pattern MC_IF = Pattern.compile("\\bIF\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_WHILE = Pattern.compile("\\bWHILE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_CASE = Pattern.compile("\\bCASE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_WHEN = Pattern.compile("\\bWHEN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_AND = Pattern.compile("\\bAND\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_OR = Pattern.compile("\\bOR\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_IIF = Pattern.compile("\\bIIF\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_CHOOSE = Pattern.compile("\\bCHOOSE\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern MC_BEGIN_CATCH = Pattern.compile("\\bBEGIN\\s+CATCH\\b", Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> MC_PATTERNS = Arrays.asList(
            MC_IF, MC_WHILE, MC_CASE, MC_WHEN, MC_AND, MC_OR, MC_IIF, MC_CHOOSE, MC_BEGIN_CATCH);

    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        List<UnitInfo> units = new ArrayList<>();

        CleanedContent cleaned = getCleanContent(sourceFile);
        List<String> normalLines = sourceFile.getLines();
        List<String> cleanedLines = SourceCodeCleanerUtils.splitInLines(cleaned.getCleanedContent());
        List<Integer> lineMap = cleaned.getFileLineIndexes();

        int i = 0;
        while (i < cleanedLines.size()) {
            String line = cleanedLines.get(i);
            Matcher sig = UNIT_SIGNATURE.matcher(line);
            if (!sig.find()) {
                i++;
                continue;
            }

            String name = extractName(line);

            int paramsEndIndex = findSignatureEnd(cleanedLines, i);
            String signatureBody = joinLines(cleanedLines, i, paramsEndIndex);
            int numberOfParameters = countParameters(signatureBody);

            boolean isInlineTvf = isInlineTableValuedFunction(signatureBody);

            int endIndex;
            if (isInlineTvf) {
                endIndex = findEndOfInlineTvf(cleanedLines, paramsEndIndex);
            } else {
                endIndex = findEndByBeginEndDepth(cleanedLines, paramsEndIndex);
            }

            if (endIndex < i) {
                endIndex = i;
            }

            int startLine = fileLine(lineMap, i) + 1;
            int endLine = fileLine(lineMap, endIndex) + 1;

            StringBuilder body = new StringBuilder();
            for (int j = i; j <= endIndex; j++) {
                body.append(cleanedLines.get(j)).append("\n");
            }

            UnitInfo unit = new UnitInfo();
            unit.setSourceFile(sourceFile);
            unit.setShortName(name.isEmpty() ? "AnonymousBlock" : name);
            unit.setStartLine(startLine);
            unit.setEndLine(endLine);
            unit.setLinesOfCode(endIndex - i + 1);
            unit.setCleanedBody(body.toString());
            unit.setBody(rawBody(normalLines, startLine, endLine));
            unit.setMcCabeIndex(computeMcCabeIndex(body.toString()));
            unit.setNumberOfParameters(numberOfParameters);
            units.add(unit);

            i = endIndex + 1;
        }

        return units;
    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        CleanedContent normallyCleanedContent = LanguageAnalyzerFactory.getInstance()
                .getLanguageAnalyzer(sourceFile).cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));
        return normallyCleanedContent;
    }

    protected String extraCleanContent(String content) {
        String cleanedContent = emptyStrings(content);
        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);
        return cleanedContent;
    }

    private String emptyStrings(String cleanedContent) {
        return cleanedContent.replaceAll("'.*?'", "''");
    }

    private int fileLine(List<Integer> lineMap, int cleanedIndex) {
        if (cleanedIndex < 0) return 0;
        if (cleanedIndex >= lineMap.size()) {
            return lineMap.isEmpty() ? 0 : lineMap.get(lineMap.size() - 1);
        }
        return lineMap.get(cleanedIndex);
    }

    private String rawBody(List<String> normalLines, int startLine1Based, int endLine1Based) {
        StringBuilder sb = new StringBuilder();
        int from = Math.max(0, startLine1Based - 1);
        int to = Math.min(normalLines.size(), endLine1Based);
        for (int k = from; k < to; k++) {
            sb.append(normalLines.get(k)).append("\n");
        }
        return sb.toString();
    }

    String extractName(String signatureLine) {
        Matcher m = NAME_AFTER_KEYWORD.matcher(signatureLine);
        if (m.find()) {
            return normaliseSpaces(m.group(1));
        }
        return "";
    }

    private String normaliseSpaces(String s) {
        return s.replaceAll("\\s*\\.\\s*", ".").trim();
    }

    /**
     * Find the cleaned-line index at which the unit signature ends (the line containing the top-level AS,
     * or the line containing RETURN(...) for inline TVFs). The returned index is inclusive.
     */
    private int findSignatureEnd(List<String> lines, int startIndex) {
        int parenDepth = 0;
        for (int k = startIndex; k < lines.size(); k++) {
            String line = lines.get(k);
            for (int c = 0; c < line.length(); c++) {
                char ch = line.charAt(c);
                if (ch == '(') parenDepth++;
                else if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            }
            if (parenDepth == 0) {
                Matcher as = AS_KEYWORD.matcher(line);
                while (as.find()) {
                    return k;
                }
                // Some triggers use FOR/AFTER/INSTEAD OF without AS — a BEGIN on its own line ends the sig too.
                if (BEGIN_OPEN.matcher(line).find()) {
                    return k;
                }
            }
        }
        return lines.size() - 1;
    }

    private String joinLines(List<String> lines, int from, int to) {
        StringBuilder sb = new StringBuilder();
        int end = Math.min(lines.size() - 1, to);
        for (int k = from; k <= end; k++) {
            sb.append(lines.get(k)).append("\n");
        }
        return sb.toString();
    }

    int countParameters(String signature) {
        String afterName = stripUntilNameEnd(signature);
        String beforeAs = stripAtAs(afterName);

        // The parameter-list parentheses (if present) are the very first non-whitespace token after the
        // unit name. Any parenthesised construct later in the signature (e.g. VARCHAR(10) in a bareword
        // parameter list) is part of a parameter declaration, not a wrapper around it.
        int firstNonWs = indexOfFirstNonWhitespace(beforeAs);
        String paramBlock;
        if (firstNonWs >= 0 && beforeAs.charAt(firstNonWs) == '(') {
            int matchingClose = findMatchingCloseParen(beforeAs, firstNonWs);
            if (matchingClose < 0) {
                paramBlock = beforeAs.substring(firstNonWs + 1);
            } else {
                paramBlock = beforeAs.substring(firstNonWs + 1, matchingClose);
            }
        } else {
            paramBlock = beforeAs;
        }

        return countAtParameters(paramBlock);
    }

    private int indexOfFirstNonWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private String stripUntilNameEnd(String signature) {
        Matcher m = NAME_AFTER_KEYWORD.matcher(signature);
        if (m.find()) {
            return signature.substring(m.end());
        }
        return signature;
    }

    private String stripAtAs(String text) {
        int parenDepth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '(') parenDepth++;
            else if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            else if (parenDepth == 0) {
                if (i + 2 <= text.length() && isWordBoundaryBefore(text, i) && matchesIgnoreCase(text, i, "AS")
                        && isWordBoundaryAfter(text, i + 2)) {
                    return text.substring(0, i);
                }
            }
        }
        return text;
    }

    private boolean isWordBoundaryBefore(String s, int pos) {
        if (pos == 0) return true;
        char prev = s.charAt(pos - 1);
        return !Character.isLetterOrDigit(prev) && prev != '_';
    }

    private boolean isWordBoundaryAfter(String s, int pos) {
        if (pos >= s.length()) return true;
        char next = s.charAt(pos);
        return !Character.isLetterOrDigit(next) && next != '_';
    }

    private boolean matchesIgnoreCase(String s, int pos, String needle) {
        if (pos + needle.length() > s.length()) return false;
        return s.regionMatches(true, pos, needle, 0, needle.length());
    }

    private int findMatchingCloseParen(String s, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(') depth++;
            else if (ch == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private int countAtParameters(String paramBlock) {
        int count = 0;
        int parenDepth = 0;
        boolean sawAt = false;
        for (int i = 0; i < paramBlock.length(); i++) {
            char ch = paramBlock.charAt(i);
            if (ch == '(') parenDepth++;
            else if (ch == ')') parenDepth = Math.max(0, parenDepth - 1);
            else if (parenDepth == 0 && ch == '@') {
                if (!sawAt) {
                    count++;
                    sawAt = true;
                }
            } else if (parenDepth == 0 && ch == ',') {
                sawAt = false;
            }
        }
        return count;
    }

    private boolean isInlineTableValuedFunction(String signatureBody) {
        if (!RETURNS_TABLE.matcher(signatureBody).find()) return false;
        return !BEGIN_OPEN.matcher(signatureBody).find();
    }

    private int findEndOfInlineTvf(List<String> lines, int signatureEndIndex) {
        // Start after the signature line; look for RETURN ( ... ) terminated by optional ; then GO or next CREATE.
        int parenDepth = 0;
        boolean returnSeen = false;
        boolean returnClosed = false;
        for (int k = signatureEndIndex; k < lines.size(); k++) {
            String line = lines.get(k);

            if (!returnSeen) {
                Matcher r = RETURN_OPEN.matcher(line);
                if (r.find()) {
                    returnSeen = true;
                    // start counting paren depth from the opening paren
                    int startPos = r.end() - 1; // position of '('
                    parenDepth = countParens(line, startPos, line.length());
                    if (parenDepth == 0) {
                        returnClosed = true;
                    }
                    if (returnClosed && endsStatementOrBatch(line, k, lines)) {
                        return k;
                    }
                    continue;
                }
            } else {
                parenDepth += countParens(line, 0, line.length());
                if (parenDepth == 0) {
                    returnClosed = true;
                }
                if (returnClosed && endsStatementOrBatch(line, k, lines)) {
                    return k;
                }
            }

            if (k + 1 < lines.size() && GO_BATCH.matcher(lines.get(k + 1)).matches()) {
                return k;
            }
        }
        return lines.size() - 1;
    }

    private int countParens(String line, int from, int to) {
        int depth = 0;
        int end = Math.min(line.length(), to);
        for (int i = from; i < end; i++) {
            char ch = line.charAt(i);
            if (ch == '(') depth++;
            else if (ch == ')') depth--;
        }
        return depth;
    }

    private boolean endsStatementOrBatch(String line, int lineIndex, List<String> lines) {
        if (line.trim().endsWith(";")) return true;
        if (lineIndex + 1 < lines.size() && GO_BATCH.matcher(lines.get(lineIndex + 1)).matches()) return true;
        return lineIndex == lines.size() - 1;
    }

    /**
     * Walks BEGIN / CASE / END tokens in order of occurrence and tracks depth. BEGIN and CASE both open a
     * scope (+1); END closes either. The unit ends when depth returns to 0 at an END after at least one
     * BEGIN has been seen. A GO batch separator on the following line is also treated as a terminator to
     * guard against malformed input.
     */
    private int findEndByBeginEndDepth(List<String> lines, int signatureEndIndex) {
        int depth = 0;
        boolean seenProcBegin = false;
        for (int k = signatureEndIndex; k < lines.size(); k++) {
            String line = lines.get(k);

            List<Token> tokens = scanBeginCaseEnd(line);
            for (Token t : tokens) {
                if (t.kind == TokenKind.BEGIN || t.kind == TokenKind.CASE) {
                    depth++;
                    if (t.kind == TokenKind.BEGIN) {
                        seenProcBegin = true;
                    }
                } else { // END
                    depth--;
                    if (seenProcBegin && depth == 0) {
                        return k;
                    }
                    if (depth < 0) {
                        return k;
                    }
                }
            }

            if (k + 1 < lines.size() && GO_BATCH.matcher(lines.get(k + 1)).matches()) {
                if (seenProcBegin && depth == 0) {
                    return k;
                }
                if (!seenProcBegin) {
                    // No BEGIN found — single-statement proc body; end at GO boundary.
                    return k;
                }
            }
        }
        return lines.size() - 1;
    }

    private enum TokenKind { BEGIN, CASE, END }

    private static class Token {
        final int pos;
        final TokenKind kind;

        Token(int pos, TokenKind kind) {
            this.pos = pos;
            this.kind = kind;
        }
    }

    private List<Token> scanBeginCaseEnd(String line) {
        List<Token> tokens = new ArrayList<>();
        Matcher beginMatcher = BEGIN_OPEN.matcher(line);
        while (beginMatcher.find()) {
            tokens.add(new Token(beginMatcher.start(), TokenKind.BEGIN));
        }
        Matcher caseMatcher = CASE_OPEN.matcher(line);
        while (caseMatcher.find()) {
            tokens.add(new Token(caseMatcher.start(), TokenKind.CASE));
        }
        Matcher endMatcher = END_CLOSE.matcher(line);
        while (endMatcher.find()) {
            tokens.add(new Token(endMatcher.start(), TokenKind.END));
        }
        tokens.sort((a, b) -> Integer.compare(a.pos, b.pos));
        return tokens;
    }

    int computeMcCabeIndex(String cleanedBody) {
        int mc = 1;
        for (Pattern p : MC_PATTERNS) {
            Matcher m = p.matcher(cleanedBody);
            while (m.find()) {
                mc++;
            }
        }
        return mc;
    }
}
