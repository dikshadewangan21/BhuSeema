package com.seemakit.field
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class Parcel(@PrimaryKey(autoGenerate = true) val id: Long = 0, val surveyNo: String, val village: String, val rorAreaSqm: Double, val synced: Boolean = false)
@Entity data class Corner(@PrimaryKey(autoGenerate = true) val id: Long = 0, val parcelId: Long, val seq: Int, val lat: Double, val lon: Double,
    val quality: Int, val hdop: Double, val sats: Int, val time: Long, val isGcp: Boolean, val ownerWitness: String, val neighbourWitness: String)

@Dao interface SurveyDao {
    @Query("SELECT * FROM Parcel ORDER BY id DESC") fun parcels(): Flow<List<Parcel>>
    @Query("SELECT * FROM Parcel WHERE id=:id") fun parcel(id: Long): Flow<Parcel?>
    @Insert suspend fun addParcel(p: Parcel): Long
    @Query("DELETE FROM Parcel WHERE id=:id") suspend fun delParcel(id: Long)
    @Query("DELETE FROM Corner WHERE parcelId=:id") suspend fun delCorners(id: Long)
    @Query("SELECT * FROM Corner WHERE parcelId=:id ORDER BY seq") fun corners(id: Long): Flow<List<Corner>>
    @Query("SELECT * FROM Corner WHERE parcelId=:id ORDER BY seq") suspend fun cornersNow(id: Long): List<Corner>
    @Insert suspend fun addCorner(c: Corner)
    @Delete suspend fun delCorner(c: Corner)
    @Query("SELECT * FROM Parcel WHERE synced=0") suspend fun unsynced(): List<Parcel>
    @Query("UPDATE Parcel SET synced=:s WHERE id=:id") suspend fun mark(id: Long, s: Boolean)
}
@Database(entities = [Parcel::class, Corner::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() { abstract fun dao(): SurveyDao }
