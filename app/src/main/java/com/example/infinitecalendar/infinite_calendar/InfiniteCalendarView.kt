package com.example.infinitecalendar.infinite_calendar

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.infinitecalendar.R
import kotlinx.android.synthetic.main.infinite_calendar_layout.view.*
import kotlinx.android.synthetic.main.calendar_day_item.view.*
import kotlinx.android.synthetic.main.calendar_month_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayDeque

@ExperimentalStdlibApi
class InfiniteCalendarView(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    interface CalendarItem

    interface CalendarDayPressedListener {
        fun itemPressed(day: Date)
    }

    companion object {
        private const val MONTHLY_VIEW_DAY_COUNT = 43
        private const val DATE_FORMAT = "MMMM,dd,yyyy"
        private const val COMMA = ","
        private const val DATE_VIEW_TYPE = 0
        private const val MONTH_VIEW_TYPE = 1
    }

    private data class DayItem(
        val date: Date = Date(),
        val calendarTime: Date = Date()
    ): CalendarItem

    private data class MonthItem(
        val month: String = ""
    ): CalendarItem

    private var calendarDayPressed: CalendarDayPressedListener? = null
    private var adapter: InfiniteCalendarAdapter
    private var layoutManager: GridLayoutManager
    private val layout = View.inflate(context, R.layout.infinite_calendar_layout, this)
    private val recyclerView: RecyclerView = layout.recyclerView
    private val calendarNext = Calendar.getInstance()
    private val calendarPrev = Calendar.getInstance()

    init {
        adapter = InfiniteCalendarAdapter(generateInitialMonths(), object : CalendarDayPressedListener {
            override fun itemPressed(day: Date) {
                Log.d("DATE", day.toString())
            }
        })
        recyclerView.adapter = adapter
        layoutManager = GridLayoutManager(context, 7)
        recyclerView.layoutManager = layoutManager
        recyclerView.layoutManager?.scrollToPosition(adapter.itemCount / 2)
        recyclerView.recycledViewPool.setMaxRecycledViews(DATE_VIEW_TYPE, 90)
        recyclerView.recycledViewPool.setMaxRecycledViews(MONTH_VIEW_TYPE, 12)
        recyclerView.addOnScrollListener(GenerateDayItemListener())
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position % MONTHLY_VIEW_DAY_COUNT == 0) {
                    7
                } else 1
            }
        }
    }

    private fun generateInitialMonths(): List<CalendarItem> {
        val currentMonth = generateDaysInMonth(calendarNext)

        calendarPrev.add(Calendar.MONTH, -1)

        val previousMonth = generateDaysInMonth(calendarPrev)

        calendarNext.add(Calendar.MONTH, 1)

        val nextMonth = generateDaysInMonth(calendarNext)

        currentMonth.addAll(nextMonth)

        previousMonth.addAll(currentMonth)

        return previousMonth
    }

    private fun generateDaysInMonth(calendar: Calendar): MutableList<CalendarItem> {
        val cells = mutableListOf<CalendarItem>()
        val calendarClone = calendar.clone() as Calendar
        cells.add(MonthItem(getNameAndYearOfMonth(calendarClone)))
        calendarClone.set(Calendar.DAY_OF_MONTH, 1)
        val monthBeginningCell = calendarClone.get(Calendar.DAY_OF_WEEK) - 1
        calendarClone.add(Calendar.DAY_OF_MONTH, -monthBeginningCell)
        while (cells.size < MONTHLY_VIEW_DAY_COUNT) {
            val dayItem = DayItem(
                calendarClone.time,
                calendar.time
            )
            cells.add(dayItem)
            calendarClone.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cells
    }

    private fun getNameAndYearOfMonth(calendar: Calendar) : String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        val date = sdf.format(calendar.time).split(COMMA)
        return "${date[0]} ${date[2]}"
    }

    private inner class GenerateDayItemListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            when {
                adapter.itemCount == layoutManager.findLastVisibleItemPosition().inc() -> {
                    recyclerView.post {
                        calendarNext.add(Calendar.MONTH, 1)
                        adapter.addNextCalendarItems(generateDaysInMonth(calendarNext))
                    }
                }
                layoutManager.findFirstVisibleItemPosition() == 0 -> {
                    recyclerView.post {
                        calendarPrev.add(Calendar.MONTH, -1)
                        adapter.addPrevDayItems(generateDaysInMonth(calendarPrev))
                    }
                }
            }
        }
    }

    private inner class InfiniteCalendarAdapter(
        dayItems: List<CalendarItem>,
        private val calendarDayPressed: CalendarDayPressedListener?
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        val days: MutableList<CalendarItem> = mutableListOf()

        init {
            days.addAll(dayItems)
        }

        override fun getItemViewType(position: Int): Int {
            return if (position % MONTHLY_VIEW_DAY_COUNT == 0) {
                MONTH_VIEW_TYPE
            } else {
                DATE_VIEW_TYPE
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                DATE_VIEW_TYPE -> DayItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.calendar_day_item,
                        parent,
                        false
                    )
                )
               else -> MonthItemViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.calendar_month_item,
                        parent,
                        false
                    )
                )
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (position % MONTHLY_VIEW_DAY_COUNT == 0) {
                val monthItem = days[position] as MonthItem
                (holder as MonthItemViewHolder).bindView(monthItem.month)
            } else {
                val dayItem = days[position] as DayItem
                (holder as DayItemViewHolder).bindView(dayItem.calendarTime, dayItem.date, calendarDayPressed)
            }
        }

        override fun getItemCount(): Int = days.size

        fun addPrevDayItems(prevDayItems: List<CalendarItem>) {
            days.addAll(0, prevDayItems)
            notifyItemRangeInserted(0, prevDayItems.size)
        }

        fun addNextCalendarItems(nextDayItems: List<CalendarItem>) {
            days.addAll(nextDayItems)
            notifyItemRangeInserted(days.size - nextDayItems.size, nextDayItems.size)
        }
    }

    private inner class DayItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(calendarTime: Date, date: Date, calendarDayPressed: CalendarDayPressedListener?) {
            val today = Date()
            itemView.calendarDay.text = date.date.toString()
            itemView.calendarDay.setTextColor(Color.parseColor("#FFFFFF"))

            if (isDayNotInMonth(date, calendarTime)) {
                itemView.calendarDay.setTextColor(Color.parseColor("#838383"))
            }

            if (isToday(today, date)) {
                itemView.calendarDay.setTextColor(Color.parseColor("#428FCB"))
            }

            itemView.setOnClickListener {
                calendarDayPressed?.itemPressed(date)
            }
        }

        private fun isDayNotInMonth(date: Date?, calendarTime: Date): Boolean {
            return date?.month != calendarTime.month || calendarTime.year != date.year
        }

        private fun isToday(today: Date, date: Date?): Boolean {
            return today.date == date?.date && today.month == date.month && today.year == date.year
        }
    }

    private inner class MonthItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bindView(month: String) {
            itemView.monthName.text = month
        }
    }
}