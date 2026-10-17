# ConnectTag Contacts & Accounts Android

[![JitPack](https://jitpack.io/v/connecttag/contacts-accounts-android.svg)](https://jitpack.io/#connecttag/contacts-accounts-android)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A robust and modern Android library for querying, managing, and synchronizing device contacts and system accounts (`AccountManager`) with intelligent backup, batch operations, and deduplication support.

---

## 🚀 Features

- 👥 **Comprehensive Contact Inspection**: Read display names, phones, emails, physical addresses, organizations, photos, websites, and notes.
- 🔐 **System Accounts Discovery**: Query registered device accounts (`AccountManager`) and authentication status.
- ⚡ **Coroutines & Pagination**: Non-blocking queries on `Dispatchers.IO` with built-in pagination (`getContactsPaginated`) and search.
- 🔄 **Smart Sync & Backup**: `ContactsBackupSyncManager` provides contact deduplication, fuzzy matching, and delta synchronization.
- ✏️ **Full CRUD Operations**: Add, update, batch delete, and query raw contact entries.

---

## 📦 Installation

### 1. Add JitPack repository

In `settings.gradle.kts`:
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

### 2. Add Dependency

In your module's `build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.connecttag:contacts-accounts-android:1.0.0")
}
```

---

## 🛠️ Usage

### Reading Contacts & Accounts

```kotlin
val repository: ContactsAndAccountsRepository = AndroidContactsAccountsCollector(context)

lifecycleScope.launch {
    // Check permission
    if (repository.hasReadContactsPermission()) {
        val contactsResult = repository.getAllContacts()
        contactsResult.onSuccess { contacts ->
            contacts.forEach { contact ->
                println("${contact.displayName}: ${contact.phoneNumbers?.firstOrNull()?.number}")
            }
        }

        // Query registered system accounts
        val accountsResult = repository.getAllAccounts()
        accountsResult.onSuccess { accounts ->
            accounts.forEach { account ->
                println("Account: ${account.name} (${account.type})")
            }
        }
    }
}
```

### Smart Sync & Backup

```kotlin
val syncManager = ContactsBackupSyncManager(repository)

lifecycleScope.launch {
    val syncResult = syncManager.syncContacts(remoteContacts)
    syncResult.onSuccess { result ->
        println("Added: ${result.added}, Updated: ${result.updated}, Skipped: ${result.skipped}")
    }
}
```

---

## 📄 License

```text
Copyright 2026 Connect Tag

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
```
