package net.akehurst.ide.gui

expect object FileSystem {

    suspend fun read(resourcePath:String): String

}