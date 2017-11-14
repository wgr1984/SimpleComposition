package de.wr.simplecomposition;

/**
 * Created by wolfgangreithmeier on 12.11.17.
 */

public class TestComp1 implements de.wr.simplecomposition.ITestComp1 {
    public TestComp1(int test) {
        this.test = test;
    }

    public int getTest() {
        return test;
    }

    public void setTest(int test) {
        this.test = test;
    }

    private int test;
}
