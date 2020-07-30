package io.euphoria.xkcd.app

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

fun Byte.hasFlags(flags: Byte) = this and flags == flags
fun Byte.withFlags(flags: Byte) = this or flags
fun Byte.withoutFlags(flags: Byte) = this and flags.inv()
