package com.app.missednotificationsreminder.payment.di

import android.content.Context
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.di.qualifiers.ForApplication
import com.app.missednotificationsreminder.payment.billing.data.PurchaseRepositoryImpl
import com.app.missednotificationsreminder.payment.billing.domain.repository.PurchaseRepository
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
class PurchaseRepositoryModule {

    @Provides
    @Singleton
    fun providePurchaseRepository(
            @ForApplication context: Context,
            resourceDataSource: ResourceDataSource
    ): PurchaseRepository {
        return PurchaseRepositoryImpl(CoroutineScope(Dispatchers.Main), resourceDataSource, context)
    }
}
