package com.eirspay.mobile.di

import com.eirspay.data.fake.*
import com.eirspay.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds domain contracts to implementations. Everything here is an in-memory fake today;
 * when the local backend is up, add API-backed implementations in :data and switch the bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton abstract fun auth(impl: FakeAuthRepository): AuthRepository
    @Binds @Singleton abstract fun merchant(impl: FakeMerchantRepository): MerchantRepository
    @Binds @Singleton abstract fun invoices(impl: FakeInvoiceRepository): InvoiceRepository
    @Binds @Singleton abstract fun receipts(impl: FakeReceiptRepository): ReceiptRepository
    @Binds @Singleton abstract fun catalog(impl: FakeCatalogRepository): CatalogRepository
    @Binds @Singleton abstract fun withholding(impl: FakeWithholdingRepository): WithholdingRepository
    @Binds @Singleton abstract fun analytics(impl: FakeAnalyticsRepository): AnalyticsRepository
    @Binds @Singleton abstract fun taxpayers(impl: FakeTaxpayerRepository): TaxpayerRepository
    @Binds @Singleton abstract fun settings(impl: FakeSettingsRepository): SettingsRepository
    @Binds @Singleton abstract fun preferences(impl: InMemoryPreferencesRepository): PreferencesRepository
}
