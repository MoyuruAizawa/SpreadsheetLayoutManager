package io.moyuru.spreadsheetlayoutmanagersample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.moyuru.spreadsheetlayoutmanager.MultiDirectionalScrollGridLayoutManager
import kotlinx.android.synthetic.main.activity_main.recycler

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val layoutManager =
      MultiDirectionalScrollGridLayoutManager(columnCount = 10, columnWidth = 100.dp, rowHeight = 100.dp)
    recycler.layoutManager = layoutManager
    recycler.addItemDecoration(DividerDecoration())
    recycler.adapter = GroupAdapter<ViewHolder>().apply { addAll(List(200) { NumberItem(it) }) }
  }

  private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}
