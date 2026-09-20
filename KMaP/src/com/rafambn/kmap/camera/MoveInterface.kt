package com.rafambn.kmap.camera

import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.utils.Reference

interface MoveInterface {
    fun positionTo(center: Reference)
    fun positionBy(center: Reference)
    fun zoomTo(zoom: Float)
    fun zoomBy(zoom: Float)
    fun zoomToCentered(zoom: Float, center: Reference)
    fun zoomByCentered(zoom: Float, center: Reference)
    fun rotateTo(degrees: Degrees)
    fun rotateBy(degrees: Degrees)
    fun rotateToCentered(degrees: Degrees, center: Reference)
    fun rotateByCentered(degrees: Degrees, center: Reference)
}
