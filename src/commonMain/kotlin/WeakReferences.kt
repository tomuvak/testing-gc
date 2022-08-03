package com.tomuvak.testing.gc

import com.tomuvak.testing.gc.core.tryToAchieveByForcingGc
import com.tomuvak.weakReference.WeakReference
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Executes the given [block], asserting that the target of the receiver weak reference [this] is reclaimable after
 * that, having not been reclaimable prior to that. Garbage collection may be attempted to be triggered in (each part
 * of) the process.
 *
 * Can be used to verify that the given [block] results (among other things) in any (strong) references to the target of
 * the receiver weak reference [this] being forgotten.
 *
 * The part typically needed for the logic of actual tests is asserting reclaimability, and that can be achieved by
 * [assertTargetReclaimable]; asserting prior non-reclaimability usually only serves to help ensure that code behaves as
 * expected and that tests don't pass for the wrong reason.
 */
suspend fun WeakReference<Any>.assertTargetOnlyReclaimableAfter(block: () -> Unit) {
    assertTargetNotReclaimable()
    block()
    assertTargetReclaimable()
}

/**
 * Asserts that the target of the receiver weak reference [this] is not reclaimable (that is, it is not reclaimed when
 * garbage collection has been triggered).
 *
 * Not normally required for the logic of actual tests, but helps ensure that code behaves as expected and that tests
 * don't pass for the wrong reason.
 */
suspend fun WeakReference<Any>.assertTargetNotReclaimable() = assertFalse(targetIsReclaimable())

/**
 * Asserts that the target of the receiver weak reference [this] is reclaimable (that is, it is reclaimed when garbage
 * collection has been triggered, if not before).
 *
 * Can be used to verify that no (strong) references to the target of the receiver weak reference [this] are still being
 * held.
 */
suspend fun WeakReference<Any>.assertTargetReclaimable() = assertTrue(targetIsReclaimable())

/**
 * Returns whether the target of the receiver weak reference [this] is reclaimable (that is, whether it is reclaimed
 * when garbage collection has been triggered, if not before).
 *
 * Can be useful for custom test logic, but for typical use cases [assertTargetReclaimable] (or possibly
 * [assertTargetNotReclaimable] or [assertTargetOnlyReclaimableAfter]) is the more natural choice.
 */
suspend fun WeakReference<Any>.targetIsReclaimable(): Boolean = tryToAchieveByForcingGc { targetOrNull == null }

/**
 * Executes the given [block], asserting that the targets of all weak references in the receiver collection [this] are
 * reclaimable after that, having not been reclaimable prior to that. Garbage collection may be attempted to be
 * triggered in (each part of) the process.
 *
 * Can be used to verify that the given [block] results (among other things) in all (strong) references to the targets
 * of all weak references in the receiver collection [this] being forgotten.
 *
 * The part typically needed for the logic of actual tests is asserting reclaimability, and that can be achieved by
 * [assertAllTargetsReclaimable]; asserting prior non-reclaimability usually only serves to help ensure that code
 * behaves as expected and that tests don't pass for the wrong reason.
 */
suspend fun Collection<WeakReference<Any>>.assertTargetsOnlyReclaimableAfter(block: () -> Unit) {
    assertNoTargetReclaimable()
    block()
    assertAllTargetsReclaimable()
}

/**
 * Asserts that the target of no weak references in the receiver collection [this] is reclaimable (that is, none of the
 * targets are reclaimed when garbage collection has been triggered).
 *
 * Not normally required for the logic of actual tests, but helps ensure that code behaves as expected and that tests
 * don't pass for the wrong reason.
 */
suspend fun Collection<WeakReference<Any>>.assertNoTargetReclaimable() =
    assertFalse(tryToAchieveByForcingGc { any { it.targetOrNull == null } })

/**
 * Asserts that the targets of all weak references in the receiver collection [this] are reclaimable (that is, they are
 * all reclaimed when garbage collection has been triggered, if not before).
 *
 * Can be used to verify that no (strong) references to the target of any of the weak references in the receiver
 * collection [this] are still being held.
 */
suspend fun Collection<WeakReference<Any>>.assertAllTargetsReclaimable() =
    assertTrue(tryToAchieveByForcingGc { all { it.targetOrNull == null } })
