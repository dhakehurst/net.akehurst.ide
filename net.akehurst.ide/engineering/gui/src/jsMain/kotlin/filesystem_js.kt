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

    override val path: String get() = TODO()

    override val name: String get() = handle.name

    override suspend fun entry(name: String): FileSystemObjectHandle? =
        fileSystem.getEntry(this, name)

    override suspend fun listContent(): List<FileSystemObjectHandle> =
        fileSystem.listDirectoryContent(this)

}

data class FileHandleJS(
    val fileSystem: UserFileSystem,
    val handle: FileSystemFileHandle
) : FileHandle {
    override val name: String get() = handle.name
    override val extension: String get() = name.substringAfterLast('.')

    override suspend fun readContent(): String? =
        fileSystem.readFileContent(this)

    override suspend fun writeContent(content: String) =
        fileSystem.writeFileContent(this, content)
}

actual object UserFileSystem {

    actual suspend fun getEntry(parentDirectory: DirectoryHandle, name: String): FileSystemObjectHandle? {
        return when (parentDirectory) {
            is DirectoryHandleJS -> {
                for (v in parentDirectory.handle.values()) {
                    when (v.name) {
                        name -> {
                            return when (v.kind) {
                                "file" -> FileHandleJS(this, parentDirectory.handle.getFileHandle(v.name).await())
                                "directory" -> DirectoryHandleJS(this, parentDirectory.handle.getDirectoryHandle(v.name))
                                else -> error("Should not happen")
                            }
                        }

                        else -> null
                    }
                }
                null
            }

            else -> null
        }
    }

    actual suspend fun selectProjectDirectoryFromDialog(useDispatcher: Boolean, selected: String?): DirectoryHandle? {
        val w: dynamic = window
        val p: Promise<dynamic> = w.showDirectoryPicker(
            objectJS {
                mode = "readwrite"
            }
        )
        return try {
            val handle: FileSystemDirectoryHandle = p.await()
            DirectoryHandleJS(this, handle)
        } catch (t: Throwable) {
            null
        }
    }

    actual suspend fun selectExistingFileFromDialog(): FileHandle? {
        val w: dynamic = window
        val p: Promise<dynamic> = w.showOpenFilePicker(
            objectJS {
                mode = "readwrite"
            }
        )
        return try {
            val handle: FileSystemFileHandle = p.await()
            FileHandleJS(this, handle)
        } catch (t: Throwable) {
            null
        }
    }

    actual suspend fun selectNewFileFromDialog(parentDirectory: DirectoryHandle): FileHandle? {
        val w: dynamic = window
        val p: Promise<dynamic> = w.showSaveFilePicker(
            objectJS {
//                types = arrayOf(
//                    objectJS {
//                        description = "SysML v2 file"
//                        accept = objectJS {}.set("text/plain", arrayOf(".sysml"))
//                    }
//                )
            }
        )
        return try {
            val handle: FileSystemFileHandle = p.await()
            FileHandleJS(this, handle)
        } catch (t: Throwable) {
            null
        }
    }

    actual suspend fun listDirectoryContent(dir: DirectoryHandle): List<FileSystemObjectHandle> =
        when (dir) {
            is DirectoryHandleJS -> {
                val list = mutableListOf<FileSystemObjectHandle>()
                for (v in dir.handle.values()) {
                    val o = when (v.kind) {
                        "file" -> FileHandleJS(this, dir.handle.getFileHandle(v.name).await())
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
                file.handle.getFile().await().text().await()
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