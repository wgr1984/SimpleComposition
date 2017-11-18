package de.wr.simplecomposition;

import de.wr.libsimplecomposition.Include;
import de.wr.simplecomposition.SideObject1Composition;
import de.wr.simplecomposition.inner.InnerObject;
import de.wr.simplecomposition.inner.InnerObjectComposition;

@Include({SideObject1.class, InnerObject.class})
public class MainObject implements SideObject1Composition, InnerObjectComposition {
    public void printName() {
        System.out.println(getName());
    }
}
