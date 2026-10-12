package com.eirspay.data.fake

import com.eirspay.data.seed.SampleData
import com.eirspay.domain.model.*
import com.eirspay.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeInvoiceRepository @Inject constructor() : InvoiceRepository {
    private val state = MutableStateFlow(SampleData.invoices())
    private var created = 0

    override fun observeInvoices(): Flow<List<Invoice>> = state.asStateFlow()

    override suspend fun register(draft: InvoiceDraft): Invoice {
        FakeLatency.wait()
        val id = "${draft.documentType.name}-NEW${++created}X${System.currentTimeMillis().toString(36).takeLast(4).uppercase()}"
        val invoice = Invoice(
            id = id, number = (104 + created).toString(), documentType = draft.documentType,
            transactionType = draft.transactionType, status = InvoiceStatus.ISSUED, issuedAt = LocalDateTime.now().withSecond(0).withNano(0),
            branch = draft.branch, sourceSystem = SourceSystem("POS", SampleData.systemFor.getValue(draft.branch)),
            seller = SampleData.seller, buyer = draft.buyer, paymentMode = draft.paymentMode, paymentTerm = draft.paymentTerm,
            items = draft.items, irn = FakeIrn.of(id), amendment = draft.amendment,
        )
        state.update { listOf(invoice) + it }
        return invoice
    }

    override suspend fun cancel(invoiceId: String, reason: CancelReason): Invoice {
        FakeLatency.wait()
        var result: Invoice? = null
        state.update { list ->
            list.map {
                if (it.id != invoiceId || it.isCanceled) it
                else it.copy(
                    status = InvoiceStatus.CANCELED,
                    cancellation = Cancellation(reason, LocalDateTime.now().withSecond(0).withNano(0), it.status),
                ).also { c -> result = c }
            }
        }
        return result ?: state.value.first { it.id == invoiceId }
    }
}

@Singleton
class FakeReceiptRepository @Inject constructor() : ReceiptRepository {
    private val state = MutableStateFlow(SampleData.receipts())
    private var created = 0

    override fun observeReceipts(): Flow<List<Receipt>> = state.asStateFlow()

    override suspend fun register(draft: ReceiptDraft): Receipt {
        FakeLatency.wait()
        val id = "RCP-N${++created}${System.currentTimeMillis().toString(36).takeLast(4).uppercase()}"
        val receipt = Receipt(
            id = id, number = (2291 + created).toString(), type = draft.type, method = draft.method,
            issuedAt = LocalDateTime.now().withSecond(0).withNano(0), branch = draft.branch,
            sourceSystem = SourceSystem("POS", SampleData.systemFor.getValue(draft.branch)), irn = FakeIrn.of(id),
            buyer = draft.buyer, items = draft.items, reference = draft.reference, provider = draft.provider,
            tendered = draft.tendered, linkedInvoiceId = draft.linkedInvoiceId, amount = draft.amount,
        )
        state.update { listOf(receipt) + it }
        return receipt
    }
}

@Singleton
class FakeCatalogRepository @Inject constructor() : CatalogRepository {
    private val state = MutableStateFlow(SampleData.catalog())
    override fun observeItems(): Flow<List<CatalogItem>> = state.asStateFlow()

    override suspend fun save(item: CatalogItem, isNew: Boolean) {
        state.update { list -> if (isNew) listOf(item) + list else list.map { if (it.code == item.code) item else it } }
    }

    override suspend fun setActive(code: String, active: Boolean) {
        state.update { list -> list.map { if (it.code == code) it.copy(active = active) else it } }
    }
}

@Singleton
class FakeWithholdingRepository @Inject constructor() : WithholdingRepository {
    private val state = MutableStateFlow(SampleData.withholding())
    override fun observeRecords(): Flow<List<WithholdingRecord>> = state.asStateFlow()
    override suspend fun markCertificateReceived(id: String) {
        state.update { list -> list.map { if (it.id == id) it.copy(certificateReceived = true) else it } }
    }
}

@Singleton
class FakeTaxpayerRepository @Inject constructor() : TaxpayerRepository {
    override suspend fun lookup(tin: String): Party? {
        return SampleData.taxpayers.find { it.tin == tin } ?: SampleData.dashen.copy(tin = tin)
    }
}

@Singleton
class FakeMerchantRepository @Inject constructor() : MerchantRepository {
    override val branches: List<BranchInfo> = SampleData.branches
    override val setupStepsDone: Flow<Int> = MutableStateFlow(5)
    override fun seller(): Party = SampleData.seller
}

@Singleton
class InMemoryPreferencesRepository @Inject constructor() : PreferencesRepository {
    private val branch = MutableStateFlow<Branch?>(null)
    override val selectedBranch: StateFlow<Branch?> = branch.asStateFlow()
    override fun selectBranch(branch: Branch?) { this.branch.value = branch }

    private val onboarding = MutableStateFlow(false)
    override val onboardingCompleted: StateFlow<Boolean> = onboarding.asStateFlow()
    override fun completeOnboarding() { onboarding.value = true }
}
