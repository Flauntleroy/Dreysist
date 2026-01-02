package com.example.dreyassist.util

import java.util.Calendar
import java.util.Locale

enum class Category {
    TRANSAKSI,
    JURNAL,
    PENGINGAT,
    MEMORY,
    QUERY  // For REMEMBER feature - asking questions
}

object VoiceParser {

    private val numberWords = mapOf(
        "satu" to 1, "dua" to 2, "tiga" to 3, "empat" to 4, "lima" to 5,
        "enam" to 6, "tujuh" to 7, "delapan" to 8, "sembilan" to 9, "sepuluh" to 10,
        "sebelas" to 11, "dua belas" to 12, "tiga belas" to 13, "empat belas" to 14,
        "lima belas" to 15, "enam belas" to 16, "tujuh belas" to 17, "delapan belas" to 18,
        "sembilan belas" to 19, "dua puluh" to 20, "seratus" to 100, "seribu" to 1000
    )

    private val monthNames = mapOf(
        "januari" to 0, "februari" to 1, "maret" to 2, "april" to 3,
        "mei" to 4, "juni" to 5, "juli" to 6, "agustus" to 7,
        "september" to 8, "oktober" to 9, "november" to 10, "desember" to 11
    )

    // ============================================================
    // TRANSACTION KEYWORDS - Pengeluaran, Pembelian, Pemasukan
    // ============================================================
    private val transactionKeywords = listOf(
        // === PEMBELIAN / PENGELUARAN ===
        "beli", "membeli", "beliin", "beli-beli", "belanjaan",
        "bayar", "membayar", "bayarin", "dibayar", "terbayar",
        "belanja", "berbelanja", "shopping", "shop",
        "jajan", "ngejajan", "beli jajan",
        "ngopi", "kopi", "beli kopi", "starbucks", "cafe",
        "makan", "makan siang", "makan malam", "sarapan", "breakfast", "lunch", "dinner",
        "minum", "beli minum", "minuman",
        "isi ulang", "top up", "topup", "isi saldo", "tambah saldo",
        "isi pulsa", "beli pulsa", "pulsa",
        "beli kuota", "kuota", "paket data", "paket internet",
        "beli token", "token listrik", "token pln",
        "beli listrik", "bayar listrik",
        "parkir", "bayar parkir", "biaya parkir", "ongkos parkir",
        "tol", "bayar tol", "e-toll", "etol",
        
        // === TRANSPORTASI ===
        "grab", "gojek", "ojol", "ojek", "ojek online",
        "taksi", "taxi", "uber", "maxim",
        "grabcar", "gocar", "grabfood", "gofood",
        "bensin", "beli bensin", "isi bensin", "ngisi bensin",
        "pertamax", "pertalite", "premium", "solar", "dexlite",
        "spbu", "pom bensin",
        "bus", "kereta", "krl", "mrt", "lrt", "transjakarta",
        "tiket", "beli tiket", "tiket pesawat", "tiket kereta",
        
        // === BELANJA RUTIN ===
        "belanja bulanan", "belanja mingguan", "belanja harian",
        "grocery", "groceries", "supermarket", "minimarket",
        "indomaret", "alfamart", "alfamidi", "lawson",
        "sembako", "beras", "minyak", "gula", "garam",
        "sayur", "buah", "daging", "ikan", "telur", "susu",
        
        // === FASHION & GADGET ===
        "beli baju", "beli celana", "beli sepatu", "beli sandal",
        "beli tas", "beli jam", "beli aksesoris",
        "beli hp", "beli handphone", "beli smartphone",
        "beli laptop", "beli komputer", "beli pc",
        "beli charger", "beli kabel", "beli earphone", "beli headset",
        
        // === KESEHATAN ===
        "beli obat", "obat", "apotek", "apotik", "farmasi",
        "ke dokter", "biaya dokter", "konsultasi dokter",
        "biaya rs", "rumah sakit", "rawat inap", "rawat jalan",
        "medical check up", "check up", "cek kesehatan",
        "tes lab", "laboratorium", "rontgen", "usg",
        
        // === CICILAN & TAGIHAN ===
        "cicilan", "bayar cicilan", "angsuran", "kredit", "bayar kredit",
        "kpr", "kta", "kartu kredit", "cc",
        "sewa", "bayar sewa", "uang sewa", "kontrak",
        "kos", "bayar kos", "uang kos", "kost", "kosan",
        "kontrakan", "bayar kontrakan",
        "listrik", "bayar listrik", "tagihan listrik", "pln",
        "pdam", "bayar air", "tagihan air", "air pdam",
        "wifi", "internet", "indihome", "firstmedia", "biznet",
        "tagihan", "bayar tagihan", "bill", "bills",
        "pajak", "bayar pajak", "pbb", "pajak kendaraan", "stnk",
        
        // === SOSIAL & KEAGAMAAN ===
        "donasi", "sumbangan", "amal",
        "sedekah", "bersedekah", "infaq", "infak",
        "zakat", "bayar zakat", "zakat fitrah", "zakat mal",
        "kurban", "qurban",
        "transfer", "kirim uang", "tf", "trf",
        "kasih", "ngasih", "kasih uang",
        "pinjam", "minjem", "pinjamin", "dipinjam",
        
        // === HIBURAN ===
        "nonton", "bioskop", "tiket nonton", "cinema", "xxi", "cgv",
        "netflix", "spotify", "youtube premium", "disney+", "vidio",
        "langganan", "subscription", "subs",
        "game", "beli game", "top up game", "diamond", "uc",
        
        // === PENDIDIKAN ===
        "spp", "bayar spp", "uang sekolah", "uang kuliah",
        "buku", "beli buku", "alat tulis", "atk",
        "kursus", "les", "bimbel", "les privat",
        
        // === PEMASUKAN / PENDAPATAN ===
        "gaji", "terima gaji", "dapat gaji", "gajian", "salary",
        "bonus", "dapat bonus", "terima bonus", "thr",
        "uang masuk", "pemasukan", "income", "pendapatan",
        "terima uang", "dapat uang", "dikasih uang", "dikirimin",
        "hasil jualan", "laba", "profit", "keuntungan", "untung",
        "cashback", "dapat cashback", "koin", "poin",
        "reimburse", "ganti rugi", "diganti", "penggantian",
        "refund", "pengembalian", "uang kembali",
        "hasil freelance", "project", "proyek", "fee",
        "komisi", "commission",
        
        // === NOMINAL INDICATORS ===
        "habis", "keluar", "menghabiskan", "mengeluarkan", "spent",
        "total", "senilai", "seharga", "sebesar", "harga", "nominal",
        "ribu", "rb", "juta", "jt", "rupiah", "rp"
    )
    
