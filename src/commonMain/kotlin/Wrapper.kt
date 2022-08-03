package com.tomuvak.testing.gc

/**
 * Can be used as a "proper object" (that can be the target of a [com.tomuvak.weakReference.WeakReference]) when wanting
 * a [value] of a type [T] which cannot be weak-referenced.
 */
class Wrapper<T>(val value: T)
