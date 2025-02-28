
import net.akehurst.ide.IdeApplication
import java.io.File

suspend fun main() {
    println("PWD: ${File(".").absolutePath}")
    IdeApplication().start()
}