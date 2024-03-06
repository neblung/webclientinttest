package com.github.neblung.webclientinttest.javalin

import io.javalin.Javalin
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.net.ServerSocket

private val namespace = ExtensionContext.Namespace.create("javalinExtension")

val ExtensionContext.javalinPort get() = getStore(namespace).get("javalinPort") as Int

class JavalinExtension : BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    override fun beforeAll(context: ExtensionContext) {
        // Set Port only for top level class, not for @Nested class
        // Nested classes should use same SpringBootContext
        if (context.requiredTestClass.isMemberClass) return
        ServerSocket(0).use {
            context.getStore(namespace).put("javalinPort", it.localPort)
        }
    }

    override fun beforeEach(context: ExtensionContext) {
        _javalin = Javalin.create().apply {
            start(context.javalinPort)
        }
    }

    override fun afterEach(context: ExtensionContext) {
        javalin.stop()
        _javalin = null
    }
}

private var _javalin: Javalin? = null
val javalin: Javalin get() = _javalin ?: error("javalin no set. Forgot to use @ExtendWith(JavalinExtension::class) ?")
