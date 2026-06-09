package at.aau.serg.websocketbrokerdemo

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import at.aau.serg.websocketbrokerdemo.model.BoardConfig
import at.aau.serg.websocketbrokerdemo.model.CardRepository
import at.aau.serg.websocketbrokerdemo.model.ClientState
import com.example.myapplication.R
import androidx.core.graphics.toColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

object GameUIHelper {
    private val colorNum = "#E91E63"

    fun dpToPx(context: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    fun createPlayerDot(context: Context, color: Int, sizeDp: Int = 16): View =
        createPlayerDotPx(context, color, dpToPx(context, sizeDp))

    fun createPlayerDotPx(context: Context, color: Int, sizePx: Int): View {
        val dot = View(context)
        dot.layoutParams = ConstraintLayout.LayoutParams(sizePx, sizePx).apply {
            startToStart = ConstraintSet.PARENT_ID
            topToTop = ConstraintSet.PARENT_ID
        }
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL
        shape.setColor(color)
        shape.setStroke(2, Color.BLACK)
        dot.background = shape
        return dot
    }

    fun buildSuspectChecklistOverlay(context: Context, container: ViewGroup, containerW: Int, containerH: Int) {
        buildSectionOverlay(context, container, containerW, containerH, BoardConfig.CHECKLIST_SUSPECTS)
    }

    fun buildWeaponChecklistOverlay(context: Context, container: ViewGroup, containerW: Int, containerH: Int) {
        buildSectionOverlay(context, container, containerW, containerH, BoardConfig.CHECKLIST_WEAPONS)
    }

    fun buildRoomChecklistOverlay(context: Context, container: ViewGroup, containerW: Int, containerH: Int) {
        buildSectionOverlay(context, container, containerW, containerH, BoardConfig.CHECKLIST_ROOMS)
    }

    private fun buildSectionOverlay(
        context: Context,
        container: ViewGroup,
        containerW: Int,
        containerH: Int,
        items: List<String>
    ) {
        // ── Tune these values to adjust placement ──────────────────────────
        val markXPercent = 0.88f         // horizontal position (0.0 = left, 1.0 = right)

        val dotSizeDp = 8                // size of the "my card" dot in dp
        val dotVerticalFactor = 0.5f     // 0.5 = center of row; lower = higher up
        val dotHorizontalOffsetDp = 0    // extra horizontal nudge for dot (+ = right)

        val checkTextSizeSp = 10f        // font size of checkmark symbol
        val checkVerticalFactor = 0.1f   // 0.5 = center of row; lower = higher up
        val checkVerticalOffsetDp = 0    // extra vertical nudge for checkmark (+ = down)
        val checkHorizontalOffsetDp = -6 // extra horizontal nudge for checkmark (+ = right)
        // ───────────────────────────────────────────────────────────────────

        container.removeAllViews()
        val rowH = containerH.toFloat() / items.size
        var currentY = 0f

        for (item in items) {
            val markX = (containerW * markXPercent).toInt()
            if (ClientState.myCards.contains(item)) {
                val dot = View(context)
                val dotSize = dpToPx(context, dotSizeDp)
                val lp = ConstraintLayout.LayoutParams(dotSize, dotSize).apply {
                    startToStart = ConstraintSet.PARENT_ID
                    topToTop = ConstraintSet.PARENT_ID
                    leftMargin = markX - dotSize / 2 + dpToPx(context, dotHorizontalOffsetDp)
                    topMargin = (currentY + rowH * dotVerticalFactor).toInt() - dotSize / 2
                }
                dot.layoutParams = lp
                val shape = GradientDrawable()
                shape.shape = GradientDrawable.OVAL
                shape.setColor(colorNum.toColorInt())
                dot.background = shape
                container.addView(dot)
            }
            if (ClientState.seenCards.contains(item) && !ClientState.myCards.contains(item)) {
                val check = TextView(context)
                check.text = context.getString(R.string.checkmark)
                check.setTextColor(colorNum.toColorInt())
                check.textSize = checkTextSizeSp
                val lp = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    startToStart = ConstraintSet.PARENT_ID
                    topToTop = ConstraintSet.PARENT_ID
                    leftMargin = markX + dpToPx(context, checkHorizontalOffsetDp)
                    topMargin = (currentY + rowH * checkVerticalFactor).toInt() + dpToPx(context, checkVerticalOffsetDp)
                }
                check.layoutParams = lp
                container.addView(check)
            }
            currentY += rowH
        }
    }

