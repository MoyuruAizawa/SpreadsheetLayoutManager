package io.moyuru.spreadsheetlayoutmanager

import androidx.recyclerview.widget.RecyclerView

class SpreadsheetLayoutManager(
  columnCount: Int,
  private val columnWidthLookUp: (columnNumber: Int) -> Int,
  private val rowHeightLookUp: (rowNumber: Int) -> Int
) : AbsMultiDirectionalScrollGridLayoutManager(columnCount) {

  private val columnWidthList = ArrayList<Int>()
  private val rowHeightList = ArrayList<Int>()

  override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
    columnWidthList.clear()
    rowHeightList.clear()
    (0 until columnCount).forEach { columnWidthList.add(columnWidthLookUp(it)) }
    (0 until itemCount step columnCount).forEach { rowHeightList.add(rowHeightLookUp(it / columnCount)) }
    super.onLayoutChildren(recycler, state)
  }

  override fun addCell(
    position: Int,
    insertPosition: Int,
    offsetX: Int,
    offsetY: Int,
    direction: Direction,
    recycler: RecyclerView.Recycler
  ): Pair<Int, Int> {
    val v = recycler.getViewForPosition(position)
    val columnNumber = position % columnCount
    val rowNumber = position / columnCount
    addView(v, insertPosition)
    measureCell(v, columnWidthList[columnNumber], rowHeightList[rowNumber])
    val width = getDecoratedMeasuredWidth(v)
    val height = getDecoratedMeasuredHeight(v)
    val left = if (direction == Direction.LEFT) offsetX - width else offsetX
    val top = if (direction == Direction.TOP) offsetY - height else offsetY
    layoutDecorated(v, left, top, left + width, top + height)
    return Pair(width, height)
  }
}