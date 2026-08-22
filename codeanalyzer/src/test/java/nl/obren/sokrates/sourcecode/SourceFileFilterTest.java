/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

public class SourceFileFilterTest {

    /**
     * Builds the source file the way the scan does: SourceCodeFiles.addFile descends with
     * File.listFiles(), so a child's path is the parent's path plus a separator plus its name - and an
     * empty parent path yields a bare name, not a leading separator, which is exactly the case that
     * made a bare relative -confFile classify differently. new File(parent, child) would insert one
     * and hide it, so the path is joined here the way listFiles() produces it.
     */
    private static SourceFile fileUnder(File sourceRoot, String relativePath) {
        String rootPath = sourceRoot.getPath();
        File file = new File(rootPath.isEmpty() ? relativePath : rootPath + File.separator + relativePath);
        return new SourceFile(file).relativize(sourceRoot);
    }

    /**
     * The defect this class's matching was changed for: patterns used to be matched against the path
     * accumulated while walking down from the source root, which carries every directory ABOVE it. A
     * repository checked out under a folder named "docs" therefore matched the standard ".*&#47;docs/.*"
     * ignore rule with every one of its files, and the analysis reported nothing at all.
     */
    @Test
    public void aDirectoryAboveTheSourceRootDoesNotTakePartInMatching() {
        SourceFileFilter ignoreDocs = new SourceFileFilter(".*/docs/.*", "");

        assertFalse("a repository under a folder named \"docs\" must not be ignored wholesale",
                ignoreDocs.matches(fileUnder(new File("/home/alice/docs/myrepo"), "src/Main.java")));
        assertFalse(ignoreDocs.matches(fileUnder(new File("/var/test/myrepo"), "src/Main.java")));

        // ... while the repository's OWN docs folder is still ignored, which is what the rule is for.
        assertTrue(ignoreDocs.matches(fileUnder(new File("/home/alice/myrepo"), "docs/guide.md")));
        assertTrue(ignoreDocs.matches(fileUnder(new File("/home/alice/docs/myrepo"), "docs/guide.md")));
    }

    /**
     * The reason the matched path keeps a leading separator rather than being the bare relative path.
     * Conventions are written anchored (".*&#47;pom[.]xml"), and a bare "pom.xml" has no separator for
     * ".*&#47;" to match - so every root-level rule in ScopingConventions, and every one in a user's own
     * configuration, would silently stop firing.
     */
    @Test
    public void aPatternAnchoredWithASeparatorStillMatchesAFileAtTheRoot() {
        File sourceRoot = new File("/home/alice/myrepo");

        assertTrue(new SourceFileFilter(".*/pom[.]xml", "").matches(fileUnder(sourceRoot, "pom.xml")));
        assertTrue(new SourceFileFilter(".*/pom[.]xml", "").matches(fileUnder(sourceRoot, "sub/pom.xml")));
        assertTrue(new SourceFileFilter(".*/[.]gitignore", "").matches(fileUnder(sourceRoot, ".gitignore")));

        // The converse, which is what makes the anchoring worth documenting: the matched path always
        // begins with a separator, so a pattern written without one matches nothing at all.
        assertFalse(new SourceFileFilter("pom[.]xml", "").matches(fileUnder(sourceRoot, "pom.xml")));
    }

    /**
     * The symptom originally reported: the same repository at the same commit classified differently
     * depending on how the CLI was invoked, because the walked path differed between invocations while
     * the file did not. Relativized paths are identical however the source root was expressed.
     */
    @Test
    public void theSameFileMatchesTheSameWayHoweverTheSourceRootWasExpressed() {
        SourceFileFilter buildFilter = new SourceFileFilter(".*/pom[.]xml", "");

        assertTrue(buildFilter.matches(fileUnder(new File("/home/alice/myrepo"), "pom.xml")));
        assertTrue(buildFilter.matches(fileUnder(new File("myrepo"), "pom.xml")));
        assertTrue(buildFilter.matches(fileUnder(new File(""), "pom.xml")));
        assertTrue(buildFilter.matches(fileUnder(new File("."), "pom.xml")));
    }

    @Test
    public void testPathMatches() throws Exception {
        assertTrue(new SourceFileFilter("/root/a/b.*", "").pathMatches("/root/a/b/c"));
        assertFalse(new SourceFileFilter("/root/a/b.*", "").pathMatches("/otherroot/a/b/c"));

        assertTrue(new SourceFileFilter("/root/a/b.*", "").pathMatches("\\root\\a\\b\\c"));
        assertTrue(new SourceFileFilter("/root/a/b.*", "").pathMatches("\\root/a/b\\c"));

        assertTrue(new SourceFileFilter("\\\\root\\\\a\\\\b.*", "").pathMatches("/root/a/b/c"));
        assertTrue(new SourceFileFilter("\\\\root\\\\a\\\\b.*", "").pathMatches("\\root\\a\\b\\c"));
        assertTrue(new SourceFileFilter("\\\\root\\\\a\\\\b.*", "").pathMatches("\\root/a/b\\c"));
    }

