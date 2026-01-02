package com.example.dreyassist

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.dreyassist.data.AppDatabase
import com.example.dreyassist.data.JournalEntity
import com.example.dreyassist.data.ReminderEntity
import com.example.dreyassist.data.TransaksiEntity
import com.example.dreyassist.databinding.ActivityMainBinding
import com.example.dreyassist.notification.NotificationHelper
import com.example.dreyassist.notification.ReminderScheduler
import com.example.dreyassist.ui.HistoryItem
import com.example.dreyassist.ui.ItemType
import com.example.dreyassist.ui.MainViewModel
import com.example.dreyassist.ui.MainViewModelFactory
import com.example.dreyassist.util.Category
import com.example.dreyassist.util.VoiceParser
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isRecording = false
    
    private lateinit var rippleAnim1: Animation
    private lateinit var rippleAnim2: Animation
    private lateinit var rippleAnim3: Animation
    private lateinit var pulseAnim: Animation
    
    private var mediaPlayer: MediaPlayer? = null
    
    private var isFabOpen = false

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale("id", "ID"))

    private val database by lazy { AppDatabase.getDatabase(this) }
    
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao())
    }

    // Data holders for combining
    private var currentTransaksi: List<TransaksiEntity> = emptyList()
    private var currentJournals: List<JournalEntity> = emptyList()
    private var currentReminders: List<ReminderEntity> = emptyList()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startVoiceInput()
        } else {
            Toast.makeText(this, "Izin merekam suara ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setupAnimations()
        setupVoiceButton()
        setupSpeechRecognizer()
        observeAllItems()

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // FAB Menu
        setupFabMenu()

        // Check if opened from widget
        if (intent.getBooleanExtra("start_voice", false)) {
            binding.voiceButton.postDelayed({
                checkPermissionAndStartVoiceInput()
            }, 500)
        }
    }

    private fun setupFabMenu() {
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabOverlay.setOnClickListener {
            closeFabMenu()
        }

        binding.fabTransaksi.setOnClickListener {
            closeFabMenu()
            startActivity(Intent(this, TransaksiListActivity::class.java))
        }

        binding.fabJurnal.setOnClickListener {
            closeFabMenu()
            startActivity(Intent(this, JournalListActivity::class.java))
        }

        binding.fabPengingat.setOnClickListener {
            closeFabMenu()
            startActivity(Intent(this, ReminderListActivity::class.java))
        }

        binding.fabBackup.setOnClickListener {
            closeFabMenu()
            startActivity(Intent(this, BackupActivity::class.java))
        }

        binding.fabAbout.setOnClickListener {
            closeFabMenu()
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun toggleFabMenu() {
        if (isFabOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }

    private fun openFabMenu() {
        isFabOpen = true
        binding.fabOverlay.visibility = View.VISIBLE
        binding.fabMenuContainer.visibility = View.VISIBLE
        binding.fabIcon.setImageResource(R.drawable.ic_close)
        
        // Animate FAB rotation
        binding.fabMain.animate().rotation(180f).setDuration(200).start()
        
        // Animate overlay fade in
        binding.fabOverlay.alpha = 0f
        binding.fabOverlay.animate().alpha(1f).setDuration(200).start()
        
        // Animate menu items with staggered effect
        val menuItems = listOf(
            binding.fabTransaksi,
            binding.fabJurnal,
            binding.fabPengingat,
            binding.fabBackup,
            binding.fabAbout
        )
        
        menuItems.forEachIndexed { index, item ->
            item.alpha = 0f
            item.translationX = 50f
            item.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay((index * 50).toLong())
                .setDuration(200)
                .start()
        }
    }

    private fun closeFabMenu() {
        isFabOpen = false
        binding.fabIcon.setImageResource(R.drawable.ic_menu)
        
        binding.fabMain.animate().rotation(0f).setDuration(200).start()
        
        // Animate menu items closing with staggered effect (reverse order)
        val menuItems = listOf(
            binding.fabAbout,
            binding.fabBackup,
            binding.fabPengingat,
            binding.fabJurnal,
            binding.fabTransaksi
        )
        
        menuItems.forEachIndexed { index, item ->
            item.animate()
                .alpha(0f)
                .translationX(50f)
                .setStartDelay((index * 30).toLong())
                .setDuration(150)
                .start()
        }
        
        binding.fabOverlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.fabOverlay.visibility = View.GONE
                binding.fabMenuContainer.visibility = View.GONE
            }
            .start()
    }

    private fun setupAnimations() {
        rippleAnim1 = AnimationUtils.loadAnimation(this, R.anim.ripple_expand)
        rippleAnim2 = AnimationUtils.loadAnimation(this, R.anim.ripple_expand).apply {
            startOffset = 600
        }
        rippleAnim3 = AnimationUtils.loadAnimation(this, R.anim.ripple_expand).apply {
            startOffset = 1200
        }
        pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)

        startIdleAnimations()
    }

    private fun startIdleAnimations() {
        binding.ripple1.startAnimation(rippleAnim1)
        binding.ripple2.startAnimation(rippleAnim2)
        binding.ripple3.startAnimation(rippleAnim3)
        
        // Start logo floating animation
        val logoFloatAnim = AnimationUtils.loadAnimation(this, R.anim.logo_float)
        binding.logoImage.startAnimation(logoFloatAnim)
    }

    private fun stopIdleAnimations() {
        binding.ripple1.clearAnimation()
        binding.ripple2.clearAnimation()
        binding.ripple3.clearAnimation()
        binding.logoImage.clearAnimation()
    }
    private fun setupVoiceButton() {
        binding.voiceButton.setOnClickListener {
            if (!isRecording) {
                checkPermissionAndStartVoiceInput()
            }
        }
    }

    private fun observeAllItems() {
        mainViewModel.allTransaksi.observe(this) { transactions ->
            currentTransaksi = transactions ?: emptyList()
            updateRecentView()
        }
        
        mainViewModel.allJournal.observe(this) { journals ->
            currentJournals = journals ?: emptyList()
            updateRecentView()
        }
        
        mainViewModel.allReminders.observe(this) { reminders ->
            currentReminders = reminders ?: emptyList()
            updateRecentView()
        }
    }

    private fun updateRecentView() {
        binding.recentContainer.removeAllViews()
        
        // Combine all items into HistoryItem list
        val allItems = mutableListOf<HistoryItem>()
        currentTransaksi.forEach { allItems.add(HistoryItem.fromTransaksi(it)) }
        currentJournals.forEach { allItems.add(HistoryItem.fromJournal(it)) }
        currentReminders.forEach { allItems.add(HistoryItem.fromReminder(it)) }
        
        // Sort by timestamp descending and take 3
        val recentItems = allItems.sortedByDescending { it.timestamp }.take(3)
        
        if (recentItems.isEmpty()) {
            binding.textEmpty.visibility = View.VISIBLE
        } else {
            binding.textEmpty.visibility = View.GONE
            
            recentItems.forEach { item ->
                val itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_recent, binding.recentContainer, false)
                
                val typeText = when (item.type) {
                    ItemType.TRANSAKSI -> "T"
                    ItemType.JURNAL -> "J"
                    ItemType.PENGINGAT -> "P"
                }
                
                itemView.findViewById<TextView>(R.id.text_type).text = typeText
                itemView.findViewById<TextView>(R.id.text_title).text = item.title
                itemView.findViewById<TextView>(R.id.text_subtitle).text = item.subtitle
                
                binding.recentContainer.addView(itemView)
            }
        }
    }

    private fun checkPermissionAndStartVoiceInput() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVoiceInput()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isRecording = true
                binding.textStatus.text = "Listening..."
                binding.voiceButton.startAnimation(pulseAnim)
                stopIdleAnimations()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                binding.textStatus.text = "Processing..."
            }

            override fun onError(error: Int) {
                isRecording = false
                binding.voiceButton.clearAnimation()
                startIdleAnimations()
                binding.textStatus.text = "Tap the button to start"
                
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada suara terdeteksi"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu habis, coba lagi"
                    else -> "Error: $error"
                }
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                binding.voiceButton.clearAnimation()
                startIdleAnimations()
                binding.textStatus.text = "Tap the button to start"

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val parsedResult = VoiceParser.parse(text)
                    
                    when (parsedResult.category) {
                        Category.JURNAL -> showJournalPreviewDialog(parsedResult.keperluan)
                        Category.TRANSAKSI -> showTransactionPreviewDialog(
                            parsedResult.keperluan,
                            parsedResult.total,
                            parsedResult.keterangan,
                            parsedResult.transactionDate
                        )
                        Category.PENGINGAT -> showReminderPreviewDialog(
                            parsedResult.keperluan,
                            parsedResult.reminderTime
                        )
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceInput() {
        // Release previous instance if any
        mediaPlayer?.release()
        
        // Play custom welcome sound
        mediaPlayer = MediaPlayer.create(this, R.raw.welcome_voice)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
        }
        // Set volume to maximum (1.0 = 100%)
        mediaPlayer?.setVolume(1.0f, 1.0f)
        mediaPlayer?.start()
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan bicara...")
        // Disable default beep sound
        intent.putExtra("android.speech.extra.BEEP", false)
        speechRecognizer.startListening(intent)
    }

    private fun showTransactionPreviewDialog(keperluan: String, total: Int, keterangan: String, transactionDate: Long = System.currentTimeMillis()) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_preview)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.text_keperluan).text = 
            if (keperluan.isNotBlank()) keperluan else "-"
        dialog.findViewById<TextView>(R.id.text_total).text = 
            if (total > 0) currencyFormat.format(total) else "Rp 0"
        dialog.findViewById<TextView>(R.id.text_keterangan).text = 
            if (keterangan.isNotBlank()) keterangan else "-"

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            if (keperluan.isNotBlank() && total > 0) {
                val transaksi = TransaksiEntity(
                    tanggal = transactionDate,
                    keperluan = keperluan,
                    total = total,
                    keterangan = keterangan
                )
                mainViewModel.insertTransaksi(transaksi)
                Toast.makeText(this, "Transaksi disimpan!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Keperluan dan Total harus diisi", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showJournalPreviewDialog(kegiatan: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_preview_journal)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.text_kegiatan).text = 
            if (kegiatan.isNotBlank()) kegiatan else "-"

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            if (kegiatan.isNotBlank()) {
                val journal = JournalEntity(
                    tanggal = System.currentTimeMillis(),
                    kegiatan = kegiatan
                )
                mainViewModel.insertJournal(journal)
                Toast.makeText(this, "Jurnal disimpan!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Kegiatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showReminderPreviewDialog(content: String, reminderTime: Long) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_preview_reminder)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.text_content).text = 
            if (content.isNotBlank()) content else "-"
        dialog.findViewById<TextView>(R.id.text_date).text = dateFormat.format(Date(reminderTime))
        dialog.findViewById<TextView>(R.id.text_time).text = timeFormat.format(Date(reminderTime))

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            if (content.isNotBlank()) {
                val reminder = ReminderEntity(
                    content = content,
                    reminderTime = reminderTime
                )
                mainViewModel.insertReminder(reminder) { reminderId ->
                    ReminderScheduler.scheduleReminder(
                        this,
                        reminderId.toInt(),
                        content,
                        reminderTime
                    )
                }
                Toast.makeText(this, "Pengingat disimpan!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Pengingat tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
