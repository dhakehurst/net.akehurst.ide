package net.akehurst.ide.gui

import korlibs.io.file.std.resourcesVfs


object AppFileSystem {

    suspend fun read(resourcePath:String): String {
        return resourcesVfs[resourcePath].readString()
    }

}