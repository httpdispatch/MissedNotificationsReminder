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
            "item_1",
            "item_2",
            "item_5",
            "item_10",
            "item_20",
            "item_50",
            "item_100")
}