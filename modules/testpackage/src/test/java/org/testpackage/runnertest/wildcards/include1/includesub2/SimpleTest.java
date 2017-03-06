package org.testpackage.runnertest.wildcards.include1.includesub2;

import org.junit.Test;

/**
 * This is supposed to have the same class name in all 'wildcards' packages.
 */
public class SimpleTest {

    @Test
    public void testTrue2() {
        assert true;
    }

    @Test
    public void testTrue1() {
        assert true;
    }
}
