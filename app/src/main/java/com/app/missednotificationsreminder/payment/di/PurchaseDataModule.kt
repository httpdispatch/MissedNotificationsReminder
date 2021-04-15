package com.app.missednotificationsreminder.payment.di

import com.app.missednotificationsreminder.payment.model.Purchase
import com.app.missednotificationsreminder.util.moshi.MoshiPreferenceWrapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tfcporciuncula.flow.FlowSharedPreferences
import com.tfcporciuncula.flow.Preference
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [PurchaseDataModuleExt::class, PurchaseRepositoryModule::class])
class PurchaseDataModule {

    @Provides
    @Singleton
    fun providePurchases(prefs: FlowSharedPreferences, moshi: Moshi): Preference<List<Purchase>> {
        return MoshiPreferenceWrapper(
                prefs,
                "PURCHASES",
                emptyList(),
                moshi.adapter(Types.newParameterizedType(List::class.java, Purchase::class.java))
        )
    }
}
