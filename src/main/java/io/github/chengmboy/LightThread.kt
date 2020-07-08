package io.github.chengmboy

import kotlinx.coroutines.*
import java.lang.Runnable

object LightThread{


    fun start(runnable: Runnable) {
        GlobalScope.launch {
            delay(5)
            runnable.run()
        }
    }
}