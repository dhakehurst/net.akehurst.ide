package net.akehurst.ide.gui

import androidx.compose.ui.window.singleWindowApplication
import korlibs.io.file.std.applicationVfs
import korlibs.io.file.std.jailedLocalVfs
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs
import korlibs.io.file.std.userHomeVfs
import net.akehurst.language.editor.api.LanguageService
import net.akehurst.language.editor.api.LogFunction
import net.akehurst.language.editor.api.LogLevel
import net.akehurst.language.editor.common.LanguageServiceByJvmThread
import java.awt.EventQueue
import java.util.concurrent.Executors

actual class Gui : GuiAbstract() {


    actual override val appFileSystem: AppFilesystem by lazy {
        if (null!=System.getProperty("net.akehurst.ide.app.directory")) {
            //applicationVfs
            FileSystemFromVfs(
                jailedLocalVfs(System.getProperty("net.akehurst.ide.app.directory"))
            )
        } else {

            FileSystemFromVfs(resourcesVfs)
        }
    }

    actual override val userSettings: AppFilesystem by lazy {
        FileSystemFromVfs(userHomeVfs[".net-akehurst-ide"].jail())
    }

    actual override val logFunction: LogFunction = { logLevel, prefix, t, msg ->
        when {
            logLevel <= LogLevel.All -> {
                println("$logLevel: $prefix - ${msg.invoke()}")
                t?.let { println("$logLevel: $t") }
            }
        }
    }

    actual override val languageService: LanguageService  = LanguageServiceByJvmThread(Executors.newSingleThreadExecutor(), logFunction)

    override suspend fun start() {
            super.start()
        singleWindowApplication(
            title = "IDE",
        ) {
            content()
        }
    }
}
