package net.akehurst.ide.gui


expect object UserFileSystem {

    suspend fun getEntry(parentDirectory: DirectoryHandle, name:String):FileSystemObjectHandle?

    suspend fun selectProjectDirectoryFromDialog(useDispatcher: Boolean, selected: String?): DirectoryHandle?
    suspend fun selectExistingFileFromDialog(): FileHandle?
    suspend fun selectNewFileFromDialog(parentDirectory: DirectoryHandle): FileHandle?

    suspend fun listDirectoryContent(dir: DirectoryHandle): List<FileSystemObjectHandle>

    suspend fun createNewFile(parentPath: DirectoryHandle): FileHandle?
    suspend fun createNewDirectory(parentPath: DirectoryHandle): DirectoryHandle?

    suspend fun readFileContent(file: FileHandle): String?

    suspend fun writeFileContent(file: FileHandle, content: String)

}