package net.akehurst.ide.common.data.product

import kotlin.jvm.JvmInline

@JvmInline
value class Path(val value:String)

@JvmInline
value class Url(val value:String)

@JvmInline
value class ProductName(val value:String)

data class ProductDetails(
    val localPath: Path,
    val name:ProductName,
    val originUrl:Url,
    val originPath:Path
)