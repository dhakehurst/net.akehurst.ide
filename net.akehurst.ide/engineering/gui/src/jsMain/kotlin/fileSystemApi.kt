package net.akehurst.ide.gui.fs

import js.iterable.AsyncIterable

external interface FileSystemDirectoryHandle {
    val values: AsyncIterable<Value>
}
external interface Value {
    val kind:String
    val name:String
}