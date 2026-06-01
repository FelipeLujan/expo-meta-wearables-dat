package expo.modules.emwdat

import com.meta.wearable.dat.core.types.Permission

internal fun permissionFromString(permission: String): Permission? = when (permission) {
    "camera" -> Permission.CAMERA
    "microphone" -> Permission.MICROPHONE
    else -> null
}

internal fun permissionToString(permission: Permission): String = when (permission) {
    Permission.CAMERA -> "camera"
    Permission.MICROPHONE -> "microphone"
    else -> "unknown"
}
