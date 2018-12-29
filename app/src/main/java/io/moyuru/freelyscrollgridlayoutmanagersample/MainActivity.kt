package io.moyuru.freelyscrollgridlayoutmanagersample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.moyuru.freelyscrollgridlayoutmanager.FreelyScrollGridLayoutManager
import kotlinx.android.synthetic.main.activity_main.recycler

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    recycler.layoutManager = FreelyScrollGridLayoutManager(this, columnCount = 10, columnWidthDp = 100, columnHeightDp = 100)
    recycler.addItemDecoration(DividerDecoration())
    recycler.adapter = GroupAdapter<ViewHolder>().apply { addAll(List(200) { NumberItem(it) }) }
  }
}
