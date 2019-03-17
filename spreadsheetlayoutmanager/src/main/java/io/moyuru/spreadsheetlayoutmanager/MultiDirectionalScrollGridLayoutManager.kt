package io.moyuru.spreadsheetlayoutmanager

import androidx.recyclerview.widget.RecyclerView.Recycler

class MultiDirectionalScrollGridLayoutManager(
  columnCount: Int,
  private val columnWidth: Int,
  private val rowHeight: Int
) : AbsMultiDirectionalScrollGridLayoutManager(columnCount) {

  override fun addCell(
    position: Int,
    insertPosition: Int,
    offsetX: Int,
    offsetY: Int,
    direction: Direction,
    recycler: Recycler
  ): Pair<Int, Int> {
    val v = recycler.getViewForPosition(position)
    addView(v, insertPosition)
    measureCell(v, columnWidth, rowHeight)
    val width = getDecoratedMeasuredWidth(v)
    val height = getDecoratedMeasuredHeight(v)
    val left = if (direction == Direction.LEFT) offsetX - width else offsetX
    val top = if (direction == Direction.TOP) offsetY - height else offsetY
    layoutDecorated(v, left, top, left + width, top + height)
    return Pair(width, height)
  }
}