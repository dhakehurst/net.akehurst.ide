package net.akehurst.ide.gui

import korlibs.io.file.VfsFile
import korlibs.io.file.baseName

interface AppFilesystem {
    val root: DirectoryHandle

    suspend fun getDirectory(resourcePath: String): DirectoryHandle?
    suspend fun getFile(resourcePath: String): FileHandle?
    suspend fun read(resourcePath: String): String
}

interface FileSystemObjectHandle {
    val name: String
}

interface FileHandle : FileSystemObjectHandle {
    val extension: String
    suspend fun readContent(): String?
    suspend fun writeContent(content: String)
}

interface DirectoryHandle : FileSystemObjectHandle {
    val path: String

    suspend fun listContent(): List<FileSystemObjectHandle>
    suspend fun entry(name: String): FileSystemObjectHandle?
    suspend fun file(name: String): FileHandle?
    suspend fun directory(name: String): DirectoryHandle?
    suspend fun createFile(name: String): FileHandle?
    suspend fun createDirectory(name: String): DirectoryHandle?
}


class FileSystemFromVfs(
    private val _vfsRoot: VfsFile
) : AppFilesystem {

    override val root get() = DirectoryHandleVfs(this, _vfsRoot)

    override suspend fun getDirectory(resourcePath: String): DirectoryHandle? {
        return _vfsRoot[resourcePath].takeIfExists()?.let {
            DirectoryHandleVfs(this, it)
        }
    }

    override suspend fun getFile(resourcePath: String): FileHandle? {
        return _vfsRoot[resourcePath].takeIfExists()?.let {
            FileHandleVfs(this, it)
        }
    }

    override suspend fun read(resourcePath: String): String {
        return _vfsRoot[resourcePath].readString()
    }
}

abstract class DirectoryHandleAbstract : DirectoryHandle {
    override suspend fun directory(name: String): DirectoryHandle? {
        val entry = entry(name)
        return when (entry) {
            is DirectoryHandle -> entry
            else -> null
        }
    }

    override suspend fun file(name: String): FileHandle? {
        val entry = entry(name)
        return when (entry) {
            is FileHandle -> entry
            else -> null
        }
    }
}

abstract class FileHandleAbstract : FileHandle {
    override val extension: String get() = name.substringAfterLast('.')
}

class DirectoryHandleVfs(
    val filesystem: AppFilesystem,
    private val _handle: VfsFile
) : DirectoryHandleAbstract() {

    override val path: String get() = _handle.path

    override val name: String get() = _handle.pathInfo.baseName

    override suspend fun listContent(): List<FileSystemObjectHandle> {
        return when {
            _handle.isFile() -> emptyList()
            _handle.isDirectory() -> _handle.listNames().map {
                entry(it) ?: error("Not found entry: $it")
            }

            else -> emptyList()
        }
    }

    override suspend fun entry(name: String): FileSystemObjectHandle? {
        return _handle[name].takeIfExists()?.let {
            when {
                it.isDirectory() -> DirectoryHandleVfs(filesystem, it)
                it.isFile() -> FileHandleVfs(filesystem, it)
                else -> null
            }
        }
    }

    override suspend fun createDirectory(name: String): DirectoryHandle? {
        TODO("not implemented")
    }

    override suspend fun createFile(name: String): FileHandle? {
        _handle[name].ensureParents()
        _handle[name].writeString("")
        return FileHandleVfs(filesystem, _handle[name])
    }
}

class FileHandleVfs(
    val filesystem: AppFilesystem,
    private val _handle: VfsFile
) : FileHandleAbstract() {

    override val name: String get() = _handle.pathInfo.baseName

    override suspend fun readContent(): String? =
        _handle.readString()

    override suspend fun writeContent(content: String) {
        _handle.writeString(content)
    }
}