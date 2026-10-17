package org.maaya.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.maaya.app.data.local.dao.ContactDao
import org.maaya.app.data.local.entity.ContactEntity
import org.maaya.app.domain.model.TrustedContact

class ContactRepository(
    private val contactDao: ContactDao
) {
    val contacts: Flow<List<TrustedContact>> = contactDao.getAllContactsFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getAllContacts(): List<TrustedContact> {
        return contactDao.getAllContacts().map { it.toDomain() }
    }

    suspend fun getPrioritySmsContacts(): List<TrustedContact> {
        return contactDao.getPrioritySmsContacts().map { it.toDomain() }
    }

    suspend fun addContact(contact: TrustedContact): Long {
        return contactDao.insertContact(contact.toEntity())
    }

    suspend fun updateContact(contact: TrustedContact) {
        contactDao.updateContact(contact.toEntity())
    }

    suspend fun deleteContact(contactId: Long) {
        contactDao.deleteContactById(contactId)
    }

    private fun ContactEntity.toDomain() = TrustedContact(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship,
        isEmergencyPriority = isEmergencyPriority,
        isSmsNotificationEnabled = isSmsNotificationEnabled
    )

    private fun TrustedContact.toEntity() = ContactEntity(
        id = id,
        name = name,
        phoneNumber = phoneNumber,
        relationship = relationship,
        isEmergencyPriority = isEmergencyPriority,
        isSmsNotificationEnabled = isSmsNotificationEnabled
    )
}
