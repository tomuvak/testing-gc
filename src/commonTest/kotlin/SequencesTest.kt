package com.tomuvak.testing.gc

import com.tomuvak.testing.coroutines.asyncTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class SequencesTest {
    @Test fun generateSequenceAndWeakReferencesGeneratesSequenceAndWeakReferences() {
        val objects = List(3) { Any() }
        val (sequence, references) = generateSequenceAndWeakReferences(3) { objects[it] }
        for ((index, element) in sequence.withIndex()) {
            assertSame(objects[index], element)
            assertSame(element, assertNotNull(references[index].targetOrNull))
        }
    }

    @Test fun dismissNextReclaimsTargetsGeneratedByGenerateSequenceAndWeakReferences() = asyncTest {
        val (sequence, references) = generateSequenceAndWeakReferences(3) { Any() }
        val iterator = sequence.iterator()
        repeat(3) { references[it].assertTargetOnlyReclaimableAfter(iterator::dismissNext) }
    }
}
