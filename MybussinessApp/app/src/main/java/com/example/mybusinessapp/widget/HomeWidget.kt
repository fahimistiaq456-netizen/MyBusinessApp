package com.example.mybusinessapp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.mybusinessapp.MainActivity
import com.example.mybusinessapp.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_home)

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val currentTime = timeFormat.format(Date())
            views.setTextViewText(R.id.widget_time, currentTime)

            views.setTextViewText(R.id.widget_profit_value, "$1.2K")
            views.setTextViewText(R.id.widget_growth_value, "+5.2%")
            views.setTextViewText(R.id.widget_orders_value, "16")
            views.setImageViewResource(R.id.widget_app_icon, R.drawable.ic_widget_logo)

            // Click Listeners
            val rootIntent = Intent(context, MainActivity::class.java)
            val rootPendingIntent = PendingIntent.getActivity(
                context,
                0,
                rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, rootPendingIntent)

            val dashIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination", "dashboard")
            }
            val dashPendingIntent = PendingIntent.getActivity(
                context,
                1,
                dashIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_dashboard, dashPendingIntent)

            val prodIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("destination", "products")
            }
            val prodPendingIntent = PendingIntent.getActivity(
                context,
                2,
                prodIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_products, prodPendingIntent)

            val openPendingIntent = PendingIntent.getActivity(
                context,
                3,
                rootIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_open, openPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
