package org.maaya.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.maaya.app.data.local.entity.ContactEntity
import org.maaya.app.data.local.entity.HealthMetricEntity
import org.maaya.app.data.local.entity.IncidentEntity
import org.maaya.app.data.local.entity.SafeRideEntity
import org.maaya.app.domain.model.SosStatus

@Dao
interface ContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY isEmergencyPriority DESC, name ASC")
    fun getAllContactsFlow(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM trusted_contacts ORDER BY isEmergencyPriority DESC, name ASC")
    suspend fun getAllContacts(): List<ContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE isEmergencyPriority = 1 AND isSmsNotificationEnabled = 1")
    suspend fun getPrioritySmsContacts(): List<ContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)
}

@Dao
interface IncidentDao {
    @Query("SELECT * FROM emergency_incidents ORDER BY startedAt DESC")
    fun getAllIncidentsFlow(): Flow<List<IncidentEntity>>

    @Query("SELECT * FROM emergency_incidents WHERE id = :id LIMIT 1")
    suspend fun getIncidentById(id: String): IncidentEntity?

    @Query("SELECT * FROM emergency_incidents WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestIncidentByStatus(status: SosStatus): IncidentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentEntity)

    @Update
    suspend fun updateIncident(incident: IncidentEntity)

    @Query("SELECT * FROM emergency_incidents WHERE isSyncedWithServer = 0")
    suspend fun getUnsyncedIncidents(): List<IncidentEntity>
}

@Dao
interface HealthMetricDao {
    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC LIMIT 100")
    fun getRecentMetricsFlow(): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetric(): HealthMetricEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: HealthMetricEntity): Long

    @Query("DELETE FROM health_metrics WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOldMetrics(cutoffTimestamp: Long)
}

@Dao
interface SafeRideDao {
    @Query("SELECT * FROM safe_ride_sessions WHERE isCompleted = 0 LIMIT 1")
    fun getActiveRideFlow(): Flow<SafeRideEntity?>

    @Query("SELECT * FROM safe_ride_sessions WHERE isCompleted = 0 LIMIT 1")
    suspend fun getActiveRide(): SafeRideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRide(ride: SafeRideEntity)

    @Query("UPDATE safe_ride_sessions SET isCompleted = 1 WHERE id = :rideId")
    suspend fun markRideCompleted(rideId: String)
}
