package com.app.missednotificationsreminder.payment.di

import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.payment.billing.data.PurchaseRepositoryImpl
import com.app.missednotificationsreminder.payment.billing.domain.repository.PurchaseRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PurchaseRepositoryModule {

    @Provides
    @Singleton
    fun providePurchaseRepository(
            resourceDataSource: ResourceDataSource
    ): PurchaseRepository {
        return PurchaseRepositoryImpl(resourceDataSource)
    }
}
