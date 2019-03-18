package io.moyuru.spreadsheetlayoutmanagersample

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.ViewHolder
import io.moyuru.spreadsheetlayoutmanager.MultiDirectionalScrollGridLayoutManager
import io.moyuru.spreadsheetlayoutmanager.SpreadsheetLayoutManager
import kotlinx.android.synthetic.main.activity_main.recyclerView

class MainActivity : AppCompatActivity() {

  private val adapter = GroupAdapter<ViewHolder>()
  private val columnCount = 10

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    recyclerView.addItemDecoration(DividerDecoration())
    recyclerView.adapter = adapter.apply { update(List(200) { CellItem(it) }) }
    useSpreadSheet()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.menu_main, menu ?: return false)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item?.itemId) {
      R.id.action_spreadsheet -> useSpreadSheet()
      R.id.action_multidirectionalscrollgrid -> useMultiDirectionalScrollGrid()
      else -> return false
    }
    return true
  }

  private fun useSpreadSheet() {
    recyclerView.layoutManager =
      SpreadsheetLayoutManager(columnCount, { if (it == 0) 35.dp else 75.dp }, { if (it == 0) 35.dp else 75.dp })
  }

  private fun useMultiDirectionalScrollGrid() {
    recyclerView.layoutManager = MultiDirectionalScrollGridLayoutManager(10, 100.dp, 100.dp)
  }
}
