package vadiole.unicode.ui.details

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vadiole.unicode.R
import vadiole.unicode.UnicodeApp.Companion.unicodeStorage
import vadiole.unicode.data.CharObj
import vadiole.unicode.data.CodePoint
import vadiole.unicode.ui.common.ActionCell
import vadiole.unicode.ui.common.CharInfoView
import vadiole.unicode.ui.common.CharTextView
import vadiole.unicode.ui.common.Screen
import vadiole.unicode.ui.common.SpacerDrawable
import vadiole.unicode.ui.common.SquircleDrawable
import vadiole.unicode.ui.common.roboto_regular
import vadiole.unicode.ui.common.roboto_semibold
import vadiole.unicode.utils.extension.dp
import vadiole.unicode.utils.extension.frameParams
import vadiole.unicode.utils.extension.getDividerPaint
import vadiole.unicode.utils.extension.linearParams
import vadiole.unicode.utils.extension.matchParent
import vadiole.unicode.utils.extension.navigationBars
import vadiole.unicode.utils.extension.onClick
import vadiole.unicode.utils.extension.onLongClick
import vadiole.unicode.utils.extension.setLineHeightX
import vadiole.unicode.utils.extension.setPadding
import vadiole.unicode.utils.extension.share
import vadiole.unicode.utils.extension.toClipboard

