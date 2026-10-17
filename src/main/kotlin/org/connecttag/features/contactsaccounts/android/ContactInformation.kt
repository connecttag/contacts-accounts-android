package org.connecttag.features.contactsaccounts.android

import kotlinx.serialization.Serializable
import androidx.annotation.Keep

@Keep
@Serializable
public data class ContactInfo(
    val id: String? = null,
    val displayName: String? = null,
    val lastModified: String? = null,
    val contactType: String? = null,
    val contactDisplayNameSource: String? = null,
    val contactRawContactId: String? = null,
    val phoneNumbers: List<PhoneNumber>? = null,
    val emails: List<Email>? = null,
    val addresses: List<Address>? = null,
    val organizations: List<Organization>? = null,
    val websites: List<String>? = null,
    val photo: ByteArray? = null,
    val notes: String? = null,
)

@Keep
@Serializable
public data class PhoneNumber(
    val number: String? = null,
    val type: String? = null,
    val dataVersion: String? = null,
    val label: String? = null,
    val isPrimary: Int? = null,
    val isSuperPrimary: Int? = null,
    val lastUpdated: Int? = null,
    val id: Int? = null,
)

@Keep
@Serializable
public data class Email(
    val address: String? = null,
    val type: String? = null,
)

@Keep
@Serializable
public data class Address(
    val street: String? = null,
    val city: String? = null,
    val region: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
)

@Keep
@Serializable
public data class Organization(
    val company: String? = null,
    val title: String? = null,
)

@Keep
@Serializable
public data class FullContactData(
    val addresses: List<AddressData> = emptyList(),
    val contactDisplayNameSource: String? = null,
    val contactRawContactId: String? = null,
    val contactType: String? = null,
    val displayName: String? = null,
    val emails: List<EmailData> = emptyList(),
    val id: String,
    val lastModified: String? = null,
    val organizations: List<OrganizationData> = emptyList(),
    val phoneNumbers: List<FullPhoneNumberData> = emptyList(),
    val websites: List<WebsiteData> = emptyList()
)

@Keep
@Serializable
public data class AddressData(
    val city: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val region: String? = null,
    val street: String? = null
)

@Keep
@Serializable
public data class EmailData(
    val address: String? = null,
    val type: String? = null
)

@Keep
@Serializable
public data class OrganizationData(
    val company: String? = null,
    val department: String? = null,
    val title: String? = null
)

@Keep
@Serializable
public data class FullPhoneNumberData(
    val dataVersion: String? = null,
    val id: Int? = null,
    val isPrimary: Int? = null,
    val isSuperPrimary: Int? = null,
    val label: String? = null,
    val lastUpdated: Long? = null,
    val number: String? = null,
    val type: String? = null
)

@Keep
@Serializable
public data class WebsiteData(
    val url: String? = null,
    val type: String? = null
)

@Keep
@Serializable
public data class ContactUpdateInfo(
    val id: String,
    val displayName: String?,
    val phoneNumbers: List<PhoneNumberToAdd>,
    val addresses: List<AddressData> = emptyList(),
    val emails: List<EmailData> = emptyList(),
    val organizations: List<OrganizationData> = emptyList(),
    val websites: List<WebsiteData> = emptyList()
)

@Keep
@Serializable
public data class ContactInfoToAdd(
    val displayName: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val phoneNumbers: List<PhoneNumberToAdd> = emptyList(),
    val emails: List<EmailToAdd> = emptyList(),
    val address: AddressToAdd? = null,
    val organization: OrganizationToAdd? = null,
    val notes: String? = null,
    val photo: ByteArray? = null,
    val websites: List<String> = emptyList()
)

@Keep
@Serializable
public data class PhoneNumberToAdd(
    val number: String,
    val type: String = "Mobile"
)

@Keep
@Serializable
public data class EmailToAdd(
    val address: String,
    val type: String = "Home"
)

@Keep
@Serializable
public data class AddressToAdd(
    val street: String = "",
    val city: String = "",
    val region: String = "",
    val postalCode: String = "",
    val country: String = ""
)

@Keep
@Serializable
public data class OrganizationToAdd(
    val company: String = "",
    val title: String = ""
)

