package com.rafambn.kmap.style.expression

import com.rafambn.kmap.style.SpriteImage
import com.rafambn.kmap.style.evaluation.EvaluationContext
import com.rafambn.kmap.style.evaluation.ExpressionEvaluator

internal fun evaluateImage(expression: List<*>, context: EvaluationContext, evaluator: ExpressionEvaluator): SpriteImage? {
    if (expression.size != 2) return null
    val name = evaluator.evaluate(expression[1], context) as? String ?: return null
    return context.sprites[name]
}