    private val transactionExclusions = listOf(
        "hari ini mengerjakan", "hari ini melakukan", "hari ini menyelesaikan",
        "aku merasa", "saya merasa", "curhat", "cerita",
        "ingatkan", "remind", "jangan lupa", "alarm"
    )

    // ============================================================
    // JOURNAL KEYWORDS - Kegiatan, Catatan Harian, Cerita
    // ============================================================
    private val journalKeywords = listOf(
        // === AKTIVITAS HARI INI ===
        "hari ini", "hari ini aku", "hari ini saya", "hari ini kami", "hari ini kita",
        "hari ini mengerjakan", "hari ini melakukan", "hari ini menyelesaikan",
        "hari ini aku mengerjakan", "hari ini saya mengerjakan",
        "hari ini aku melakukan", "hari ini saya melakukan",
        "hari ini aku menyelesaikan", "hari ini saya menyelesaikan",
        "hari ini berhasil", "hari ini gagal",
        "kegiatan hari ini", "aktivitas hari ini",
        
        // === CERITA / CURHATAN ===
        "curhat", "mau curhat", "curhat ya", "aku mau curhat", "saya mau curhat",
        "cerita", "mau cerita", "cerita ya", "aku mau cerita", "saya mau cerita",
        "aku cerita", "saya cerita", "aku curhat", "saya curhat",
        "jadi gini", "jadi begini", "jadi ceritanya", "ceritanya",
        "jadi tadi", "tadi tuh", "tadi kan",
        
        // === WAKTU KEJADIAN ===
        "tadi", "tadi aku", "tadi saya", "tadi kami", "tadi kita",
        "barusan", "barusan aku", "barusan saya", "baru aja", "baru saja",
        "pagi ini", "pagi tadi", "pas pagi",
        "siang ini", "siang tadi", "pas siang",
        "sore ini", "sore tadi", "pas sore",
        "malam ini", "tadi malam", "semalam", "kemarin malam",
        "kemarin", "kemarin aku", "kemarin saya",
        "minggu lalu", "bulan lalu", "waktu itu",
        
        // === PERASAAN / REFLEKSI ===
        "aku merasa", "saya merasa", "gue merasa", "gw merasa",
        "aku rasa", "saya rasa", "rasanya", "perasaanku",
        "aku bersyukur", "saya bersyukur", "bersyukur banget", "alhamdulillah",
        "aku senang", "saya senang", "seneng banget", "happy",
        "aku sedih", "saya sedih", "sedih banget", "sad",
        "aku kecewa", "saya kecewa", "kecewa banget", "disappointed",
        "aku lelah", "saya lelah", "capek banget", "exhausted",
        "aku capek", "saya capek", "cape", "tired",
        "aku bangga", "saya bangga", "bangga banget", "proud",
        "aku berhasil", "saya berhasil", "berhasil!", "sukses!",
        "aku gagal", "saya gagal", "gagal", "failed",
        "aku menyesal", "saya menyesal", "nyesel", "regret",
        "aku belajar", "saya belajar", "belajar dari",
        "aku sadar", "saya sadar", "tersadar", "baru sadar",
        "aku kesal", "saya kesal", "kesel banget", "marah",
        "aku bingung", "saya bingung", "bingung banget", "confused",
        "aku takut", "saya takut", "takut banget", "khawatir",
        "aku cemas", "saya cemas", "anxiety", "anxious",
        "aku lega", "saya lega", "lega banget", "relieved",
        "aku excited", "aku semangat", "saya semangat", "semangat banget",
        
        // === PENCAPAIAN ===
        "berhasil", "sukses", "selesai", "tuntas", "complete", "completed",
        "akhirnya", "akhirnya aku", "akhirnya saya", "finally",
        "bisa", "bisa aku", "bisa saya", "mampu", "berhasil",
        "tercapai", "achieved", "accomplish",
        "milestone", "progress", "maju",
        
        // === CATATAN / DIARY ===
        "catat", "catatan", "note", "notes", "mencatat",
        "tulis", "tulisan", "menulis",
        "diary", "diari", "buku harian",
        "jurnal", "jurnal hari ini", "daily journal", "journal",
        "memo", "memorandum",
        "log", "daily log", "work log",
        
        // === KEJADIAN / EVENT ===
        "kejadian", "peristiwa", "event", "acara",
        "meeting", "rapat", "pertemuan", "diskusi",
        "presentasi", "present", "demo",
        "wawancara", "interview",
        "ulang tahun", "ultah", "birthday", "anniversary",
        "pernikahan", "nikahan", "wedding",
        "wisuda", "graduation", "lulus",
        "liburan", "jalan-jalan", "traveling", "trip",
        "olahraga", "gym", "lari", "jogging", "workout",
        
        // === PEKERJAAN ===
        "hari ini kerja", "kerja hari ini",
        "di kantor", "di office", "wfh", "work from home",
        "ngoding", "coding", "programming",
        "desain", "design", "bikin design",
        "nulis", "writing", "bikin artikel",
        "riset", "research", "analisis"
    )
    
