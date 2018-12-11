package io.moyuru.freelyscrollgridlayoutmanager

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
import kotlin.math.max
import kotlin.math.min

class FreelyScrollGridLayoutManager(private val columnCount: Int) : RecyclerView.LayoutManager() {
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
      detachAndScrapAttachedViews(recycler)
      return
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

  override fun canScrollVertically() = true

  override fun canScrollHorizontally() = true

  override fun scrollVerticallyBy(dy: Int, recycler: Recycler, state: State): Int {
    if (dy == 0) return 0

    val firstItem = findViewByPosition(firstVisiblePosition) ?: return 0
    val lastItem = findViewByPosition(lastVisiblePosition) ?: return 0
    val actualScrollAmount = calcVerticallyScrollAmount(firstItem, lastItem, dy)

    if (dy > 0) onUpSwipe(firstItem, lastItem, actualScrollAmount, recycler)
    else onDownSwipe(firstItem, lastItem, actualScrollAmount, recycler)

    offsetChildrenVertical(-actualScrollAmount)
    return actualScrollAmount
  }

  override fun scrollHorizontallyBy(dx: Int, recycler: Recycler, state: State): Int {
    if (dx == 0) return 0

    val firstItem = findViewByPosition(firstVisiblePosition) ?: return 0
    val lastItem = findViewByPosition(lastVisiblePosition) ?: return 0
    val actualScrollAmount = calcHorizontallyScrollAmount(firstItem, lastItem, dx)

    if (dx > 0) onLeftSwipe(firstItem, actualScrollAmount, recycler)
    else onRightSwipe(firstItem, actualScrollAmount, recycler)

    offsetChildrenHorizontal(-actualScrollAmount)
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

  private fun onUpSwipe(firstItem: View, lastItem: View, scrollAmount: Int, recycler: Recycler) {
    val bottom = getDecoratedBottom(lastItem)

    if (getDecoratedBottom(firstItem) - scrollAmount < parentTop)
      recycleRow(firstVisiblePosition, visibleRightTopPosition, recycler)

    val firstInNextRow = getBelowCell(visibleLeftBottomPosition)
    if ((bottom - scrollAmount) <= parentBottom && firstInNextRow < itemCount)
      fillRow(firstInNextRow, getDecoratedLeft(firstItem), bottom, recycler)
  }

  private fun onDownSwipe(firstItem: View, lastItem: View, scrollAmount: Int, recycler: Recycler) {
    val top = getDecoratedTop(firstItem)

    if (getDecoratedTop(lastItem) - scrollAmount > parentBottom)
      recycleRow(visibleLeftBottomPosition, lastVisiblePosition, recycler)

    val firstInPreviousRow = getAboveCell(firstVisiblePosition)
    if (top - scrollAmount >= parentTop && firstInPreviousRow >= 0)
      fillRow(firstInPreviousRow, getDecoratedLeft(firstItem), getDecoratedTop(firstItem), recycler)
  }

  private fun onLeftSwipe(firstItem: View, scrollAmount: Int, recycler: Recycler) {
    if (getDecoratedRight(firstItem) - scrollAmount < parentLeft)
      recycleColumn(firstVisiblePosition, visibleLeftBottomPosition, recycler)

    if (lastVisiblePosition.isLastInRow) return

    val right = getDecoratedRight(findViewByPosition(visibleRightTopPosition) ?: return)
    val firstInNextColumn = visibleRightTopPosition + 1
    if (right - scrollAmount < parentRight && firstInNextColumn < itemCount)
      fillColumn(firstInNextColumn, right, getDecoratedTop(firstItem), recycler)
  }

  private fun onRightSwipe(firstItem: View, scrollAmount: Int, recycler: Recycler) {
    val leftOfLastItemInRow = getDecoratedLeft(findViewByPosition(visibleRightTopPosition) ?: return)
    if (leftOfLastItemInRow - scrollAmount > parentRight)
      recycleColumn(visibleRightTopPosition, lastVisiblePosition, recycler)

    if (firstVisiblePosition.isFirstInRow) return

    val left = getDecoratedLeft(firstItem)
    val firstInPreviousColumn = firstVisiblePosition - 1
    if (left - scrollAmount >= parentLeft && firstInPreviousColumn >= 0)
      fillColumn(firstInPreviousColumn, left, getDecoratedTop(firstItem), recycler)
  }

  private fun measureCell(view: View): Pair<Int, Int> {
    measureChildWithMargins(view, 0, 0)
    return Pair(getDecoratedMeasuredWidth(view), getDecoratedMeasuredHeight(view))
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

      val (width, height) = measureCell(v)
      val l = offsetX
      val t = if (isPrepend) startY - height else startY

      if (isPrepend) addView(v, insertPosition) else addView(v)
      layoutDecorated(v, l, t, l + v.measuredWidth, t + v.measuredHeight)

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
    var insertPosition = if (isPrepend) 0 else visibleColumnCount + 1

    var position = from
    while (position < itemCount && offsetY < parentBottom) {
      val v = recycler.getViewForPosition(position)
      val (width, height) = measureCell(v)
      val l = if (isPrepend) startX - width else startX
      val t = offsetY

      addView(v, insertPosition)
      layoutDecorated(v, l, t, l + v.measuredWidth, t + v.measuredHeight)

      offsetY += height
      columnWidth = max(columnWidth, width)
      insertPosition += visibleColumnCount
      position = getBelowCell(position)
    }

    if (isPrepend) firstVisiblePosition = from
    else lastVisiblePosition = if (position == from) position else getAboveCell(position)
    return columnWidth
  }

  private fun recycleCell(position: Int, recycler: Recycler) {
    findViewByPosition(position)?.let {
      (it.layoutParams as RecyclerView.LayoutParams)
      removeAndRecycleView(it, recycler)
    }
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

  private fun getAboveCell(position: Int) = position - columnCount

  private fun getBelowCell(position: Int) = position + columnCount

  private fun getLastCellInSameRow(position: Int) = position + (columnCount - 1) - position % columnCount
}