package org.connecttag.features.contactsaccounts.android

public data class ContactsSyncResult(
    public val added: Int = 0,
    public val updated: Int = 0,
    public val skipped: Int = 0,
    public val failed: Int = 0,
)

public open class ContactsBackupSyncManager(
    private val repository: ContactsAndAccountsRepository,
) {

    public suspend fun replaceAllContacts(contacts: List<ContactInfo>): Result<ContactsSyncResult> {
        val localContacts = repository.getAllContacts().getOrElse {
            return Result.failure(it)
        }

        if (localContacts.isNotEmpty()) {
            val localIds = localContacts.mapNotNull { it.id }.filter { it.isNotBlank() }
            if (localIds.isNotEmpty()) {
                repository.deleteContacts(localIds).getOrElse {
                    return Result.failure(it)
                }
            }
        }

        var added = 0
        var skipped = 0
        var failed = 0

        contacts.forEach { remote ->
            val remoteSnapshot = remote.toSnapshot()
            if (!remoteSnapshot.isUseful()) {
                skipped++
                return@forEach
            }

            val result = repository.addContact(remote.toContactInfoToAdd())
            if (result.isSuccess) {
                added++
            } else {
                failed++
            }
        }

        return Result.success(
            ContactsSyncResult(
                added = added,
                updated = 0,
                skipped = skipped,
                failed = failed,
            ),
        )
    }

    public suspend fun syncContacts(contacts: List<ContactInfo>): Result<ContactsSyncResult> {
        val localContacts = repository.getAllContacts().getOrElse {
            return Result.failure(it)
        }

        val localSnapshots = localContacts.map { it.toSnapshot() }

        var added = 0
        var updated = 0
        var skipped = 0
        var failed = 0

        contacts.forEach { remote ->
            val remoteSnapshot = remote.toSnapshot()
            if (!remoteSnapshot.isUseful()) {
                skipped++
                return@forEach
            }

            val existing = localSnapshots.firstOrNull { local ->
                local.matches(remoteSnapshot)
            }?.source
            val existingId = existing?.id
            val result = if (existingId.isNullOrBlank()) {
                repository.addContact(remote.toContactInfoToAdd())
            } else {
                repository.updateContact(existingId, remote.toContactInfoToAdd())
            }

            if (result.isSuccess) {
                if (existing == null) {
                    added++
                } else {
                    updated++
                }
            } else {
                failed++
            }
        }

        return Result.success(
            ContactsSyncResult(
                added = added,
                updated = updated,
                skipped = skipped,
                failed = failed,
            ),
        )
    }

    private data class ContactSnapshot(
        val source: ContactInfo,
        val normalizedName: String,
        val normalizedPhones: Set<String>,
        val normalizedEmails: Set<String>,
    ) {
        fun isUseful(): Boolean {
            return normalizedName.isNotBlank() ||
                normalizedPhones.isNotEmpty() ||
                normalizedEmails.isNotEmpty()
        }

        fun matches(other: ContactSnapshot): Boolean {
            val phonesOverlap = normalizedPhones.isNotEmpty() &&
                other.normalizedPhones.isNotEmpty() &&
                normalizedPhones.intersect(other.normalizedPhones).isNotEmpty()

            val emailsOverlap = normalizedEmails.isNotEmpty() &&
                other.normalizedEmails.isNotEmpty() &&
                normalizedEmails.intersect(other.normalizedEmails).isNotEmpty()

            val sameName = normalizedName.isNotBlank() &&
                other.normalizedName.isNotBlank() &&
                normalizedName == other.normalizedName

            return phonesOverlap ||
                emailsOverlap ||
                (sameName && (normalizedPhones.isEmpty() || other.normalizedPhones.isEmpty()))
        }
    }

    private fun ContactInfo.toContactInfoToAdd(): ContactInfoToAdd {
        return ContactInfoToAdd(
            displayName = displayName.orEmpty(),
            phoneNumbers = phoneNumbers.orEmpty().mapNotNull { phone ->
                phone.number?.takeIf { it.isNotBlank() }?.let {
                    PhoneNumberToAdd(
                        number = it,
                        type = phone.type ?: "Mobile",
                    )
                }
            },
            emails = emails.orEmpty().mapNotNull { email ->
                email.address?.takeIf { it.isNotBlank() }?.let {
                    EmailToAdd(
                        address = it,
                        type = email.type ?: "Home",
                    )
                }
            },
            address = addresses.orEmpty().firstOrNull()?.let { address ->
                AddressToAdd(
                    street = address.street.orEmpty(),
                    city = address.city.orEmpty(),
                    region = address.region.orEmpty(),
                    postalCode = address.postalCode.orEmpty(),
                    country = address.country.orEmpty(),
                )
            },
            organization = organizations.orEmpty().firstOrNull()?.let { organization ->
                OrganizationToAdd(
                    company = organization.company.orEmpty(),
                    title = organization.title.orEmpty(),
                )
            },
            notes = notes,
            photo = photo,
            websites = websites.orEmpty(),
        )
    }

    private fun ContactInfo.toSnapshot(): ContactSnapshot {
        return ContactSnapshot(
            source = this,
            normalizedName = displayName.normalizeName(),
            normalizedPhones = phoneNumbers.orEmpty()
                .mapNotNull { it.number.normalizePhone() }
                .toSet(),
            normalizedEmails = emails.orEmpty()
                .mapNotNull { it.address.normalizeEmail() }
                .toSet(),
        )
    }

    private fun String?.normalizeName(): String {
        return this
            ?.trim()
            ?.lowercase()
            ?.replace(Regex("\\s+"), " ")
            .orEmpty()
    }

    private fun String?.normalizePhone(): String? {
        val value = this
            ?.filter { it.isDigit() || it == '+' }
            .orEmpty()
        return value.takeIf { it.isNotBlank() }
    }

    private fun String?.normalizeEmail(): String? {
        val value = this
            ?.trim()
            ?.lowercase()
            .orEmpty()
        return value.takeIf { it.isNotBlank() }
    }
}
