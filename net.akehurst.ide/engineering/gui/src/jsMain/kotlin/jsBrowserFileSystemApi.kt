package net.akehurst.ide.gui.fs

import js.buffer.ArrayBuffer
import js.core.Void
import js.iterable.AsyncIterable
import kotlin.js.Date
import kotlin.js.Promise

external interface FileSystemHandle {
    /**
     * 'file' | 'directory'
     */
    val kind:String
    val name:String
}

external interface FileSystemDirectoryHandle : FileSystemHandle {
    fun values(): AsyncIterable<Value>

    fun getDirectoryHandle(name:String):FileSystemDirectoryHandle
    fun getFileHandle(name:String):FileSystemFileHandle
}
external interface Value {
    val kind:String
    val name:String
}

external interface FileSystemFileHandle : FileSystemHandle {
    val file: Promise<File>
    fun createWritable():Promise<FileSystemWritableFileStream>
}

external interface FileSystemWritableFileStream : WritableStream {
    /**
     * data: ArrayBuffer | TypesArray | DataView | Blob | String | { type,data,position,size }
     */
    fun write(data:Any):Promise<Void>
}

external interface WritableStream {
    fun close():Promise<Void>
}

external interface File : Blob {
    val lastModified:Number
    val lasModifiedDate:Date
    val name : String
    val webkitRelativePath:String
}

external interface Blob {
    val size:Number
    val type:String

    fun arrayBuffer():Promise<ArrayBuffer>
//    fun slice():Blob
//    fun stream():Any
    fun text():Promise<String>
}