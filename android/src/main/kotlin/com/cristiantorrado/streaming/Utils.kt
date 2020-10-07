package com.cristiantorrado.streaming

private  val pattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$".toRegex()

fun validateHexadecimalColorPattern(string: String):Boolean{
    return pattern.matches(string)
}