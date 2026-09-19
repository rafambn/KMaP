This document analyzes the text styling properties from the Mapbox Style Specification and assesses the feasibility of their implementation in Jetpack Compose for the KMaP project.

---

### Text Property Analysis

**Property: `text-font`**
- **Use**: Specifies the font family or a list of fallback font families to use for the text.
- **Compose Implementation Feasibility**: **Possible but Complex**. Requires a font management system where fonts are bundled as assets and loaded at runtime. Compose's `TextStyle` accepts a `FontFamily`, but the fonts must be programmatically available.

**Property: `text-optional`**
- **Use**: A rule for collision detection. If true, the text label is not displayed if it collides with other previously rendered symbols.
- **Compose Implementation Feasibility**: **Complex**. This requires a project-wide collision detection system that tracks the screen-space bounding boxes of all rendered symbols. It's not a simple drawing operation but a stateful layout system.

**Property: `text-padding`**
- **Use**: Defines a padding around the text's bounding box, which is used for collision detection.
- **Compose Implementation Feasibility**: **Complex**. This is part of the collision detection system. The padding value would be used to increase the size of the bounding box before checking for overlaps.

**Property: `text-overlap` / `text-allow-overlap`**
- **Use**: If true, the text label is displayed even if it collides with other symbols.
- **Compose Implementation Feasibility**: **Complex**. This is a flag for the collision detection system, dictating whether a given label should ignore the results of collision checks.

**Property: `text-ignore-placement`**
- **Use**: If true, the text is drawn even if it would be clipped by the boundaries of its tile.
- **Compose Implementation Feasibility**: **Possible**. This would require modifying the drawing logic to conditionally disable the canvas clipping that is currently applied for each tile.

**Property: `text-max-angle`**
- **Use**: When text is placed along a line (`symbol-placement: line`), this property sets the maximum angle allowed between consecutive characters.
- **Compose Implementation Feasibility**: **Very Complex**. This requires laying out and rendering text character by character along a path, which is not supported out-of-the-box by Compose's text APIs. It would necessitate a custom text-on-a-path rendering engine.
