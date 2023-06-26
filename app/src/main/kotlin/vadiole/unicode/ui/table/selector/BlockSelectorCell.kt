package vadiole.unicode.ui.table.selector

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import vadiole.unicode.UnicodeApp.Companion.themeManager
import vadiole.unicode.ui.common.StateColorDrawable
import vadiole.unicode.ui.theme.ThemeDelegate
import vadiole.unicode.ui.theme.key_windowTextPrimary
import vadiole.unicode.ui.theme.keysWindowPressable
import vadiole.unicode.ui.theme.roboto_regular
import vadiole.unicode.ui.theme.statesPressable
import vadiole.unicode.utils.extension.dp

class BlockSelectorCell(context: Context) : TextView(context), ThemeDelegate {

    private val backgroundDrawable = StateColorDrawable()

    init {
        themeManager.observe(this)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        typeface = roboto_regular
        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false
        setPadding(0, 0, 16.dp(context), 0)
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        background = backgroundDrawable
    }

    override fun applyTheme() {
        setTextColor(themeManager.getColor(key_windowTextPrimary))
        backgroundDrawable.colors = themeManager.getColors(
            statesPressable,
            keysWindowPressable
        )
    }
}
