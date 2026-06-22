package com.pck.bricks.features.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pck.bricks.BricksApp
import com.pck.bricks.MainActivity
import com.pck.bricks.features.notifications.NotificationBuilder
import com.pck.bricks.features.wall.BrickLayoutCalculator
import com.pck.bricks.features.wall.WallRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Preference keys ──────────────────────────────────────────────────────────

private val KEY_INDEX = intPreferencesKey("widget_habit_index")
private val KEY_COUNT = intPreferencesKey("widget_habit_count")

// ── Navigation actions ────────────────────────────────────────────────────────

class PrevHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val idx   = prefs[KEY_INDEX] ?: 0
            val count = (prefs[KEY_COUNT] ?: 1).coerceAtLeast(1)
            prefs.toMutablePreferences().also { it[KEY_INDEX] = (idx - 1 + count) % count }
        }
        BricksWidget().update(context, glanceId)
    }
}

class NextHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val idx   = prefs[KEY_INDEX] ?: 0
            val count = (prefs[KEY_COUNT] ?: 1).coerceAtLeast(1)
            prefs.toMutablePreferences().also { it[KEY_INDEX] = (idx + 1) % count }
        }
        BricksWidget().update(context, glanceId)
    }
}

// ── Widget ────────────────────────────────────────────────────────────────────

class BricksWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app    = context.applicationContext as BricksApp
        val habits = app.habitRepository.getActiveHabitsOnce()
        val count  = habits.size

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { p ->
            p.toMutablePreferences().also { it[KEY_COUNT] = count }
        }

        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val index = (prefs[KEY_INDEX] ?: 0).coerceIn(0, (count - 1).coerceAtLeast(0))

        val habit    = habits.getOrNull(index)
        val progress = habit?.let { app.habitRepository.getProgressOnce(it.habitId) }

        val wallBitmap: Bitmap? = if (habit != null && progress != null) {
            withContext(Dispatchers.Default) {
                val layout = BrickLayoutCalculator().calculateLayout(progress.currentTier)
                val model  = WallRenderer().renderWall(progress, layout)
                WallBitmapRenderer().render(model, WALL_W, WALL_H)
            }
        } else null

        provideContent {
            val state      = currentState<Preferences>()
            val currentIdx = (state[KEY_INDEX] ?: 0).coerceIn(0, (count - 1).coerceAtLeast(0))
            WidgetContent(
                habitName    = habit?.name ?: "No habits",
                habitId      = habit?.habitId,
                wallBitmap   = wallBitmap,
                currentIndex = currentIdx,
                totalHabits  = count
            )
        }
    }

    companion object {
        const val WALL_W = 420
        const val WALL_H = 280

        suspend fun refreshAll(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids     = manager.getGlanceIds(BricksWidget::class.java)
            val widget  = BricksWidget()
            for (id in ids) widget.update(context, id)
        }
    }
}

// ── UI ────────────────────────────────────────────────────────────────────────

@GlanceComposable
@Composable
private fun WidgetContent(
    habitName: String,
    habitId: String?,
    wallBitmap: Bitmap?,
    currentIndex: Int,
    totalHabits: Int
) {
    val ctx = LocalContext.current
    val openApp = actionStartActivity(
        Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (habitId != null) putExtra(NotificationBuilder.EXTRA_HABIT_ID, habitId)
        }
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(android.graphics.Color.argb(232, 20, 20, 20)))
            .padding(8.dp)
            .clickable(openApp)
    ) {
        // ── Habit name ──────────────────────────────────────────────────────
        Text(
            text     = habitName,
            maxLines = 1,
            style    = TextStyle(
                color      = ColorProvider(android.graphics.Color.WHITE),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.fillMaxWidth()
        )

        Spacer(GlanceModifier.size(6.dp))

        // ── Wall bitmap — fills all remaining height ─────────────────────────
        if (wallBitmap != null) {
            Image(
                provider           = ImageProvider(wallBitmap),
                contentDescription = null,
                contentScale       = ContentScale.FillBounds,
                modifier           = GlanceModifier.fillMaxWidth().fillMaxHeight()
            )
        } else {
            Box(
                modifier         = GlanceModifier.fillMaxWidth().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Open Bricks to add habits",
                    style = TextStyle(
                        color    = ColorProvider(android.graphics.Color.argb(153, 255, 255, 255)),
                        fontSize = 12.sp
                    )
                )
            }
        }

        // ── Navigation row ──────────────────────────────────────────────────
        if (totalHabits > 1) {
            Spacer(GlanceModifier.size(4.dp))
            Row(
                modifier          = GlanceModifier.fillMaxWidth().wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier         = GlanceModifier
                        .size(40.dp)
                        .clickable(actionRunCallback<PrevHabitAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "‹",
                        style = TextStyle(
                            color    = ColorProvider(android.graphics.Color.argb(170, 255, 255, 255)),
                            fontSize = 26.sp
                        )
                    )
                }

                Text(
                    text  = "${currentIndex + 1} / $totalHabits",
                    style = TextStyle(
                        color     = ColorProvider(android.graphics.Color.argb(136, 255, 255, 255)),
                        fontSize  = 11.sp,
                        textAlign = TextAlign.Center
                    )
                )

                Box(
                    modifier         = GlanceModifier
                        .size(40.dp)
                        .clickable(actionRunCallback<NextHabitAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "›",
                        style = TextStyle(
                            color    = ColorProvider(android.graphics.Color.argb(170, 255, 255, 255)),
                            fontSize = 26.sp
                        )
                    )
                }
            }
        }
    }
}
