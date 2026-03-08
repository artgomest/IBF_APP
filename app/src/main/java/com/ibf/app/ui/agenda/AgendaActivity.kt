package com.ibf.app.ui.agenda

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.ibf.app.R
import com.ibf.app.data.models.Reuniao
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AgendaActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: ReuniaoAdapter
    private lateinit var calendarGrid: GridLayout
    private lateinit var textMonthYear: TextView
    private lateinit var textFilterLabel: TextView

    private val allReunioes = mutableListOf<Reuniao>()
    private val filteredReunioes = mutableListOf<Reuniao>()

    private val displayCalendar = Calendar.getInstance()
    private var selectedDate: Calendar? = null

    private var redeSelecionada: String? = null

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agenda)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        redeSelecionada = intent.getStringExtra("REDE_SELECIONADA")

        // Reset display calendar to first day of current month
        displayCalendar.set(Calendar.DAY_OF_MONTH, 1)
        displayCalendar.set(Calendar.HOUR_OF_DAY, 0)
        displayCalendar.set(Calendar.MINUTE, 0)
        displayCalendar.set(Calendar.SECOND, 0)
        displayCalendar.set(Calendar.MILLISECOND, 0)

        initViews()
    }

    override fun onResume() {
        super.onResume()
        carregarReunioes()
    }

    private fun initViews() {
        findViewById<ImageView>(R.id.button_back).setOnClickListener { finish() }

        textMonthYear = findViewById(R.id.text_month_year)
        calendarGrid = findViewById(R.id.calendar_grid)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        textFilterLabel = findViewById(R.id.text_filter_label)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view_agenda)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ReuniaoAdapter(filteredReunioes)
        recyclerView.adapter = adapter

        swipeRefreshLayout.setOnRefreshListener { carregarReunioes() }

        findViewById<FloatingActionButton>(R.id.fab_adicionar_reuniao).setOnClickListener {
            val intent = Intent(this, AgendarReuniaoActivity::class.java)
            intent.putExtra("REDE_SELECIONADA", redeSelecionada)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.btn_prev_month).setOnClickListener {
            displayCalendar.add(Calendar.MONTH, -1)
            selectedDate = null
            buildCalendar()
            filterAndUpdateList()
        }

        findViewById<ImageView>(R.id.btn_next_month).setOnClickListener {
            displayCalendar.add(Calendar.MONTH, 1)
            selectedDate = null
            buildCalendar()
            filterAndUpdateList()
        }
    }

    private fun carregarReunioes() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("reunioes")
            .whereEqualTo("liderUid", uid)
            .get()
            .addOnSuccessListener { documents ->
                allReunioes.clear()
                for (doc in documents) {
                    val reuniao = doc.toObject(Reuniao::class.java)
                    reuniao.id = doc.id
                    allReunioes.add(reuniao)
                }
                // Sort in memory to avoid requiring a Firestore composite index
                allReunioes.sortBy { it.dataHora?.toDate() }
                buildCalendar()
                filterAndUpdateList()
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Log.e("AgendaActivity", "Erro ao carregar agenda", e)
                Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                swipeRefreshLayout.isRefreshing = false
            }
    }

    private fun getMeetingDateStrings(): Set<String> {
        return allReunioes.mapNotNull { r ->
            r.dataHora?.toDate()?.let { dateFmt.format(it) }
        }.toSet()
    }

    private fun buildCalendar() {
        calendarGrid.removeAllViews()

        val monthFmt = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
        textMonthYear.text = monthFmt.format(displayCalendar.time)
            .replaceFirstChar { it.uppercase() }

        val meetingDates = getMeetingDateStrings()
        val today = Calendar.getInstance()
        val todayStr = dateFmt.format(today.time)
        val selectedStr = selectedDate?.let { dateFmt.format(it.time) }

        val daysInMonth = displayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        // DAY_OF_WEEK: 1=Sun...7=Sat → offset 0..6
        val monthCal = displayCalendar.clone() as Calendar
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOffset = monthCal.get(Calendar.DAY_OF_WEEK) - 1

        val totalCells = firstDayOffset + daysInMonth
        val rows = Math.ceil(totalCells / 7.0).toInt()

        val accentColor = ContextCompat.getColor(this, R.color.accent)
        val textPrimaryColor = ContextCompat.getColor(this, R.color.ibf_on_surface)
        val textSecondaryColor = ContextCompat.getColor(this, R.color.ibf_on_surface_variant)

        fun dp(value: Int): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()

        for (row in 0 until rows) {
            for (col in 0 until 7) {
                val cellIndex = row * 7 + col
                val day = cellIndex - firstDayOffset + 1

                // Cell container
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = GridLayout.LayoutParams(
                        GridLayout.spec(row),
                        GridLayout.spec(col, 1f)
                    ).apply {
                        width = 0
                        height = dp(44)
                    }
                }

                if (day < 1 || day > daysInMonth) {
                    calendarGrid.addView(cell)
                    continue
                }

                val dayCal = displayCalendar.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, day)
                val dayStr = dateFmt.format(dayCal.time)

                val hasMeeting = meetingDates.contains(dayStr)
                val isToday = dayStr == todayStr
                val isSelected = dayStr == selectedStr

                // Day number text
                val numberView = TextView(this).apply {
                    text = day.toString()
                    textSize = 13f
                    gravity = Gravity.CENTER
                    val size = dp(32)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    when {
                        isSelected -> {
                            val circle = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(accentColor)
                            }
                            background = circle
                            setTextColor(Color.WHITE)
                        }
                        isToday -> {
                            setTextColor(accentColor)
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        }
                        hasMeeting -> setTextColor(textPrimaryColor)
                        else -> setTextColor(textSecondaryColor)
                    }
                }

                // Meeting dot indicator
                val dot = android.view.View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(5), dp(5)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        topMargin = dp(2)
                    }
                    visibility = if (hasMeeting && !isSelected) {
                        val dotDrawable = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(accentColor)
                        }
                        background = dotDrawable
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.INVISIBLE
                    }
                }

                cell.addView(numberView)
                cell.addView(dot)

                cell.setOnClickListener {
                    if (selectedStr == dayStr) {
                        // Tap same day → deselect (show all)
                        selectedDate = null
                    } else {
                        selectedDate = dayCal.clone() as Calendar
                    }
                    buildCalendar()
                    filterAndUpdateList()
                }

                calendarGrid.addView(cell)
            }
        }
    }

    private fun filterAndUpdateList() {
        filteredReunioes.clear()
        val selStr = selectedDate?.let { dateFmt.format(it.time) }

        if (selStr == null) {
            // No date selected → show ALL meetings
            filteredReunioes.addAll(allReunioes)
            textFilterLabel.text = if (filteredReunioes.isEmpty())
                "Nenhum discipulado agendado"
            else
                "Todos os discipulados (${filteredReunioes.size})"
        } else {
            filteredReunioes.addAll(allReunioes.filter { r ->
                r.dataHora?.toDate()?.let { dateFmt.format(it) } == selStr
            })
            val dayFmt = SimpleDateFormat("dd/MM", Locale("pt", "BR"))
            val dayLabel = dayFmt.format(selectedDate!!.time)
            textFilterLabel.text = if (filteredReunioes.isEmpty())
                "Nenhum discipulado em $dayLabel"
            else
                "Discipulados em $dayLabel (${filteredReunioes.size})"
        }

        adapter.updateList(filteredReunioes)
    }
}
