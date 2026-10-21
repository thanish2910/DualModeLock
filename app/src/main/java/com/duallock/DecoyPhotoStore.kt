package com.duallock

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/** Stores user-selected avatar URIs in app-private preferences. It never reads WhatsApp data. */
class DecoyPhotoStore(context: Context) {
    private val prefs = context.getSharedPreferences("decoy_photos", Context.MODE_PRIVATE)

    fun get(key: String): Uri? = prefs.getString(keyFor(key), null)?.let(Uri::parse)
    fun set(key: String, uri: Uri) { prefs.edit().putString(keyFor(key), uri.toString()).apply() }
    fun remove(key: String) { prefs.edit().remove(keyFor(key)).apply() }
    private fun keyFor(key: String) = "avatar:" + key

    /** Returns the selected Android contact's saved photo URI, if one exists. */
    fun contactPhotoUri(context: Context, selectedContactUri: Uri): Uri? {
        val resolver = context.contentResolver
        val contactLookup = ContactsContract.Contacts.lookupContact(resolver, selectedContactUri) ?: return null
        // ContactsContract.Contacts.getPhotoUri is not available in all Android SDKs.
        // The standard contact-photo content URI works with the resolved contact URI.
        return Uri.withAppendedPath(contactLookup, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
    }
}
