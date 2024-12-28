package com.rafambn.kmap.core

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import com.rafambn.kmap.utils.Reference

interface AnimateInterface {
    suspend fun positionTo(center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun positionBy(center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomTo(zoom: Float, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomBy(zoom: Float, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomToCentered(zoom: Float, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomByCentered(zoom: Float, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateTo(degrees: Double, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateBy(degrees: Double, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateToCentered(degrees: Double, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateByCentered(degrees: Double, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
}