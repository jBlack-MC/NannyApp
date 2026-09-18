package com.nannyapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nannyapp.data.db.dao.*
import com.nannyapp.data.db.entity.*

@Database(
    entities = [
        UserEntity::class,
        NannyProfileEntity::class,
        ChildEntity::class,
        BookingEntity::class,
        PaymentEntity::class,
        ReviewEntity::class,
        SavedNannyEntity::class,
        ChatMessageEntity::class,
        NotificationEntity::class,
        AvailabilityEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NannyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun nannyProfileDao(): NannyProfileDao
    abstract fun childDao(): ChildDao
    abstract fun bookingDao(): BookingDao
    abstract fun paymentDao(): PaymentDao
    abstract fun savedNannyDao(): SavedNannyDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun notificationDao(): NotificationDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun reviewDao(): ReviewDao
}
