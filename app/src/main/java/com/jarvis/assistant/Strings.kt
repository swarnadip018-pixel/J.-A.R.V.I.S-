package com.jarvis.assistant

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object L {
    var bn = false

    fun locale(): Locale = if (bn) Locale("bn", "BD") else Locale.ENGLISH
    fun time(ts: Long): String = SimpleDateFormat("hh:mm a", locale()).format(Date(ts))
    fun date(ts: Long): String = SimpleDateFormat("d MMMM yyyy, EEEE", locale()).format(Date(ts))

    fun t(k: String): String {
        val v = M[k] ?: return k
        return if (bn) v[1] else v[0]
    }

    fun f(k: String, vararg a: Any): String = String.format(Locale.ROOT, t(k), *a)

    private val M: Map<String, List<String>> = mapOf(
        "hint" to listOf("Tap the orb once, then speak naturally.", "গোলকে একবার চাপুন, তারপর স্বাভাবিকভাবে কথা বলুন।"),
        "menu" to listOf("Menu", "মেনু"),
        "new_chat" to listOf("New chat", "নতুন চ্যাট"),
        "history" to listOf("History", "ইতিহাস"),
        "listen_lang" to listOf("Listening language", "শোনার ভাষা"),
        "about" to listOf("About", "সম্পর্কে"),
        "settings" to listOf("Settings", "সেটিংস"),
        "s_key" to listOf("API key", "API Key"),
        "s_key_sub" to listOf("Gemini key for the AI brain", "AI ব্রেইনের জন্য Gemini Key"),
        "s_engine" to listOf("Voice engine", "ভয়েস ইঞ্জিন"),
        "s_voice_api" to listOf("Voice API key & Reference ID", "ভয়েস API Key ও Reference ID"),
        "s_voice_api_sub" to listOf("For the Fish Audio cloned voice", "Fish Audio ক্লোন ভয়েসের জন্য"),
        "s_gem" to listOf("Gemini voice", "Gemini ভয়েস"),
        "s_gem_sub" to listOf("Pick a voice and how it should sound", "কণ্ঠ ও কথা বলার ধরন বাছুন"),
        "s_access" to listOf("Accessibility", "অ্যাক্সেসিবিলিটি"),
        "s_access_sub" to listOf("Opens the phone's accessibility settings", "ফোনের অ্যাক্সেসিবিলিটি সেটিংস খোলে"),
        "s_lang" to listOf("App language", "অ্যাপের ভাষা"),
        "s_icon" to listOf("App icon", "অ্যাপ আইকন"),
        "s_icon_sub" to listOf("Make a home-screen icon from your own image", "নিজের ছবি দিয়ে হোম স্ক্রিন আইকন বানান"),
        "eng_title" to listOf("Voice engine", "ভয়েস ইঞ্জিন"),
        "eng_phone" to listOf("Phone voice (on-device, private)", "ফোনের ভয়েস (ফোনের ভেতরেই, প্রাইভেট)"),
        "eng_gemini" to listOf("Gemini voice (natural, uses your Gemini key)", "Gemini ভয়েস (স্বাভাবিক, আপনার Gemini Key লাগে)"),
        "eng_fish" to listOf("Cloned voice (Fish Audio)", "ক্লোন ভয়েস (Fish Audio)"),
        "eng_n_phone" to listOf("Phone voice", "ফোনের ভয়েস"),
        "eng_n_gemini" to listOf("Gemini voice", "Gemini ভয়েস"),
        "eng_n_fish" to listOf("Cloned voice", "ক্লোন ভয়েস"),
        "eng_set" to listOf("Voice engine: %s", "ভয়েস ইঞ্জিন: %s"),
        "new_started" to listOf("New chat started. Tap the orb.", "নতুন চ্যাট শুরু। গোলকে চাপুন।"),
        "stopped" to listOf("Chat stopped. Tap the orb to start again.", "কথোপকথন বন্ধ। আবার শুরু করতে গোলকে চাপুন।"),
        "listening" to listOf("Listening…", "শুনছি…"),
        "listening_stop" to listOf("Listening… (tap the orb to stop)", "শুনছি… (বন্ধ করতে গোলকে চাপুন)"),
        "thinking" to listOf("Thinking…", "ভাবছি…"),
        "long_silence" to listOf("I haven't heard anything for a while, so I stopped. Tap the orb to start again.", "অনেকক্ষণ কিছু শুনিনি, তাই বন্ধ করলাম। আবার শুরু করতে গোলকে চাপুন।"),
        "mic_err" to listOf("Couldn't catch that. Tap the orb and try again (mic error %d).", "বুঝতে পারিনি, আবার গোলকে চেপে বলুন (মাইক এরর %d)।"),
        "mic_denied" to listOf("I can't listen without microphone permission.", "মাইকের অনুমতি ছাড়া শুনতে পারব না।"),
        "no_recog" to listOf("Speech recognition isn't available on this phone.", "এই ফোনে ভয়েস চেনার সুবিধা নেই।"),
        "need_key" to listOf("To use the AI, first add your key in Settings → API key.", "AI চালাতে আগে সেটিংস → API Key-তে Key বসান।"),
        "err_key" to listOf("The API key is wrong or not allowed (server code %d). Re-enter it in Settings → API key.", "API Key ঠিক নয় বা অনুমতি নেই (সার্ভার কোড %d)। সেটিংস → API Key-তে আবার বসান।"),
        "err_quota" to listOf("Free limit reached. Please try again in a while (server code 429).", "ফ্রি সীমা শেষ। কিছুক্ষণ পরে আবার চেষ্টা করুন (সার্ভার কোড 429)।"),
        "err_net" to listOf("Network problem. Check that your internet is on.\n(%s)", "ইন্টারনেট বা সংযোগে সমস্যা। নেট চালু আছে কি না দেখুন।\n(%s)"),
        "err_parse" to listOf("I couldn't understand the AI's reply. Please try again.", "AI-র উত্তর বুঝতে পারিনি। আবার বলুন।"),
        "err_server" to listOf("Server problem (server code %d). Please try again.", "সার্ভারে সমস্যা হয়েছে (সার্ভার কোড %d)। আবার চেষ্টা করুন।"),
        "err_404" to listOf("AI model not found (server code 404).\n\nModels your key can use:\n%s\n\nSend a screenshot of this list to get help.", "AI মডেল পাওয়া যায়নি (সার্ভার কোড 404)।\n\nআপনার Key যে মডেলগুলো ব্যবহার করতে পারে:\n%s\n\nসাহায্যের জন্য এই তালিকার স্ক্রিনশট পাঠান।"),
        "no_voice" to listOf("(The phone has no voice for this language. Install one in Settings → Text-to-speech → Google Speech Services.)", "(এই ভাষার ভয়েস ফোনে নেই। Settings → Text-to-speech → Google Speech Services থেকে ডাউনলোড করুন।)"),
        "fish_key_bad" to listOf("Fish Audio API key is wrong", "Fish Audio API Key ঠিক নয়"),
        "fish_credit" to listOf("Fish Audio credit is used up", "Fish Audio-র ক্রেডিট শেষ"),
        "fish_id" to listOf("Reference ID or model problem", "Reference ID বা মডেলে সমস্যা"),
        "fish_limit" to listOf("Fish Audio limit reached", "Fish Audio-র সীমা শেষ"),
        "fish_fail" to listOf("The cloned voice didn't work", "ক্লোন ভয়েস কাজ করেনি"),
        "gem_fail" to listOf("The Gemini voice didn't work", "Gemini ভয়েস কাজ করেনি"),
        "voice_fallback" to listOf("(%s, code %d. Using the phone voice.)", "(%s, কোড %d। ফোনের ভয়েসে পড়ছি।)"),
        "c_denied" to listOf("I can't look up contacts without permission. Saying the number works too.", "অনুমতি ছাড়া কন্টাক্ট খুঁজতে পারব না। নম্বর বললেও চলবে।"),
        "c_none" to listOf("I couldn't find a contact or number for “%s”.", "“%s” নামে কোনো কন্টাক্ট বা নম্বর পাইনি।"),
        "app_none" to listOf("I couldn't find an app named “%s”.", "“%s” নামে কোনো অ্যাপ পাইনি।"),
        "no_torch" to listOf("This phone has no flashlight.", "এই ফোনে টর্চ নেই।"),
        "no_app" to listOf("No app on the phone can do this.", "এই কাজের জন্য ফোনে কোনো অ্যাপ নেই।"),
        "act_fail" to listOf("That didn't work. (%s)", "কাজটা করা গেল না। (%s)"),
        "back" to listOf("BACK", "ফিরুন"),
        "profile" to listOf("PROFILE", "প্রোফাইল"),
        "verified" to listOf("> IDENTITY VERIFIED_", "> পরিচয় যাচাই সম্পন্ন_"),
        "change_photo" to listOf("[ change photo ]", "[ ছবি বদলান ]"),
        "f_user" to listOf("USERNAME", "ইউজারনেম"),
        "f_full" to listOf("FULL NAME", "পুরো নাম"),
        "f_mail" to listOf("GMAIL ID", "জিমেইল আইডি"),
        "f_phone" to listOf("PHONE NUMBER", "ফোন নম্বর"),
        "h_user" to listOf("username", "ইউজারনেম"),
        "h_full" to listOf("full name", "পুরো নাম"),
        "h_mail" to listOf("name@gmail.com", "name@gmail.com"),
        "h_phone" to listOf("+880...", "+880..."),
        "save" to listOf("SAVE", "সেভ"),
        "logout" to listOf("LOG OUT", "লগ আউট"),
        "saved" to listOf("Saved", "সেভ হয়েছে"),
        "lo_title" to listOf("Log out", "লগ আউট"),
        "lo_msg" to listOf("Your profile details and photo will be removed from this phone. (Chat history, API keys and voice settings stay.) Continue?", "আপনার প্রোফাইলের তথ্য ও ছবি এই ফোন থেকে মুছে যাবে। (চ্যাটের ইতিহাস, API Key ও ভয়েস সেটিং থাকবে।) নিশ্চিত?"),
        "yes_del" to listOf("Yes, delete", "হ্যাঁ, মুছুন"),
        "no" to listOf("No", "না"),
        "logged_out" to listOf("Logged out", "লগ আউট হয়েছে"),
        "photo_fail" to listOf("Couldn't load that photo", "ছবিটা নেওয়া যায়নি"),
        "choose_photo" to listOf("Choose photo", "ছবি বাছুন"),
        "hist_title" to listOf("HISTORY", "ইতিহাস"),
        "hist_empty" to listOf("No saved conversations yet.", "এখনও কোনো কথোপকথন সেভ হয়নি।"),
        "hist_cap" to listOf("Showing the latest 500 messages", "সর্বশেষ ৫০০টা বার্তা দেখানো হচ্ছে"),
        "del" to listOf("Delete", "মুছুন"),
        "hist_del_title" to listOf("Delete history", "ইতিহাস মুছুন"),
        "hist_del_msg" to listOf("All saved conversations will be deleted permanently. Continue?", "সব সেভ করা কথোপকথন চিরতরে মুছে যাবে। নিশ্চিত?"),
        "hist_deleted" to listOf("History deleted", "ইতিহাস মুছে ফেলা হয়েছে"),
        "dlg_save" to listOf("Save", "সেভ"),
        "dlg_close" to listOf("Close", "বন্ধ"),
        "ok" to listOf("OK", "ঠিক আছে"),
        "key_title" to listOf("API key", "API Key"),
        "key_title_has" to listOf("API key (one is saved)", "API Key (আগেরটা সেভ আছে)"),
        "key_hint" to listOf("AQ... or AIza...", "AQ... বা AIza..."),
        "key_info" to listOf("Paste the Gemini API key from aistudio.google.com. It stays only on your phone.", "aistudio.google.com থেকে নেওয়া Gemini API Key এখানে পেস্ট করুন। এটা শুধু আপনার ফোনে থাকবে।"),
        "key_saved" to listOf("Key saved", "Key সেভ হয়েছে"),
        "fish_title" to listOf("Voice API key & Reference ID", "ভয়েস API Key ও Reference ID"),
        "fish_info" to listOf("Enter your Fish Audio API key and the Reference ID of your cloned voice. The text of replies is sent to Fish Audio's servers. Use only your own voice or one you have permission to use.", "Fish Audio-র API Key ও ক্লোন ভয়েসের Reference ID বসান। উত্তরের লেখা Fish Audio-র সার্ভারে যাবে। শুধু নিজের বা অনুমতি পাওয়া কণ্ঠ ব্যবহার করুন।"),
        "fish_saved" to listOf("Saved. Now choose “Cloned voice” in Voice engine.", "সেভ হয়েছে। এবার ভয়েস ইঞ্জিনে “ক্লোন ভয়েস” বাছুন।"),
        "fish_key_hint" to listOf("Fish Audio API key", "Fish Audio API Key"),
        "fish_id_hint" to listOf("Reference ID", "Reference ID"),
        "gem_title" to listOf("Gemini voice", "Gemini ভয়েস"),
        "gem_info" to listOf("Pick a voice. It uses your Gemini key and needs internet.", "একটা কণ্ঠ বাছুন। এটা আপনার Gemini Key ব্যবহার করে, ইন্টারনেট লাগে।"),
        "gem_style" to listOf("How should it sound? (style)", "কেমন শোনাবে? (ধরন)"),
        "gem_saved" to listOf("Saved. Now choose “Gemini voice” in Voice engine.", "সেভ হয়েছে। এবার ভয়েস ইঞ্জিনে “Gemini ভয়েস” বাছুন।"),
        "ll_title" to listOf("Which language will you speak?", "আপনি কোন ভাষায় কথা বলবেন?"),
        "ll_set" to listOf("Listening language: %s", "শোনার ভাষা: %s"),
        "al_title" to listOf("App language", "অ্যাপের ভাষা"),
        "ic_title" to listOf("App icon", "অ্যাপ আইকন"),
        "ic_msg" to listOf("Choose a square-ish image with the main subject in the center. Jarvis will ask your launcher to add a home-screen icon with it.\n\nNote: Android doesn't let apps change the main icon in the app drawer, so that one stays as it is.", "মাঝখানে মূল অংশ আছে এমন একটা প্রায় চৌকো ছবি বাছুন। জার্ভিস সেই ছবি দিয়ে হোম স্ক্রিনে একটা আইকন যোগ করতে বলবে।\n\nমনে রাখবেন: অ্যাপ ড্রয়ারের মূল আইকন Android অ্যাপকে বদলাতে দেয় না, ওটা আগের মতোই থাকবে।"),
        "ic_pick" to listOf("Choose image", "ছবি বাছুন"),
        "ic_cancel" to listOf("Cancel", "বাতিল"),
        "ic_ok" to listOf("Confirm the shortcut to add it to your home screen.", "হোম স্ক্রিনে যোগ করতে শর্টকাটটা নিশ্চিত করুন।"),
        "ic_unsupported" to listOf("Your launcher doesn't support adding shortcuts like this.", "আপনার লঞ্চার এভাবে শর্টকাট যোগ করতে দেয় না।"),
        "ic_fail" to listOf("Couldn't use that image.", "ছবিটা ব্যবহার করা গেল না।"),
        "about_msg" to listOf("Your personal assistant\nVersion 2.8\nAI model: %s", "আপনার ব্যক্তিগত সহকারী\nসংস্করণ 2.8\nAI মডেল: %s"),
        "about_none" to listOf("not chosen yet", "এখনও বাছা হয়নি")
    )
}

