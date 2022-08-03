package com.tomuvak.testing.gc

import com.tomuvak.testing.coroutines.asyncTest
import com.tomuvak.weakReference.WeakReference
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeakReferencesTest {
    private val objectHeldOnTo = Any()
    private val stableReference = WeakReference(objectHeldOnTo)

    @Test fun targetIsReclaimable() = asyncTest { assertTrue(generateFleetingReference().targetIsReclaimable()) }
    @Test fun targetIsNotReclaimable() = asyncTest { assertFalse(stableReference.targetIsReclaimable()) }

    @Test fun assertTargetReclaimableSucceedsWhenTargetReclaimable() =
        asyncTest { generateFleetingReference().assertTargetReclaimable() }
    @Test fun assertTargetReclaimableFailsWhenTargetNotReclaimable() = asyncTest {
        thenFails { stableReference.assertTargetReclaimable() }
    }

    @Test fun assertTargetNotReclaimableSucceedsWhenTargetNotReclaimable() =
        asyncTest { stableReference.assertTargetNotReclaimable() }
    @Test fun assertTargetNotReclaimableFailsWhenTargetReclaimable() = asyncTest {
        thenFails { generateFleetingReference().assertTargetNotReclaimable() }
    }

    @Test fun assertTargetOnlyReclaimableAfterFailsWhenTargetReclaimableBefore() = asyncTest {
        var blockHasBeenExecuted = false
        thenFails { generateFleetingReference().assertTargetOnlyReclaimableAfter { blockHasBeenExecuted = true } }
        assertFalse(blockHasBeenExecuted)
    }
    @Test fun assertTargetOnlyReclaimableAfterFailsWhenTargetNotReclaimableAfter() = asyncTest {
        var blockHasBeenExecuted = false
        thenFails { stableReference.assertTargetOnlyReclaimableAfter { blockHasBeenExecuted = true } }
        assertTrue(blockHasBeenExecuted)
    }
    @Test fun assertTargetOnlyReclaimableAfterSucceedsWhenTargetOnlyReclaimableAfter() = asyncTest {
        val (sequence, references) = generateSequenceAndWeakReferences(3) { Any() }
        val iterator = sequence.iterator()
        repeat(3) { references[it].assertTargetOnlyReclaimableAfter(iterator::dismissNext) }
    }

    @Test fun assertAllTargetsReclaimableSucceedsWhenAllTargetsReclaimable() =
        asyncTest { List(3) { generateFleetingReference() }.assertAllTargetsReclaimable() }
    @Test fun assertAllTargetsReclaimableFailsWhenOneTargetNotReclaimable() = asyncTest { thenFails {
        listOf(
            generateFleetingReference(),
            stableReference,
            generateFleetingReference()
        ).assertAllTargetsReclaimable()
    } }

    @Test fun assertNoTargetReclaimableSucceedsWhenNoTargetReclaimable() =
        asyncTest { List(3) { stableReference }.assertNoTargetReclaimable() }
    @Test fun assertNoTargetReclaimableFailsWhenOneTargetReclaimable() = asyncTest { thenFails {
        listOf(
            stableReference,
            generateFleetingReference(),
            stableReference
        ).assertNoTargetReclaimable()
    } }

    @Test fun assertTargetsOnlyReclaimableAfterFailsWhenOneTargetReclaimableBefore() = asyncTest {
        var blockHasBeenExecuted = false
        thenFails {
            listOf(
                stableReference,
                generateFleetingReference(),
                stableReference
            ).assertTargetsOnlyReclaimableAfter { blockHasBeenExecuted = true }
        }
        assertFalse(blockHasBeenExecuted)
    }
    @Test fun assertTargetsOnlyReclaimableAfterFailsWhenOneTargetNotReclaimableAfter() = asyncTest {
        var blockHasBeenExecuted = false
        val (sequence, references) = generateSequenceAndWeakReferences(2) { Any() }
        val iterator = sequence.iterator()
        thenFails {
            listOf(
                references[0],
                stableReference,
                references[1]
            ).assertTargetsOnlyReclaimableAfter {
                repeat(2) { iterator.dismissNext() }
                blockHasBeenExecuted = true
            }
        }
        assertTrue(blockHasBeenExecuted)
    }
    @Test fun assertTargetsOnlyReclaimableAfterSucceedsWhenTargetsOnlyReclaimableAfter() = asyncTest {
        val (sequence, references) = generateSequenceAndWeakReferences(3) { Any() }
        val iterator = sequence.iterator()
        references.assertTargetsOnlyReclaimableAfter { repeat(3) { iterator.dismissNext() } }
    }

    private fun generateFleetingReference(): WeakReference<Any> = WeakReference(Any())

    // A replacement for assertFailsWith<AssertionError>, as the latter currently (1.7.10) results in an error in JS
    // in the (coroutine-based) tests above: CoroutinesInternalError: Fatal exception in coroutines machinery for
    // CancellableContinuation(DispatchedContinuation[NodeDispatcher@1, [object Object]])
    private suspend fun thenFails(block: suspend () -> Unit) {
        var hasFailedCorrectly = false
        try { block() } catch (_: AssertionError) { hasFailedCorrectly = true }
        assertTrue(hasFailedCorrectly)
    }
}
