import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.test.Test

class test_GlobRegex {

    @Test
    fun f() {

        // Corrected Regex
        val GLOB_DETECT_REGEX = "([a-zA-Z0-9_*/.?{},\\[\\]-]|\\\\.)+"
        val GLOB_PATTERN = Regex(GLOB_DETECT_REGEX);

        fun isGlob(str: String?): Boolean {
            return if (str == null || str.isEmpty()) {
                false
            }else {
                GLOB_PATTERN.matches(str) // Checks if any part of the string matches the glob pattern elements
            }
        }

        println("--- Testing with Corrected Regex ---")
        println("file.txt: " + isGlob("file.txt"))
        println("file*.txt: " + isGlob("file*.txt"))
        println("photo?.jpg: " + isGlob("photo?.jpg"))
        println("doc[1-3].pdf: " + isGlob("doc[1-3].pdf"))
        println("img[a-zA-Z].png: " + isGlob("img[a-zA-Z].png"))
        println("test[].md: " + isGlob("test[].md"))
        println("test[abc].md: " + isGlob("test[abc].md"))
        println("{src,lib}/**/*.js: " + isGlob("{src,lib}/**/*.js"))
        println("file\\*.txt: " + isGlob("file\\*.txt"))
        println("another{foo,bar}test: " + isGlob("another{foo,bar}test"))
        println("just{a,b}.txt: " + isGlob("just{a,b}.txt"))
        println("no_glob_here{}.txt: " + isGlob("no_glob_here{}.txt"))
        println("path/**/file: " + isGlob("path/**/file"))

    }

}