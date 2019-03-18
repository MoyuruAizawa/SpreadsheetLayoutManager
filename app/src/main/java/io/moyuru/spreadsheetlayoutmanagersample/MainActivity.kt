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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    recyclerView.addItemDecoration(DividerDecoration())
    recyclerView.adapter = GroupAdapter<ViewHolder>().apply { addAll(List(200) { CellItem(it) }) }
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
    recyclerView.layoutManager = SpreadsheetLayoutManager(10,
      {
        when {
          it == 0 -> 50.dp
          it % 2 == 0 -> 100.dp
          else -> 150.dp
        }
      },
      { 50.dp })
  }

  private fun useMultiDirectionalScrollGrid() {
    recyclerView.layoutManager = MultiDirectionalScrollGridLayoutManager(10, 100.dp, 100.dp)
  }
}
