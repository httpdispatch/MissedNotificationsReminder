package com.app.missednotificationsreminder.payment.di

import android.content.Context
import com.app.missednotificationsreminder.data.source.ResourceDataSource
import com.app.missednotificationsreminder.di.qualifiers.ForApplication
import com.app.missednotificationsreminder.payment.billing.data.source.PurchaseRepository
import com.app.missednotificationsreminder.payment.data.model.Purchase
import com.app.missednotificationsreminder.util.moshi.MoshiPreferenceWrapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tfcporciuncula.flow.FlowSharedPreferences
import com.tfcporciuncula.flow.Preference
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(includes = [PurchaseDataModuleExt::class])
class PurchaseDataModule {

    @Provides
    @Singleton
    fun providePurchaseRepository(@ForApplication context: Context, resourceDataSource: ResourceDataSource) = PurchaseRepository(CoroutineScope(Dispatchers.Main), resourceDataSource, context)

    @Provides
    @Singleton
    fun providePurchases(prefs: FlowSharedPreferences, moshi: Moshi): Preference<List<Purchase>> {
        return MoshiPreferenceWrapper(prefs, "PURCHASES",
                emptyList(),
                moshi.adapter(Types.newParameterizedType(List::class.java, Purchase::class.java)))
    }
}