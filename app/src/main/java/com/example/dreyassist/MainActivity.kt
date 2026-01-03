package com.example.dreyassist

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
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
import com.example.dreyassist.data.MemoryEntity
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
import com.example.dreyassist.util.CategoryDetector
import com.example.dreyassist.util.QueryHandler
import com.example.dreyassist.util.VoiceParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    
    // Audio manager for silent mode during voice input
    private lateinit var audioManager: AudioManager
    private var previousRingerMode: Int = AudioManager.RINGER_MODE_NORMAL

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }
    
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale("id", "ID"))

    private val database by lazy { AppDatabase.getDatabase(this) }
    
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModelFactory(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }
    
    // QueryHandler for REMEMBER feature
    private val queryHandler by lazy {
        QueryHandler(database.transaksiDao(), database.journalDao(), database.reminderDao(), database.memoryDao())
    }

    // Data holders for combining
    private var currentTransaksi: List<TransaksiEntity> = emptyList()
    private var currentJournals: List<JournalEntity> = emptyList()
    private var currentReminders: List<ReminderEntity> = emptyList()
    private var currentMemories: List<MemoryEntity> = emptyList()

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
        
        // Insights card click
        binding.cardInsights.setOnClickListener {
            startActivity(Intent(this, InsightsActivity::class.java))
        }
        
        // Load today's spending for insights card
        loadInsightsSpending()
    }
    
    private fun loadInsightsSpending() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val todayStart = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                val spending = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    database.transaksiDao().getSpendingSince(todayStart) ?: 0
                }
                binding.textInsightsSpending.text = currencyFormat.format(spending)
                
                // Generate humanized narrative
                val narrative = when {
                    spending == 0 -> "Belum ada pengeluaran hari ini."
                    spending < 50000 -> "Pengeluaran ringan hari ini."
                    spending < 100000 -> "Pengeluaran sedang hari ini."
                    spending < 200000 -> "Lumayan banyak hari ini."
                    else -> "Hari yang sibuk untuk dompetmu!"
                }
                binding.textInsightNarrative.text = narrative
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        binding.fabCatatan.setOnClickListener {
            closeFabMenu()
            startActivity(Intent(this, MemoryListActivity::class.java))
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
            binding.fabCatatan,
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
            binding.fabCatatan,
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
        
        mainViewModel.allMemories.observe(this) { memories ->
            currentMemories = memories ?: emptyList()
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
        currentMemories.forEach { allItems.add(HistoryItem.fromMemory(it)) }
        
        // Sort by timestamp descending and take 1 (simplified view)
        val recentItems = allItems.sortedByDescending { it.timestamp }.take(1)
        
        if (recentItems.isEmpty()) {
            binding.textEmpty.visibility = View.VISIBLE
        } else {
            binding.textEmpty.visibility = View.GONE
            
            recentItems.forEach { item ->
                val itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_recent, binding.recentContainer, false)
                
                itemView.findViewById<TextView>(R.id.text_title).text = item.title
                itemView.findViewById<TextView>(R.id.text_date).text = dateFormat.format(Date(item.timestamp))
                
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
                restoreRingerMode() // Restore ringer mode
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
                restoreRingerMode() // Restore ringer mode
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
                            parsedResult.transactionDate,
                            parsedResult.transactionCategory
                        )
                        Category.PENGINGAT -> showReminderPreviewDialog(
                            parsedResult.keperluan,
                            parsedResult.reminderTime,
                            parsedResult.recurrenceType,
                            parsedResult.recurrenceInterval
                        )
                        Category.MEMORY -> showMemoryPreviewDialog(parsedResult.keperluan)
                        Category.QUERY -> handleQuery(parsedResult.keperluan)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startVoiceInput() {
        // Initialize audio manager if not done
        if (!::audioManager.isInitialized) {
            audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        }
        
        // Enable silent mode during voice input
        enableSilentMode()
        
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
    
    private fun enableSilentMode() {
        try {
            if (!::audioManager.isInitialized) {
                audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            }
            
            // Save current volumes
            previousRingerMode = audioManager.getStreamVolume(AudioManager.STREAM_RING)
            
            // Mute notification and ring streams (works without DND permission)
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun restoreRingerMode() {
        try {
            if (!::audioManager.isInitialized) return
            
            // Unmute streams
            audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
            audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleQuery(queryText: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = queryHandler.processQuery(queryText)
                showQueryAnswerDialog(queryText, result.answer)
            } catch (e: Exception) {
                showQueryAnswerDialog(queryText, "Maaf, terjadi error: ${e.message}")
            }
        }
    }

    private fun showQueryAnswerDialog(question: String, answer: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_query_answer)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.text_question).text = "\"$question\""
        dialog.findViewById<TextView>(R.id.text_answer).text = answer

        dialog.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTransactionPreviewDialog(keperluan: String, total: Int, keterangan: String, transactionDate: Long = System.currentTimeMillis(), category: String = "OTHER") {
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
        
        // Display category
        dialog.findViewById<android.widget.ImageView>(R.id.img_category).setImageResource(
            CategoryDetector.getCategoryIconResId(category)
        )
        dialog.findViewById<TextView>(R.id.text_category).text = 
            CategoryDetector.getCategoryName(category)

        // Close button (X)
        dialog.findViewById<android.widget.ImageButton>(R.id.btn_close).setOnClickListener {
            dialog.dismiss()
        }

        // Edit button - opens edit dialog
        dialog.findViewById<Button>(R.id.btn_edit).setOnClickListener {
            dialog.dismiss()
            showTransactionEditDialog(keperluan, total, keterangan, transactionDate, category)
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            if (keperluan.isNotBlank() && total > 0) {
                val transaksi = TransaksiEntity(
                    tanggal = transactionDate,
                    keperluan = keperluan,
                    total = total,
                    keterangan = keterangan,
                    category = category
                )
                mainViewModel.insertTransaksi(transaksi)
                Toast.makeText(this, "Transaksi disimpan!", Toast.LENGTH_SHORT).show()
                loadInsightsSpending() // Refresh insights card
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Keperluan dan Total harus diisi", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showTransactionEditDialog(initialKeperluan: String, initialTotal: Int, initialKeterangan: String, transactionDate: Long, initialCategory: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_transaksi)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val title = dialog.findViewById<TextView>(R.id.dialog_title)
        val editKeperluan = dialog.findViewById<android.widget.EditText>(R.id.edit_keperluan)
        val editTotal = dialog.findViewById<android.widget.EditText>(R.id.edit_total)
        val editKeterangan = dialog.findViewById<android.widget.EditText>(R.id.edit_keterangan)
        val spinnerCategory = dialog.findViewById<android.widget.Spinner>(R.id.spinner_category)

        title.text = "Edit Transaksi"
        editKeperluan.setText(initialKeperluan)
        editTotal.setText(initialTotal.toString())
        editKeterangan.setText(initialKeterangan)

        // Setup category spinner
        val categories = CategoryDetector.Category.entries.map { it.displayName }
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Set current category
        val currentCategoryIndex = CategoryDetector.Category.entries.indexOfFirst { it.name == initialCategory }
        if (currentCategoryIndex >= 0) {
            spinnerCategory.setSelection(currentCategoryIndex)
        }

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
            // Go back to preview with original values
            showTransactionPreviewDialog(initialKeperluan, initialTotal, initialKeterangan, transactionDate, initialCategory)
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            val keperluan = editKeperluan.text.toString().trim()
            val totalStr = editTotal.text.toString().replace(Regex("[^0-9]"), "")
            val total = totalStr.toIntOrNull() ?: 0
            val keterangan = editKeterangan.text.toString().trim()
            val selectedCategoryIndex = spinnerCategory.selectedItemPosition
            val category = CategoryDetector.Category.entries[selectedCategoryIndex].name

            if (keperluan.isNotBlank() && total > 0) {
                dialog.dismiss()
                // Show preview again with updated values
                showTransactionPreviewDialog(keperluan, total, keterangan, transactionDate, category)
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

    private fun showReminderPreviewDialog(content: String, reminderTime: Long, recurrenceType: String = "NONE", recurrenceInterval: Int = 1) {
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
                    reminderTime = reminderTime,
                    recurrenceType = recurrenceType,
                    recurrenceInterval = recurrenceInterval
                )
                mainViewModel.insertReminder(reminder) { reminderId ->
                    ReminderScheduler.scheduleReminder(
                        this,
                        reminderId.toInt(),
                        content,
                        reminderTime
                    )
                }
                val recurrenceText = if (recurrenceType != "NONE") " (${recurrenceType.lowercase()})" else ""
                Toast.makeText(this, "Pengingat disimpan!$recurrenceText", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Pengingat tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showMemoryPreviewDialog(content: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_preview_memory)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        dialog.findViewById<TextView>(R.id.text_content).text = 
            if (content.isNotBlank()) content else "-"

        dialog.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btn_save).setOnClickListener {
            if (content.isNotBlank()) {
                val memory = MemoryEntity(
                    content = content
                )
                mainViewModel.insertMemory(memory)
                Toast.makeText(this, "Catatan disimpan!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
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
