/*
 * Copyright 2013 Deloitte Digital and Richard North
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.testpackage;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.twitter.common.testing.runner.StreamSource;
import jline.TerminalFactory;
import org.fusesource.jansi.Ansi;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.testpackage.debugging.EventDebugger;
import org.testpackage.pluginsupport.Plugin;
import org.testpackage.streams.StreamCapture;
import org.testpackage.util.Throwables2;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;
import static org.testpackage.AnsiSupport.ansiPrintf;
import static org.testpackage.output.StringRepresentations.testName;

/**
 * A JUnit run listener which generates user-facing output on System.out to indicate progress of a test run.
 *
 * @author rnorth
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ColouredOutputRunListener extends RunListener implements StreamSource {

    private static final String TICK_MARK = "\u2714";
    private static final String CROSS_MARK = "\u2718";

    private final boolean failFast;
    private final boolean verbose;
    private final boolean quiet;
    private final int testTotalCount;
    private final Collection<? extends Plugin> plugins;
    private final Configuration configuration;
    private final int terminalWidth;

    private StreamCapture streamCapture;
    private Description currentDescription;
    private long currentTestStartTime;
    private int testRunCount = 0;
    private int testFailureCount = 0;
    private int testIgnoredCount = 0;
    private int testAssumptionFailedCount = 0;
    private boolean currentTestDidFail = false;

    private Map<Class, String> stdOutStreamStore = Maps.newHashMap();
    private Map<Class, String> stdErrStreamStore = Maps.newHashMap();

    public ColouredOutputRunListener(boolean failFast, boolean verbose, boolean quiet, int testTotalCount, Collection<? extends Plugin> plugins, Configuration configuration) {
        this.failFast = failFast;
        this.verbose = verbose;
        this.quiet = quiet;
        this.testTotalCount = testTotalCount;
        this.plugins = plugins;
        this.configuration = configuration;

        this.terminalWidth = TerminalFactory.get().getWidth();
    }

    @Override
    public void testRunStarted(final Description description) throws Exception {
        EventDebugger.add("testRunStarted: " + description);

        super.testRunStarted(description);

        currentDescription = description;
        currentTestDidFail = false;
    }

    @Override
    public void testStarted(Description description) throws Exception {
        EventDebugger.add("testStarted: " + description);

        if (!quiet) {
            displayTestMethodPlaceholder(description);
        }

        currentTestStartTime = System.currentTimeMillis();
        currentTestDidFail = false;

        // Tee output if not running in verbose mode, so that it is output in realtime
        boolean teeOutput = verbose && !quiet;
        streamCapture = StreamCapture.grabStreams(teeOutput, description.getDisplayName());
        currentDescription = description;
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
        EventDebugger.add("testFailure: " + failure.getDescription());

        currentTestDidFail = true;
        testFailureCount++;

//        if (failure.getDescription().equals(currentDescription)) {
//            StreamCapture.restore();
//        }

        if (failFast) {
            System.out.flush();
            System.out.println();
            System.out.println();
            System.out.println("*** TESTS ABORTED");
            ansiPrintf("*** @|bg_red Fail-fast triggered by test failure:|@\n");

            reportFailure(failure);
        }
    }

    @Override
    public void testFinished(Description description) throws Exception {
        EventDebugger.add("testFinished: " + description);

        stdOutStreamStore.put(description.getTestClass(), streamCapture.getStdOut());
        stdErrStreamStore.put(description.getTestClass(), streamCapture.getStdErr());

        StreamCapture.restore();

        if (!currentTestDidFail) {

            testRunCount++;

            if (!quiet) {
                replaceTestMethodPlaceholder(true);
            }

            if (!quiet && !verbose && streamCapture.getStdOut().length() > 0) {
                System.out.println("    STDOUT:");
                System.out.print(streamCapture.getStdOut());
            }

            if (!quiet && !verbose && streamCapture.getStdErr().length() > 0) {
                System.out.println("\n    STDERR:");
                System.out.print(streamCapture.getStdErr());
            }
        } else {
            replaceTestMethodPlaceholder(false);

            if (!quiet && !verbose && streamCapture.getStdOut().length() > 0) {
                System.out.println("    STDOUT:");
                System.out.print(streamCapture.getStdOut());
            }

            if (!quiet && !verbose && streamCapture.getStdErr().length() > 0) {
                System.out.println("\n    STDERR:");
                System.out.print(streamCapture.getStdErr());
            }
        }
    }

    @Override
    public void testAssumptionFailure(final Failure failure) {
        EventDebugger.add("testAssumptionFailure: " + failure.getDescription());

        currentTestDidFail = false;
        testAssumptionFailedCount++;
    }

    @Override
    public void testIgnored(Description description) throws Exception {
        EventDebugger.add("testIgnored: " + description);

        testIgnoredCount++;
    }


    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void testRunFinished(Result result) throws Exception {
        EventDebugger.add("testRunFinished: " + result);

        int failureCount = this.testFailureCount;
        int ignoredCount = this.testIgnoredCount;
        int assumptionFailedCount = this.testAssumptionFailedCount;
        int passed = this.testRunCount - assumptionFailedCount;
        List<Failure> failures = Lists.newArrayList();

        failures.addAll(result.getFailures());

        System.out.flush();
        System.out.println();
        System.out.println();
        System.out.println("*** TESTS COMPLETE");

        String passedStatement;
        if (passed > 0 && failureCount == 0) {
            passedStatement = "@|bg_green %d passed|@";
        } else {
            passedStatement = "%d passed";
        }
        String failedStatement;
        if (failureCount > 0) {
            failedStatement = "@|bg_red %d failed|@";
        } else {
            failedStatement = "0 failed";
        }
        String ignoredStatement;
        if (ignoredCount > 0 && ignoredCount > passed) {
            ignoredStatement = "@|bg_red %d ignored|@";
        } else if (ignoredCount > 0) {
            ignoredStatement = "@|bg_yellow %d ignored|@";
        } else {
            ignoredStatement = "0 ignored\n";
        }

        String assumptionStatement;
        if (this.testAssumptionFailedCount > 0) {
            assumptionStatement = ", @|blue %d assumption(s) failed|@";
        } else {
            assumptionStatement = "";
        }

        ansiPrintf("*** " + passedStatement + ", " + failedStatement + ", " + ignoredStatement + assumptionStatement, passed, failureCount, ignoredCount, assumptionFailedCount);

        if (failureCount > 0 && !quiet) {
            System.out.println();
            System.out.println();
            System.out.println("Failures:");
            for (Failure failure : failures) {
                reportFailure(failure);
            }
        }
        System.out.flush();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void reportFailure(Failure failure) {
        ansiPrintf("    @|red %s|@:\n", failure.getDescription());
        ansiPrintf("      @|yellow %s: %s|@\n", failure.getException().getClass().getSimpleName(), indentNewlines(failure.getMessage()));
        Throwable exception = failure.getException();
        Throwable rootCause = Throwables.getRootCause(exception);

        final StackTraceElement lastResponsibleCause = Throwables2.getLastResponsibleCause(exception, configuration.getTestPackageNames());

        if (exception.equals(rootCause)) {
            System.out.printf("             At %s\n", rootCause.getStackTrace()[0]);
        } else {
            System.out.printf("             At %s\n", exception.getStackTrace()[0]);
                   ansiPrintf("               Root cause: @|yellow %s: %s|@\n", rootCause.getClass().getSimpleName(), indentNewlines(rootCause.getMessage()));
            System.out.printf("             At %s\n", rootCause.getStackTrace()[0]);
        }

        if (lastResponsibleCause != null) {
            System.out.printf("        Suspect %s\n\n", lastResponsibleCause);
        }

        System.out.flush();
    }

    private static String indentNewlines(String textWithPossibleNewlines) {

        if (textWithPossibleNewlines == null) {
            return "";
        }

        return textWithPossibleNewlines.replaceAll("\\n", "\n      ");
    }

    private void displayTestMethodPlaceholder(Description description) {
        System.out.print(ansi().saveCursorPosition());
        final StringBuilder thisTestDescription = new StringBuilder();
        thisTestDescription.append(">>  ");
        thisTestDescription.append(description.getTestClass().getSimpleName());
        thisTestDescription.append(".");
        thisTestDescription.append(description.getMethodName());

        for (Plugin plugin : plugins) {
            thisTestDescription.append(plugin.messageDuringTest(description.toString()));
        }

        final StringBuilder overviewDescription = new StringBuilder();
        overviewDescription.append("[ ").append(testRunCount).append("/").append(testTotalCount).append(" tests run");
        if (testIgnoredCount > 0) {
            overviewDescription.append(", @|yellow ").append(testIgnoredCount).append(" ignored|@");
        }
        if (testFailureCount > 0) {
            overviewDescription.append(", @|red ").append(testFailureCount).append(" failed|@");

            // TODO: implement count of new failures
//            int newTestFailureCount = 0;
//            if (newTestFailureCount > 0) {
//                overviewDescription.append("@|bold,red  (0 new)|@");
//            }
        }
        overviewDescription.append(" ] ");

        ansiPrintf(alignLeftRight(thisTestDescription.toString(), overviewDescription.toString()));
        if (verbose) {
            // Add newline so that tee-d stdout/err appear on the line below. In non-verbose mode we omit
            //  the newline so that this placeholder can be erased on completion of the test method.
            System.out.println();
        }
        System.out.flush();
    }

    private void replaceTestMethodPlaceholder(boolean success) {
        long elapsedTime = System.currentTimeMillis() - currentTestStartTime;
        System.out.print(ansi().eraseLine(Ansi.Erase.ALL).restorCursorPosition());
        String colour;
        String symbol;
        if (success) {
            colour = "green";
            symbol = TICK_MARK;
        } else {
            colour = "red";
            symbol = CROSS_MARK;
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(" @|")
                .append(colour)
                .append(" ")
                .append(symbol)
                .append("  ")
                .append(testName(currentDescription, 30))
                .append("|@ @|blue (")
                .append(elapsedTime)
                .append(" ms)|@ ");

        for (Plugin plugin : plugins) {
            sb.append(plugin.messageAfterTest(testName(currentDescription)));
        }

        sb.append("\n");

        ansiPrintf(sb.toString());
    }

    @Override
    public byte[] readOut(Class<?> testClass) throws IOException {
        if (this.stdOutStreamStore.get(testClass) != null) {
            return this.stdOutStreamStore.get(testClass).getBytes();
        } else {
            return new byte[0];
        }
    }

    @Override
    public byte[] readErr(Class<?> testClass) throws IOException {
        if (this.stdErrStreamStore.get(testClass) != null) {
            return this.stdErrStreamStore.get(testClass).getBytes();
        } else {
            return new byte[0];
        }
    }

    private String alignLeftRight(final String leftString, final String rightString) {

        String cleansedLeftString = leftString.replaceAll("@\\|[\\w,]+\\s|\\|@", "");
        String cleansedRightString = rightString.replaceAll("@\\|[\\w,]+\\s|\\|@", "");

        int spaces = 0;
        if (terminalWidth > 0) {
            // Leftover after left and right string together wraps the terminal
            int leftoverFromLeftAndRight = (cleansedLeftString.length() + cleansedRightString.length()) % terminalWidth;
            // Get the number of spaces to fill the rest of the terminal
            spaces = terminalWidth - leftoverFromLeftAndRight;
        }
        return leftString + Strings.repeat(" ", spaces) + rightString;
    }
}
