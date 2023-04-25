package net.cubosoft.weelab.io.weelab3

import android.content.Context

open class SingletonCounter<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val checkInstance = instance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}

class CounterManager private constructor(context: Context) {
    companion object : SingletonCounter<CounterManager, Context>(::CounterManager)

    private var counter: Int = 0
    private var idThreadLastUpdate: String = ""
    private var idThreadToClose: String = ""

    fun initCounter(name: String) {
        try {
            idThreadLastUpdate = name
            counter++
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun setCounterAndName(name: String){
        idThreadLastUpdate = name
        if (counter < 1000)
            counter++
        else
            counter = 1
    }

    fun getCounter(): Int {
        return counter
    }

    fun getIdThreadLastUpdate(): String {
        return idThreadLastUpdate
    }

    fun getIdThreadToClose(): String {
        return idThreadToClose
    }

    fun setIdThreadToClose(name: String){
        idThreadToClose = name
    }

}