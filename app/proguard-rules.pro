# R8 is enabled for the release build (isMinifyEnabled = true in
# app/build.gradle.kts). Most of the stack ships R8-safe consumer rules:
# Room (KSP-generated *_Impl), Compose, Vico, and WorkManager all keep what
# they need. The one app-specific reflection risk is the WorkManager worker:
# WorkManager instantiates it by class name via reflection, so its class and
# its (Context, WorkerParameters) constructor must survive obfuscation/shrink.
-keep class org.mtopol.moodtracker.reminder.ReminderWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
