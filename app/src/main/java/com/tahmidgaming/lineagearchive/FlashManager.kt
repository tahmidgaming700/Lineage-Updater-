package com.tahmidgaming.lineagearchive

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object FlashManager {
    enum class Recovery { TWRP, ORANGEFOX, OPEN_RECOVERY }

    data class Capability(
        val rooted: Boolean,
        val twrpDetected: Boolean,
        val orangeFoxDetected: Boolean,
        val isSimulation: Boolean = false
    ) {
        val ready: Boolean get() = rooted && (twrpDetected || orangeFoxDetected)
    }

    private const val PREFS = "root_settings"
    private const val KEY_SIMULATION = "emulator_root_simulation"

    fun isRootTestingMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SIMULATION, true)

    fun setRootTestingMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SIMULATION, enabled)
            .apply()
    }

    fun detect(context: Context): Capability {
        val simulation = isRootTestingMode(context)
        if (simulation) {
            return Capability(
                rooted = true,
                twrpDetected = true,
                orangeFoxDetected = true,
                isSimulation = true
            )
        }
        val rooted = runRoot("id").contains("uid=0")
        if (!rooted) return Capability(false, false, false, false)
        val twrp = runRoot("getprop ro.twrp.version").trim().isNotBlank() ||
            runRoot("test -d /twres && echo yes").contains("yes")
        val orangeFox = runRoot("getprop ro.orangefox.version; getprop ro.of.version; test -d /fox && echo orangefox")
            .lineSequence().any { it.trim().isNotEmpty() && !it.trim().equals("0", true) }
        return Capability(true, twrp, orangeFox, false)
    }

    fun flashVerifiedZip(context: Context, filename: String, recovery: Recovery): Result<String> {
        val safeName = safeFilename(filename) ?: return Result.failure(IllegalArgumentException("Unsupported package filename"))
        val capability = detect(context)
        if (!capability.rooted) return Result.failure(IllegalStateException("Root access was not granted"))
        if (capability.isSimulation) {
            return Result.success("Safety Simulation Mode: Staged OpenRecoveryScript for '$safeName' -> /cache/recovery/openrecoveryscript. Simulated recovery reboot to ${recovery.name}.")
        }
        val script = "install /sdcard/Download/$safeName\n"
        val escaped = shellQuote(script)
        val command = listOf(
            "mkdir -p /cache/recovery /data/cache/recovery /persist/cache/recovery 2>/dev/null",
            "wrote=0",
            "for f in /cache/recovery/openrecoveryscript /data/cache/recovery/openrecoveryscript /persist/cache/recovery/openrecoveryscript; do",
            "d=\$(dirname \"\$f\")",
            "if [ -d \"\$d\" ] && printf '%s' $escaped > \"\$f\"; then chmod 0644 \"\$f\"; wrote=1; break; fi",
            "done",
            "[ \"\$wrote\" = \"1\" ] || exit 2",
            "echo FLASH_READY_${recovery.name}"
        ).joinToString(" ")
        val output = runRoot(command)
        return if (output.contains("FLASH_READY_${recovery.name}")) {
            runRoot("reboot recovery")
            Result.success("OpenRecoveryScript staged. Device is rebooting to recovery.")
        } else Result.failure(IllegalStateException("Unable to prepare OpenRecoveryScript"))
    }

    fun flashImageWithDd(context: Context, filename: String, target: String): Result<String> {
        val safeName = safeFilename(filename) ?: return Result.failure(IllegalArgumentException("Unsupported image filename"))
        if (!safeName.endsWith(".img", true)) return Result.failure(IllegalArgumentException("dd flashing requires a .img file"))
        val safeTarget = target.takeIf { it.matches(Regex("[A-Za-z0-9_+.-]+")) }
            ?: return Result.failure(IllegalArgumentException("Unsupported partition name"))
        val capability = detect(context)
        if (!capability.rooted) return Result.failure(IllegalStateException("Root access was not granted"))
        if (capability.isSimulation) {
            return Result.success("Safety Simulation Mode: Partition /dev/block/by-name/$safeTarget targeted. Flashed 4M blocks of '$safeName' with fsync verified in emulator.")
        }
        val command = listOf(
            "src=/sdcard/Download/$safeName",
            "dst=/dev/block/by-name/$safeTarget",
            "[ -f \"\$src\" ] || exit 2",
            "[ -b \"\$dst\" ] || exit 3",
            "dd if=\"\$src\" of=\"\$dst\" bs=4M conv=fsync",
            "sync",
            "echo DD_FLASH_OK"
        ).joinToString(" ")
        return if (runRoot(command).contains("DD_FLASH_OK")) Result.success("Partition $target written successfully.")
        else Result.failure(IllegalStateException("dd flash failed"))
    }

    private fun safeFilename(filename: String): String? =
        filename.takeIf { it.matches(Regex("[A-Za-z0-9._+()\\- ]+")) }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    private fun runRoot(command: String): String = runCatching {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        process.waitFor()
        output
    }.getOrDefault("")
}