    private val journalExclusions = listOf(
        "ingatkan", "remind", "jangan lupa", "alarm",
        "beli", "bayar", "transfer", "ribu", "juta", "rupiah"
    )

    // ============================================================
    // REMINDER KEYWORDS - Pengingat, Alarm, Todo
    // ============================================================
    private val reminderKeywords = listOf(
        // === INGATKAN ===
        "ingatkan", "ingatkan aku", "ingatkan saya", "ingatkan gue", "ingatkan gw",
        "tolong ingatkan", "coba ingatkan", "jangan lupa ingatkan",
        "ingetin", "ingetin aku", "ingetin gue", "ingetin dong",
        "remind", "remind me", "reminder", "set reminder",
        "kasih tau", "kasih tahu", "beritahu",
        
        // === JANGAN LUPA ===
        "jangan lupa", "jangan sampai lupa", "jangan kelupaan", "jangan sampe lupa",
        "harus ingat", "ingat-ingat", "harus diingat",
        "awas lupa", "takut lupa", "biar gak lupa", "biar nggak lupa",
        "supaya ingat", "agar ingat",
        
        // === ALARM / WAKTU ===
        "alarm", "set alarm", "pasang alarm", "bikin alarm",
        "timer", "set timer", "countdown",
        "jam", "pukul", "nanti jam", "besok jam", "lusa jam",
        "jadwal", "jadwalkan", "schedule", "scheduling",
        "booking", "book", "reservasi", "pesan",
        
        // === WAKTU RELATIF ===
        "besok", "besok pagi", "besok siang", "besok sore", "besok malam",
        "lusa", "lusa pagi", "lusa siang",
        "minggu depan", "pekan depan", "next week",
        "bulan depan", "next month",
        "tahun depan", "next year",
        "nanti", "nanti sore", "nanti malam", "nanti pagi", "nanti siang",
        "sebentar lagi", "beberapa jam lagi", "sejam lagi", "setengah jam lagi",
        "sebentar", "bentar lagi",
        
        // === HARI TERTENTU ===
        "senin", "senin depan", "hari senin",
        "selasa", "selasa depan", "hari selasa",
        "rabu", "rabu depan", "hari rabu",
        "kamis", "kamis depan", "hari kamis",
        "jumat", "jumat depan", "hari jumat",
        "sabtu", "sabtu depan", "hari sabtu", "sabtu ini",
        "minggu", "minggu depan", "hari minggu", "minggu ini",
        "weekend", "akhir pekan",
        
        // === DEADLINE ===
        "deadline", "batas waktu", "due date", "tenggat", "tenggat waktu",
        "harus selesai", "harus beres", "harus dikumpulkan", "harus submit",
        "harus bayar", "wajib bayar", "jatuh tempo",
        "expired", "kadaluarsa", "kedaluwarsa",
        "last day", "hari terakhir",
        
        // === TUGAS / TODO ===
        "todo", "to do", "to-do", "tugas", "task", "tasks",
        "harus", "wajib", "perlu", "mesti", "kudu",
        "segera", "urgent", "mendesak", "darurat",
        "penting", "important", "prioritas", "priority",
        "pending", "belum selesai", "belum beres"
    )
    
