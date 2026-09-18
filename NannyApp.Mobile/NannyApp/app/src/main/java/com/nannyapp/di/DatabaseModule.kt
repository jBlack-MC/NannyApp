package com.nannyapp.di

import android.content.Context
import androidx.room.Room
import com.nannyapp.data.db.NannyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NannyDatabase =
        Room.databaseBuilder(context, NannyDatabase::class.java, "nannyapp.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserDao(db: NannyDatabase) = db.userDao()
    @Provides fun provideNannyProfileDao(db: NannyDatabase) = db.nannyProfileDao()
    @Provides fun provideChildDao(db: NannyDatabase) = db.childDao()
    @Provides fun provideBookingDao(db: NannyDatabase) = db.bookingDao()
    @Provides fun providePaymentDao(db: NannyDatabase) = db.paymentDao()
    @Provides fun provideSavedNannyDao(db: NannyDatabase) = db.savedNannyDao()
    @Provides fun provideChatMessageDao(db: NannyDatabase) = db.chatMessageDao()
    @Provides fun provideNotificationDao(db: NannyDatabase) = db.notificationDao()
    @Provides fun provideAvailabilityDao(db: NannyDatabase) = db.availabilityDao()
    @Provides fun provideReviewDao(db: NannyDatabase) = db.reviewDao()
}
