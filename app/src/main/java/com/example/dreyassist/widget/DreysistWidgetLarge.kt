package com.example.dreyassist.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.dreyassist.MainActivity
import com.example.dreyassist.R
import com.example.dreyassist.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class DreysistWidgetLarge : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // First widget added
    }

    override fun onDisabled(context: Context) {
        // Last widget removed
    }

    companion object {
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
        
        // Quotes from famous philosophers and leaders
        private val quotes = listOf(
            Pair("The unexamined life is not worth living.", "Socrates"),
            Pair("Wise men speak because they have something to say; fools because they have to say something.", "Plato"),
            Pair("Knowing yourself is the beginning of all wisdom.", "Aristotle"),
            Pair("He who learns but does not think, is lost.", "Confucius"),
            Pair("Knowing others is intelligence; knowing yourself is true wisdom.", "Laozi"),
            Pair("You have power over your mind — not outside events.", "Marcus Aurelius"),
            Pair("It's not what happens to you, but how you react to it that matters.", "Epictetus"),
            Pair("Luck is what happens when preparation meets opportunity.", "Seneca"),
            Pair("Science is organized knowledge. Wisdom is organized life.", "Immanuel Kant"),
            Pair("He who has a why to live can bear almost any how.", "Friedrich Nietzsche"),
            Pair("Man is born free, and everywhere he is in chains.", "Jean-Jacques Rousseau"),
            Pair("Reading furnishes the mind only with materials of knowledge.", "John Locke"),
            Pair("Reason is, and ought only to be, the slave of the passions.", "David Hume"),
            Pair("The philosophers have only interpreted the world; the point is to change it.", "Karl Marx"),
            Pair("The trouble with the world is that the stupid are cocksure and the intelligent are full of doubt.", "Bertrand Russell"),
            Pair("In the depth of winter, I finally learned that within me there lay an invincible summer.", "Albert Camus"),
            Pair("Freedom is what you do with what has been done to you.", "Jean-Paul Sartre"),
            Pair("The most radical revolutionary will become a conservative the day after the revolution.", "Hannah Arendt"),
            Pair("Victorious warriors win first and then go to war.", "Sun Tzu"),
            Pair("It is better to be feared than loved, if you cannot be both.", "Niccolò Machiavelli"),
            Pair("Happiness is when what you think, what you say, and what you do are in harmony.", "Mahatma Gandhi"),
            Pair("Resentment is like drinking poison and hoping it will kill your enemies.", "Nelson Mandela"),
            Pair("Character is like a tree and reputation like its shadow.", "Abraham Lincoln"),
            Pair("Injustice anywhere is a threat to justice everywhere.", "Martin Luther King Jr."),
            Pair("Try not to become a man of success, but rather try to become a man of value.", "Albert Einstein"),
            Pair("Intelligence is the ability to adapt to change.", "Stephen Hawking"),
            Pair("All truths are easy to understand once they are discovered.", "Galileo Galilei"),
            Pair("If I have seen further it is by standing on the shoulders of giants.", "Isaac Newton"),
            Pair("In a time of deceit, telling the truth is a revolutionary act.", "George Orwell"),
            Pair("Everyone thinks of changing the world, but no one thinks of changing himself.", "Leo Tolstoy"),
            Pair("The soul is healed by being with children.", "Fyodor Dostoevsky"),
            Pair("When we are no longer able to change a situation, we are challenged to change ourselves.", "Victor Frankl"),
            Pair("Until you make the unconscious conscious, it will direct your life.", "Carl Jung"),
            Pair("Unexpressed emotions will never die.", "Sigmund Freud"),
            Pair("The highest activity a human being can attain is learning for understanding.", "Baruch Spinoza"),
            Pair("Judge a man by his questions rather than his answers.", "Voltaire"),
            Pair("Knowledge is power.", "Thomas Hobbes"),
            Pair("Talent hits a target no one else can hit.", "Arthur Schopenhauer"),
            Pair("What lies behind us and what lies before us are tiny matters compared to what lies within us.", "Ralph Waldo Emerson"),
            Pair("Go confidently in the direction of your dreams.", "Henry David Thoreau"),
            Pair("The heart has its reasons which reason knows nothing of.", "Blaise Pascal"),
            Pair("Change your life today.", "Simone de Beauvoir"),
            Pair("The greatest thing in the world is to know how to belong to oneself.", "Michel de Montaigne"),
            Pair("No man ever steps in the same river twice.", "Heraclitus"),
            Pair("Silence is better than meaningless words.", "Pythagoras"),
            Pair("Beware of the person of one book.", "Thomas Aquinas"),
            Pair("Science is the great antidote to the poison of enthusiasm and superstition.", "Adam Smith"),
            Pair("Unseasonable kindness may be the greatest cruelty.", "John Stuart Mill"),
            Pair("Act as if what you do makes a difference. It does.", "William James"),
            Pair("Life can only be understood backwards; but it must be lived forwards.", "Søren Kierkegaard")
        )

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_dreysist_large)
            // Day and date need manual update (on screen unlock)
            val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
            val now = Date()
            
            views.setTextViewText(R.id.text_date, dayFormat.format(now))
            views.setTextViewText(R.id.text_date_full, dateFormat.format(now))
            
            // Set random quote - changes on each update (lock/unlock)
            val randomQuote = quotes[Random.nextInt(quotes.size)]
            views.setTextViewText(R.id.text_quote, "\"${randomQuote.first}\"")
            views.setTextViewText(R.id.text_author, "— ${randomQuote.second}")

            // Set up click intent for whole widget (opens voice input)
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("start_voice", true)
            }
            val voicePendingIntent = PendingIntent.getActivity(
                context,
                1,
                voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, voicePendingIntent)

            // Load data asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    
                    val spending = database.transaksiDao().getSpendingSince(todayStart) ?: 0
                    val activeReminders = database.reminderDao().getActiveCount()
                    
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.text_spending, currencyFormat.format(spending))
                        views.setTextViewText(R.id.text_reminders, activeReminders.toString())
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Update widget with initial values
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, DreysistWidgetLarge::class.java)
            )
            for (widgetId in widgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }
}
