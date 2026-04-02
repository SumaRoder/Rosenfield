package io.github.sumaroder.rosenfeld.interpreter

import io.github.sumaroder.rosenfeld.ast.*

sealed class RosenfeldValue {
    abstract fun typeName(): String
    open fun isTruthy(): Boolean = true
    open fun toDisplayString(): String = toString()
}

object NullValue : RosenfeldValue() {
    override fun typeName() = "Null"
    override fun isTruthy() = false
    override fun toString() = "null"
}

data class IntValue(val value: Long) : RosenfeldValue() {
    override fun typeName() = "Int"
    override fun toString() = value.toString()
}

data class FloatValue(val value: Double) : RosenfeldValue() {
    override fun typeName() = "Float"
    override fun toString() = value.toString()
}

data class StringValue(val value: String) : RosenfeldValue() {
    override fun typeName() = "Str"
    override fun isTruthy() = value.isNotEmpty()
    override fun toString() = value
    override fun toDisplayString() = value
}

data class BoolValue(val value: Boolean) : RosenfeldValue() {
    override fun typeName() = "Bool"
    override fun isTruthy() = value
    override fun toString() = value.toString()
}

data class FunctionValue(
    val name: String,
    val params: List<Parameter>,
    val body: Stmt,
    val closure: Environment,
    val isInit: Boolean = false
) : RosenfeldValue() {
    override fun typeName() = "Function"
    override fun toString() = "<fn $name>"
}

data class ClassValue(
    val name: String,
    val methods: Map<String, FunctionValue>,
    val properties: Map<String, PropertyDecl>,
    val init: FunctionValue?
) : RosenfeldValue() {
    override fun typeName() = "Class"
    override fun toString() = "<class $name>"

    fun call(interpreter: Interpreter, arguments: List<RosenfeldValue>): RosenfeldValue {
        val instance = InstanceValue(this)
        
        for ((name, propDecl) in properties) {
            propDecl.initializer?.let { initExpr ->
                val value = interpreter.evaluate(initExpr)
                instance.fields[name] = value
            }
        }
        
        init?.let {
            val boundInit = it.bind(instance)
            interpreter.callFunction(boundInit, arguments)
        }
        return instance
    }
}

data class InstanceValue(
    val clazz: ClassValue,
    val fields: MutableMap<String, RosenfeldValue> = mutableMapOf(),
    val properties: MutableMap<String, RosenfeldValue> = mutableMapOf()
) : RosenfeldValue() {
    override fun typeName() = "Instance"
    override fun toString() = "<${clazz.name} instance>"

    fun get(name: String, interpreter: Interpreter): RosenfeldValue {
        fields[name]?.let { return it }

        clazz.properties[name]?.let { propDecl ->
            val getter = propDecl.getter
            if (getter != null && getter.body is Identifier && getter.body.name == "__field__") {
                return fields[name] ?: NullValue
            } else if (getter != null) {
                return interpreter.evaluateGetter(this, propDecl, getter)
            }
            return fields[name] ?: NullValue
        }

        clazz.methods[name]?.let { return it.bind(this) }

        throw RuntimeException("Undefined property '$name' on ${clazz.name}")
    }

    fun set(name: String, value: RosenfeldValue, interpreter: Interpreter) {
        clazz.properties[name]?.let { propDecl ->
            if (propDecl.setter != null) {
                interpreter.executeSetter(this, propDecl, value)
                return
            }
        }

        fields[name] = value
    }
}

sealed class ResultValue : RosenfeldValue() {
    override fun typeName() = "R"

    data class Ok(val value: RosenfeldValue) : ResultValue() {
        override fun isTruthy() = true
        override fun toString() = "Ok($value)"
    }

    data class Err(val error: RosenfeldValue) : ResultValue() {
        override fun isTruthy() = false
        override fun toString() = "Err($error)"
    }
}

data class ListValue(
    val elements: MutableList<RosenfeldValue>
) : RosenfeldValue() {
    override fun typeName() = "List"
    override fun toString() = elements.joinToString(", ", "[", "]")
}

fun FunctionValue.bind(instance: InstanceValue): FunctionValue {
    val newClosure = Environment(closure)
    newClosure.define("this", instance)
    return FunctionValue(name, params, body, newClosure, isInit)
}

class ReturnException(val value: RosenfeldValue) : Exception()
class BreakException : Exception()
class ContinueException : Exception()
