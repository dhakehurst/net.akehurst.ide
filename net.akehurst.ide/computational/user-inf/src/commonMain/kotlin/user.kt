package net.akehurst.ide.user.inf

import net.akehurst.ide.common.data.product.Path
import net.akehurst.ide.common.data.product.Url


interface User : EditorProvide{
    suspend fun start()
}

data class Credentials(
    val username:String,
    val password:String
)

interface Authentication {
    fun authenticate(credentials:Credentials)
}

interface ProjectManagementRequest {
    fun listExistingProducts()
    fun createNewProduct(localPath: Path)
    fun fetchExistingProduct(originUrl:Url, originPath:Path, localPath:Path)
    fun openExistingProduct(path: Path)
}