package com.androidharness.app.professor

/**
 * Detects incoming requests to disclose information about the device owner.
 * This is a conservative phrase-level guard; the AI is also instructed to
 * understand the request semantically. False positives hold a reply for review
 * rather than disclose private information.
 */
object ProfessorPrivateRequestClassifier {
    const val SAFE_ACKNOWLEDGEMENT =
        "বিষয়টি ব্যক্তিগত তথ্যের মধ্যে পড়তে পারে। মালিকের অনুমতি নিয়ে জানাচ্ছি। এর মধ্যে অন্য বিষয়ে কথা চালিয়ে যেতে পারি।"

    private val englishRequest = Regex(
        """(?i)\b(?:what is|what's|tell me|share|give me|send me|do you know|where do|where is|how old|who is)\b"""
    )
    private val englishPrivateInfo = Regex(
        """(?i)\b(?:your|owner's|the owner's|his|her|personal|private)\b.{0,36}\b(?:phone|number|address|email|salary|income|bank|account|password|otp|verification code|location|date of birth|birthday|age|national id|nid|passport|family|home|relationship|identity)\b"""
    )
    private val englishLocationQuestion = Regex(
        """(?i)\b(?:where do you live|what is your home address|where is your home)\b"""
    )
    private val bengaliPrivateInfo = Regex(
        """(তোমার|আপনার|ওনার|মালিকের|ব্যক্তিগত|গোপন).{0,36}(ফোন|মোবাইল|নম্বর|ঠিকানা|ইমেইল|বেতন|আয়|ব্যাংক|অ্যাকাউন্ট|পাসওয়ার্ড|ওটিপি|কোড|লোকেশন|অবস্থান|জন্মতারিখ|জন্মদিন|বয়স|এনআইডি|পাসপোর্ট|পরিবার|বাসা|বাড়ি|পরিচয়|সম্পর্ক)"""
    )
    private val bengaliRequest = Regex(
        """(কি|কী|কত|কোথায়|কোথায়|জানাও|বলো|দাও|পাঠাও|শেয়ার|শেয়ার).{0,24}(তোমার|আপনার|ওনার|মালিকের|ব্যক্তিগত|গোপন)"""
    )

    fun isPrivateInformationRequest(text: String): Boolean {
        if (text.isBlank()) return false
        return (englishRequest.containsMatchIn(text) && englishPrivateInfo.containsMatchIn(text)) ||
            englishLocationQuestion.containsMatchIn(text) ||
            bengaliPrivateInfo.containsMatchIn(text) ||
            bengaliRequest.containsMatchIn(text)
    }
}
