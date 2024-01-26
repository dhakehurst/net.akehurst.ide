package net.akehurst.ide.gui

import korlibs.io.file.std.resourcesVfs

actual object FileSystem {

    actual suspend fun read(resourcePath:String): String {
        return resourcesVfs[resourcePath].readString()
    }

}