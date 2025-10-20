package com.example.app_iesmdb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip

data class TeacherStudentUI(
    val code: String,
    val name: String,
    val status: String // "PRESENTE" | "TARDE" | "AUSENTE" | "—"
)

class TeacherStudentsAdapter :
    ListAdapter<TeacherStudentUI, TeacherStudentsAdapter.VH>(Diff()) {

    class Diff : DiffUtil.ItemCallback<TeacherStudentUI>() {
        override fun areItemsTheSame(o: TeacherStudentUI, n: TeacherStudentUI) = o.code == n.code
        override fun areContentsTheSame(o: TeacherStudentUI, n: TeacherStudentUI) = o == n
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvCode: TextView = view.findViewById(R.id.tvCode)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val chip: Chip = view.findViewById(R.id.chipStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_teacher_student, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val it = getItem(pos)
        h.tvCode.text = it.code
        h.tvName.text = it.name

        val estado = it.status.lowercase()
        when (estado) {
            "puntual" -> {
                h.chip.text = "Puntual"
                h.chip.setTextColor(h.itemView.context.getColor(R.color.chip_puntual_text))
                h.chip.chipBackgroundColor =
                    android.content.res.ColorStateList.valueOf(h.itemView.context.getColor(R.color.chip_puntual_bg))
            }
            "tarde" -> {
                h.chip.text = "Tardanza"
                h.chip.setTextColor(h.itemView.context.getColor(R.color.chip_tarde_text))
                h.chip.chipBackgroundColor =
                    android.content.res.ColorStateList.valueOf(h.itemView.context.getColor(R.color.chip_tarde_bg))
            }
            "falta" -> {
                h.chip.text = "Falta"
                h.chip.setTextColor(h.itemView.context.getColor(R.color.chip_falta_text))
                h.chip.chipBackgroundColor =
                    android.content.res.ColorStateList.valueOf(h.itemView.context.getColor(R.color.chip_falta_bg))
            }
            else -> {
                h.chip.text = "—"
                h.chip.setTextColor(h.itemView.context.getColor(android.R.color.darker_gray))
                h.chip.chipBackgroundColor =
                    android.content.res.ColorStateList.valueOf(h.itemView.context.getColor(android.R.color.transparent))
            }
        }
    }

}