    @Test
    public void testPathMatchesPatternWithForwardSlashes() {
        // The pattern is also tried with "\" replaced by "/". Here the pattern is the regex a\.b
        // (a, any character, b): only the pattern rewritten to a/.b matches the path a/1b.
        assertTrue(new SourceFileFilter("a\\.b", "").pathMatches("a/1b"));
        assertFalse(new SourceFileFilter("a\\.b", "").pathMatches("a/1c"));
    }

    @Test
    public void testPathMatchesPathWithBackSlashes() {
        // The path is also tried with "/" replaced by "\", so a pattern that excludes forward
        // slashes still matches a path that contains them.
        assertTrue(new SourceFileFilter("[^/]*[.]java", "").pathMatches("comp1/src/Test.java"));
        assertFalse(new SourceFileFilter("[^/]*[.]java", "").pathMatches("comp1/src/Test.html"));
    }

    @Test
    public void testPathMatchesPatternWithForwardSlashesAndPathWithBackSlashes() {
        // The pattern rewritten to forward slashes is also tried against the path rewritten to
        // backslashes. Here the pattern is the regex [^\/]ab (one character that is neither a backslash
        // nor a slash, then ab): only the pattern rewritten to [^//]ab, against the path rewritten to
        // \ab, matches - the path alone or the pattern alone still has a separator the class rejects.
        assertTrue(new SourceFileFilter("[^\\\\/]ab", "").pathMatches("/ab"));
        assertFalse(new SourceFileFilter("[^\\\\/]ab", "").pathMatches("/ac"));
    }

    @Test
    public void testToString() {
        assertEquals(new SourceFileFilter("a", "").toString(), "path like \"a\"");
        assertEquals(new SourceFileFilter("", "b").toString(), "content like \"b\"");
        assertEquals(new SourceFileFilter("a", "b").toString(), "path like \"a\" AND content like \"b\"");
    }

    @Test
    public void testGetMatchingLinesCount() throws Exception {
        List<String> lines = Arrays.asList(("package nl.obren.codeexplorer.model.elements;\n" +
                "\n" +
                "public class ModelElement {\n" +
                "    private String note = \"\";\n" +
                "\n" +
                "    public String getNote() {\n" +
                "        return note;\n" +
                "    }\n" +
                "\n" +
                "    public void setNote(String note) {\n" +
                "        this.note = note;\n" +
                "    }\n" +
                "}\n").split("\n"));

        assertEquals(lines.size(), 13);

        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*"), 13);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, "package.*"), 1);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, "public.*"), 1);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*public.*"), 3);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*return.*"), 1);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*String.*"), 3);
        assertEquals(SourceFileFilter.getMatchingLinesCount(lines, ".*[}]"), 3);
    }

    @Test
    public void testMatches() throws Exception {
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), "Z.*"));
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), ".*Z"));
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), "A.*"));
        assertTrue(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), ".*"));

        assertFalse(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), "E.*"));
        assertFalse(SourceFileFilter.matchesAnyLine(Arrays.asList("ABC DG", "Z"), ".*T"));
    }

    @Test
    public void testGetPathMatch() throws Exception {
        SourceFile sourceFile = new SourceFile();
        sourceFile.setRelativePath("comp1/src/main/test/Test.java");

        assertTrue(new SourceFileFilter("", "").pathMatches(sourceFile.getRelativePath()));
        assertTrue(new SourceFileFilter(".*/Test[.]java", "").pathMatches(sourceFile.getRelativePath()));
        assertTrue(new SourceFileFilter(".*[.]java", "").pathMatches(sourceFile.getRelativePath()));
        assertTrue(new SourceFileFilter(".*/main/.*", "").pathMatches(sourceFile.getRelativePath()));

        assertFalse(new SourceFileFilter(".*/comp2/.*", "").pathMatches(sourceFile.getRelativePath()));
        assertFalse(new SourceFileFilter(".*[.]html", "").pathMatches(sourceFile.getRelativePath()));
    }

    @Test
    public void testGetContentMatch() throws Exception {
        SourceFile sourceFile = new SourceFile();
        sourceFile.setContent("package nl.obren.codeexplorer.model.elements;\n" +
                "\n" +
                "public class ModelElement {\n" +
                "    private String note = \"\";\n" +
                "\n" +
                "    public String getNote() {\n" +
                "        return note;\n" +
                "    }\n" +
                "\n" +
                "    public void setNote(String note) {\n" +
                "        this.note = note;\n" +
                "    }\n" +
                "}\n");

        assertTrue(new SourceFileFilter("", "package.*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*public.*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*private.*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*;").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*}").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", ".*[{].*").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", " ").contentMatches(sourceFile.getLines()));
        assertTrue(new SourceFileFilter("", "").contentMatches(sourceFile.getLines()));

        assertFalse(new SourceFileFilter("", "packasge.*").contentMatches(sourceFile.getLines()));
        assertFalse(new SourceFileFilter("", ".*[}] .*").contentMatches(sourceFile.getLines()));
        assertFalse(new SourceFileFilter("", ".*test.*").contentMatches(sourceFile.getLines()));
    }
}
