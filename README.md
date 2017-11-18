# Simple Composition
This projects provides an annotation processor to provide
simple composition / mixins / delegates using java 8 default interface

# The idea
The idea using default interfaces in oder to implement mixings was inspired by
http://hannesdorfmann.com/android/java-mixins

Interfaces containing default methods linking / delegating the
to an instance hosted inside the interface it self.
This allows to integrate multiple functional objects (mixins) into
one single class.

# How it is done
Quite simple just create one class which will be enriched by
mixed in functionality and one or more classes to
encapsulate these kind of functionality

## Simple Sample
```Java
public class SideObject1 {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

@Include(SideObject1.class)
public class MainObject implements SideObject1Composition {
    public void printName() {
        System.out.println(getName());
    }
}
```
This will expose the functionality of *SideObject1* into *MainObject*
by using the ```@Include``` annotation and implement the generated interface
*\<ObjectName\>Composition*