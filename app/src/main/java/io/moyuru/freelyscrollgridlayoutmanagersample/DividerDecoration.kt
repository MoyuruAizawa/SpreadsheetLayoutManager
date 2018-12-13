package io.moyuru.freelyscrollgridlayoutmanagersample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State

class DividerDecoration(context: Context) : ItemDecoration() {
  private val paint = Paint().apply {
    color = Color.BLACK
    strokeWidth = context.resources.displayMetrics.density * 1
  }

  override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
    super.onDraw(c, parent, state)
    (0 until parent.childCount).map(parent::getChildAt)
      .forEach {
        c.drawLine(it.left.toFloat(), it.bottom.toFloat(), it.right.toFloat(), it.bottom.toFloat(), paint)
        c.drawLine(it.right.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat(), paint)
      }
  }
}