    private val reminderExclusions = listOf(
        "hari ini mengerjakan", "curhat", "cerita",
        "beli", "bayar", "transfer"
    )

    // ============================================================
    // MEMORY KEYWORDS - Simpan Info, Catatan Personal
    // ============================================================
    private val memoryKeywords = listOf(
        // === SIMPAN INFORMASI ===
        "simpan", "simpan info", "simpan informasi", "simpen",
        "ingat ini", "ingat bahwa", "ingat ya", "inget ini",
        "catat ini", "catat bahwa",
        "save", "save this", "keep",
        
        // === CREDENTIAL ===
        "password", "passwordnya", "pass", "kata sandi", "sandi",
        "pin", "pin-nya", "kode pin",
        "kode", "kode akses", "access code",
        "otp", "kode otp", "verification code",
        
        // === KONTAK ===
        "alamat", "alamatnya", "address",
        "nomor hp", "nomor telepon", "no hp", "no telp", "phone number",
        "kontak", "contact",
        "email", "emailnya", "e-mail",
        "username", "user", "id",
        "akun", "account",
        
        // === PREFERENSI ===
        "aku suka", "saya suka", "gue suka", "sukanya",
        "favoritku", "favorit saya", "favorit gue", "favorite",
        "aku tidak suka", "saya tidak suka", "gue gak suka", "nggak suka",
        "makanan favorit", "minuman favorit", "warna favorit",
        "genre favorit", "film favorit", "lagu favorit",
        "kesukaan", "kesukaanku",
        
        // === FAKTA / DATA ===
        "info penting", "informasi penting", "important info",
        "data", "detail", "detailnya", "spesifikasi", "spek",
        "harga", "harganya", "price",
        "ukuran", "size", "dimensi",
        "nomor", "nomornya", "number",
        "tanggal lahir", "ultah", "birthday",
        "nik", "ktp", "sim", "npwp", "bpjs",
        "nomor rekening", "rekening", "no rek",
        "plat nomor", "nopol", "nomor polisi",
        
        // === REFERENSI ===
        "link", "linknya", "website", "web", "situs",
        "url", "alamat web",
        "referensi", "reference", "sumber",
        "buku", "judul buku", "book",
        "artikel", "article",
        "video", "youtube", "tutorial"
    )
    
    private val memoryExclusions = listOf(
        "ingatkan", "remind", "alarm",
        "beli", "bayar", "transfer",
        "hari ini mengerjakan", "curhat"
    )

    // Legacy keywords for backward compatibility
    private val totalKeywords = listOf("total", "seharga", "senilai", "harga", "sebesar", "nominal")
    private val keteranganKeywords = listOf("rincian", "keterangan", "detail", "isi", "untuk", "yaitu", "berupa")

    // ============================================================
    // QUERY KEYWORDS - For REMEMBER feature (asking questions)
    // ============================================================
    private val queryKeywords = listOf(
        // === PERTANYAAN UMUM ===
        "berapa", "berapa banyak", "berapa kali", "berapa total",
        "apa", "apa saja", "apa itu", "apakah",
        "kapan", "kapan terakhir", "kapan pertama",
        "dimana", "di mana", "where",
        "siapa", "who",
        "bagaimana", "gimana", "how",
        "mengapa", "kenapa", "why",
        
        // === PERTANYAAN SPENDING ===
        "total pengeluaran", "total spending", "total belanja",
        "pengeluaran hari ini", "spending today",
        "pengeluaran minggu ini", "pengeluaran bulan ini",
        "sudah habis berapa", "sudah keluar berapa",
        "uang keluar", "money out",
        "termahal", "paling mahal", "most expensive",
        "paling sering beli", "most frequent",
        "rata-rata pengeluaran", "average spending",
        
        // === PERTANYAAN TRANSAKSI ===
        "transaksi terakhir", "last transaction",
        "beli apa", "beli terakhir", "terakhir beli",
        "bayar apa", "bayar terakhir",
        "pernah beli", "sudah pernah",
        "ada transaksi", "cari transaksi",
        
        // === PERTANYAAN JURNAL ===
        "jurnal terakhir", "catatan terakhir",
        "kemarin ngapain", "tadi ngapain", "hari ini ngapain",
        "ada berapa jurnal", "berapa jurnal",
        "kegiatan terakhir", "aktivitas terakhir",
        "cari jurnal", "cari kegiatan",
        
        // === PERTANYAAN REMINDER ===
        "pengingat aktif", "reminder aktif",
        "ada pengingat", "pengingat apa",
        "berapa pengingat", "how many reminders",
        "jadwal hari ini", "schedule today",
        
        // === PERTANYAAN CATATAN ===
        "password apa", "pin apa",
        "alamatnya apa", "emailnya apa",
        "cari catatan", "search notes",
        "ada catatan", "catatan tentang"
    )

