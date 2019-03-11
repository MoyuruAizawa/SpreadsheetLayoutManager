package io.moyuru.multidirectionalscrollgridlayoutmanagersample

import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

class DividerDecoration : ItemDecoration() {

  private val dividerWidth = 2.dp
  private val paint = Paint().apply { color = Color.BLACK }

  override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
    (0 until parent.childCount).map(parent::getChildAt)
      .forEach {
        c.drawRect(
          it.left.toFloat(),
          it.bottom.toFloat(),
          it.right.toFloat(),
          it.bottom.toFloat() + dividerWidth,
          paint
        )
        c.drawRect(
          it.right.toFloat(),
          it.top.toFloat(),
          it.right.toFloat() + dividerWidth,
          it.bottom.toFloat() + dividerWidth,
          paint
        )
      }
  }

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
    super.getItemOffsets(outRect, view, parent, state)
    outRect.set(0, 0, 2.dp, 2.dp)
  }

  val Int.dp get() = (Resources.getSystem().displayMetrics.density * this).toInt()
}