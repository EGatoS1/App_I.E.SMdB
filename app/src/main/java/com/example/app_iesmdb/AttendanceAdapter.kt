package com.example.app_iesmdb.attendance

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app_iesmdb.R

class AttendanceAdapter(
    private val items: MutableList<Attendance>
) : RecyclerView.Adapter<AttendanceAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvCode: TextView = v.findViewById(R.id.tvCode)
        val tvStatus: TextView = v.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = items[pos]
        h.tvDate.text = it.date
        h.tvCode.text = it.studentCode

        when (it.status) {
            Status.PRESENTE -> {
                h.tvStatus.text = "Presente"
                h.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                ViewCompat.setBackgroundTintList(
                    h.tvStatus,
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                )
            }
            Status.AUSENTE -> {
                h.tvStatus.text = "Ausente"
                h.tvStatus.setTextColor(Color.parseColor("#C62828"))
                ViewCompat.setBackgroundTintList(
                    h.tvStatus,
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                )
            }
            Status.TARDANZA -> {
                h.tvStatus.text = "Tardanza"
                h.tvStatus.setTextColor(Color.parseColor("#F9A825"))
                ViewCompat.setBackgroundTintList(
                    h.tvStatus,
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF9C4"))
                )
            }
        }
    }

    fun submit(list: List<Attendance>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
}