    fun parse(text: String): ParsedResult {
        val lowercasedText = text.lowercase(Locale.ROOT)
        
        // Check for query (REMEMBER feature) first - has highest priority
        val queryScore = calculateQueryScore(lowercasedText)
        if (queryScore >= 3) {
            return parseAsQuery(text, lowercasedText)
        }
        
        // Calculate confidence scores for each category
        val reminderScore = calculateScore(lowercasedText, reminderKeywords, reminderExclusions)
        val journalScore = calculateScore(lowercasedText, journalKeywords, journalExclusions)
        val memoryScore = calculateScore(lowercasedText, memoryKeywords, memoryExclusions)
        val transactionScore = calculateScore(lowercasedText, transactionKeywords, transactionExclusions)
        
        // Boost transaction score if contains number
        val hasNumber = lowercasedText.contains(Regex("""\d+""")) || 
                        listOf("ribu", "rb", "juta", "jt", "rupiah", "rp").any { lowercasedText.contains(it) }
        val adjustedTransactionScore = if (hasNumber) transactionScore + 3 else transactionScore
        
        // Get highest score
        val scores = mapOf(
            Category.PENGINGAT to reminderScore,
            Category.JURNAL to journalScore,
            Category.MEMORY to memoryScore,
            Category.TRANSAKSI to adjustedTransactionScore
        )
        
        val maxEntry = scores.maxByOrNull { it.value }
        val category = maxEntry?.key ?: Category.MEMORY
        val score = maxEntry?.value ?: 0
        
        return when {
            score < 1 -> parseAsMemory(text, lowercasedText) // Default to memory if unclear
            category == Category.PENGINGAT -> parseReminder(text, lowercasedText)
            category == Category.JURNAL -> parseJournal(text, lowercasedText)
            category == Category.MEMORY -> parseAsMemory(text, lowercasedText)
            else -> parseTransaction(text, lowercasedText)
        }
    }
    
    private fun calculateQueryScore(text: String): Int {
        var score = 0
        for (keyword in queryKeywords) {
            if (text.contains(keyword)) {
                score += when {
                    keyword.length >= 15 -> 5
                    keyword.length >= 10 -> 4
                    keyword.length >= 6 -> 3
                    else -> 2
                }
            }
            if (text.startsWith(keyword)) {
                score += 3 // Bonus for starting with question word
            }
        }
        return score
    }
    
    private fun parseAsQuery(originalText: String, lowercasedText: String): ParsedResult {
        // Return the query text for processing by QueryHandler
        return ParsedResult(
            category = Category.QUERY,
            keperluan = originalText, // Store the full query
            total = 0,
            keterangan = "",
            reminderTime = 0
        )
    }
    
    private fun calculateScore(text: String, keywords: List<String>, exclusions: List<String>): Int {
        var score = 0
        
        // Check exclusions first - if found, return 0
        for (exclusion in exclusions) {
            if (text.contains(exclusion)) {
                return 0
            }
        }
        
        // Count keyword matches with different weights
        for (keyword in keywords) {
            if (text.contains(keyword)) {
                // Longer keywords get higher scores (more specific)
                score += when {
                    keyword.length >= 15 -> 5
                    keyword.length >= 10 -> 4
                    keyword.length >= 6 -> 3
                    keyword.length >= 4 -> 2
                    else -> 1
                }
            }
            // Bonus for starting with keyword
            if (text.startsWith(keyword)) {
                score += 3
            }
        }
        
        return score
    }

