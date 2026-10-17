package org.maaya.app.data.local

import android.content.Context
import androidx.room.*
import org.maaya.app.data.local.dao.ContactDao
import org.maaya.app.data.local.dao.HealthMetricDao
import org.maaya.app.data.local.dao.IncidentDao
import org.maaya.app.data.local.dao.SafeRideDao
import org.maaya.app.data.local.entity.ContactEntity
import org.maaya.app.data.local.entity.HealthMetricEntity
import org.maaya.app.data.local.entity.IncidentEntity
import org.maaya.app.data.local.entity.SafeRideEntity
import org.maaya.app.domain.model.SosStatus
import org.maaya.app.domain.model.SosTriggerType

class Converters {
    @TypeConverter
    fun fromSosStatus(value: SosStatus): String = value.name

    @TypeConverter
    fun toSosStatus(value: String): SosStatus = try {
        SosStatus.valueOf(value)
    } catch (e: Exception) {
        SosStatus.IDLE
    }

    @TypeConverter
    fun fromSosTriggerType(value: SosTriggerType): String = value.name

    @TypeConverter
    fun toSosTriggerType(value: String): SosTriggerType = try {
        SosTriggerType.valueOf(value)
    } catch (e: Exception) {
        SosTriggerType.MANUAL_IN_APP
    }
}

@Database(
    entities = [
        ContactEntity::class,
        IncidentEntity::class,
        HealthMetricEntity::class,
        SafeRideEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MaayaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun incidentDao(): IncidentDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun safeRideDao(): SafeRideDao

    companion object {
        @Volatile
        private var INSTANCE: MaayaDatabase? = null

        fun getInstance(context: Context): MaayaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaayaDatabase::class.java,
                    "maaya_local_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
