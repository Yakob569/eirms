package com.eirspay.mobile.feature.items

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.*
import com.eirspay.domain.usecase.CatalogValidation
import com.eirspay.domain.usecase.ObserveCatalogUseCase
import com.eirspay.domain.usecase.SaveCatalogItemUseCase
import com.eirspay.domain.usecase.ValidateCatalogItemUseCase
import com.eirspay.mobile.feature.receipts.FieldLabel
import com.eirspay.mobile.feature.receipts.FormCard
import com.eirspay.mobile.feature.receipts.Wrap
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

data class ItemForm(
    val isNew: Boolean = true,
    val code: String = "",
    val description: String = "",
    val nature: NatureOfSupply = NatureOfSupply.GOODS,
    val hs: String = "",
    val tax: TaxCode = TaxCode.VAT15,
    val unit: String = "PCS",
    val price: String = "",
    val active: Boolean = true,
    val category: String = "",
)

data class ItemFormUiState(val form: ItemForm = ItemForm(), val readyText: String = "", val ready: Boolean = false, val gross: String = "")

@HiltViewModel
class ItemFormViewModel @Inject constructor(
    observeCatalog: ObserveCatalogUseCase,
    private val validate: ValidateCatalogItemUseCase,
    private val save: SaveCatalogItemUseCase,
) : ViewModel() {
    private val form = MutableStateFlow(ItemForm())
    private var bound = false
    private var existing: List<CatalogItem> = emptyList()
    private val saved = Channel<Unit>(Channel.BUFFERED)
    val savedEvents: Flow<Unit> = saved.receiveAsFlow()

    private val catalog = observeCatalog()

    val state: StateFlow<ItemFormUiState> = combine(form, catalog) { f, cat ->
        existing = cat
        val v = validate(f.toItem(), f.isNew, cat)
        ItemFormUiState(
            form = f, ready = v is CatalogValidation.Ok,
            readyText = when (v) {
                CatalogValidation.DuplicateCode -> "That code is already in the catalog."
                is CatalogValidation.Missing -> "Missing: " + v.fields.joinToString(", ")
                CatalogValidation.Ok -> "Ready to save."
            },
            gross = Fmt.etb(f.toItem().grossPrice) + " per " + f.unit,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ItemFormUiState())

    fun bind(code: String?) {
        if (bound) return
        bound = true
        if (code == null) { form.value = ItemForm(); return }
        viewModelScope.launch {
            val item = catalog.first().find { it.code == code } ?: return@launch
            form.value = ItemForm(
                false, item.code, item.description, item.nature, item.hsCode, item.taxCode, item.unit,
                item.unitPrice.toPlainString(), item.active, item.category,
            )
        }
    }

    fun update(block: (ItemForm) -> ItemForm) = form.update(block)

    fun submit() {
        val s = state.value
        if (!s.ready) return
        val f = s.form
        viewModelScope.launch {
            save(f.toItem().copy(code = f.code.trim(), description = f.description.trim()), f.isNew)
            saved.send(Unit)
        }
    }

    private fun ItemForm.toItem() = CatalogItem(
        code = code, description = description, nature = nature, hsCode = hs, taxCode = tax, unit = unit,
        unitPrice = price.toBigDecimalOrNull()?.setScale(2, java.math.RoundingMode.HALF_UP) ?: BigDecimal.ZERO,
        active = active, category = category.ifEmpty { if (nature == NatureOfSupply.SERVICE) "Services" else "General" },
    )
}

@Composable
fun ItemFormScreen(code: String?, onBack: () -> Unit) {
    val vm: ItemFormViewModel = hiltViewModel()
    LaunchedEffect(code) { vm.bind(code) }
    val latestBack by rememberUpdatedState(onBack)
    LaunchedEffect(vm) { vm.savedEvents.collect { latestBack() } }
    val s by vm.state.collectAsStateWithLifecycle()
    val f = s.form
    val c = Eirs.colors
    val goods = f.nature == NatureOfSupply.GOODS

    ViewScaffold(
        header = { ViewHeader("ITEMS", onBack, title = { ViewTitle(if (f.isNew) "New item" else "Edit item") }) },
        bottomBar = {
            Text(s.readyText, style = EirsType.sans(12.sp).copy(color = c.muted))
            PrimaryButton("Save item", vm::submit, enabled = s.ready, height = 52.dp, fontSize = 15.sp, horizontalPadding = 18.dp)
        },
    ) {
        Text(
            if (f.isNew) "Adds to the catalog your team picks from." else "Changes apply to new documents. Registered invoices keep their original lines.",
            style = EirsType.sans(13.sp, lineHeight = 1.45).copy(color = c.muted),
        )
        FormCard {
            if (f.isNew) {
                FieldLabel("Item code *") {
                    FormField(f.code, { v -> vm.update { it.copy(code = v.uppercase()) } }, placeholder = "ITM-0000", mono = true)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.background).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Item code", style = EirsType.sans(13.sp).copy(color = c.muted))
                    MonoText(f.code, size = 13.sp, weight = FontWeight.Medium)
                }
            }
            FieldLabel("Description *") {
                FormField(f.description, { v -> vm.update { it.copy(description = v) } }, placeholder = "As printed on the receipt")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Nature of supply", style = EirsType.sans(13.sp, FontWeight.Medium))
                SegmentedControl(
                    listOf(SegmentOption(NatureOfSupply.GOODS, "Goods"), SegmentOption(NatureOfSupply.SERVICE, "Service")),
                    f.nature, { n -> vm.update { it.copy(nature = n) } }, height = 40.dp, fontSize = 14.sp, solidSelection = true,
                )
            }
            FieldLabel(if (goods) "Harmonization (HS) code *" else "Harmonization (HS) code") {
                FormField(
                    f.hs, { v -> vm.update { it.copy(hs = v.filter { ch -> ch.isDigit() || ch == '.' }) } },
                    placeholder = "e.g. 0901.11.00", mono = true, keyboardType = KeyboardType.Decimal,
                )
            }
            Text(
                if (goods) "Required for goods. Used in MoR and customs reporting." else "Optional for services.",
                Modifier.offset(y = (-6).dp), style = EirsType.sans(12.sp).copy(color = c.muted),
            )
        }
        FormCard {
            Text("Tax code", style = EirsType.sans(13.sp, FontWeight.Medium))
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TaxCode.entries.forEach { t ->
                    val on = f.tax == t
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(if (on) c.tint1 else Color.White)
                            .border(1.5.dp, if (on) c.brand else c.line, RoundedCornerShape(11.dp))
                            .plainClickable { vm.update { it.copy(tax = t) } }.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        MonoText(t.name, size = 12.sp, weight = FontWeight.Medium)
                        Text(t.description, style = EirsType.sans(10.sp).copy(color = c.muted))
                    }
                }
            }
            Text("Unit", style = EirsType.sans(13.sp, FontWeight.Medium))
            Wrap {
                CatalogUnits.forEach { u ->
                    val on = f.unit == u
                    Box(
                        Modifier.height(36.dp).clip(RoundedCornerShape(9.dp)).background(if (on) c.ink else Color.White)
                            .border(1.dp, if (on) c.ink else c.line, RoundedCornerShape(9.dp))
                            .plainClickable { vm.update { it.copy(unit = u) } }.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) { MonoText(u, size = 12.sp, color = if (on) Color.White else c.ink) }
                }
            }
            FieldLabel("Unit price (excl. tax) *") {
                FormField(
                    f.price, { v -> vm.update { it.copy(price = v.decimalOnly()) } },
                    placeholder = "0.00", mono = true, textAlign = TextAlign.End, keyboardType = KeyboardType.Decimal,
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.tint1).padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Customer pays", style = EirsType.sans(13.sp).copy(color = c.brandDark))
                MonoText(s.gross, size = 12.sp, weight = FontWeight.Medium, color = c.brandDark)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                EirsSwitch(f.active, { vm.update { it.copy(active = !it.active) } })
                Text("Active", style = EirsType.sans(14.sp, FontWeight.Medium))
            }
        }
    }
}
