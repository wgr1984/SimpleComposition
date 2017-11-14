package de.wr.simplecomposition;

/**
 * Created by wolfgangreithmeier on 12.11.17.
 */

public class TestComp2 implements de.wr.simplecomposition.ITestComp2 {
    public int getTest() {
        return test;
    }

    public void setTest(int test) {
        this.test = test;
    }

    private int test;
}
