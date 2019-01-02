package io.moyuru.freelyscrollgridlayoutmanager

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class FreelyScrollGridLayoutManager(
  private val columnCount: Int,
  private val cellWidth: Int,
  private val cellHeight: Int
) : RecyclerView.LayoutManager() {

  constructor(context: Context, columnCount: Int, columnWidthDp: Int, columnHeightDp: Int) : this(
    columnCount,
    (context.resources.displayMetrics.density * columnWidthDp).toInt(),
    (context.resources.displayMetrics.density * columnHeightDp).toInt()
  )

  companion object {
    const val KEY_FIRST_VISIBLE_POSITION = "firstVisible"
    const val KEY_LAST_VISIBLE_POSITION = "lastVisiblePosition"
  }

  private val parentLeft get() = paddingLeft
  private val parentTop get() = paddingTop
  private val parentRight get() = width - paddingRight
  private val parentBottom get() = height - paddingBottom

  private val Int.isFirstInRow get() = this % columnCount == 0
  private val Int.isLastInRow get() = this % columnCount == columnCount - 1
  private val Int.isFirstInColumn get() = this < columnCount
  private val Int.isLastInColumn get() = this >= itemCount - columnCount

  private val visibleColumnCount get() = visibleRightTopPosition - firstVisiblePosition + 1

  private var pendingScrollPosition: Int? = null

  var firstVisiblePosition = 0
    private set(value) {
      field = value
    }
  var lastVisiblePosition = 0
    private set(value) {
      field = value
    }
  private val visibleRightTopPosition
    get() = firstVisiblePosition + lastVisiblePosition % columnCount - firstVisiblePosition % columnCount
  private val visibleLeftBottomPosition
    get() = lastVisiblePosition - lastVisiblePosition % columnCount + firstVisiblePosition % columnCount

  override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
    return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
  }

  override fun onSaveInstanceState() = Bundle().apply {
    putInt(KEY_FIRST_VISIBLE_POSITION, firstVisiblePosition)
    putInt(KEY_LAST_VISIBLE_POSITION, lastVisiblePosition)
  }

  override fun onRestoreInstanceState(state: Parcelable?) {
    (state as? Bundle)?.let {
      firstVisiblePosition = it.getInt(KEY_FIRST_VISIBLE_POSITION)
      lastVisiblePosition = it.getInt(KEY_LAST_VISIBLE_POSITION)
    }
  }

  override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
    if (itemCount == 0) {
      firstVisiblePosition = 0
      lastVisiblePosition = 0
      pendingScrollPosition = null
      detachAndScrapAttachedViews(recycler)
      return
    }

    pendingScrollPosition?.let {
      firstVisiblePosition = it
      pendingScrollPosition = null
    }
    var position = firstVisiblePosition
    val offsetX = parentLeft
    var offsetY = findViewByPosition(position)?.top ?: parentTop
    detachAndScrapAttachedViews(recycler)
    while (position < itemCount && offsetY < parentBottom) {
      offsetY += fillRow(position, offsetX, offsetY, recycler)
      position += columnCount
    }
  }

  override fun findViewByPosition(position: Int): View? {
    if (position < firstVisiblePosition || position > lastVisiblePosition) return null
    val absX = position % columnCount
    if (absX < firstVisiblePosition % columnCount || absX > lastVisiblePosition % columnCount) return null

    return super.findViewByPosition(position)
  }

  override fun scrollToPosition(position: Int) {
    pendingScrollPosition = adjustScrollPosition(position)
    requestLayout()
  }

  override fun canScrollVertically() = true

  override fun canScrollHorizontally() = true

  override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
    if (dy == 0) return 0

    val firstItem = getChildAt(0) ?: return 0
    val lastItem = getChildAt(childCount - 1) ?: return 0
    val scrollAmount = calcVerticallyScrollAmount(firstItem, lastItem, dy)

    offsetChildrenVertical(-scrollAmount)

    val firstTop = getDecoratedTop(firstItem)
    val lastBottom = getDecoratedBottom(lastItem)

    if (dy > 0) {
      val position = getBelowCell(visibleLeftBottomPosition)
      if (lastBottom <= parentBottom && position < itemCount)
        fillRow(position, getDecoratedLeft(firstItem), lastBottom, recycler)
    } else {
      val position = getAboveCell(firstVisiblePosition)
      if (firstTop >= parentTop && position >= 0)
        fillRow(position, getDecoratedLeft(firstItem), getDecoratedTop(firstItem), recycler)
    }

    val gap = calcVerticallyLayoutGap(dy)
    if (gap != 0) offsetChildrenVertical(gap)
    val actualScrollAmount = scrollAmount - gap

    if (dy > 0) {
      if (getDecoratedBottom(firstItem) < parentTop) recycleRow(firstVisiblePosition, visibleRightTopPosition, recycler)
    } else {
      if (getDecoratedTop(lastItem) > parentBottom) recycleRow(visibleLeftBottomPosition, lastVisiblePosition, recycler)
    }

    return actualScrollAmount
  }

  override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: State): Int {
    if (dx == 0) return 0

    val firstItem = getChildAt(0) ?: return 0
    val lastItem = getChildAt(childCount - 1) ?: return 0
    val scrollAmount = calcHorizontallyScrollAmount(firstItem, lastItem, dx)

    offsetChildrenHorizontal(-scrollAmount)

    val firstLeft = getDecoratedLeft(firstItem)
    val lastRight = getDecoratedRight(lastItem)

    if (dx > 0) {
      if (!lastVisiblePosition.isLastInRow) {
        val position = visibleRightTopPosition + 1
        if (lastRight < parentRight && position < itemCount)
          fillColumn(position, lastRight, getDecoratedTop(firstItem), recycler)
      }
    } else {
      if (!firstVisiblePosition.isFirstInRow) {
        val firstInPreviousColumn = firstVisiblePosition - 1
        if (firstLeft >= parentLeft && firstInPreviousColumn >= 0)
          fillColumn(firstInPreviousColumn, firstLeft, getDecoratedTop(firstItem), recycler)
      }
    }

    val gap = calcHorizontallyLayoutGap(dx)
    if (gap != 0) offsetChildrenHorizontal(gap)
    val actualScrollAmount = scrollAmount - gap

    if (dx > 0) {
      if (getDecoratedRight(firstItem) < parentLeft) recycleColumn(firstVisiblePosition, visibleLeftBottomPosition, recycler)
    } else {
      if (getDecoratedLeft(lastItem) > parentRight) recycleColumn(visibleRightTopPosition, lastVisiblePosition, recycler)
    }

    return actualScrollAmount
  }

  private fun calcVerticallyScrollAmount(firstItem: View, lastItem: View, dy: Int): Int {
    if (dy == 0) return 0

    return if (dy > 0) { // up swipe
      val bottom = getDecoratedBottom(lastItem)
      if (lastVisiblePosition.isLastInColumn) if (bottom <= parentBottom) 0 else min(dy, bottom - parentBottom)
      else dy
    } else {
      val top = getDecoratedTop(firstItem)
      if (firstVisiblePosition.isFirstInColumn) if (top >= parentTop) 0 else max(dy, -(parentTop - top))
      else dy
    }
  }

  private fun calcVerticallyLayoutGap(dy: Int): Int {
    val gap = if (dy > 0) parentBottom - getDecoratedBottom(getChildAt(childCount - 1) ?: return 0)
    else parentTop - getDecoratedTop(getChildAt(0) ?: return 0)

    return if ((dy > 0 && gap > 0) || (dy < 0 && gap < 0)) gap
    else 0
  }

  private fun calcHorizontallyScrollAmount(firstItem: View, lastItem: View, dx: Int): Int {
    if (dx == 0) return 0

    return if (dx > 0) { // left swipe
      val right = getDecoratedRight(lastItem)
      if (lastVisiblePosition.isLastInRow) if (right <= parentRight) 0 else min(dx, right - parentRight)
      else dx
    } else {
      val left = getDecoratedLeft(firstItem)
      if (firstVisiblePosition.isFirstInRow) if (left >= parentLeft) 0 else max(dx, -(parentLeft - left))
      else dx
    }
  }

  private fun calcHorizontallyLayoutGap(dx: Int): Int {
    val gap = if (dx > 0) parentRight - getDecoratedRight(getChildAt(childCount - 1) ?: return 0)
    else parentLeft - getDecoratedLeft(getChildAt(0) ?: return 0)

    return if ((dx > 0 && gap > 0) || (dx < 0 && gap < 0)) gap
    else 0
  }

  private fun measureCell(view: View) {
    val insets = Rect().apply { calculateItemDecorationsForChild(view, this) }
    val width = cellWidth + insets.left + insets.right
    val height = cellHeight + insets.top + insets.bottom
    view.measure(
      View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
  }

  private fun fillRow(from: Int, startX: Int, startY: Int, recycler: RecyclerView.Recycler): Int {
    val isPrepend = from < firstVisiblePosition
    val to = getLastCellInSameRow(from)
    var offsetX = startX
    var rowHeight = 0

    var insertPosition = 0 // only for prepend
    var position = from
    while (position <= to && position < itemCount && offsetX < parentRight) {
      val v = recycler.getViewForPosition(position)
      if (isPrepend) addView(v, insertPosition) else addView(v)

      measureCell(v)
      val width = getDecoratedMeasuredWidth(v)
      val height = getDecoratedMeasuredHeight(v)
      val l = offsetX
      val t = if (isPrepend) startY - height else startY
      layoutDecorated(v, l, t, l + width, t + height)

      offsetX += width
      rowHeight = max(rowHeight, height)
      insertPosition++
      position++
    }

    if (isPrepend) firstVisiblePosition = from
    else lastVisiblePosition = if (position == from) position else position - 1

    return rowHeight
  }

  private fun fillColumn(from: Int, startX: Int, startY: Int, recycler: RecyclerView.Recycler): Int {
    val isPrepend = from < firstVisiblePosition
    var offsetY = startY
    var columnWidth = 0
    val visibleColumnCount = visibleColumnCount
    var insertPosition = if (isPrepend) 0 else visibleColumnCount

    var position = from
    while (position < itemCount && offsetY < parentBottom) {
      val v = recycler.getViewForPosition(position)
      addView(v, insertPosition)

      measureCell(v)
      val width = getDecoratedMeasuredWidth(v)
      val height = getDecoratedMeasuredHeight(v)
      val l = if (isPrepend) startX - width else startX
      val t = offsetY
      layoutDecorated(v, l, t, l + width, t + height)

      offsetY += height
      columnWidth = max(columnWidth, width)
      insertPosition += visibleColumnCount + 1
      position = getBelowCell(position)
    }

    if (isPrepend) firstVisiblePosition = from
    else lastVisiblePosition = if (position == from) position else getAboveCell(position)
    return columnWidth
  }

  private fun recycleCell(position: Int, recycler: Recycler) {
    findViewByPosition(position)?.let { removeAndRecycleView(it, recycler) }
  }

  private fun recycleRow(from: Int, to: Int, recycler: Recycler) {
    (from..to).forEach { recycleCell(it, recycler) }

    if (from == firstVisiblePosition) firstVisiblePosition = getBelowCell(firstVisiblePosition)
    if (to == lastVisiblePosition) lastVisiblePosition = getAboveCell(lastVisiblePosition)
  }

  private fun recycleColumn(from: Int, to: Int, recycler: Recycler) {
    var position = from
    while (position <= to) {
      recycleCell(position, recycler)
      position = getBelowCell(position)
    }

    if (from == firstVisiblePosition) firstVisiblePosition += 1
    if (to == lastVisiblePosition) lastVisiblePosition -= 1
  }

  private fun adjustScrollPosition(targetScrollPosition: Int): Int {
    val rowPosition = targetScrollPosition / columnCount
    val rowCount = (itemCount.toFloat() / columnCount).roundToInt()
    val availableRowCount = (parentBottom.toFloat() / cellHeight).roundToInt()
    val step1 = if (rowCount - rowPosition < availableRowCount) {
      val shortageRowCount = availableRowCount - (rowCount - rowPosition)
      (targetScrollPosition - shortageRowCount * columnCount).let { if (it < 0) 0 else it }
    } else {
      targetScrollPosition
    }

    val columnPosition = (step1 % columnCount)
    val availableColumnCount = (parentRight.toFloat() / cellWidth).roundToInt()
    return if (columnCount - columnPosition < availableColumnCount) {
      val shortageColumnCount = availableColumnCount - (columnCount - columnPosition)
      (step1 - shortageColumnCount).let { if (it < 0) 0 else it }
    } else {
      step1
    }
  }

  private fun getAboveCell(position: Int) = position - columnCount

  private fun getBelowCell(position: Int) = position + columnCount

  private fun getLastCellInSameRow(position: Int) = position + (columnCount - 1) - position % columnCount
}