class DetailsSheet(
    context: Context,
    private val delegate: Delegate,
) : Screen(context) {
    interface Delegate {
        fun findInTable(codePoint: CodePoint)
    }

    private var charObj: CharObj? = null
    private val screenPadding = 20.dp(context)
    private val verticalPadding = 10.dp(context)
    private val backgroundDrawable = SquircleDrawable(20.dp(context)).apply {
        skipBottomRight = true
        skipBottomLeft = true
    }
    private val backgroundPaint = Paint()
    private var vertical = 0
    private val titleHeight = 21.dp(context)
    private val title = TextView(context).apply(fun TextView.() {
        layoutParams = frameParams(matchParent, titleHeight, gravity = Gravity.TOP)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        setLineHeightX(21.dp(context))
        gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        includeFontPadding = false
        vertical += titleHeight
        ellipsize = TextUtils.TruncateAt.END
        typeface = roboto_semibold
        letterSpacing = 0.03f
        isSingleLine = true
    })
    private val subtitleHeight = 18.dp(context)
    private val subtitle = TextView(context).apply(fun TextView.() {
        layoutParams = frameParams(matchParent, subtitleHeight, gravity = Gravity.TOP, marginTop = vertical)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        setLineHeightX(18.dp(context))
        gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        textAlignment = TEXT_ALIGNMENT_CENTER
        vertical += subtitleHeight + verticalPadding
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
        typeface = roboto_regular
        letterSpacing = 0.02f
        isSingleLine = true
    })
    private var divider1PositionY = 2f * screenPadding + titleHeight + subtitleHeight
    private val charViewHeight = 200.dp(context)
    private val charView = CharTextView(context).apply {
        vertical += verticalPadding
        layoutParams = frameParams(matchParent, charViewHeight, gravity = Gravity.TOP, marginTop = vertical)
        vertical += charViewHeight
        textSize = 100f.dp(context)
    }
    private val infoViewHeight = 56.dp(context)
    private val infoViews = List(4) {
        CharInfoView(context).apply {
            layoutParams = linearParams(matchParent, infoViewHeight, weight = 1f)
            onLongClick = {
                charObj?.let { value ->
                    val info = value.infoValues[it]
                    context.toClipboard("Unicode", info)
                    Toast.makeText(context, "$info copied to clipboard", LENGTH_SHORT).show()
                }
            }
        }
    }
    private val infoViewsContainer = LinearLayout(context).apply {
        layoutParams = frameParams(matchParent, infoViewHeight, Gravity.TOP, marginTop = vertical)
        vertical += infoViewHeight + verticalPadding * 3
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        dividerDrawable = SpacerDrawable(width = 8.dp(context))
        infoViews.forEach { infoView ->
            addView(infoView)
        }
    }

    private val actionCellHeight = 48.dp(context)
    private val actionViewInTable = ActionCell(context, "Find in Table").apply {
        layoutParams = frameParams(matchParent, actionCellHeight, marginTop = vertical)
        setIcon(R.drawable.ic_find_in_table)
        vertical += actionCellHeight + 16.dp(context)
        onClick = {
            charObj?.let { value ->
                delegate.findInTable(CodePoint(value.codePointRaw))
            }
        }
    }

    private val actionCopy = ActionCell(context, "Copy to Clipboard", topItem = true).apply {
        layoutParams = frameParams(matchParent, actionCellHeight, marginTop = vertical)
        setIcon(R.drawable.ic_copy)
        vertical += actionCellHeight
        onClick = {
            charObj?.let { value ->
                val char = value.char
                context.toClipboard("Unicode", char)
                Toast.makeText(context, "$char copied to clipboard", LENGTH_SHORT).show()
            }
        }
    }
    private var divider2PositionY = vertical.toFloat() + screenPadding
    private val actionShare = ActionCell(context, "Share Link", bottomItem = true).apply {
        layoutParams = frameParams(matchParent, actionCellHeight, marginTop = vertical)
        setIcon(R.drawable.ic_link)
        vertical += actionCellHeight
        var canClick = true
        onClick = {
            if (canClick) {
                launch {
                    canClick = false
                    charObj?.let { value ->
                        context.share(value.getLink())
                    }
                    delay(500)
                    canClick = true
                }
            }
        }
        onLongClick = {
            charObj?.let { value ->
                context.toClipboard("Unicode", value.getLink())
                Toast.makeText(context, "Link copied to clipboard", LENGTH_SHORT).show()
            }
        }
    }

    init {
        backgroundDrawable.colors = this.context.getColorStateList(R.color.dialogBackground)
        backgroundPaint.color = this.context.getColor(R.color.dialogBackground)
        charView.textColor = this.context.getColor(R.color.windowTextPrimary)
        title.setTextColor(this.context.getColor(R.color.windowTextPrimary))
        subtitle.setTextColor(this.context.getColor(R.color.windowTextSecondary))
        background = backgroundDrawable
        val height = vertical + screenPadding * 2 + 36.dp(context)
        layoutParams = frameParams(matchParent, height, gravity = Gravity.BOTTOM)
        clipChildren = false
        setPadding(screenPadding)
        setWillNotDraw(false)
        addView(title)
        addView(subtitle)
        addView(charView)
        addView(infoViewsContainer)
        addView(actionViewInTable)
        addView(actionCopy)
        addView(actionShare)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bottomInset = insets.navigationBars.bottom
            updateLayoutParams<LayoutParams> {
                this.height = height + bottomInset
            }
            insets
        }
    }

    // TODO: add strings to localeManager
    private val infoNames: Array<String> = arrayOf("Code", "HTML", "CSS", "Version")
    fun bind(codePoint: CodePoint, abbreviations: Map<CodePoint, String>) = launch {
        val obj: CharObj = unicodeStorage.getCharObj(codePoint) ?: return@launch
        title.text = obj.name
        subtitle.text = obj.blockName
        charView.text = obj.char
        charView.abbreviation = abbreviations[codePoint]
        infoViews.forEachIndexed { index, infoView ->
            infoView.name = infoNames[index]
            infoView.value = obj.infoValues[index]
        }
        charObj = obj
    }

    private val dividerPaint = getDividerPaint()
    override fun draw(canvas: Canvas) {
        canvas.drawRect(
            0f, measuredHeight - 20f.dp(context), measuredWidth.toFloat(), measuredHeight * 200f,
            backgroundPaint
        )
        super.draw(canvas)
        canvas.drawLine(0f, divider1PositionY, measuredWidth.toFloat(), divider1PositionY, dividerPaint)
        canvas.drawLine(
            screenPadding.toFloat(),
            divider2PositionY,
            measuredWidth.toFloat() - screenPadding, divider2PositionY,
            dividerPaint,
        )
    }
}