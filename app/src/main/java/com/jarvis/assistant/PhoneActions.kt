package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import org.json.JSONObject
import java.net.URLEncoder

class PhoneActions(private val act: Activity, private val note: (String) -> Unit) {

    private fun isNumber(s: String): Boolean =
        s.replace(" ", "").replace("-", "").matches(Regex("^\\+?[0-9]{5,15}$"))

    private fun findNumber(q: String): String? {
        if (isNumber(q)) return q.replace(" ", "").replace("-", "")
        try {
            val c = act.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                arrayOf("%" + q + "%"),
                null
            )
            c?.use { if (it.moveToFirst()) return it.getString(0) }
        } catch (e: Exception) {
        }
        return null
    }

    private fun openApp(name: String): Boolean {
        val pm = act.packageManager
        val main = Intent(Intent.ACTION_MAIN)
        main.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val q = name.lowercase()
        for (a in apps) {
            val label = a.loadLabel(pm).toString().lowercase()
            if (label == q || label.contains(q) || q.contains(label)) {
                val launch = pm.getLaunchIntentForPackage(a.activityInfo.packageName)
                if (launch != null) {
                    act.startActivity(launch)
                    return true
                }
            }
        }
        note("“$name” নামে কোনো অ্যাপ খুঁজে পাইনি।")
        return false
    }

    private fun setTorch(on: Boolean) {
        val cm = act.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var id: String? = null
        for (i in cm.cameraIdList) {
            if (cm.getCameraCharacteristics(i).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                id = i
                break
            }
        }
        if (id == null) {
            note("এই ফোনে টর্চ পাওয়া যায়নি।")
            return
        }
        cm.setTorchMode(id, on)
    }

    // ফেরত মান: 0 = অ্যাপের ভেতরেই শেষ, 1 = অন্য অ্যাপ খুলেছে, 2 = কন্টাক্টের অনুমতি লাগবে
    fun run(o: JSONObject): Int {
        val a = o.optString("action", "none")
        val arg = o.optString("arg", "").trim()
        val arg2 = o.optString("arg2", "").trim()
        try {
            if ((a == "call_contact" || a == "sms") && !isNumber(arg) &&
                act.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return 2
            }
            when (a) {
                "open_app" -> return if (openApp(arg)) 1 else 0
                "dial" -> {
                    act.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(arg))))
                    return 1
                }
                "call_contact" -> {
                    val n = findNumber(arg)
                    if (n == null) {
                        note("“$arg” নামে কোনো কন্টাক্ট পাইনি। ফোনে নামটা কীভাবে সেভ আছে বলুন।")
                        return 0
                    }
                    act.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(n))))
                    return 1
                }
                "sms" -> {
                    val n = findNumber(arg)
                    if (n == null) {
                        note("“$arg” নামে কোনো কন্টাক্ট বা নম্বর পাইনি।")
                        return 0
                    }
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(n)))
                    i.putExtra("sms_body", arg2)
                    act.startActivity(i)
                    return 1
                }
                "torch" -> {
                    setTorch(arg.lowercase() != "off")
                    return 0
                }
                "alarm" -> {
                    val parts = arg.split(":")
                    val h = parts[0].trim().toInt()
                    val m = if (parts.size > 1) parts[1].trim().toInt() else 0
                    val i = Intent(AlarmClock.ACTION_SET_ALARM)
                    i.putExtra(AlarmClock.EXTRA_HOUR, h)
                    i.putExtra(AlarmClock.EXTRA_MINUTES, m)
                    i.putExtra(AlarmClock.EXTRA_MESSAGE, if (arg2.isEmpty()) "Jarvis" else arg2)
                    i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    act.startActivity(i)
                    return 0
                }
                "timer" -> {
                    val i = Intent(AlarmClock.ACTION_SET_TIMER)
                    i.putExtra(AlarmClock.EXTRA_LENGTH, arg.toInt())
                    i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    act.startActivity(i)
                    return 0
                }
                "search" -> {
                    val u = "https://www.google.com/search?q=" + URLEncoder.encode(arg, "UTF-8")
                    act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    return 1
                }
                "youtube" -> {
                    val u = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(arg, "UTF-8")
                    act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    return 1
                }
                "maps" -> {
                    act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(arg))))
                    return 1
                }
            }
        } catch (e: ActivityNotFoundException) {
            note("এই কাজের জন্য ফোনে কোনো অ্যাপ পাওয়া যায়নি।")
        } catch (e: Exception) {
            note("কাজটা করা গেল না। (${e.javaClass.simpleName})")
        }
        return 0
    }
}
