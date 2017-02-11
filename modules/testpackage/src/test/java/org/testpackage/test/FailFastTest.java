package org.testpackage.test;

import org.testpackage.TestPackage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;


/**
 * Created by richardnorth on 01/01/2014.
 */
public class FailFastTest extends StreamCaptureBaseTest {


    @Before
    public void setupPackageName() throws IOException {
        System.setProperty("package", "org.testpackage.runnertest.failfasttests");
    }

    @Test
    public void testAllTestsRunWithoutFlag() throws IOException {

        TestPackage testPackage = new TestPackage();
        testPackage.run();

        String capturedStdOut = getCapturedStdOut();
        int aaa_position = capturedStdOut.indexOf("aaa_FailingTest");
        int zzz_position = capturedStdOut.indexOf("zzz_PassingTest");
        assertTrue("one test is expected to have failed", capturedStdOut.contains("1 failed"));
        assertTrue("one test is expected to have passed", capturedStdOut.contains("1 passed"));
        assertTrue("the passing test ran after the failing test had already failed", aaa_position < zzz_position);
    }

    @Test
    public void testOnlyFailingTestRunWithFlag() throws IOException {

        TestPackage testPackage = new TestPackage();
        testPackage.getConfiguration().setFailFast(true);
        int exitCode = testPackage.run();

        String capturedStdOut = getCapturedStdOut();
        assertTrue("no test should have passed", !capturedStdOut.contains("1 passed") && !capturedStdOut.contains("zzz_PassingTest"));
        assertTrue("stdout should contain 'TESTS ABORTED'", capturedStdOut.contains("TESTS ABORTED"));
        assertEquals("the exit code should be 1", 1, exitCode);
    }
}