object Prompt {
    val SYS = """You are J.A.R.V.I.S, the user's personal voice assistant. Speak calmly and politely. Give clear, complete answers: short for simple questions, but as detailed as needed when the user asks for an explanation. Your reply will be read aloud, so write in plain, natural spoken language.
Always reply in the same language the user spoke (for example Bengali, English or Hindi).
Respond ONLY with this JSON and nothing else:
{"lang":"language code of your reply (bn, en, hi, ur, ar, ta, ...)","reply":"your reply","action":"none","arg":"","arg2":""}
Do not use markdown, asterisks or emojis in reply.

To make the phone do something, set action to one of these (use "none" otherwise):
- open_app: arg = app name in English, e.g. YouTube, WhatsApp, Camera, Settings, Chrome, Gallery
- dial: arg = phone number (puts the number in the dialer; the user presses call)
- call_contact: arg = contact name (as it is likely saved on the phone; also consider the English spelling of Bengali names)
- sms: arg = number or contact name, arg2 = message text (opens the message screen; the user presses send)
- torch: arg = "on" or "off"
- alarm: arg = "HH:MM" (24-hour), arg2 = label
- timer: arg = number of seconds
- search: arg = words to search on Google
- youtube: arg = words to search on YouTube
- maps: arg = place name

Rules: when doing an action, say briefly what you are doing in reply, for example "Opening YouTube". For calls or SMS say you have set it up and the user should press the call or send button. If you don't know the number or message, don't guess: use action none and ask. You cannot yet do things outside this list (Wi-Fi/Bluetooth toggles, tapping inside other apps, taking photos, reading out search results): say so honestly. Never make up facts."""
}
