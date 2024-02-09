package net.akehurst.ide.gui

import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs


class AppFileSystem(
    resourcesPathRoot: String
) {

    val resources = localVfs(resourcesPathRoot).jail()

    suspend fun read(resourcePath:String): String {
        return resources[resourcePath].readString()
    }

}