    private fun parseReminder(originalText: String, lowercasedText: String): ParsedResult {
        var content = originalText
        
        // Remove reminder prefix keywords
        val allReminderPrefixes = listOf(
            "tolong ingatkan", "coba ingatkan", "jangan lupa ingatkan",
            "ingatkan aku", "ingatkan saya", "ingatkan gue", "ingatkan gw",
            "ingetin aku", "ingetin gue", "ingetin dong", "ingetin",
            "remind me", "set reminder", "reminder", "remind",
            "ingatkan", "kasih tau", "kasih tahu", "beritahu",
            "jangan lupa", "jangan sampai lupa", "jangan sampe lupa",
            "harus ingat", "awas lupa", "takut lupa", "biar gak lupa"
        )
        
        for (keyword in allReminderPrefixes.sortedByDescending { it.length }) {
            if (lowercasedText.startsWith(keyword)) {
                content = originalText.substring(keyword.length).trim()
                break
            }
        }

        // Parse date and time
        val calendar = Calendar.getInstance()
        var foundTime = false

        // Parse "tanggal X bulan tahun"
        val dateRegex = Regex("""tanggal\s+(\d{1,2})\s+(\w+)(?:\s+(\d{4}))?""")
        val dateMatch = dateRegex.find(lowercasedText)
        if (dateMatch != null) {
            val day = dateMatch.groupValues[1].toIntOrNull() ?: 1
            val monthName = dateMatch.groupValues[2]
            val year = dateMatch.groupValues[3].toIntOrNull() ?: calendar.get(Calendar.YEAR)
            
            val month = monthNames[monthName] ?: calendar.get(Calendar.MONTH)
            
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.YEAR, year)
            
            content = content.replace(dateRegex, "").trim()
        }

