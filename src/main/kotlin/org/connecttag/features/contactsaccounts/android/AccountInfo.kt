package org.connecttag.features.contactsaccounts.android

import kotlinx.serialization.Serializable
import androidx.annotation.Keep

@Keep
@Serializable
public data class AccountInfo(
    val name: String? = null,
    val type: String? = null,
    val features: Array<String>? = null,
    val isAuthenticated: Boolean? = null,
    val userData: String? = null,
)

