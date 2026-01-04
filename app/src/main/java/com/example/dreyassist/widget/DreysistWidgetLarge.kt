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
            // Classic Greek/Ancient Philosophers
            Pair("The unexamined life is not worth living.", "Socrates"),
            Pair("Wise men speak because they have something to say; fools because they have to say something.", "Plato"),
            Pair("Knowing yourself is the beginning of all wisdom.", "Aristotle"),
            Pair("No man ever steps in the same river twice.", "Heraclitus"),
            Pair("Silence is better than meaningless words.", "Pythagoras"),
            Pair("Withdraw into yourself and look.", "Plotinus"),
            Pair("The foundation of every state is the education of its youth.", "Diogenes"),
            
            // Stoics
            Pair("You have power over your mind — not outside events.", "Marcus Aurelius"),
            Pair("It's not what happens to you, but how you react to it that matters.", "Epictetus"),
            Pair("We suffer more often in imagination than in reality.", "Seneca"),
            Pair("Luck is what happens when preparation meets opportunity.", "Seneca"),
            Pair("It is not the man who has too little, but the man who craves more, that is poor.", "Seneca"),
            Pair("We have two ears and one mouth, so we should listen more than we speak.", "Zeno of Citium"),
            Pair("Man conquers the world by conquering himself.", "Zeno of Citium"),
            Pair("We begin to lose our hesitation to do immoral things once we lose our hesitation to speak of them.", "Musonius Rufus"),
            Pair("Practice what you preach.", "Gaius Musonius"),
            Pair("I begin to speak only when I am certain what I will say is not better left unsaid.", "Cato the Younger"),
            Pair("Fate guides the willing and drags the unwilling.", "Cleanthes"),
            Pair("If you wish to be free, do not wish to control what you cannot.", "Chrysippus"),
            Pair("Self-control is the chief element in self-respect.", "Hierocles"),
            Pair("Bear and forbear.", "Arrian"),
            Pair("A life ruled by reason is the true life.", "Posidonius"),
            Pair("Virtue is the only good.", "Panaitios"),
            Pair("Happiness depends on ourselves.", "Arius Didymus"),
            
            // Eastern Philosophy
            Pair("He who learns but does not think, is lost.", "Confucius"),
            Pair("He who conquers himself is the mightiest warrior.", "Confucius"),
            Pair("Knowing others is intelligence; knowing yourself is true wisdom.", "Laozi"),
            Pair("The mind is everything. What you think, you become.", "Buddha"),
            Pair("Victorious warriors win first and then go to war.", "Sun Tzu"),
            Pair("Happiness is the absence of striving for happiness.", "Zhuangzi"),
            Pair("The great man is he who does not lose his child's heart.", "Mencius"),
            Pair("To study the self is to forget the self.", "Dogen"),
            Pair("Not thinking about anything is Zen.", "Bodhidharma"),
            Pair("If you search for the Buddha, you will lose the Buddha.", "Huang Po"),
            Pair("Why are you so busy with this or that? Good and bad, both pass.", "Rumi"),
            Pair("Where there is love, there is no law.", "Ibn Arabi"),
            Pair("Knowledge without action is wastefulness.", "Al-Ghazali"),
            Pair("Where do you search me? I am with you.", "Kabir"),
            Pair("Your own Self-realization is the greatest service you can render the world.", "Ramana Maharshi"),
            Pair("There is nothing that is not dependently arisen.", "Nagarjuna"),
            Pair("Brahman is real, the world is an appearance.", "Shankara"),
            Pair("Peace is every step.", "Thich Nhat Hanh"),
            Pair("Truth is a pathless land.", "Jiddu Krishnamurti"),
            Pair("Be realistic: plan for a miracle.", "Osho"),
            Pair("Happiness is not something ready-made. It comes from your own actions.", "Dalai Lama"),
            
            // Modern Western Philosophy
            Pair("Science is organized knowledge. Wisdom is organized life.", "Immanuel Kant"),
            Pair("He who has a why to live can bear almost any how.", "Friedrich Nietzsche"),
            Pair("Man is born free, and everywhere he is in chains.", "Jean-Jacques Rousseau"),
            Pair("Reading furnishes the mind only with materials of knowledge.", "John Locke"),
            Pair("Reason is, and ought only to be, the slave of the passions.", "David Hume"),
            Pair("The philosophers have only interpreted the world; the point is to change it.", "Karl Marx"),
            Pair("The trouble with the world is that the stupid are cocksure and the intelligent are full of doubt.", "Bertrand Russell"),
            Pair("The only thing we learn from history is that we learn nothing from history.", "G.W.F. Hegel"),
            Pair("The limits of my language mean the limits of my world.", "Ludwig Wittgenstein"),
            Pair("True ignorance is not the absence of knowledge, but the refusal to acquire it.", "Karl Popper"),
            Pair("Scientific revolutions are inaugurated by a growing sense that existing institutions have ceased adequately to meet the problems.", "Thomas Kuhn"),
            Pair("Knowledge itself is power.", "Francis Bacon"),
            Pair("Knowledge is power.", "Thomas Hobbes"),
            Pair("Beware of the person of one book.", "Thomas Aquinas"),
            Pair("Science is the great antidote to the poison of enthusiasm and superstition.", "Adam Smith"),
            Pair("Unseasonable kindness may be the greatest cruelty.", "John Stuart Mill"),
            Pair("Act as if what you do makes a difference. It does.", "William James"),
            Pair("The heart has its reasons which reason knows nothing of.", "Blaise Pascal"),
            Pair("The greatest thing in the world is to know how to belong to oneself.", "Michel de Montaigne"),
            Pair("Knowledge is not for knowing: knowledge is for cutting.", "Michel Foucault"),
            Pair("There is nothing outside of context.", "Jacques Derrida"),
            Pair("Talent hits a target no one else can hit.", "Arthur Schopenhauer"),
            Pair("Nations are imagined communities.", "Benedict Anderson"),
            Pair("Politics is the slow boring of hard boards.", "Max Weber"),
            
            // Existentialists
            Pair("Life can only be understood backwards; but it must be lived forwards.", "Søren Kierkegaard"),
            Pair("In the depth of winter, I finally learned that within me there lay an invincible summer.", "Albert Camus"),
            Pair("The struggle itself is enough to fill a man's heart.", "Albert Camus"),
            Pair("Freedom is what you do with what has been done to you.", "Jean-Paul Sartre"),
            Pair("Man is condemned to be free.", "Jean-Paul Sartre"),
            Pair("Change your life today.", "Simone de Beauvoir"),
            Pair("When we are no longer able to change a situation, we are challenged to change ourselves.", "Viktor Frankl"),
            Pair("Man is not the lord of beings. Man is the shepherd of Being.", "Martin Heidegger"),
            Pair("The decisive question is whether man can remain human.", "Karl Jaspers"),
            Pair("Life is not a problem to be solved, but a mystery to be lived.", "Gabriel Marcel"),
            Pair("We do not really die, we merely stop living.", "Miguel de Unamuno"),
            Pair("The mystery of human existence lies not in just staying alive, but in finding something to live for.", "Fyodor Dostoevsky"),
            Pair("The soul is healed by being with children.", "Fyodor Dostoevsky"),
            Pair("You do not need to leave your room. Remain sitting at your table and listen.", "Franz Kafka"),
            Pair("It is not worth the bother of killing yourself, since you always kill yourself too late.", "Emil Cioran"),
            Pair("Decision is a risk rooted in the courage of being free.", "Paul Tillich"),
            Pair("Optimism is cowardice.", "Oswald Spengler"),
            
            // Political & Social Philosophers
            Pair("The most radical revolutionary will become a conservative the day after the revolution.", "Hannah Arendt"),
            Pair("The sad truth is that most evil is done by people who never make up their minds to be good or evil.", "Hannah Arendt"),
            Pair("The only thing necessary for the triumph of evil is for good men to do nothing.", "Edmund Burke"),
            Pair("Freedom for the wolves has often meant death for the sheep.", "Isaiah Berlin"),
            Pair("The smart way to keep people passive is to strictly limit the spectrum of acceptable opinion.", "Noam Chomsky"),
            Pair("I swear by my life and my love of it that I will never live for the sake of another man.", "Ayn Rand"),
            Pair("Justice is what love looks like in public.", "Cornel West"),
            Pair("Attention is the rarest and purest form of generosity.", "Simone Weil"),
            Pair("The task we must set for ourselves is not to feel secure, but to be able to tolerate insecurity.", "Erich Fromm"),
            Pair("Excess positivity leads to burnout.", "Byung-Chul Han"),
            Pair("The problem is not that people don't know; the problem is that they know too much in the wrong way.", "Slavoj Žižek"),
            Pair("The question is not, can they reason? but, can they suffer?", "Peter Singer"),
            Pair("I can only answer the question 'What am I to do?' if I can answer the question 'Of what story am I a part?'", "Alasdair MacIntyre"),
            
            // Education & Character
            Pair("Education has for its object the formation of character.", "Herbert Spencer"),
            Pair("Education is not preparation for life; education is life itself.", "John Dewey"),
            Pair("Civilization advances by extending the number of operations we can perform without thinking.", "Alfred North Whitehead"),
            
            // Psychology
            Pair("Until you make the unconscious conscious, it will direct your life.", "Carl Jung"),
            Pair("Unexpressed emotions will never die.", "Sigmund Freud"),
            
            // Historical Figures & Leaders
            Pair("It is better to be feared than loved, if you cannot be both.", "Niccolò Machiavelli"),
            Pair("Happiness is when what you think, what you say, and what you do are in harmony.", "Mahatma Gandhi"),
            Pair("Resentment is like drinking poison and hoping it will kill your enemies.", "Nelson Mandela"),
            Pair("Character is like a tree and reputation like its shadow.", "Abraham Lincoln"),
            Pair("Injustice anywhere is a threat to justice everywhere.", "Martin Luther King Jr."),
            Pair("Those who cannot remember the past are condemned to repeat it.", "George Santayana"),
            Pair("Example is not the main thing in influencing others. It is the only thing.", "Albert Schweitzer"),
            Pair("Man must cease attributing his problems to his environment.", "Albert Schweitzer"),
            
            // Scientists & Modern Thinkers
            Pair("Try not to become a man of success, but rather try to become a man of value.", "Albert Einstein"),
            Pair("Intelligence is the ability to adapt to change.", "Stephen Hawking"),
            Pair("All truths are easy to understand once they are discovered.", "Galileo Galilei"),
            Pair("If I have seen further it is by standing on the shoulders of giants.", "Isaac Newton"),
            
            // Writers
            Pair("In a time of deceit, telling the truth is a revolutionary act.", "George Orwell"),
            Pair("Everyone thinks of changing the world, but no one thinks of changing himself.", "Leo Tolstoy"),
            
            // American Transcendentalists
            Pair("What lies behind us and what lies before us are tiny matters compared to what lies within us.", "Ralph Waldo Emerson"),
            Pair("Go confidently in the direction of your dreams.", "Henry David Thoreau"),
            Pair("The highest activity a human being can attain is learning for understanding.", "Baruch Spinoza"),
            Pair("Judge a man by his questions rather than his answers.", "Voltaire")
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
