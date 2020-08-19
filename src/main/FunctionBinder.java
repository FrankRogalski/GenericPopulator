package main;

import java.lang.invoke.MethodHandle;

public class FunctionBinder {
    private MethodHandle getter;
    private MethodHandle setter;

    public MethodHandle getGetter() {
        return getter;
    }

    public void setGetter(final MethodHandle getter) {
        this.getter = getter;
    }

    public MethodHandle getSetter() {
        return setter;
    }

    public void setSetter(final MethodHandle setter) {
        this.setter = setter;
    }
}