        // Parse relative days
        when {
            lowercasedText.contains("besok") -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                content = content.replace(Regex("""besok""", RegexOption.IGNORE_CASE), "").trim()
            }
            lowercasedText.contains("lusa") -> {
                calendar.add(Calendar.DAY_OF_MONTH, 2)
                content = content.replace(Regex("""lusa""", RegexOption.IGNORE_CASE), "").trim()
            }
        }
        
        // Parse day names
        val dayNames = mapOf(
            "senin" to Calendar.MONDAY, "selasa" to Calendar.TUESDAY,
            "rabu" to Calendar.WEDNESDAY, "kamis" to Calendar.THURSDAY,
            "jumat" to Calendar.FRIDAY, "sabtu" to Calendar.SATURDAY,
            "minggu" to Calendar.SUNDAY
        )
        
        for ((dayName, dayValue) in dayNames) {
            if (lowercasedText.contains(dayName)) {
                val today = calendar.get(Calendar.DAY_OF_WEEK)
                var daysToAdd = dayValue - today
                if (daysToAdd <= 0) daysToAdd += 7
                calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
                content = content.replace(Regex("""$dayName(\s+depan)?""", RegexOption.IGNORE_CASE), "").trim()
                break
            }
        }

        // Parse "jam X" - supports 12:52, 12.52, 12 52
        val timeRegex = Regex("""jam\s+(\d{1,2})(?:[:.\s](\d{1,2}))?(?:\s+(pagi|siang|sore|malam))?""")
        val timeMatch = timeRegex.find(lowercasedText)
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 9
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val period = timeMatch.groupValues[3]
            
            when (period) {
                "pagi" -> if (hour == 12) hour = 0
                "siang" -> if (hour < 12) hour += 12
                "sore" -> if (hour < 12) hour += 12
                "malam" -> if (hour < 12) hour += 12
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            foundTime = true
            
            content = content.replace(timeRegex, "").trim()
        }

        // Clean up content
        content = content
            .replace(Regex("""pada\s*""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        // If no time specified, default to 9:00 AM
        if (!foundTime) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        // Detect recurrence pattern
        var recurrenceType = "NONE"
        var recurrenceInterval = 1
        
        when {
            lowercasedText.contains("setiap hari") || lowercasedText.contains("tiap hari") || 
            lowercasedText.contains("sehari-hari") || lowercasedText.contains("harian") -> {
                recurrenceType = "DAILY"
                content = content.replace(Regex("""setiap hari|tiap hari|sehari-hari|harian""", RegexOption.IGNORE_CASE), "").trim()
            }
            lowercasedText.contains("setiap minggu") || lowercasedText.contains("tiap minggu") || 
            lowercasedText.contains("mingguan") || lowercasedText.contains("setiap week") -> {
                recurrenceType = "WEEKLY"
                content = content.replace(Regex("""setiap minggu|tiap minggu|mingguan|setiap week""", RegexOption.IGNORE_CASE), "").trim()
            }
            lowercasedText.contains("setiap bulan") || lowercasedText.contains("tiap bulan") || 
            lowercasedText.contains("bulanan") || lowercasedText.contains("setiap tanggal") -> {
                recurrenceType = "MONTHLY"
                content = content.replace(Regex("""setiap bulan|tiap bulan|bulanan|setiap tanggal""", RegexOption.IGNORE_CASE), "").trim()
            }
        }

        return ParsedResult(
            category = Category.PENGINGAT,
            keperluan = content.capitalizeWords(),
            total = 0,
            keterangan = "",
            reminderTime = calendar.timeInMillis,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval
        )
    }

    private fun parseJournal(originalText: String, lowercasedText: String): ParsedResult {
        var kegiatan = originalText
        
        // Remove journal prefix keywords
        val journalPrefixes = listOf(
            "hari ini aku mengerjakan", "hari ini saya mengerjakan",
            "hari ini aku melakukan", "hari ini saya melakukan",
            "hari ini aku menyelesaikan", "hari ini saya menyelesaikan",
            "hari ini mengerjakan", "hari ini melakukan", "hari ini menyelesaikan",
            "hari ini berhasil", "hari ini gagal",
            "kegiatan hari ini", "aktivitas hari ini",
            "hari ini aku", "hari ini saya", "hari ini kami", "hari ini kita",
            "hari ini",
            "mau curhat", "aku mau curhat", "saya mau curhat",
            "mau cerita", "aku mau cerita", "saya mau cerita",
            "jadi gini", "jadi begini", "jadi ceritanya",
            "curhat ya", "cerita ya",
            "curhat", "cerita",
            "tadi aku", "tadi saya", "tadi kami",
            "barusan aku", "barusan saya", "baru aja", "baru saja",
            "barusan", "tadi"
        )
        
        for (keyword in journalPrefixes.sortedByDescending { it.length }) {
            if (lowercasedText.startsWith(keyword)) {
                kegiatan = originalText.substring(keyword.length).trim()
                break
            }
        }
        
        return ParsedResult(
            category = Category.JURNAL,
            keperluan = kegiatan.capitalizeWords(),
            total = 0,
            keterangan = "",
            reminderTime = 0
        )
    }
    
    private fun parseAsMemory(originalText: String, lowercasedText: String): ParsedResult {
        var content = originalText
        
        // Remove memory prefix keywords
        val memoryPrefixes = listOf(
            "simpan info", "simpan informasi", "simpen",
            "ingat ini", "ingat bahwa", "ingat ya", "inget ini",
            "catat ini", "catat bahwa",
            "save this", "save",
            "simpan", "keep"
        )
        
        for (keyword in memoryPrefixes.sortedByDescending { it.length }) {
            if (lowercasedText.startsWith(keyword)) {
                content = originalText.substring(keyword.length).trim()
                break
            }
        }
        
        return ParsedResult(
            category = Category.MEMORY,
            keperluan = content.capitalizeWords(),
            total = 0,
            keterangan = "",
            reminderTime = 0
        )
    }

    private fun parseTransaction(originalText: String, lowercasedText: String): ParsedResult {
        var keperluan = ""
        var total = 0
        var keterangan = ""
        var transactionDate = System.currentTimeMillis()

        // Parse date first - "tanggal X bulan tahun"
        val dateRegex = Regex("""tanggal\s+(\d{1,2})\s+(\w+)(?:\s+(\d{4}))?""")
        val dateMatch = dateRegex.find(lowercasedText)
        var textWithoutDate = lowercasedText
        
        if (dateMatch != null) {
            val calendar = Calendar.getInstance()
            val day = dateMatch.groupValues[1].toIntOrNull() ?: 1
            val monthName = dateMatch.groupValues[2]
            val year = dateMatch.groupValues[3].toIntOrNull() ?: calendar.get(Calendar.YEAR)
            
            val month = monthNames[monthName] ?: calendar.get(Calendar.MONTH)
            
            calendar.set(Calendar.DAY_OF_MONTH, day)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            
            transactionDate = calendar.timeInMillis
            textWithoutDate = lowercasedText.replace(dateRegex, "").trim()
        }

        var totalKeyword = ""
        var totalStartIndex = -1
        for (keyword in totalKeywords) {
            val index = textWithoutDate.indexOf(keyword)
            if (index != -1 && (totalStartIndex == -1 || index < totalStartIndex)) {
                totalStartIndex = index
                totalKeyword = keyword
            }
        }

        var keteranganKeyword = ""
        var keteranganStartIndex = -1
        for (keyword in keteranganKeywords) {
            val index = textWithoutDate.indexOf(keyword)
            if (index != -1) {
                if (totalStartIndex == -1 || index > totalStartIndex) {
                    if (keteranganStartIndex == -1 || index < keteranganStartIndex) {
                        keteranganStartIndex = index
                        keteranganKeyword = keyword
                    }
                }
            }
        }

        when {
            totalStartIndex != -1 && keteranganStartIndex != -1 -> {
                keperluan = textWithoutDate.substring(0, totalStartIndex).trim()
                val totalEndIndex = keteranganStartIndex
                val totalText = textWithoutDate.substring(totalStartIndex + totalKeyword.length, totalEndIndex).trim()
                total = extractNumber(totalText)
                keterangan = textWithoutDate.substring(keteranganStartIndex + keteranganKeyword.length).trim()
            }
            totalStartIndex != -1 -> {
                keperluan = textWithoutDate.substring(0, totalStartIndex).trim()
                val totalText = textWithoutDate.substring(totalStartIndex + totalKeyword.length).trim()
                total = extractNumber(totalText)
            }
            keteranganStartIndex != -1 -> {
                val beforeKeterangan = textWithoutDate.substring(0, keteranganStartIndex).trim()
                val numberMatch = extractNumberAndPosition(beforeKeterangan)
                if (numberMatch != null) {
                    keperluan = beforeKeterangan.substring(0, numberMatch.second).trim()
                    total = numberMatch.first
                } else {
                    keperluan = beforeKeterangan
                }
                keterangan = textWithoutDate.substring(keteranganStartIndex + keteranganKeyword.length).trim()
            }
            else -> {
                val numberMatch = extractNumberAndPosition(textWithoutDate)
                if (numberMatch != null) {
                    keperluan = textWithoutDate.substring(0, numberMatch.second).trim()
                    total = numberMatch.first
                    val afterNumber = textWithoutDate.substring(numberMatch.third).trim()
                    if (afterNumber.isNotEmpty()) {
                        keterangan = afterNumber
                    }
                } else {
                    keperluan = originalText
                }
            }
        }

        keperluan = keperluan.trim()
        if (keperluan.isEmpty() && total == 0) {
            keperluan = originalText
        }

        return ParsedResult(
            category = Category.TRANSAKSI,
            keperluan = keperluan.capitalizeWords(),
            total = total,
            keterangan = keterangan.capitalizeWords(),
            reminderTime = 0,
            transactionDate = transactionDate,
            transactionCategory = CategoryDetector.detect(originalText).name
        )
    }

    private fun extractNumber(text: String): Int {
        val numberWithSeparatorRegex = Regex("""(\d{1,3}(?:[.,]\d{3})+|\d+)\s*(ribu|rb|juta|jt)?""")
        val match = numberWithSeparatorRegex.find(text)
        
        if (match != null) {
            val numberPart = match.groupValues[1].replace(".", "").replace(",", "")
            val suffix = match.groupValues[2].lowercase()
            var number = numberPart.toIntOrNull() ?: 0
            
            when (suffix) {
                "ribu", "rb" -> number *= 1000
                "juta", "jt" -> number *= 1000000
            }
            
            return number
        }
        
        return convertNumberWordsToInt(text)
    }

    private fun extractNumberAndPosition(text: String): Triple<Int, Int, Int>? {
        val numberRegex = Regex("""(\d{1,3}(?:[.,]\d{3})+|\d+)\s*(ribu|rb|juta|jt)?""")
        val match = numberRegex.find(text)
        
        if (match != null) {
            val numberPart = match.groupValues[1].replace(".", "").replace(",", "")
            val suffix = match.groupValues[2].lowercase()
            var number = numberPart.toIntOrNull() ?: return null
            
            when (suffix) {
                "ribu", "rb" -> number *= 1000
                "juta", "jt" -> number *= 1000000
            }
            
            return Triple(number, match.range.first, match.range.last + 1)
        }
        return null
    }

    private fun convertNumberWordsToInt(text: String): Int {
        val words = text.split(" ", "-").filter { it.isNotBlank() }
        var total = 0
        var currentNumber = 0

        for (word in words) {
            val cleanedWord = word.replace(".", "").replace(",", "")
            val directNumber = cleanedWord.toIntOrNull()
            if (directNumber != null) {
                currentNumber += directNumber
                continue
            }

            val number = numberWords[word]
            if (number != null) {
                if (number >= 1000) {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    total += currentNumber * number
                    currentNumber = 0
                } else if (number >= 100) {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    currentNumber *= number
                } else {
                    currentNumber += number
                }
            } else when (word) {
                "ribu", "rb" -> {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    total += currentNumber * 1000
                    currentNumber = 0
                }
                "juta", "jt" -> {
                    currentNumber = if (currentNumber == 0) 1 else currentNumber
                    total += currentNumber * 1_000_000
                    currentNumber = 0
                }
                "puluh" -> currentNumber *= 10
                "ratus" -> currentNumber *= 100
            }
        }
        total += currentNumber
        return total
    }
    
    private fun String.capitalizeWords(): String = 
        split(' ').joinToString(" ") { 
            it.replaceFirstChar { char -> 
                if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() 
            }
        }
}

data class ParsedResult(
    val category: Category,
    val keperluan: String,
    val total: Int,
    val keterangan: String,
    val reminderTime: Long = 0,
    val transactionDate: Long = System.currentTimeMillis(),
    val transactionCategory: String = "OTHER",
    val recurrenceType: String = "NONE",
    val recurrenceInterval: Int = 1
)

