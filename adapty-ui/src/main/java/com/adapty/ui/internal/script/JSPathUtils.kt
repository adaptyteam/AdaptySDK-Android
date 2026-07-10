package com.adapty.ui.internal.script

internal data class PathComponent(val value: String)

internal object JSPathUtils {

    fun parsePath(path: String): List<PathComponent> {
        val parts = mutableListOf<PathComponent>()
        var i = 0

        while (i < path.length) {
            when {
                path[i].isWhitespace() -> {
                    i++
                }
                path[i] == '[' -> {
                    i++
                    while (i < path.length && path[i].isWhitespace()) i++

                    if (i < path.length && (path[i] == '"' || path[i] == '\'' || path[i] == '`')) {
                        val quoteChar = path[i]
                        i++
                        val start = i
                        while (i < path.length && path[i] != quoteChar) {
                            if (path[i] == '\\' && i + 1 < path.length) i += 2 else i++
                        }
                        val part = path.substring(start, i)
                        i++
                        while (i < path.length && path[i].isWhitespace()) i++
                        if (i < path.length && path[i] == ']') i++
                        if (part.isNotEmpty()) parts.add(PathComponent(part))
                    } else {
                        val start = i
                        while (i < path.length && path[i] != ']') i++
                        val part = path.substring(start, i).trim()
                        if (i < path.length) i++
                        if (part.isNotEmpty()) parts.add(PathComponent(part))
                    }
                }
                path[i] == '.' -> {
                    i++
                }
                else -> {
                    val start = i
                    while (i < path.length && path[i] != '.' && path[i] != '[' && !path[i].isWhitespace()) {
                        i++
                    }
                    val part = path.substring(start, i)
                    if (part.isNotEmpty()) {
                        parts.add(PathComponent(part))
                    }
                }
            }
        }

        return parts
    }

    fun buildPath(basePath: String?, variableName: String): List<PathComponent> {
        val base = if (basePath != null) parsePath(basePath) else emptyList()
        return base + parsePath(variableName)
    }

    fun escapeJsString(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (ch in s) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\u0000' -> sb.append("\\0")
                '\u2028' -> sb.append("\\u2028")
                '\u2029' -> sb.append("\\u2029")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val JS_IDENTIFIER_REGEX = Regex("^[a-zA-Z_\$][a-zA-Z0-9_\$]*$")

    fun isValidJsIdentifier(s: String): Boolean = JS_IDENTIFIER_REGEX.matches(s)

    private fun prologueStatements(firstComponent: PathComponent): String {
        val escaped = escapeJsString(firstComponent.value)
        val sb = StringBuilder("var g=typeof globalThis!==\"undefined\"?globalThis:this;")
        sb.append("var r=g[\"$escaped\"];")
        if (isValidJsIdentifier(firstComponent.value)) {
            sb.append("if(r===undefined)try{r=${firstComponent.value}}catch(x){}")
        }
        return sb.toString()
    }

    private fun bracketChain(baseVar: String, components: List<PathComponent>): String {
        if (components.isEmpty()) return baseVar
        return components.joinToString("") { "[\"${escapeJsString(it.value)}\"]" }
            .let { baseVar + it }
    }

    fun generateHasFunctionScript(path: List<PathComponent>): String {
        require(path.isNotEmpty())
        val prol = prologueStatements(path[0])
        val accessor = bracketChain("r", path.drop(1))
        return "(function(){try{${prol}return typeof $accessor===\"function\"}catch(e){return false}})()"
    }

    fun generateCallScript(path: List<PathComponent>, paramsJson: String): String {
        require(path.isNotEmpty()) { "Path must not be empty for function call" }

        val sb = StringBuilder("(function(){")
        sb.append(prologueStatements(path[0]))

        if (path.size == 1) {
            sb.append("if(typeof r===\"function\")r($paramsJson);")
        } else {
            val parent = bracketChain("r", path.subList(1, path.size - 1))
            val lastKey = "\"${escapeJsString(path.last().value)}\""
            sb.append("var p=$parent;if(p!=null&&typeof p[$lastKey]===\"function\")p[$lastKey]($paramsJson);")
        }

        sb.append("})()")
        return sb.toString()
    }

    fun generateSetValueScript(path: List<PathComponent>, valueJson: String): String {
        require(path.isNotEmpty()) { "Path must not be empty for set value" }

        if (path.size == 1) {
            val key = "\"${escapeJsString(path[0].value)}\""
            return "(function(){var g=typeof globalThis!==\"undefined\"?globalThis:this;" +
                "g[$key]=$valueJson;})()"
        }

        val sb = StringBuilder("(function(){")
        sb.append(prologueStatements(path[0]))
        val firstKey = "\"${escapeJsString(path[0].value)}\""
        sb.append("if(r===undefined||r===null)r=g[$firstKey]={};")

        var accessor = "r"
        for (i in 1 until path.size - 1) {
            val key = "\"${escapeJsString(path[i].value)}\""
            val next = "$accessor[$key]"
            sb.append("if($next===undefined||$next===null)$accessor[$key]={};")
            accessor = next
        }

        val lastKey = "\"${escapeJsString(path.last().value)}\""
        sb.append("$accessor[$lastKey]=$valueJson;")

        sb.append("})()")
        return sb.toString()
    }

    fun generateGetValueScript(path: List<PathComponent>): String {
        require(path.isNotEmpty())
        val prol = prologueStatements(path[0])
        val accessor = bracketChain("r", path.drop(1))
        return "(function(){try{${prol}return $accessor}catch(e){return undefined}})()"
    }

    fun generateHasScript(path: List<PathComponent>): String {
        require(path.isNotEmpty())
        val prol = prologueStatements(path[0])
        val accessor = bracketChain("r", path.drop(1))
        return "(function(){try{${prol}return typeof $accessor!==\"undefined\"}catch(e){return false}})()"
    }

    fun generateGetJsonScript(path: List<PathComponent>): String {
        require(path.isNotEmpty())
        val prol = prologueStatements(path[0])
        val accessor = bracketChain("r", path.drop(1))
        return "(function(){try{${prol}return JSON.stringify($accessor)}catch(e){return \"null\"}})()"
    }
}
