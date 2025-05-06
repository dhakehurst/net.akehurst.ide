package net.akehurst.ide.common.data.fileextensionmap

import kotlin.test.Test
import kotlin.test.assertEquals

class test_FileExtensionMap {

    @Test
    fun qualifiedName() {
        val sentence = """
            .ext : some.qual.name
        """

        val actual = FileExtensionMap.process(sentence)

        val expected = mapOf(
            ".ext" to "some.qual.name"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun path() {
        val sentence = """
            .ext : /some/path/name
        """

        val actual = FileExtensionMap.process(sentence)

        val expected = mapOf(
            ".ext" to "/some/path/name"
        )
        assertEquals(expected, actual)
    }


}