package ai.nextbillion.nbassettrackingdemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Copyright © NextBillion.ai. All rights reserved.
 * Use of this source code is governed by license that can be found in the LICENSE file.
 */
class CodeExampleList : AppCompatActivity(), CodeExampleListAdapter.OnItemClickListener {
    private var activityList: MutableList<CodeExampleListItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_code_example_list)
        initActivityData()
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        val adapter = CodeExampleListAdapter(activityList, this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun initActivityData() {
        activityList.add(
            CodeExampleListItem(
                getString(R.string.activity_simple_tracking),
                getString(R.string.description_activity_simple_tracking),
                SimpleTrackingExample::class.java
            )
        )

        activityList.add(
            CodeExampleListItem(
                getString(R.string.activity_asset_profile),
                getString(R.string.description_activity_asset_profile),
                AssetProfileOperations::class.java
            )
        )

        activityList.add(
            CodeExampleListItem(
                getString(R.string.activity_get_asset_callback),
                getString(R.string.description_activity_get_asset_callback),
                GetAssetCallback::class.java
            )
        )

        activityList.add(
            CodeExampleListItem(
                getString(R.string.activity_update_configuration),
                getString(R.string.description_activity_update_configuration),
                UpdateConfiguration::class.java
            )
        )

        activityList.add(
            CodeExampleListItem(
                getString(R.string.extended_tracking_activity),
                getString(R.string.description_activity_main),
                ExtendedTrackingActivity::class.java
            )
        )
    }

    override fun onItemClick(position: Int) {
        val clickedActivity = activityList[position]
        val intent = Intent(this, clickedActivity.activityClass)
        startActivity(intent)
    }
}