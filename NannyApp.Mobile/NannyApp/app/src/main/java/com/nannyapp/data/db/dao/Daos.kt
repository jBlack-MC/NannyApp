package com.nannyapp.data.db.dao

import androidx.room.*
import com.nannyapp.data.db.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM cached_user WHERE id = :id")
    fun observe(id: Int): Flow<UserEntity?>

    @Upsert
    suspend fun upsert(user: UserEntity)
}

@Dao
interface NannyProfileDao {
    @Query("SELECT * FROM cached_nanny_profile ORDER BY averageRating DESC")
    fun observeAll(): Flow<List<NannyProfileEntity>>

    @Query("SELECT * FROM cached_nanny_profile WHERE userId = :id")
    fun observe(id: Int): Flow<NannyProfileEntity?>

    @Upsert
    suspend fun upsertAll(items: List<NannyProfileEntity>)

    @Upsert
    suspend fun upsert(item: NannyProfileEntity)

    @Query("DELETE FROM cached_nanny_profile")
    suspend fun clear()
}

@Dao
interface ChildDao {
    @Query("SELECT * FROM cached_child WHERE parentId = :parentId")
    fun observeForParent(parentId: Int): Flow<List<ChildEntity>>

    @Query("SELECT * FROM cached_child")
    fun observeAllCached(): Flow<List<ChildEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ChildEntity>)

    @Upsert
    suspend fun upsert(item: ChildEntity)

    @Query("DELETE FROM cached_child WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM cached_child WHERE parentId = :parentId")
    suspend fun clearForParent(parentId: Int)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM cached_booking ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM cached_booking WHERE id = :id")
    fun observe(id: Int): Flow<BookingEntity?>

    @Upsert
    suspend fun upsertAll(items: List<BookingEntity>)

    @Upsert
    suspend fun upsert(item: BookingEntity)

    @Query("DELETE FROM cached_booking")
    suspend fun clear()
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM cached_payment ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Upsert
    suspend fun upsertAll(items: List<PaymentEntity>)
}

@Dao
interface SavedNannyDao {
    @Query("SELECT * FROM cached_saved_nanny")
    fun observeAll(): Flow<List<SavedNannyEntity>>

    @Upsert
    suspend fun upsertAll(items: List<SavedNannyEntity>)

    @Query("DELETE FROM cached_saved_nanny WHERE nannyId = :nannyId")
    suspend fun deleteByNanny(nannyId: Int)

    @Query("DELETE FROM cached_saved_nanny")
    suspend fun clear()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM cached_message WHERE (senderId = :a AND receiverId = :b) OR (senderId = :b AND receiverId = :a) ORDER BY createdAt ASC")
    fun observeThread(a: Int, b: Int): Flow<List<ChatMessageEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ChatMessageEntity>)

    @Upsert
    suspend fun upsert(item: ChatMessageEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM cached_notification ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM cached_notification WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Upsert
    suspend fun upsertAll(items: List<NotificationEntity>)

    @Query("UPDATE cached_notification SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)

    @Query("UPDATE cached_notification SET isRead = 1")
    suspend fun markAllRead()
}

@Dao
interface AvailabilityDao {
    @Query("SELECT * FROM cached_availability WHERE nannyId = :nannyId ORDER BY dayOfWeek")
    fun observe(nannyId: Int): Flow<List<AvailabilityEntity>>

    @Query("DELETE FROM cached_availability WHERE nannyId = :nannyId")
    suspend fun clearForNanny(nannyId: Int)

    @Insert
    suspend fun insertAll(items: List<AvailabilityEntity>)
}
