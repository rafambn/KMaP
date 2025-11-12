package com.rafambn.kmap.mapSource.tiled.tiles

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TileTest {

    private class TestTile(zoom: Int, row: Int, col: Int) : Tile(zoom, row, col)

    @Test
    fun `isParentOf returns true for a direct child`() {
        val parent = TestTile(10, 5, 5)
        val child = TestTile(11, 10, 10)
        assertTrue(parent.isParentOf(child))
    }

    @Test
    fun `isParentOf returns true for another direct child`() {
        val parent = TestTile(10, 5, 5)
        val child = TestTile(11, 11, 11)
        assertTrue(parent.isParentOf(child))
    }

    @Test
    fun `isParentOf returns true for a grandchild`() {
        val parent = TestTile(0, 0, 0)
        val child = TestTile(2, 0, 0)
        assertTrue(parent.isParentOf(child))
    }

    @Test
    fun `isParentOf returns true for another grandchild`() {
        val parent = TestTile(10, 5, 5)
        val child = TestTile(12, 23, 23)
        assertTrue(parent.isParentOf(child))
    }

    @Test
    fun `isParentOf returns false for a tile with the same zoom`() {
        val tile1 = TestTile(10, 5, 5)
        val tile2 = TestTile(10, 5, 6)
        assertFalse(tile1.isParentOf(tile2))
    }

    @Test
    fun `isParentOf returns false for a tile with a smaller zoom`() {
        val parentCandidate = TestTile(11, 10, 10)
        val childCandidate = TestTile(10, 5, 5)
        assertFalse(parentCandidate.isParentOf(childCandidate))
    }

    @Test
    fun `isParentOf returns false for a tile that is not a descendant`() {
        val parent = TestTile(10, 5, 5)
        val notAChild = TestTile(11, 12, 10)
        assertFalse(parent.isParentOf(notAChild))
    }

    @Test
    fun `isParentOf returns false for another tile that is not a descendant`() {
        val parent = TestTile(10, 5, 5)
        val notAChild = TestTile(12, 19, 20)
        assertFalse(parent.isParentOf(notAChild))
    }

    @Test
    fun `isChildOf returns true for a direct parent`() {
        val child = TestTile(11, 10, 10)
        val parent = TestTile(10, 5, 5)
        assertTrue(child.isChildOf(parent))
    }

    @Test
    fun `isChildOf returns true for another direct parent`() {
        val child = TestTile(11, 11, 11)
        val parent = TestTile(10, 5, 5)
        assertTrue(child.isChildOf(parent))
    }

    @Test
    fun `isChildOf returns true for a grandparent`() {
        val child = TestTile(2, 0, 0)
        val parent = TestTile(0, 0, 0)
        assertTrue(child.isChildOf(parent))
    }

    @Test
    fun `isChildOf returns true for another grandparent`() {
        val child = TestTile(12, 23, 23)
        val parent = TestTile(10, 5, 5)
        assertTrue(child.isChildOf(parent))
    }

    @Test
    fun `isChildOf returns false for a tile with the same zoom`() {
        val tile1 = TestTile(10, 5, 5)
        val tile2 = TestTile(10, 5, 6)
        assertFalse(tile1.isChildOf(tile2))
    }

    @Test
    fun `isChildOf returns false for a tile with a larger zoom`() {
        val childCandidate = TestTile(10, 5, 5)
        val parentCandidate = TestTile(11, 10, 10)
        assertFalse(childCandidate.isChildOf(parentCandidate))
    }

    @Test
    fun `isChildOf returns false for a tile that is not an ancestor`() {
        val child = TestTile(11, 12, 10)
        val notAParent = TestTile(10, 5, 5)
        assertFalse(child.isChildOf(notAParent))
    }

    @Test
    fun `isChildOf returns false for another tile that is not an ancestor`() {
        val child = TestTile(12, 19, 20)
        val notAParent = TestTile(10, 5, 5)
        assertFalse(child.isChildOf(notAParent))
    }
}
