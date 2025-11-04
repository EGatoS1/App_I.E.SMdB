package com.example.app_iesmdb

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsersAdapter(
    private val onEdit: (UserAccount) -> Unit,
    private val onDelete: (UserAccount) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private val items = mutableListOf<UserAccount>()

    fun submitList(list: List<UserAccount>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgRole: ImageView = itemView.findViewById(R.id.imgRole)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(user: UserAccount) {
            tvName.text = user.fullName.ifBlank { "(Sin nombre)" }
            tvEmail.text = "${user.username}${UserConstants.EMAIL_DOMAIN}"

            // Icono según rol (ajusta los drawables si quieres algo más bonito)
            val iconRes = when (user.role) {
                UserConstants.ROLE_ADMIN -> R.drawable.ic_person_24
                UserConstants.ROLE_TUTOR -> R.drawable.ic_person_24
                UserConstants.ROLE_AUX -> R.drawable.ic_person_24
                else -> R.drawable.ic_person_24
            }
            imgRole.setImageResource(iconRes)

            btnEdit.setOnClickListener { onEdit(user) }
            btnDelete.setOnClickListener { onDelete(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
