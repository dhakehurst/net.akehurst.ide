package net.akehurst.ide

import kotlinx.coroutines.Deferred
import net.akehurst.ide.gui.Gui
import net.akehurst.kotlinx.filesystem.FileSystemFromVfs
import net.akehurst.kotlinx.filesystem.ResourcesFileSystem
import net.akehurst.kotlinx.filesystem.UserFileSystem
import net.akehurst.kotlinx.logging.api.LogFunction
import net.akehurst.language.editor.api.LanguageService

class IdeApplication(
    logFunction: LogFunction,
    appFilesystem: FileSystemFromVfs,
    userSettingsFilesystem: FileSystemFromVfs,
    languageService: LanguageService
) {

    //val ide=  IdeCore()
    val gui = Gui(logFunction, appFilesystem, userSettingsFilesystem, languageService)

    suspend fun start(guiStart: suspend (Gui) -> Deferred<Unit>) {
        val guiJob = guiStart.invoke(gui)

        guiJob.join()
    }

}