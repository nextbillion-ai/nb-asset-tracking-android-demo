package ai.nextbillion.nbassettrackingdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Copyright © NextBillion.ai. All rights reserved.
 * Use of this source code is governed by license that can be found in the LICENSE file.
 */

class CodeExampleListAdapter(
    private val activityList: List<CodeExampleListItem>,
    private val itemClickListener: OnItemClickListener
) :
    RecyclerView.Adapter<CodeExampleListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feature, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val activity = activityList[position]
        holder.nameTextView.text = activity.name
        holder.descriptionTextView.text = activity.description
    }

    override fun getItemCount(): Int = activityList.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionView)

        init {
            itemView.setOnClickListener {
                itemClickListener.onItemClick(adapterPosition)
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}