package io.moyuru.multidirectionalscrollgridlayoutmanagersample

import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.item_number.view.number

class NumberItem(private val n: Int) : Item<ViewHolder>() {
  override fun getLayout() = R.layout.item_number

  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.number.text = n.toString()
  }
}