    fun showCardSelectionOverlay(
        context: Context,
        parent: ViewGroup,
        title: String,
        includeRooms: Boolean,
        currentRoom: String?,
        onConfirm: (suspect: String, room: String, weapon: String) -> Unit
    ) {
        val overlay = ConstraintLayout(context)
        overlay.setBackgroundColor(Color.argb(200, 0, 0, 0))
        overlay.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val scroll = ScrollView(context)
        val scrollLp = ConstraintLayout.LayoutParams(dpToPx(context, 500), 0).apply {
            startToStart = ConstraintSet.PARENT_ID
            endToEnd = ConstraintSet.PARENT_ID
            topToTop = ConstraintSet.PARENT_ID
            bottomToBottom = ConstraintSet.PARENT_ID
        }
        scroll.layoutParams = scrollLp

        val content = LinearLayout(context)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(
            dpToPx(context, 12),
            dpToPx(context, 8),
            dpToPx(context, 12),
            dpToPx(context, 8)
        )

        val titleView = TextView(context)
        titleView.text = title
        titleView.setTextColor(ContextCompat.getColor(context, R.color.cluedo_pink))
        titleView.textSize = 22f
        titleView.typeface = ResourcesCompat.getFont(context, R.font.freckle_face)
        titleView.gravity = Gravity.CENTER
        titleView.setPadding(0, dpToPx(context, 4), 0, dpToPx(context, 8))
        content.addView(titleView)

        var selectedSuspect: String? = null
        var selectedWeapon: String? = null
        var selectedRoom: String? = if (!includeRooms) currentRoom else null

        fun addSection(
            label: String,
            items: List<String>,
            onSelect: (String) -> Unit
        ): LinearLayout {
            val sectionLabel = TextView(context)
            sectionLabel.text = label
            sectionLabel.setTextColor(ContextCompat.getColor(context, R.color.cluedo_pink))
            sectionLabel.typeface = ResourcesCompat.getFont(context, R.font.freckle_face)
            sectionLabel.gravity = Gravity.CENTER
            sectionLabel.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            sectionLabel.textSize = 14f
            sectionLabel.setPadding(0, dpToPx(context, 4), 0, dpToPx(context, 2))
            content.addView(sectionLabel)

            val grid = LinearLayout(context)
            grid.orientation = LinearLayout.HORIZONTAL
            grid.gravity = Gravity.CENTER
            val glp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            grid.layoutParams = glp

            val views = mutableListOf<ImageView>()
            for (item in items) {
                val card = CardRepository.cards.find { it.cardId == item }
                val img = ImageView(context)
                val imgLp = LinearLayout.LayoutParams(dpToPx(context, 60), dpToPx(context, 90))
                imgLp.setMargins(dpToPx(context, 3), 0, dpToPx(context, 3), 0)
                img.layoutParams = imgLp
                img.scaleType = ImageView.ScaleType.FIT_CENTER
                img.setImageResource(card?.imageResId ?: android.R.drawable.ic_menu_help)
                img.alpha = 0.6f
                img.setOnClickListener {
                    views.forEach { v -> v.alpha = 0.6f; v.setBackgroundColor(Color.TRANSPARENT) }
                    img.alpha = 1.0f
                    img.setBackgroundColor("#44E91E63".toColorInt())
                    onSelect(item)
                }
                views.add(img)
                grid.addView(img)
            }
            content.addView(grid)
            return grid
        }

        addSection(
            context.getString(R.string.suspect),
            BoardConfig.ALL_CHARACTERS
        ) { selectedSuspect = it }
        addSection(context.getString(R.string.weapon), BoardConfig.ALL_WEAPONS) {
            selectedWeapon = it
        }
        if (includeRooms) {
            addSection(context.getString(R.string.room), BoardConfig.ALL_ROOMS) {
                selectedRoom = it
            }
        }

        val btnRow = LinearLayout(context)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.gravity = Gravity.CENTER
        btnRow.setPadding(0, dpToPx(context, 8), 0, dpToPx(context, 4))

        val btnConfirm = Button(context)
        btnConfirm.text = context.getString(R.string.confirm)
        btnConfirm.setTextColor(Color.WHITE)
        btnConfirm.typeface = ResourcesCompat.getFont(context, R.font.freckle_face)
        btnConfirm.isAllCaps = false
        btnConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cluedo_pink))
        btnConfirm.setOnClickListener {
            val s = selectedSuspect
            val w = selectedWeapon
            val r = selectedRoom
            if (s != null && w != null && r != null) {
                parent.removeView(overlay)
                onConfirm(s, r, w)
            }
        }
        btnRow.addView(btnConfirm)

        val btnCancel = Button(context)
        btnCancel.text = context.getString(R.string.cancel)
        btnCancel.setTextColor(Color.WHITE)
        btnCancel.typeface = ResourcesCompat.getFont(context, R.font.freckle_face)
        btnCancel.isAllCaps = false
        btnCancel.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.cluedo_pink))
        btnCancel.setOnClickListener { parent.removeView(overlay) }
        btnRow.addView(btnCancel)

        content.addView(btnRow)
        scroll.addView(content)
        overlay.addView(scroll)
        parent.addView(overlay)
    }

    fun showSuggestionTimer(context: Context, parent: ViewGroup, onDone: () -> Unit) {
        val timerView = TextView(context)
        timerView.setTextColor(Color.WHITE)
        timerView.textSize = 20f
        timerView.setBackgroundColor(Color.argb(180, 0, 0, 0))
        timerView.gravity = Gravity.CENTER
        timerView.setPadding(dpToPx(context, 16), dpToPx(context, 8), dpToPx(context, 16), dpToPx(context, 8))
        val lp = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintSet.PARENT_ID
            endToEnd = ConstraintSet.PARENT_ID
            topToTop = ConstraintSet.PARENT_ID
            bottomToBottom = ConstraintSet.PARENT_ID
        }
        timerView.layoutParams = lp
        parent.addView(timerView)

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var seconds = 5
        val runnable = object : Runnable {
            override fun run() {
                if (seconds > 0) {
                    timerView.text = context.getString(R.string.checking_cards, seconds)
                    seconds--
                    handler.postDelayed(this, 1000)
                } else {
                    parent.removeView(timerView)
                    onDone()
                }
            }
        }
        runnable.run()
    }

    fun showResultCards(context: Context, parent: ViewGroup, cardNames: List<String>, durationMs: Long = 4000) {
        if (cardNames.isEmpty()) return
        val overlay = ConstraintLayout(context)
        overlay.setBackgroundColor(Color.argb(180, 0, 0, 0))
        overlay.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        val rlp = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintSet.PARENT_ID
            endToEnd = ConstraintSet.PARENT_ID
            topToTop = ConstraintSet.PARENT_ID
            bottomToBottom = ConstraintSet.PARENT_ID
        }
        row.layoutParams = rlp

        for (name in cardNames) {
            val card = CardRepository.cards.find { it.cardId == name }
            val img = ImageView(context)
            val ilp = LinearLayout.LayoutParams(dpToPx(context, 80), dpToPx(context, 120))
            ilp.setMargins(dpToPx(context, 4), 0, dpToPx(context, 4), 0)
            img.layoutParams = ilp
            img.scaleType = ImageView.ScaleType.FIT_CENTER
            img.setImageResource(card?.imageResId ?: android.R.drawable.ic_menu_help)
            row.addView(img)
        }
        overlay.addView(row)
        parent.addView(overlay)

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            parent.removeView(overlay)
        }, durationMs)
    }

    fun showGameEndOverlay(
        context: Context,
        parent: ViewGroup,
        message: String,
        isWin: Boolean = false
    ) {
        val overlay = android.widget.FrameLayout(context)
        overlay.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val bgImage = ImageView(context)
        bgImage.layoutParams = android.widget.FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        bgImage.scaleType = ImageView.ScaleType.FIT_XY
        bgImage.setImageResource(if (isWin) R.drawable.winner else R.drawable.gameover)
        overlay.addView(bgImage)

        if (isWin) {
            val content = LinearLayout(context)
            content.orientation = LinearLayout.VERTICAL
            content.gravity = Gravity.CENTER_HORIZONTAL
            content.layoutParams = android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )

            val tv = TextView(context)
            tv.text = message
            tv.setTextColor(ContextCompat.getColor(context, R.color.cluedo_pink))
            tv.textSize = 18f
            tv.gravity = Gravity.CENTER
            tv.typeface = ResourcesCompat.getFont(context, R.font.freckle_face)
            tv.setShadowLayer(4f, 2f, 2f, Color.BLACK)
            tv.setPadding(0, 0, 0, dpToPx(context, 12))
            content.addView(tv)

            overlay.addView(content)
        }
        parent.addView(overlay)
    }
}