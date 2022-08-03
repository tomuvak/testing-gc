package com.tomuvak.testing.gc

import com.tomuvak.testing.coroutines.asyncTest
import com.tomuvak.weakReference.WeakReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class WrapperTest {
    @Test fun wrapperKeepsValue() = assertEquals(3, Wrapper(3).value)

    @Test fun wrapperAdmitsWeakReference() {
        val wrapper = Wrapper(3)
        assertSame(wrapper, assertNotNull(WeakReference(wrapper).targetOrNull))
    }

    @Test fun wrapperAsWeakReferenceTargetCanBeReclaimed() = asyncTest {
        fun generateWeakReference(): WeakReference<Any> = WeakReference(Wrapper(3))
        generateWeakReference().assertTargetReclaimable()
    }
}
