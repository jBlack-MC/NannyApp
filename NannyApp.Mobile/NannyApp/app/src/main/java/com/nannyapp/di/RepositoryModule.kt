package com.nannyapp.di

import com.nannyapp.data.repository.*
import com.nannyapp.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindNannyRepository(impl: NannyRepositoryImpl): NannyRepository
    @Binds @Singleton abstract fun bindSavedNannyRepository(impl: SavedNannyRepositoryImpl): SavedNannyRepository
    @Binds @Singleton abstract fun bindChildRepository(impl: ChildRepositoryImpl): ChildRepository
    @Binds @Singleton abstract fun bindBookingRepository(impl: BookingRepositoryImpl): BookingRepository
    @Binds @Singleton abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository
    @Binds @Singleton abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository
    @Binds @Singleton abstract fun bindMessageRepository(impl: MessageRepositoryImpl): MessageRepository
    @Binds @Singleton abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
    @Binds @Singleton abstract fun bindSupportRepository(impl: SupportRepositoryImpl): SupportRepository
    @Binds @Singleton abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository
    @Binds @Singleton abstract fun bindStaticContentRepository(impl: StaticContentRepositoryImpl): StaticContentRepository
}
