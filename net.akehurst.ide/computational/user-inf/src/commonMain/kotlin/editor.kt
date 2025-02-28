package net.akehurst.ide.user.inf

interface EditorProvide {
   // fun lineTokens(lineStart:Int, tokens:List<List<Any>>)

}

interface EditorRequire {
    fun textChange()
}