package com.app.missednotificationsreminder.payment.di

import com.app.missednotificationsreminder.payment.di.qualifiers.AvailableSkus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class PurchaseDataModuleExt {

    @Provides
    @Singleton
    @AvailableSkus
    fun provideAvailableSkus() = listOf(
            "android.test.purchased",
            "android.test.canceled",
            "android.test.refunded",
            "android.test.item_unavailable",
            "not_existing")
}