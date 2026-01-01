# Dreysist

Dreysist adalah aplikasi asisten pribadi berbasis suara untuk Android yang membantu Anda mencatat transaksi keuangan, menulis jurnal harian, dan mengatur pengingat dengan mudah menggunakan perintah suara dalam Bahasa Indonesia.

## Fitur Utama

### Pencatatan Suara
- Gunakan suara untuk mencatat transaksi, jurnal, atau pengingat
- Mendukung pengenalan suara dalam Bahasa Indonesia
- Parser cerdas yang memahami konteks dan mengekstrak informasi secara otomatis

### Transaksi Keuangan
- Catat pengeluaran dan pemasukan dengan suara
- Mendukung format angka dalam berbagai bentuk (15000, 15.000, 15 ribu, lima belas ribu)
- Otomatis mengekstrak keperluan, nominal, dan keterangan dari ucapan
- Mendukung pencatatan transaksi dengan tanggal tertentu

**Contoh perintah:**
- "Makan siang total 25 ribu"
- "Beli bensin seharga 50.000 keterangan motor honda"
- "Tanggal 15 Januari bayar listrik sebesar 200 ribu"

### Jurnal Harian
- Catat aktivitas harian dengan mudah
- Awali dengan frasa seperti "hari ini mengerjakan..." atau "hari ini melakukan..."

**Contoh perintah:**
- "Hari ini mengerjakan laporan keuangan"
- "Hari ini melakukan meeting dengan tim"

### Pengingat
- Atur pengingat dengan suara untuk tanggal dan waktu tertentu
- Mendukung format waktu yang fleksibel (jam 9, jam 14.30, jam 3 sore)
- Mendukung kata kunci seperti "besok", "lusa", dan nama bulan
- Notifikasi otomatis sesuai jadwal

**Contoh perintah:**
- "Ingatkan aku besok jam 9 meeting dengan klien"
- "Pengingat tanggal 20 Januari jam 14.30 bayar cicilan"
- "Ingatkan saya lusa jam 8 pagi olahraga"

### Fitur Lainnya
- Home screen widget untuk akses cepat ke input suara
- Riwayat aktivitas terbaru di halaman utama
- Backup dan restore data
- Tampilan modern dengan animasi yang menarik

## Arsitektur Aplikasi

### Teknologi yang Digunakan
- **Bahasa:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 36
- **Database:** Room Persistence Library
- **Architecture:** MVVM dengan ViewModels dan LiveData
- **View Binding:** Enabled
- **KSP:** Untuk pemrosesan anotasi Room

### Struktur Proyek

```
app/src/main/java/com/example/dreyassist/
|-- MainActivity.kt           # Halaman utama dengan input suara
|-- TransaksiListActivity.kt  # Daftar semua transaksi
|-- JournalListActivity.kt    # Daftar semua jurnal
|-- ReminderListActivity.kt   # Daftar semua pengingat
|-- HistoryActivity.kt        # Riwayat semua aktivitas
|-- BackupActivity.kt         # Backup dan restore data
|-- AboutActivity.kt          # Informasi aplikasi
|
|-- data/
|   |-- AppDatabase.kt        # Database Room
|   |-- TransaksiEntity.kt    # Entity transaksi
|   |-- TransaksiDao.kt       # DAO transaksi
|   |-- JournalEntity.kt      # Entity jurnal
|   |-- JournalDao.kt         # DAO jurnal
|   |-- ReminderEntity.kt     # Entity pengingat
|   |-- ReminderDao.kt        # DAO pengingat
|
|-- notification/             # Sistem notifikasi untuk pengingat
|-- ui/                       # ViewModel, Adapter, dan komponen UI
|-- util/                     # Utilitas termasuk VoiceParser
|-- widget/                   # Home screen widget
```

## Persyaratan Sistem

- Android 8.0 (API 26) atau lebih tinggi
- Izin mikrofon untuk input suara
- Izin notifikasi untuk fitur pengingat

## Cara Build

1. Clone repository ini
2. Buka proyek dengan Android Studio
3. Sinkronkan Gradle
4. Build dan jalankan di emulator atau perangkat fisik

```bash
./gradlew assembleDebug
```

## Izin Aplikasi

Aplikasi ini memerlukan izin berikut:
- `RECORD_AUDIO` - Untuk input suara
- `POST_NOTIFICATIONS` - Untuk menampilkan notifikasi pengingat
- `SCHEDULE_EXACT_ALARM` - Untuk menjadwalkan pengingat tepat waktu
- `USE_EXACT_ALARM` - Untuk alarm yang presisi
- `USE_FULL_SCREEN_INTENT` - Untuk popup pengingat di layar kunci

## Lisensi

Hak Cipta (c) 2025

## Kontribusi

Kontribusi sangat diterima. Silakan buat issue atau pull request untuk perbaikan dan fitur baru.
