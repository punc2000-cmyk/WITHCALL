package com.withcall.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class DeviceAdapter(private val items: List<DeviceItem>) :
    RecyclerView.Adapter<DeviceAdapter.VH>() {

    private val LABELS = listOf("연락처1", "연락처2", "연락처3", "연락처4", "연락처5")

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvPhone119: TextView           = view.findViewById(R.id.tvPhone119)
        val tvPhoneCare: TextView          = view.findViewById(R.id.tvPhoneCare)

        // 119비상벨 섹션
        val layout119Section: View         = view.findViewById(R.id.layout119Section)
        val btnReply119: MaterialButton    = view.findViewById(R.id.btnReply119)
        val containerContacts119: LinearLayout = view.findViewById(R.id.containerContacts119)

        // 돌봄비상벨 섹션
        val layoutCareSection: View        = view.findViewById(R.id.layoutCareSection)
        val btnReplyCare: MaterialButton   = view.findViewById(R.id.btnReplyCare)
        val containerContactsCare: LinearLayout = view.findViewById(R.id.containerContactsCare)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = items[position]
        val d = item.device

        h.tvPhone119.text  = d.phone119
        h.tvPhoneCare.text = d.phoneCare

        // ── 119비상벨 섹션 ──────────────────────────────────────────
        if (item.smsSent119) {
            h.layout119Section.visibility = View.VISIBLE
            h.btnReply119.visibility =
                if (item.replyReceived119) View.VISIBLE else View.GONE
            bindContacts(h.containerContacts119, item.contacts119)
        } else {
            h.layout119Section.visibility = View.GONE
        }

        // ── 돌봄비상벨 섹션 ────────────────────────────────────────
        if (item.smsSentCare) {
            h.layoutCareSection.visibility = View.VISIBLE
            h.btnReplyCare.visibility =
                if (item.replyReceivedCare) View.VISIBLE else View.GONE
            bindContacts(h.containerContactsCare, item.contactsCare)
        } else {
            h.layoutCareSection.visibility = View.GONE
        }
    }

    private fun bindContacts(container: LinearLayout, contacts: List<ContactState>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)
        contacts.forEachIndexed { i, cs ->
            if (!cs.shown) return@forEachIndexed
            val row = inflater.inflate(R.layout.item_contact_row, container, false)
            row.findViewById<TextView>(R.id.tvContactLabel).text = LABELS[i]
            row.findViewById<TextView>(R.id.tvContactNumber).text = cs.number
            row.findViewById<MaterialButton>(R.id.btnDone).visibility =
                if (cs.done) View.VISIBLE else View.GONE
            container.addView(row)
        }
    }

    override fun getItemCount() = items.size
}
