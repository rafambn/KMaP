package com.rafambn.kmap.core

import com.rafambn.kmap.utils.Reference

interface MoveInterface {
    fun positionTo(center: Reference)
    fun positionBy(center: Reference)
    fun zoomTo(zoom: Float)
    fun zoomBy(zoom: Float)
    fun zoomToCentered(zoom: Float, center: Reference)
    fun zoomByCentered(zoom: Float, center: Reference)
    fun rotateTo(degrees: Double)
    fun rotateBy(degrees: Double)
    fun rotateToCentered(degrees: Double, center: Reference)
    fun rotateByCentered(degrees: Double, center: Reference)
}