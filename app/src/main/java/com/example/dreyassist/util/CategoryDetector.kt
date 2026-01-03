package com.example.dreyassist.util

import com.example.dreyassist.R
import java.util.Locale

/**
 * CategoryDetector - Auto-detect transaction category based on keywords
 */
object CategoryDetector {

    enum class Category(val displayName: String, val iconResId: Int, val stringResId: Int) {
        TRANSPORT("Transport", R.drawable.ic_category_transport, R.string.cat_transport),
        FOOD("Food", R.drawable.ic_category_food, R.string.cat_food),
        GROCERIES("Groceries", R.drawable.ic_category_groceries, R.string.cat_groceries),
        SHOPPING("Shopping", R.drawable.ic_category_shopping, R.string.cat_shopping),
        BILLS("Bills", R.drawable.ic_category_bills, R.string.cat_bills),
        ENTERTAINMENT("Entertainment", R.drawable.ic_category_entertainment, R.string.cat_entertainment),
        HEALTH("Health", R.drawable.ic_category_health, R.string.cat_health),
        EDUCATION("Education", R.drawable.ic_category_education, R.string.cat_education),
        OTHER("Other", R.drawable.ic_category_other, R.string.cat_other)
    }

    private val categoryKeywords = mapOf(
        Category.TRANSPORT to listOf(
            // Fuel
            "bensin", "pertalite", "pertamax", "solar", "pertamina", "shell", "bbm",
            "fuel", "isi bensin", "tankin", "ngisi",
            // Transport services
            "grab", "gojek", "gocar", "grabcar", "grabike", "ojol", "ojek online",
            "maxim", "indriver", "taxi", "taksi", "uber",
            // Parking & toll
            "parkir", "parking", "tol", "e-toll", "etoll",
            // Vehicle
            "servis motor", "servis mobil", "ganti oli", "cuci motor", "cuci mobil",
            "tambal ban", "ban bocor"
        ),
        
        Category.FOOD to listOf(
            // General
            "makan", "makanan", "food", "meal", "sarapan", "breakfast",
            "makan siang", "lunch", "makan malam", "dinner", "brunch",
            "jajan", "ngemil", "snack", "cemilan",
            // Drinks
            "minum", "minuman", "drink", "kopi", "coffee", "teh", "tea",
            "jus", "juice", "boba", "bubble tea", "es", "susu",
            // Restaurants & Places
            "restoran", "restaurant", "resto", "cafe", "kafe",
            "warteg", "warung", "kantin", "food court",
            "mcd", "mcdonalds", "kfc", "burger king", "pizza hut",
            "solaria", "hokben", "yoshinoya", "cfc", "a&w",
            "starbucks", "sbux", "janji jiwa", "kopi kenangan", "fore",
            "mixue", "chatime", "gulu gulu",
            // Delivery
            "grabfood", "gofood", "shopeefood"
        ),

        Category.GROCERIES to listOf(
            // Stores
            "indomaret", "alfamart", "alfamidi", "supermarket", "minimarket",
            "pasar", "warung kelontong", "toko sembako", "hypermart", "lotte", 
            "super indo", "giant", "hero", "transmart", "carefour",
            // Online Groceries
            "sayurbox", "happyfresh", "segari", "astro",
            // Items
            "sembako", "beras", "minyak", "gula", "telur", "mie instan", 
            "sabun", "shampoo", "odol", "deterjen", "tisu", "tissue",
            "pampers", "susu bayi", "susu bubuk",
            "sayur", "buah", "daging", "ayam", "ikan", "bumbu",
            "garam", "penyedap", "kebutuhan dapur", "belanja bulanan"
        ),
        
        Category.SHOPPING to listOf(
            // General
            "beli", "belanja", "shopping", "shop",
            // Online
            "shopee", "tokopedia", "tokped", "lazada", "bukalapak", "blibli",
            "tiktok shop", "online shop", "olshop",
            // Stores
            "mall", "toko", "store", "departement store", "outlet",
            // Items
            "baju", "celana", "sepatu", "tas", "aksesoris", "gadget",
            "elektronik", "perabotan", "furniture", "kosmetik", "make up"
        ),
        
        Category.BILLS to listOf(
            // Utilities
            "listrik", "pln", "token listrik", "electricity",
            "air", "pdam", "water",
            "gas", "lpg", "elpiji",
            // Internet & Communication
            "wifi", "internet", "indihome", "firstmedia", "biznet",
            "pulsa", "paket data", "kuota", "topup",
            "telkomsel", "xl", "indosat", "tri", "smartfren",
            // Subscriptions
            "tagihan", "bill", "bayar bulanan", "iuran",
            "netflix", "spotify", "youtube premium", "disney", "viu",
            // Finance
            "cicilan", "kredit", "angsuran", "asuransi", "insurance"
        ),
        
        Category.ENTERTAINMENT to listOf(
            // Movies & Shows
            "nonton", "bioskop", "cinema", "xxi", "cgv", "cinepolis",
            "movie", "film", "konser", "concert", "theatre", "teater",
            // Games
            "game", "games", "gaming", "topup game", "diamond", "uc",
            "steam", "playstation", "ps", "xbox", "nintendo",
            // Recreation
            "liburan", "vacation", "holiday", "wisata", "travel",
            "hotel", "resort", "tiket", "ticket",
            "karaoke", "billiard", "bowling", "escape room",
            // Sports
            "gym", "fitness", "olahraga", "sport", "futsal", "badminton"
        ),
        
        Category.HEALTH to listOf(
            // Medicine
            "obat", "medicine", "drug", "vitamin", "suplemen",
            // Healthcare
            "apotek", "pharmacy", "apotik", "kimia farma", "k24",
            "dokter", "doctor", "rumah sakit", "rs", "hospital",
            "klinik", "clinic", "puskesmas",
            "cek kesehatan", "medical checkup", "lab", "laboratorium",
            // Personal care
            "skincare", "perawatan", "salon", "barber", "potong rambut"
        ),
        
        Category.EDUCATION to listOf(
            // School
            "sekolah", "school", "kuliah", "university", "kampus",
            "spp", "uang sekolah", "tuition",
            // Courses
            "kursus", "course", "les", "bimbel", "belajar",
            // Materials
            "buku", "book", "alat tulis", "stationery",
            "print", "fotocopy", "fotokopi"
        )
    )

