package io.github.rafambn.kmap


open class DefaultPlacer(
    val coordinates: Position,
    val drawPosition: DrawPosition,
    val groupId: Int,
    val zIndex: Int,
    val isGrouping: Boolean,
    val placerType: MapComponentType
)

class MarkerPlacer(
    coordinates: Position,
    drawPosition: DrawPosition,
    groupId: Int,
    zIndex: Int,
    isGrouping: Boolean,
) : DefaultPlacer(coordinates, drawPosition, groupId, zIndex, isGrouping, MapComponentType.MARKER)

class PathPlacer(
    coordinates: Position,
    drawPosition: DrawPosition,
    groupId: Int,
    zIndex: Int,
    isGrouping: Boolean,
) : DefaultPlacer(coordinates, drawPosition, groupId, zIndex, isGrouping, MapComponentType.PATH)


class DrawPosition {

}
