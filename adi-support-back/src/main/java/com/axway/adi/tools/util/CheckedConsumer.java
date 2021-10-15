package com.axway.adi.tools.util;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception> {
    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    void accept(T t) throws E;
}