    /**
     * Detect category from transaction description
     */
    fun detect(keperluan: String): Category {
        val lowercased = keperluan.lowercase(Locale.ROOT)
        
        // Calculate match score for each category
        val scores = mutableMapOf<Category, Int>()
        
        for ((category, keywords) in categoryKeywords) {
            var score = 0
            for (keyword in keywords) {
                if (lowercased.contains(keyword)) {
                    // Longer keywords get higher scores
                    score += when {
                        keyword.length >= 10 -> 5
                        keyword.length >= 6 -> 3
                        keyword.length >= 4 -> 2
                        else -> 1
                    }
                }
            }
            if (score > 0) {
                scores[category] = score
            }
        }
        
        // Return category with highest score, or OTHER if no match
        return scores.maxByOrNull { it.value }?.key ?: Category.OTHER
    }

    /**
     * Get category display name
     */
    fun getCategoryName(categoryString: String, context: android.content.Context? = null): String {
        return try {
            val category = Category.valueOf(categoryString)
            context?.getString(category.stringResId) ?: category.displayName
        } catch (e: Exception) {
            categoryString
        }
    }

    /**
     * Get category icon resource ID
     */
    fun getCategoryIconResId(categoryString: String): Int {
        return try {
            Category.valueOf(categoryString).iconResId
        } catch (e: Exception) {
            R.drawable.ic_category_other
        }
    }
}
