package io.github.sumaroder.rosenfeld.interpreter

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, RosenfeldValue>()

    fun define(name: String, value: RosenfeldValue) {
        values[name] = value
    }

    fun get(name: String): RosenfeldValue {
        if (values.containsKey(name)) {
            return values[name]!!
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }
        throw RuntimeException("Undefined variable '$name'")
    }

    fun assign(name: String, value: RosenfeldValue) {
        if (values.containsKey(name)) {
            values[name] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw RuntimeException("Undefined variable '$name'")
    }

    fun getAt(distance: Int, name: String): RosenfeldValue {
        return ancestor(distance).get(name)
    }

    fun assignAt(distance: Int, name: String, value: RosenfeldValue) {
        ancestor(distance).assign(name, value)
    }

    private fun ancestor(distance: Int): Environment {
        var environment = this
        for (i in 0 until distance) {
            environment = environment.enclosing!!
        }
        return environment
    }

    fun contains(name: String): Boolean {
        return values.containsKey(name) || enclosing?.contains(name) == true
    }
}