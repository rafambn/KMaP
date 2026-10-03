package com.rafambn.kmap.gesture.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import de.infix.testBalloon.framework.core.testSuite
import kotlin.test.assertFalse
import kotlin.test.assertTrue

val PathTesterTest by testSuite {
    test("quadratic and cubic outlines include points between their endpoints") {
        val path = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(50f, 100f, 100f, 0f)
            moveTo(200f, 0f)
            cubicTo(200f, 100f, 300f, 100f, 300f, 0f)
        }
        val tester = PathTester(path, 1f, false)
        assertTrue(tester.checkHit(Offset(51f, 51f)))
        assertTrue(tester.checkHit(Offset(251f, 76f)))
        assertFalse(tester.checkHit(Offset(51f, 81f)))
        assertFalse(tester.checkHit(Offset(151f, 1f)))
    }

    test("closed edges are included without connecting separate contours") {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(100f, 0f)
            lineTo(100f, 100f)
            close()
            moveTo(200f, 200f)
            lineTo(250f, 200f)
        }
        val tester = PathTester(path, 2f, false)
        assertTrue(tester.checkHit(Offset(52f, 52f)))
        assertTrue(tester.checkHit(Offset(227f, 202f)))
        assertFalse(tester.checkHit(Offset(152f, 152f)))
    }

    test("conic curves retain their outline with negative path coordinates") {
        val path = Path().apply { addOval(Rect(-100f, -100f, 0f, 0f)) }
        val tester = PathTester(path, 1f, false)
        assertTrue(tester.checkHit(Offset(101f, 51f)))
        assertTrue(tester.checkHit(Offset(86.35534f, 86.35534f)))
        assertFalse(tester.checkHit(Offset(51f, 51f)))
    }

    test("fill testing respects holes and can be disabled") {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, 100f, 100f))
            addRect(Rect(25f, 25f, 75f, 75f))
        }
        val tester = PathTester(path, 1f, true)
        assertTrue(tester.checkHit(Offset(11f, 11f)))
        assertFalse(tester.checkHit(Offset(51f, 51f)))
        assertTrue(tester.checkHit(Offset(26f, 51f)))
        assertFalse(PathTester(path, 1f, false).checkHit(Offset(11f, 11f)))
    }

    test("an empty path never reports an outline hit") {
        assertFalse(PathTester(Path(), 10f, false).checkHit(Offset(10f, 10f)))
    }
}
