package org.connecttag.features.contactsaccounts.android

public interface ContactsAndAccountsReader {
    public suspend fun hasReadContactsPermission(): Boolean
    public suspend fun hasWriteContactsPermission(): Boolean
    public suspend fun getAllContacts(): Result<List<ContactInfo>>
    public suspend fun getContactsPaginated(offset: Int = 0, limit: Int = 50): Result<List<ContactInfo>>
    public suspend fun searchContacts(query: String): Result<List<ContactInfo>>
    public suspend fun getContactById(contactId: String): ContactInfo?
    public suspend fun getAllAccounts(): Result<List<AccountInfo>>
}

public interface ContactsAndAccountsWriter {
    public suspend fun addContact(contactInfo: ContactInfoToAdd): Result<Boolean>
    public suspend fun updateContact(contactId: String, contactInfo: ContactInfoToAdd): Result<Boolean>
    public suspend fun deleteContact(contactId: String): Result<Boolean>
    public suspend fun deleteContacts(contactIds: List<String>): Result<Int>
}

public interface ContactsAndAccountsRepository : ContactsAndAccountsReader, ContactsAndAccountsWriter

