package com.rafambn.kmap.style.expression

import androidx.compose.ui.graphics.ImageBitmap
import com.rafambn.kmap.style.EvaluationContext
import com.rafambn.kmap.style.ExpressionEvaluator

internal fun evaluateImage(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): ImageBitmap? {
    if (expression.size != 2) return null
    val name = evaluator.evaluate(expression[1], context) as? String ?: return null
    return context.sprites[name]
}
