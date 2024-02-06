package net.akehurst.ide.gui


import javax.swing.JFileChooser
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

data class DirectoryHandleJVM(
    val fileSystem: UserFileSystem,
    val handle: java.io.File
) : DirectoryHandle {

    override val name: String  get() = handle.name

    override suspend fun listContent(): List<FileSystemObjectHandle> =
        fileSystem.listDirectoryContent(this)

}

data class FileHandleJVM(
    val fileSystem: UserFileSystem,
    val handle: java.io.File
) : FileHandle {

    override val name: String  get() = handle.name

    override suspend fun readContent(): String? =
        fileSystem.readFileContent(this)

    override suspend fun writeContent(content: String) =
        fileSystem.writeFileContent(this, content)
}


actual object UserFileSystem {

    actual suspend fun selectProjectDirectoryFromDialog(): DirectoryHandle? {
        val fc = JFileChooser()
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        return if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            DirectoryHandleJVM(this, fc.selectedFile)
        } else {
            null
        }
    }

    actual suspend fun selectFileFromDialog(): FileHandle? {
        val fc = JFileChooser()
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        return if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            FileHandleJVM(this,fc.selectedFile)
        } else {
            null
        }
    }

    actual suspend fun listDirectoryContent(dir: DirectoryHandle): List<FileSystemObjectHandle> {
        return when (dir) {
            is DirectoryHandleJVM -> {
                val dirEntries = dir.handle.toPath().listDirectoryEntries()
                dirEntries.map {
                    when {
                        it.isDirectory() -> DirectoryHandleJVM(this, it.toFile())
                        it.isRegularFile() -> FileHandleJVM(this, it.toFile())
                        else -> error("shoudl not happen")
                    }
                }
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${dir::class.simpleName}")
        }
    }

    actual suspend fun createNewFile(parentPath: DirectoryHandle): FileHandle? {
        return when (parentPath) {
            is DirectoryHandleJVM -> {
TODO()
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${parentPath::class.simpleName}")
        }
    }

    actual suspend fun createNewDirectory(parentPath: DirectoryHandle): DirectoryHandle? {
        return when (parentPath) {
            is DirectoryHandleJVM -> {
                TODO()
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${parentPath::class.simpleName}")
        }
    }

    actual suspend fun readFileContent(file: FileHandle): String? {
        return when (file) {
            is FileHandleJVM -> {
                file.handle.readText()
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${file::class.simpleName}")
        }
    }

    actual suspend fun writeFileContent(file: FileHandle, content: String) {
        return when (file) {
            is FileHandleJVM -> {
                file.handle.writeText(content)
            }

            else -> error("DirectoryHandle is not a DirectoryHandleJS: ${file::class.simpleName}")
        }
    }

}