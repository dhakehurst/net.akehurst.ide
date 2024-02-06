package net.akehurst.ide.gui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import net.akehurst.ide.gui.fs.FileSystemDirectoryHandle
import net.akehurst.ide.gui.fs.FileSystemFileHandle
import net.akehurst.language.editor.common.objectJS
import kotlin.js.Promise

data class DirectoryHandleJS(
    val fileSystem: UserFileSystem,
    val handle: FileSystemDirectoryHandle
) : DirectoryHandle {
    override val name: String get() = handle.name

    override suspend fun listContent(): List<FileSystemObjectHandle> =
        fileSystem.listDirectoryContent(this)

}

data class FileHandleJS(
    val fileSystem: UserFileSystem,
    val handle: FileSystemFileHandle
) : FileHandle {
    override val name: String get() = handle.name

    override suspend fun readContent(): String? =
        fileSystem.readFileContent(this)

    override suspend fun writeContent(content: String) =
        fileSystem.writeFileContent(this, content)
}

actual object UserFileSystem {

    actual suspend fun selectProjectDirectoryFromDialog(): DirectoryHandle? {
        val w: dynamic = window
        val p: Promise<dynamic> = w.showDirectoryPicker(
            objectJS {
                mode = "readwrite"
            }
        )
        val handle: FileSystemDirectoryHandle = p.await()
        return DirectoryHandleJS(this, handle)
    }

    actual suspend fun selectFileFromDialog(): FileHandle? {
        val w: dynamic = window
        val p: Promise<dynamic> = w.showOpenFilePicker(
            objectJS {
                mode = "readwrite"
            }
        )
        val handle: FileSystemFileHandle = p.await()
        return FileHandleJS(this, handle)
    }

    actual suspend fun listDirectoryContent(dir: DirectoryHandle): List<FileSystemObjectHandle> =
        when (dir) {
            is DirectoryHandleJS -> {
                val list = mutableListOf<FileSystemObjectHandle>()
                for (v in dir.handle.values()) {
                    val o = when (v.kind) {
                        "file" -> FileHandleJS(this, dir.handle.getFileHandle(v.name))
                        "directory" -> DirectoryHandleJS(this, dir.handle.getDirectoryHandle(v.name))
                        else -> error("Should not happen")
                    }
                    list.add(o)
                }
                list
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${dir::class.simpleName}")
        }

    actual suspend fun createNewFile(parentPath: DirectoryHandle): FileHandle? {
        return when (parentPath) {
            is DirectoryHandleJS -> {
TODO()
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${parentPath::class.simpleName}")
        }
    }

    actual suspend fun createNewDirectory(parentPath: DirectoryHandle): DirectoryHandle? {
        return when (parentPath) {
            is DirectoryHandleJS -> {
                TODO()
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${parentPath::class.simpleName}")
        }
    }

    actual suspend fun readFileContent(file: FileHandle): String? =
        when (file) {
            is FileHandleJS -> {
                file.handle.file.await().text().await()
            }

            else -> error("FileHandle is not a FileHandleJS: ${file::class.simpleName}")
        }


    actual suspend fun writeFileContent(file: FileHandle, content: String) {
        when (file) {
            is FileHandleJS -> {
                val w = file.handle.createWritable().await()
                w.write(content).await()
                w.close().await()
            }

            else -> error("FileHandle is not a FileHandleJS: ${file::class.simpleName}")
        }
    }


}