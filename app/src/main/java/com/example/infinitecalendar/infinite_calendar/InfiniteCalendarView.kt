package com.example.infinitecalendar.infinite_calendar

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.infinitecalendar.R
import kotlinx.android.synthetic.main.app_calendar_layout.view.*
import kotlinx.android.synthetic.main.grid_view_calendar_layout.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayDeque

@ExperimentalStdlibApi
class InfiniteCalendarView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    interface CalendarDayPressedListener {
        fun itemPressed(day: Date)
    }

    companion object {
        private const val MONTHLY_VIEW_DAY_COUNT = 42
        private const val DATE_FORMAT = "MMMM,dd,yyyy"
        private const val COMMA = ","
    }

    data class CalendarMonth(
        val month: String,
        val days: List<Date>,
        val time: Date
    )

    private var calendarDayPressed: CalendarDayPressedListener? = null
    private var adapter: AppCalendarAdapter
    private var layoutManager: LinearLayoutManager
    private val pagerSnapHelper: PagerSnapHelper = PagerSnapHelper()
    private val layout = View.inflate(context, R.layout.app_calendar_layout, this)
    private val scrollLeftButton: ImageView = layout.scrollLeftButton
    private val scrollRightButton: ImageView = layout.scrollRightButton
    private val recyclerView: RecyclerView = layout.recyclerView
    private var currentPosition: Int = 0
    private val calendarNext = Calendar.getInstance()
    private val calendarPrev = Calendar.getInstance()

    init {
        adapter = AppCalendarAdapter(generateInitialMonths(), null)
        recyclerView.adapter = adapter
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.layoutManager?.scrollToPosition(adapter.itemCount / 2)
        pagerSnapHelper.attachToRecyclerView(recyclerView)
        setOnScrollListeners()
        setClickListeners()
    }

    private fun generateInitialMonths(): List<CalendarMonth> {
        val currentMonth = CalendarMonth(
            generateNameAndYearOfMonth(calendarNext),
            generateDaysInMonth(calendarNext),
            calendarNext.time
        )

        calendarPrev.add(Calendar.MONTH, -1)

        val previousMonth = CalendarMonth(
            generateNameAndYearOfMonth(calendarPrev),
            generateDaysInMonth(calendarPrev),
            calendarPrev.time
        )

        calendarNext.add(Calendar.MONTH, 1)

        val nextMonth = CalendarMonth(
            generateNameAndYearOfMonth(calendarNext),
            generateDaysInMonth(calendarNext),
            calendarNext.time
        )

        return listOf(previousMonth, currentMonth, nextMonth)
    }

    private fun generateDaysInMonth(calendar: Calendar): List<Date> {
        val cells = mutableListOf<Date>()
        val calendarClone = calendar.clone() as Calendar
        calendarClone.set(Calendar.DAY_OF_MONTH, 1)
        val monthBeginningCell = calendarClone.get(Calendar.DAY_OF_WEEK) - 1
        calendarClone.add(Calendar.DAY_OF_MONTH, -monthBeginningCell)
        while (cells.size < MONTHLY_VIEW_DAY_COUNT) {
            cells.add(calendarClone.time)
            calendarClone.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }

    private fun generateNameAndYearOfMonth(calendar: Calendar) : String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        val date = sdf.format(calendar.time).split(COMMA)
        return "${date[0]} ${date[2]}"
    }

    private fun setOnScrollListeners() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                currentPosition = layoutManager.findFirstVisibleItemPosition()
                layout.date.text = adapter.getItems()[currentPosition].month
                when {
                    adapter.itemCount == layoutManager.findLastVisibleItemPosition().inc() -> {
                        recyclerView.post {
                            calendarNext.add(Calendar.MONTH, 1)
                            adapter.addItemToFront(CalendarMonth(
                                generateNameAndYearOfMonth(calendarNext),
                                generateDaysInMonth(calendarNext),
                                calendarNext.time
                            ))
                        }
                    }
                    layoutManager.findFirstVisibleItemPosition() == 0 -> {
                        recyclerView.post {
                            calendarPrev.add(Calendar.MONTH, -1)
                            adapter.addItemToBack(CalendarMonth(
                                generateNameAndYearOfMonth(calendarPrev),
                                generateDaysInMonth(calendarPrev),
                                calendarPrev.time
                            ))
                        }
                    }
                }
            }
        })
    }

    private fun setClickListeners() {
        scrollLeftButton.setOnClickListener {
            if (currentPosition > 0) {
                recyclerView.smoothScrollToPosition(currentPosition - 1)
            }
        }
        scrollRightButton.setOnClickListener {
            if (currentPosition < adapter.itemCount) {
                recyclerView.smoothScrollToPosition(currentPosition + 1)
            }
        }
    }

    @ExperimentalStdlibApi
    private class AppCalendarAdapter(
        initialMonths: List<CalendarMonth>,
        private val calendarDayPressed: CalendarDayPressedListener?
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        @ExperimentalStdlibApi
        private val deque: ArrayDeque<CalendarMonth> = ArrayDeque()

        init {
            deque.addAll(initialMonths)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return CalendarGridViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.grid_view_calendar_layout,
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder as CalendarGridViewHolder
            holder.bindView(deque[position], calendarDayPressed)
        }

        override fun getItemCount(): Int = deque.size

        fun getItems(): ArrayDeque<CalendarMonth> = deque

        fun addItemToBack(item: CalendarMonth) {
            deque.addFirst(item)
            notifyItemRangeInserted(0, 1)
        }

        fun addItemToFront(item: CalendarMonth) {
            deque.addLast(item)
            notifyItemRangeInserted(deque.size, 1)
        }
    }

    @ExperimentalStdlibApi
    private class CalendarGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val gridView: GridView = itemView.calendar_grid
        private lateinit var adapter: CalendarGridViewAdapter

        fun bindView(
            calendarMonth: CalendarMonth,
            calendarDayPressed: CalendarDayPressedListener?
        ) {
            adapter = CalendarGridViewAdapter(
                itemView.context,
                calendarMonth.days.toMutableList(),
                calendarMonth.time
            )
            gridView.adapter = adapter
            gridView.setOnItemClickListener { _, _, position, _ ->
                val date = gridView.getItemAtPosition(position) as Date
                calendarDayPressed?.itemPressed(date)
            }
        }
    }

    private class CalendarGridViewAdapter(
        context: Context,
        days: MutableList<Date>,
        private val time: Date
    ) : ArrayAdapter<Date>(context, R.layout.grid_view_calendar_day_item, days) {

        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getView(position: Int, view: View?, parent: ViewGroup): View {
            val today = Date()
            val date = getItem(position)
            var itemView = view

            if (view == null) {
                itemView = inflater.inflate(R.layout.grid_view_calendar_day_item, parent, false)
            }

            itemView as LinearLayout

            val calendarDayText = itemView.findViewById<TextView>(R.id.calendar_day_text)

            calendarDayText.text = date?.date.toString()
            calendarDayText.setTextColor(Color.parseColor("#FFFFFF"))

            if (isDayNotInMonth(date)) {
                calendarDayText.setTextColor(Color.parseColor("#bdbdbd"))
                calendarDayText.isClickable = false
            }

            if (isToday(today, date)) {
                calendarDayText.setTextColor(Color.parseColor("#42a5f5"))
            }

            return itemView
        }

        private fun isDayNotInMonth(date: Date?): Boolean {
            return date?.month != time.month || time.year != date.year
        }

        private fun isToday(today: Date, date: Date?): Boolean {
            return today.date == date?.date && today.month == date.month
        }
    }
}