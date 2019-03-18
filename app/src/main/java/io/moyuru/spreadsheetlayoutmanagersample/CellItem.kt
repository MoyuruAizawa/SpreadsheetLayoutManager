package io.moyuru.spreadsheetlayoutmanagersample

import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.item_cell.view.number

class CellItem(private val n: Int) : Item<ViewHolder>() {
  override fun getLayout() = R.layout.item_cell
  override fun bind(viewHolder: ViewHolder, position: Int) {
    viewHolder.itemView.number.text = n.toString()
  }
}
