package org.testpackage.test;

import org.junit.Before;
import org.junit.Test;
import org.testpackage.TestPackage;

import java.io.IOException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;


/**
 * Created by rnorth on 28/01/2017.
 */
public class ClassTypeTests extends StreamCaptureBaseTest {


    @Before
    public void setupPackageName() throws IOException {
        System.setProperty("package", "org.testpackage.runnertest.classtypetests");
    }

    @Test
    public void test() throws IOException {

        TestPackage testPackage = new TestPackage();
        testPackage.getConfiguration().setQuiet(false);
        testPackage.run();

        String capturedStdOut = getCapturedStdOut();
        assertTrue("The runnable test class is run", capturedStdOut.contains("✔  NormalTest.shouldBeRun"));
        assertFalse("Non-runnable test classes are not run", capturedStdOut.contains("shouldNotBeRun"));
    }

}