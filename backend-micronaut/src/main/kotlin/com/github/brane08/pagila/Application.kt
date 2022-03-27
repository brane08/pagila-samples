package com.github.brane08.pagila

import io.micronaut.runtime.Micronaut.*
fun main(args: Array<String>) {
	build()
	    .args(*args)
		.packages("com.github.brane08.pagila")
		.start()
}

