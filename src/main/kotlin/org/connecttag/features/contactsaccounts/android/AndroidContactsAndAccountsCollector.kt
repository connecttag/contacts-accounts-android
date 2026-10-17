@file:Suppress(
    "unused",
    "RedundantSuspendModifier",
    "RemoveRedundantQualifierName",
    "KotlinConstantConditions",
    "UNUSED_PARAMETER",
)

package org.connecttag.features.contactsaccounts.android

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.RawContacts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public open class AndroidContactsAccountsCollector(
    private val context: Context,
) : ContactsAndAccountsRepository {

    override suspend fun hasReadContactsPermission(): Boolean = withContext(Dispatchers.Main) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun hasWriteContactsPermission(): Boolean = withContext(Dispatchers.Main) {
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getAllContacts(): Result<List<ContactInfo>> = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission()) {
            return@withContext Result.failure(SecurityException("READ_CONTACTS permission not granted"))
        }
        val contacts = mutableListOf<ContactInfo>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    processContactCursor(it)?.let { contact -> contacts.add(contact) }
                }
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getContactsPaginated(offset: Int, limit: Int ): Result<List<ContactInfo>> = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission()) {
            return@withContext Result.failure(SecurityException("READ_CONTACTS permission not granted"))
        }
        val contacts = mutableListOf<ContactInfo>()
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT $limit OFFSET $offset"
            )
            cursor?.use {
                while (it.moveToNext()) {
                    processContactCursor(it)?.let { contact -> contacts.add(contact) }
                }
            }
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchContacts(query: String): Result<List<ContactInfo>> = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission() || query.isBlank()) {
            return@withContext Result.success(emptyList())
        }
        val contacts = mutableListOf<ContactInfo>()
        try {
            val selection = "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ? OR ${CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%")
            val cursor = context.contentResolver.query(
                CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(CommonDataKinds.Phone.CONTACT_ID),
                selection, selectionArgs, null
            )
            cursor?.use {
                val idIndex = it.getColumnIndex(CommonDataKinds.Phone.CONTACT_ID)
                if (idIndex != -1) {
                    val ids = mutableSetOf<String>()
                    while (it.moveToNext()) {
                        val id = it.getString(idIndex) ?: continue
                        if (ids.add(id)) {
                            getContactById(id)?.let { contact -> contacts.add(contact) }
                        }
                    }
                }
            }
            Result.success(contacts)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getContactById(contactId: String): ContactInfo? = withContext(Dispatchers.IO) {
        if (!hasReadContactsPermission()) return@withContext null
        try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null, "${ContactsContract.Contacts._ID} = ?", arrayOf(contactId), null
            )
            cursor?.use { if (it.moveToFirst()) return@withContext processContactCursor(it) }
            null
        } catch (e: Exception) { null }
    }

    @SuppressLint("Range")
    private suspend fun processContactCursor(cursor: android.database.Cursor): ContactInfo? {
        return try {
            val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)) ?: return null
            val displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
            val hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
            val lastModified = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.CONTACT_LAST_UPDATED_TIMESTAMP)) ?: ""
            } else ""

            ContactInfo(
                id = contactId,
                displayName = displayName,
                lastModified = lastModified,
                phoneNumbers = if (hasPhoneNumber) getPhoneNumbers(contactId) else emptyList(),
                emails = getEmails(contactId),
                addresses = getAddresses(contactId),
                organizations = getOrganizations(contactId),
                photo = getContactPhoto(contactId),
                notes = getContactNotes(contactId)
            )
        } catch (e: Exception) { null }
    }

    @SuppressLint("Range")
    private suspend fun getPhoneNumbers(contactId: String): List<PhoneNumber> {
        val phoneNumbers = mutableListOf<PhoneNumber>()
        try {
            val phoneCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId), null
            )
            phoneCursor?.use { pc ->
                while (pc.moveToNext()) {
                    phoneNumbers.add(
                        PhoneNumber(
                            number = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: "",
                            type = getPhoneType(pc.getInt(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))),
                            dataVersion = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA_VERSION)) ?: "",
                            label = pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)) ?: "",
                            isPrimary = pc.getInt(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)),
                            isSuperPrimary = pc.getInt(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)),
                            id = pc.getInt(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID))
                        )
                    )
                }
            }
        } catch (e: Exception) {}
        return phoneNumbers
    }

    @SuppressLint("Range")
    private suspend fun getEmails(contactId: String): List<Email> {
        val emails = mutableListOf<Email>()
        try {
            val emailCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contactId), null
            )
            emailCursor?.use { ec ->
                while (ec.moveToNext()) {
                    emails.add(
                        Email(
                            address = ec.getString(ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)) ?: "",
                            type = getEmailType(ec.getInt(ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)))
                        )
                    )
                }
            }
        } catch (e: Exception) {}
        return emails
    }

    @SuppressLint("Range")
    private suspend fun getAddresses(contactId: String): List<Address> {
        val addresses = mutableListOf<Address>()
        try {
            val addressCursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + " = ?",
                arrayOf(contactId), null
            )
            addressCursor?.use { ac ->
                while (ac.moveToNext()) {
                    addresses.add(
                        Address(
                            street = ac.getString(ac.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)) ?: "",
                            city = ac.getString(ac.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)) ?: "",
                            region = ac.getString(ac.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)) ?: "",
                            postalCode = ac.getString(ac.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)) ?: "",
                            country = ac.getString(ac.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {}
        return addresses
    }

    @SuppressLint("Range")
    private suspend fun getOrganizations(contactId: String): List<Organization> {
        val organizations = mutableListOf<Organization>()
        try {
            val orgCursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                arrayOf(contactId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
                null
            )
            orgCursor?.use { oc ->
                while (oc.moveToNext()) {
                    organizations.add(
                        Organization(
                            company = oc.getString(oc.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)) ?: "",
                            title = oc.getString(oc.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)) ?: ""
                        )
                    )
                }
            }
        } catch (e: Exception) {}
        return organizations
    }

    private suspend fun getContactPhoto(contactId: String): ByteArray? {
        return try {
            val photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
            val photoInputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.contentResolver, photoUri)
            photoInputStream?.readBytes()
        } catch (e: Exception) { null }
    }

    @SuppressLint("Range")
    private suspend fun getContactNotes(contactId: String): String? {
        return try {
            val noteCursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                arrayOf(contactId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
                null
            )
            noteCursor?.use { nc ->
                if (nc.moveToFirst()) nc.getString(nc.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)) else null
            }
        } catch (e: Exception) { null }
    }

    override suspend fun addContact(contactInfo: ContactInfoToAdd): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!hasWriteContactsPermission()) return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        try {
            val operations = ArrayList<ContentProviderOperation>()
            val rawContactId = operations.size
            operations.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI).withValue(RawContacts.ACCOUNT_TYPE, null).withValue(RawContacts.ACCOUNT_NAME, null).build())

            if (contactInfo.displayName.isNotBlank()) {
                operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, contactInfo.displayName).build())
            }

            contactInfo.phoneNumbers.forEach { phone ->
                operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                    .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(CommonDataKinds.Phone.NUMBER, phone.number)
                    .withValue(CommonDataKinds.Phone.TYPE, getPhoneTypeInt(phone.type)).build())
            }

            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateContact(contactId: String, contactInfo: ContactInfoToAdd): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!hasWriteContactsPermission()) return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        try {
            val rawContactId = getRawContactId(contactId) ?: return@withContext Result.failure(Exception("Raw contact not found"))
            val operations = ArrayList<ContentProviderOperation>()
            if (contactInfo.displayName.isNotBlank()) {
                operations.add(ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                    .withSelection("${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?", arrayOf(rawContactId.toString(), CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE))
                    .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, contactInfo.displayName).build())
            }
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            Result.success(true)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteContact(contactId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!hasWriteContactsPermission()) return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        try {
            val deleted = context.contentResolver.delete(RawContacts.CONTENT_URI, "${RawContacts.CONTACT_ID} = ?", arrayOf(contactId))
            Result.success(deleted > 0)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteContacts(contactIds: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        if (!hasWriteContactsPermission()) return@withContext Result.failure(SecurityException("WRITE_CONTACTS permission not granted"))
        try {
            val placeholders = contactIds.joinToString(",") { "?" }
            val deleted = context.contentResolver.delete(RawContacts.CONTENT_URI, "${RawContacts.CONTACT_ID} IN ($placeholders)", contactIds.toTypedArray())
            Result.success(deleted)
        } catch (e: Exception) { Result.failure(e) }
    }

    @SuppressLint("Range")
    private suspend fun getRawContactId(contactId: String): Int? = withContext(Dispatchers.IO) {
        try {
            val cursor = context.contentResolver.query(RawContacts.CONTENT_URI, arrayOf(RawContacts._ID), "${RawContacts.CONTACT_ID} = ?", arrayOf(contactId), null)
            cursor?.use { if (it.moveToFirst()) return@use it.getInt(it.getColumnIndex(RawContacts._ID)) }
            null
        } catch (e: Exception) { null }
    }

    override suspend fun getAllAccounts(): Result<List<AccountInfo>> = withContext(Dispatchers.IO) {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.accounts.map { account ->
                AccountInfo(
                    name = account.name,
                    type = account.type,
                    isAuthenticated = accountManager.peekAuthToken(account, account.type) != null,
                    userData = accountManager.getUserData(account, account.type)
                )
            }
            Result.success(accounts)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun getPhoneType(type: Int): String = when (type) {
        CommonDataKinds.Phone.TYPE_HOME -> "Home"
        CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
        CommonDataKinds.Phone.TYPE_WORK -> "Work"
        else -> "Other"
    }

    private fun getPhoneTypeInt(type: String): Int = when (type.lowercase()) {
        "home" -> CommonDataKinds.Phone.TYPE_HOME
        "mobile" -> CommonDataKinds.Phone.TYPE_MOBILE
        "work" -> CommonDataKinds.Phone.TYPE_WORK
        else -> CommonDataKinds.Phone.TYPE_OTHER
    }

    private fun getEmailType(type: Int): String = when (type) {
        CommonDataKinds.Email.TYPE_HOME -> "Home"
        CommonDataKinds.Email.TYPE_WORK -> "Work"
        else -> "Other"
    }
}

