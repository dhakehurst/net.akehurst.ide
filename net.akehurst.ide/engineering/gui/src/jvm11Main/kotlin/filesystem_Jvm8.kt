package net.akehurst.ide.gui


import java.awt.EventQueue
import java.io.File
import javax.swing.JFileChooser
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

data class DirectoryHandleJVM(
    val fileSystem: UserFileSystem,
    val handle: java.io.File
) : DirectoryHandleAbstract() {

    override val name: String get() = handle.name

    override val path: String get() = handle.path

    override suspend fun entry(name: String): FileSystemObjectHandle? =
        fileSystem.getEntry(this, name)


    override suspend fun listContent(): List<FileSystemObjectHandle> =
        fileSystem.listDirectoryContent(this)

    override suspend fun createDirectory(name: String): DirectoryHandle? {
        TODO("not implemented")
    }

    override suspend fun createFile(name: String): FileHandle? {
        TODO("not implemented")
    }
}

data class FileHandleJVM(
    val fileSystem: UserFileSystem,
    val handle: java.io.File
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
            is DirectoryHandleJVM -> {
                val f = parentDirectory.handle.resolve(name)
                FileHandleJVM(parentDirectory.fileSystem, f)
            }

            else -> null
        }
    }

    actual suspend fun selectProjectDirectoryFromDialog(useDispatcher: Boolean, selected: String?): DirectoryHandle? {
        return if (useDispatcher) {
            var handle: DirectoryHandle? = null
            EventQueue.invokeAndWait() {
                handle = chooseDirectory2(selected)
            }
            handle
        } else {
            chooseDirectory2(selected)
        }
    }

    private fun chooseDirectory(selected: String?): DirectoryHandle? {
        val fc = JFileChooser()
        selected?.let { fc.selectedFile = File(it) }
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        fc.isFileHidingEnabled = false
        return if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            DirectoryHandleJVM(this, fc.selectedFile)
        } else {
            null
        }
    }

    private fun chooseDirectory2(selected: String?): DirectoryHandle? {
        val parentFrame =  java.awt.Frame("Choose Directory")
        System.setProperty("apple.awt.fileDialogForDirectories", "true")
        val fd = java.awt.FileDialog(parentFrame, "Choose directory")
        fd.directory = selected
        fd.isVisible = true
        val selectedFile = fd.file?.let{ File(fd.directory+"/"+it) }
        return selectedFile?.let { DirectoryHandleJVM(this, it) }
    }

    actual suspend fun selectExistingFileFromDialog(): FileHandle? {
        val fc = JFileChooser()
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        return if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            FileHandleJVM(this, fc.selectedFile)
        } else {
            null
        }
    }

    actual suspend fun selectNewFileFromDialog(parentDirectory: DirectoryHandle): FileHandle? {
        val fc = JFileChooser()
        fc.currentDirectory = (parentDirectory as DirectoryHandleJVM).handle
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY)
        fc.setAcceptAllFileFilterUsed(false)
        return if (fc.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            val file = fc.selectedFile
            when {
                file.exists() -> Unit
                else -> file.createNewFile()
            }
            FileHandleJVM(this, file)
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