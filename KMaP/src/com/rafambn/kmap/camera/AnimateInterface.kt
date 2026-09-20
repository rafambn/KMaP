package com.rafambn.kmap.camera

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import com.rafambn.kmap.geometry.angle.Degrees
import com.rafambn.kmap.utils.Reference

interface AnimateInterface {
    suspend fun positionTo(center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun positionBy(center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomTo(zoom: Float, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomBy(zoom: Float, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomToCentered(zoom: Float, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun zoomByCentered(zoom: Float, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateTo(degrees: Degrees, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateBy(degrees: Degrees, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateToCentered(degrees: Degrees, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
    suspend fun rotateByCentered(degrees: Degrees, center: Reference, animationSpec: AnimationSpec<Float> = SpringSpec())
}
