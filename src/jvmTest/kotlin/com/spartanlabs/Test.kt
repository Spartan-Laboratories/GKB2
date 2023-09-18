package com.spartanlabs

import com.spartanlabs.generaltools.read
import org.junit.Test
import org.springframework.context.annotation.ComponentScan

class Test {
    @Test
    fun testRead(){
        println(read("keys.txt"))
    }
}