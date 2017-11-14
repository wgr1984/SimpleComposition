package de.wr.simplecomposition;

import de.wr.libsimplecomposition.Include;

/**
 * Created by wolfgangreithmeier on 29.10.17.
 */

@Include({TestComp1.class, TestComp2.class, TestComp3.class})
public class TestObject implements de.wr.simplecomposition.TestComp1Composition,
        de.wr.simplecomposition.TestComp2Composition,
        de.wr.simplecomposition.TestComp3Composition
{

    public TestObject() {
        compTestComp1.init(new TestComp1(4));
        compTestComp3.init(new TestComp3(2, "test"));
    }

    @Override
    public int getTest() {
        return compTestComp1.get().getTest() + compTestComp2.getTest();
    }

    @Override
    public void setTest(int test) {
        compTestComp1.get().setTest(test);
        compTestComp2.setTest(test+1);
    }
}
