package io.moyuru.spreadsheetlayoutmanagersample

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

class SpreadsheetDividerDecoration(private val columnCount: Int) : ItemDecoration() {

  private val paint = Paint().apply { color = Color.BLACK }

  override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
    (0 until parent.childCount).map(parent::getChildAt)
      .forEach {
        val position = (it.layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
        c.drawRect(
          it.left.toFloat(),
          it.bottom.toFloat(),
          it.right.toFloat(),
          it.bottom.toFloat() + horizontalDividerWidth(position),
          paint
        )
        c.drawRect(
          it.right.toFloat(),
          it.top.toFloat(),
          it.right.toFloat() + verticalDividerWidth(position),
          it.bottom.toFloat() + horizontalDividerWidth(position),
          paint
        )
      }
  }

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
    super.getItemOffsets(outRect, view, parent, state)
    val position = view.layoutPosition
    outRect.set(0, 0, verticalDividerWidth(position), horizontalDividerWidth(position))
  }

  private fun horizontalDividerWidth(position: Int): Int {
    return if (position < columnCount) 2.dp else 1.dp
  }

  private fun verticalDividerWidth(position: Int): Int {
    return if (position % columnCount == 0) 2.dp else 1.dp
  }

  private val View.layoutPosition get() = (layoutParams as RecyclerView.LayoutParams).viewLayoutPosition
}