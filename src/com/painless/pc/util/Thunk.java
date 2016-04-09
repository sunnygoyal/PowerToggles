package com.painless.pc.util;

/**
 * Indicates that the given field or method has package visibility solely to prevent the creation
 * of a synthetic method. In practice, you should treat this field/method as if it were private.
 * <p>
 *
 * When a private method is called from an inner class, the Java compiler generates a simple
 * package private shim method that the class generated from the inner class can call. This results
 * in unnecessary bloat and runtime method call overhead. It also gets us closer to the dex method
 * count limit.
 * <p>
 *
 * If you'd like to see warnings for these synthetic methods in eclipse, turn on:
 * Window > Preferences > Java > Compiler > Errors/Warnings > "Access to a non-accessible member
 * of an enclosing type".
 * <p>
 *
 */
public @interface Thunk { }