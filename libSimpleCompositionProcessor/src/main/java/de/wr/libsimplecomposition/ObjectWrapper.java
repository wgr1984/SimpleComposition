package de.wr.libsimplecomposition;

/**
 * Created by wolfgangreithmeier on 14.11.17.
 */

public class ObjectWrapper<T> {
    private T obj;

    public T get() {
        return obj;
    }

    public void init(T obj) {
        this.obj = obj;
    }

    public ObjectWrapper() {
        this.obj = null;
    }
}
