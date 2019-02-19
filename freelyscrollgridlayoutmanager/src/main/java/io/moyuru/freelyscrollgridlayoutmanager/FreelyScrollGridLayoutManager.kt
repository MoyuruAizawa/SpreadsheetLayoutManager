package io.moyuru.freelyscrollgridlayoutmanager

import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
import kotlin.math.max
import kotlin.math.min

class FreelyScrollGridLayoutManager(
  private val columnCount: Int,
  private val columnWidth: Int,
  private val rowHeight: Int
) : RecyclerView.LayoutManager() {

  private class Anchor {
    var topLeft = NO_POSITION
    var bottomLeft = NO_POSITION
    var topRight = NO_POSITION
    var bottomRight = NO_POSITION

    fun reset() {
      topLeft = NO_POSITION
      bottomLeft = NO_POSITION
      topRight = NO_POSITION
      bottomRight = NO_POSITION
    }
  }

  private enum class Direction {
    LEFT, TOP, RIGHT, BOTTOM
  }

  private val parentLeft get() = paddingLeft
  private val parentTop get() = paddingTop
  private val parentRight get() = width - paddingRight
  private val parentBottom get() = height - paddingBottom
  private val visibleColumnCount get() = anchor.topRight - anchor.topLeft + 1

  private var anchor = Anchor()
  private var savedState: SavedState? = null

  override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
    return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
  }

  override fun onSaveInstanceState(): Parcelable? {
    val topLeft = findViewByPosition(anchor.topLeft) ?: return null
    val left = topLeft.let(::getDecoratedLeft)
    val top = topLeft.let(::getDecoratedTop)
    return SavedState(anchor.topLeft, left, top)
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    savedState = (state as? SavedState)
  }

  override fun onLayoutChildren(recycler: Recycler, state: State) {
    if (itemCount == 0) {
      anchor.reset()
      detachAndScrapAttachedViews(recycler)
      return
    }

    val topLeft = findViewByPosition(anchor.topLeft)
    val restoredFirstPosition = savedState?.position ?: anchor.topLeft
    val restoredOffsetX = savedState?.left ?: topLeft?.let(::getDecoratedLeft)
    val restoredOffsetY = savedState?.top ?: topLeft?.let(::getDecoratedTop)

    detachAndScrapAttachedViews(recycler)
    anchor.reset()

    if (restoredFirstPosition != NO_POSITION && restoredOffsetX != null && restoredOffsetY != null) {
      anchor.topLeft = restoredFirstPosition
      fillVerticalChunk(restoredFirstPosition, restoredOffsetX, restoredOffsetY, true, recycler)
    } else {
      anchor.topLeft = 0
      fillVerticalChunk(0, parentLeft, parentTop, true, recycler)
    }
  }

  override fun onLayoutCompleted(state: State?) {
    savedState = null
  }

  override fun findViewByPosition(position: Int): View? {
    if (position < anchor.topLeft || position > anchor.bottomRight) return null
    return super.findViewByPosition(position)
  }

  override fun canScrollVertically() = true

  override fun canScrollHorizontally() = true

  override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
    if (dy == 0) return 0

    val topLeftItem = findViewByPosition(anchor.topLeft) ?: return 0
    val bottomLeftItem = findViewByPosition(anchor.bottomLeft) ?: return 0

    val scrollAmount = calcVerticallyScrollAmount(topLeftItem, bottomLeftItem, dy)
    offsetChildrenVertical(-scrollAmount)

    if (dy > 0) {
      val bottom = getDecoratedBottom(bottomLeftItem)
      val position = getBelowCell(anchor.bottomLeft)
      if (bottom <= parentBottom && position < itemCount)
        fillVerticalChunk(position, getDecoratedLeft(topLeftItem), bottom, true, recycler)

      if (anchor.bottomLeft.isLastInColumn) fixVerticalLayoutGap(recycler)

      recycleTopRows(recycler)
    } else {
      val top = getDecoratedTop(topLeftItem)
      val position = getAboveCell(anchor.topLeft)
      if (top >= parentTop && position >= 0)
        fillVerticalChunk(position, getDecoratedLeft(topLeftItem), getDecoratedTop(topLeftItem), false, recycler)

      if (anchor.topLeft.isFirstInColumn) fixVerticalLayoutGap(recycler)

      recycleBottomRows(recycler)
    }

    return scrollAmount
  }

  override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: State): Int {
    if (dx == 0) return 0

    val topLeftItem = findViewByPosition(anchor.topLeft) ?: return 0
    val topRightItem = findViewByPosition(anchor.topRight) ?: return 0

    val scrollAmount = calcHorizontallyScrollAmount(topLeftItem, topRightItem, dx)
    offsetChildrenHorizontal(-scrollAmount)

    if (dx > 0) {
      val right = getDecoratedRight(topRightItem)
      val nextPosition = anchor.topRight + 1
      if (right < parentRight && !anchor.topRight.isLastInRow && nextPosition < itemCount)
        fillHorizontalChunk(nextPosition, right, getDecoratedTop(topLeftItem), true, recycler)

      if (anchor.topRight.isLastInRow) fixHorizontalLayoutGap(recycler)

      recycleLeftColumns(recycler)
    } else {
      val left = getDecoratedLeft(topLeftItem)
      val previousPosition = anchor.topLeft - 1
      if (left >= parentLeft && !anchor.topLeft.isFirstInRow && previousPosition >= 0)
        fillHorizontalChunk(previousPosition, left, getDecoratedTop(topLeftItem), false, recycler)

      if (anchor.topLeft.isFirstInRow) fixHorizontalLayoutGap(recycler)

      recycleRightColumns(recycler)
    }

    return scrollAmount
  }

  private fun calcVerticallyScrollAmount(topLeftItem: View, bottomLeftItem: View, dy: Int): Int {
    if (dy == 0) return 0

    return if (dy > 0) { // up swipe
      val bottom = getDecoratedBottom(bottomLeftItem)
      if (anchor.bottomLeft.isLastInColumn) if (bottom <= parentBottom) 0 else min(dy, bottom - parentBottom)
      else dy
    } else {
      val top = getDecoratedTop(topLeftItem)
      if (anchor.topLeft.isFirstInColumn) if (top >= parentTop) 0 else max(dy, -(parentTop - top))
      else dy
    }
  }

  private fun calcHorizontallyScrollAmount(topLeftItem: View, topRightItem: View, dx: Int): Int {
    if (dx == 0) return 0

    return if (dx > 0) { // left swipe
      val right = getDecoratedRight(topRightItem)
      if (anchor.topRight.isLastInRow) if (right <= parentRight) 0 else min(dx, right - parentRight)
      else dx
    } else {
      val left = getDecoratedLeft(topLeftItem)
      if (anchor.topLeft.isFirstInRow) if (left >= parentLeft) 0 else max(dx, -(parentLeft - left))
      else dx
    }
  }

  private fun fixVerticalLayoutGap(recycler: Recycler) {
    val topLeftItem = findViewByPosition(anchor.topLeft) ?: return
    val bottomLeftItem = findViewByPosition(anchor.bottomLeft) ?: return

    val top = getDecoratedTop(topLeftItem)
    val bottom = getDecoratedBottom(bottomLeftItem)
    val left = getDecoratedLeft(topLeftItem)

    if (top > parentTop) {
      val gap = top - parentTop
      offsetChildrenVertical(-gap)

      if (bottom - gap < parentBottom)
        fillVerticalChunk(getBelowCell(anchor.bottomLeft), left, bottom - gap, true, recycler)
    }

    if (bottom < parentBottom) {
      val gap = parentBottom - bottom
      offsetChildrenVertical(gap)

      if (top + gap > parentTop)
        fillVerticalChunk(getAboveCell(anchor.topLeft), left, top + gap, false, recycler)
    }
  }

  private fun fixHorizontalLayoutGap(recycler: Recycler) {
    val topLeftItem = findViewByPosition(anchor.topLeft) ?: return
    val topRightItem = findViewByPosition(anchor.topRight) ?: return

    val left = getDecoratedLeft(topLeftItem)
    val right = getDecoratedRight(topRightItem)
    val top = getDecoratedTop(topLeftItem)

    if (left > parentLeft) {
      val gap = left - parentLeft
      offsetChildrenHorizontal(-gap)

      if (right - gap < parentRight)
        fillHorizontalChunk(anchor.topRight + 1, right - gap, top, true, recycler)
    }

    if (right < parentRight) {
      val gap = parentRight - right
      offsetChildrenHorizontal(gap)

      if (left + gap > parentLeft)
        fillHorizontalChunk(anchor.topLeft - 1, left + gap, top, false, recycler)
    }
  }

  private fun fillVerticalChunk(from: Int, offsetX: Int, startY: Int, isAppend: Boolean, recycler: Recycler) {
    var offsetY = startY
    val progression = if (isAppend) from until itemCount step columnCount else from downTo 0 step columnCount
    for (position in progression) {
      val rowHeight = fillRow(position, offsetX, offsetY, isAppend, recycler)
      offsetY += if (isAppend) rowHeight else -rowHeight
      if (if (isAppend) offsetY > parentBottom else offsetY < parentTop) break
    }
  }

  private fun fillHorizontalChunk(from: Int, startX: Int, offsetY: Int, isAppend: Boolean, recycler: Recycler) {
    var offsetX = startX
    val range = if (isAppend) from..getLastCellInSameRow(from) else from downTo getFirstCellInSameRow(from)
    for (position in range) {
      val columnWidth = fillColumn(position, offsetX, offsetY, isAppend, recycler)
      offsetX += if (isAppend) columnWidth else -columnWidth
      if (if (isAppend) offsetX > parentRight else offsetX < parentLeft) break
    }
  }

  private fun fillRow(from: Int, startX: Int, startY: Int, isAppend: Boolean, recycler: Recycler): Int {
    if (isAppend) anchor.bottomLeft = from else anchor.topLeft = from
    val direction = if (isAppend) Direction.BOTTOM else Direction.TOP

    var offsetX = startX
    var rowHeight = 0
    var addPosition = if (isAppend) -1 else 0
    for (position in from..getLastCellInSameRow(from)) {
      val (width, height) = addCell(position, addPosition, offsetX, startY, direction, recycler)

      if (from == anchor.topLeft && isAppend) anchor.topRight = position
      if (isAppend) anchor.bottomRight = position else anchor.topRight = position
      rowHeight = max(rowHeight, height)
      offsetX += width
      if (offsetX > parentRight) break
      if (!isAppend) addPosition++
    }
    return rowHeight
  }

  private fun fillColumn(from: Int, startX: Int, startY: Int, isAppend: Boolean, recycler: Recycler): Int {
    if (isAppend) anchor.topRight = from else anchor.topLeft = from
    val direction = if (isAppend) Direction.RIGHT else Direction.LEFT

    var offsetY = startY
    var columnWidth = 0
    var addPosition = 0
    var position = from
    while (position < itemCount && offsetY <= parentBottom) {
      val (width, height) = addCell(position, addPosition, startX, offsetY, direction, recycler)

      if (isAppend) anchor.bottomRight = position else anchor.bottomLeft = position
      columnWidth = max(columnWidth, width)
      offsetY += height
      addPosition += visibleColumnCount + 1
      position = getBelowCell(position)
    }
    return columnWidth
  }

  private fun addCell(
    position: Int,
    insertPosition: Int,
    offsetX: Int,
    offsetY: Int,
    direction: Direction,
    recycler: Recycler
  ): Pair<Int, Int> {
    val v = recycler.getViewForPosition(position)
    addView(v, insertPosition)
    measureCell(v)
    val width = getDecoratedMeasuredWidth(v)
    val height = getDecoratedMeasuredHeight(v)
    val left = if (direction == Direction.LEFT) offsetX - width else offsetX
    val top = if (direction == Direction.TOP) offsetY - height else offsetY
    layoutDecorated(v, left, top, left + width, top + height)
    return Pair(width, height)
  }

  private fun recycleTopRows(recycler: Recycler) {
    for (i in anchor.topLeft..anchor.bottomLeft step columnCount) {
      if (getDecoratedBottom(findViewByPosition(anchor.topLeft) ?: return) > parentTop) return

      recycleRow(anchor.topLeft, anchor.topRight, recycler)
    }
  }

  private fun recycleBottomRows(recycler: Recycler) {
    for (i in anchor.bottomLeft downTo anchor.topLeft step columnCount) {
      if (getDecoratedTop(findViewByPosition(anchor.bottomLeft) ?: return) < parentBottom) return

      recycleRow(anchor.bottomLeft, anchor.bottomRight, recycler)
    }
  }

  private fun recycleLeftColumns(recycler: Recycler) {
    for (i in anchor.topLeft..anchor.topRight) {
      val view = findViewByPosition(anchor.topLeft) ?: return
      if (getDecoratedRight(view) > parentLeft) return

      recycleColumn(anchor.topLeft, anchor.bottomLeft, recycler)
    }
  }

  private fun recycleRightColumns(recycler: Recycler) {
    for (i in anchor.topRight downTo anchor.topLeft) {
      val view = findViewByPosition(anchor.topRight) ?: return
      if (getDecoratedLeft(view) < parentRight) return

      recycleColumn(anchor.topRight, anchor.bottomRight, recycler)
    }
  }

  private fun recycleRow(from: Int, to: Int, recycler: Recycler) {
    (from..to).forEach { recycleCell(it, recycler) }

    if (from == anchor.topLeft) {
      anchor.topLeft = getBelowCell(anchor.topLeft)
      anchor.topRight = getBelowCell(anchor.topRight)
    }
    if (from == anchor.bottomLeft) {
      anchor.bottomLeft = getAboveCell(anchor.bottomLeft)
      anchor.bottomRight = getAboveCell(anchor.bottomRight)
    }
  }

  private fun recycleColumn(from: Int, to: Int, recycler: Recycler) {
    (from..to step columnCount).forEach { recycleCell(it, recycler) }

    if (from == anchor.topLeft) {
      anchor.topLeft++
      anchor.bottomLeft++
    }
    if (from == anchor.topRight) {
      anchor.topRight--
      anchor.bottomRight--
    }
  }

  private fun recycleCell(position: Int, recycler: Recycler) {
    findViewByPosition(position)?.let { removeAndRecycleView(it, recycler) }
  }

  private fun measureCell(view: View) {
    val lp = view.layoutParams as RecyclerView.LayoutParams
    lp.width = columnWidth
    lp.height = rowHeight

    val insets = Rect().apply { calculateItemDecorationsForChild(view, this) }
    val widthSpec = getChildMeasureSpec(
      width,
      widthMode,
      paddingLeft + paddingRight + insets.left + insets.right,
      lp.width,
      true
    )
    val heightSpec = getChildMeasureSpec(
      height,
      heightMode,
      paddingTop + paddingBottom + insets.top + insets.bottom,
      lp.height,
      true
    )
    view.measure(widthSpec, heightSpec)
  }

  private fun getAboveCell(position: Int) = position - columnCount

  private fun getBelowCell(position: Int) = position + columnCount

  private fun getFirstCellInSameRow(position: Int) = position - position % columnCount

  private fun getLastCellInSameRow(position: Int) = position + columnCount - (position % columnCount + 1)

  private val Int.isFirstInRow get() = this % columnCount == 0
  private val Int.isLastInRow get() = this % columnCount == columnCount - 1
  private val Int.isFirstInColumn get() = this < columnCount
  private val Int.isLastInColumn get() = this >= itemCount - columnCount

  private data class SavedState(val position: Int, val left: Int, val top: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
      parcel.readInt(),
      parcel.readInt(),
      parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
      parcel.writeInt(position)
      parcel.writeInt(left)
      parcel.writeInt(top)
    }

    override fun describeContents(): Int {
      return 0
    }

    companion object CREATOR : Parcelable.Creator<SavedState> {
      override fun createFromParcel(parcel: Parcel): SavedState {
        return SavedState(parcel)
      }

      override fun newArray(size: Int): Array<SavedState?> {
        return arrayOfNulls(size)
      }
    }

  }
}