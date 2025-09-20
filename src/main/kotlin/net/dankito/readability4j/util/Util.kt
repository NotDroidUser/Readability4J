package net.dankito.readability4j.util

import org.jsoup.nodes.Node

//todo Test if this is better than node.outerHTML().split("\n").first()
fun logNode(node: Node): String {
    return if (node.normalName().startsWith('#') && node.normalName()!="#root"){
        node.toString()
    } else
        "<${node.nodeName()} ${node.attributes().joinToString(separator = " ") { "${it.key}=\\\"${it.value}\\\"" }}>"
}

fun Node.log():String{
    return logNode(this)
}

fun String?.logDebug():String? {
    return this?.replace("\n","\\n")?.replace("\"","\\\"")
}
