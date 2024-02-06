package net.akehurst.ide.gui

interface FileSystemObjectHandle {
    val name:String
}

interface FileHandle : FileSystemObjectHandle {
    suspend fun readContent() : String?
    suspend fun writeContent(content:String)
}

interface DirectoryHandle : FileSystemObjectHandle {
    suspend fun listContent() : List<FileSystemObjectHandle>
}

expect object UserFileSystem {

    suspend fun selectProjectDirectoryFromDialog(): DirectoryHandle?
    suspend fun selectFileFromDialog(): FileHandle?

    suspend fun listDirectoryContent(dir: DirectoryHandle): List<FileSystemObjectHandle>

    suspend fun createNewFile(parentPath: DirectoryHandle): FileHandle?
    suspend fun createNewDirectory(parentPath: DirectoryHandle): DirectoryHandle?

    suspend fun readFileContent(file: FileHandle): String?

    suspend fun writeFileContent(file: FileHandle, content: String)

}