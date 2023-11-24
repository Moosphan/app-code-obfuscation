package com.dorck.app.code.guard.extension

/**
 * Base capabilities extension for plugins.
 * @author Dorck
 * @since 2023/11/24
 */
open class BasePluginExtension {
    // Is plugin capability enabled.
    var enable: Boolean = true
    // Whether to enable debugging logs.
    var logDebug: Boolean = true
    // Whether to support increment processing.
    var supportIncremental: Boolean = false
}