package de.wr.simplecomposition;

import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.Toast;

/**
 * Created by wolfgangreithmeier on 12.11.17.
 */

public class TestComp3 implements View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {
    public int getTest() {
        return test;
    }

    public void setTest(int test) {
        this.test = test;
    }

    private int test;

    public TestComp3(int test, String hallo) {
        this.test = test;
        this.hallo = hallo;
    }

    public String getHallo() {
        return hallo;
    }

    private String hallo;

    @Override
    public void onClick(View v) {
        Toast.makeText(v.getContext(), "HALLO MIXIN !", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRefresh() {

    }
}
