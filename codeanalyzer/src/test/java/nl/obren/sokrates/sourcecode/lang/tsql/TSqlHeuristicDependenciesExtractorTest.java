package nl.obren.sokrates.sourcecode.lang.tsql;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TSqlHeuristicDependenciesExtractorTest {

    @Test
    @Ignore
    public void extractDependencyAnchors() {
        TSqlHeuristicDependenciesExtractor extractor = new TSqlHeuristicDependenciesExtractor();
        SourceFile sourceFile = new SourceFile(new File("test_anchors.tsql"), TSqlExamples.CONTENT_5);

        List<DependencyAnchor> anchors = extractor.extractDependencyAnchors(sourceFile);

        assertEquals(5, anchors.size());
        assertEquals("c_package", anchors.get(0).getAnchor());
        assertEquals(1, anchors.get(0).getDependencyPatterns().size());
        assertEquals("\\s*c_package[.]+\\S+", anchors.get(0).getDependencyPatterns().get(0));
        assertEquals("test_anchors", anchors.get(4).getAnchor());
        assertEquals(1, anchors.get(4).getDependencyPatterns().size());
        assertEquals("\\s*test_anchors[.]+\\S+", anchors.get(4).getDependencyPatterns().get(0));
    }

    @Test
    @Ignore
    public void extractDependencyAnchorsMultipleFiles() {

        SourceFile sourceFile1 = new SourceFile(new File("test_root.tsql"), TSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_child.tsql"), TSqlExamples.CONTENT_6);

        TSqlHeuristicDependenciesExtractor extractor = new TSqlHeuristicDependenciesExtractor();
        List<DependencyAnchor> anchors = extractor.getDependencyAnchors(Arrays.asList(sourceFile1, sourceFile2));

        assertEquals(3, anchors.size());
        assertEquals("c_package", anchors.get(0).getAnchor());
        assertEquals("test_root", anchors.get(1).getAnchor());
        assertEquals("test_child", anchors.get(2).getAnchor());
        assertEquals("\\s*c_package[.]+\\S+", anchors.get(0).getDependencyPatterns().get(0));
    }

    @Test
    @Ignore
    public void extractDependencyAnchorsMultipleFiles2() {

        SourceFile sourceFile1 = new SourceFile(new File("test_root1.tsql"), TSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_root2.tsql"), TSqlExamples.CONTENT_7);
        SourceFile sourceFile3 = new SourceFile(new File("test_child1.tsql"), TSqlExamples.CONTENT_6);
        SourceFile sourceFile4 = new SourceFile(new File("test_child2.tsql"), TSqlExamples.CONTENT_8);

        TSqlHeuristicDependenciesExtractor extractor = new TSqlHeuristicDependenciesExtractor();
        List<DependencyAnchor> anchors = extractor
                .getDependencyAnchors(Arrays.asList(sourceFile1, sourceFile2, sourceFile3, sourceFile4));

        assertEquals(6, anchors.size());
        assertEquals("c_package", anchors.get(0).getAnchor());
        assertEquals("test_root1", anchors.get(1).getAnchor());
        assertEquals("emp_bonus", anchors.get(2).getAnchor());
        assertEquals("test_root2", anchors.get(3).getAnchor());
        assertEquals("test_child1", anchors.get(4).getAnchor());
        assertEquals("test_child2", anchors.get(5).getAnchor());
    }

    @Test
    @Ignore
    public void extractDependencies() throws Exception {
        TSqlAnalyzer analyzer = new TSqlAnalyzer();
        SourceFile sourceFile1 = new SourceFile(new File("test_root.tsql"), TSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_child.tsql"), TSqlExamples.CONTENT_6);

        List<Dependency> dependencies = analyzer
                .extractDependencies(Arrays.asList(sourceFile1, sourceFile2), new ProgressFeedback())
                .getDependencies();

        assertEquals(1, dependencies.size());
        assertEquals("test_child -> c_package", dependencies.get(0).getDependencyString());
        assertEquals("test_child -> c_package", dependencies.get(0).getComponentDependency(""));
    }

    @Test
    @Ignore
    public void extractDependenciesMultipleFiles() throws Exception {
        TSqlAnalyzer analyzer = new TSqlAnalyzer();
        SourceFile sourceFile1 = new SourceFile(new File("test_root1.tsql"), TSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_root2.tsql"), TSqlExamples.CONTENT_7);
        SourceFile sourceFile3 = new SourceFile(new File("test_child1.tsql"), TSqlExamples.CONTENT_6);
        SourceFile sourceFile4 = new SourceFile(new File("test_child2.tsql"), TSqlExamples.CONTENT_8);

        List<Dependency> dependencies = analyzer
                .extractDependencies(Arrays.asList(sourceFile1, sourceFile2, sourceFile3, sourceFile4),
                        new ProgressFeedback())
                .getDependencies();

        assertEquals(3, dependencies.size());
        assertEquals("test_child1 -> c_package", dependencies.get(0).getDependencyString());
        assertEquals("test_child1 -> c_package", dependencies.get(0).getComponentDependency(""));
        assertEquals("test_child2 -> c_package", dependencies.get(1).getDependencyString());
        assertEquals("test_child2 -> emp_bonus", dependencies.get(2).getDependencyString());
    }
}
