package com.eirspay.mobile.feature.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.eirspay.domain.model.CatalogItem
import com.eirspay.domain.model.NatureOfSupply
import com.eirspay.domain.usecase.ObserveCatalogUseCase
import com.eirspay.domain.usecase.SetCatalogItemActiveUseCase
import com.eirspay.mobile.ui.designsystem.*
import com.eirspay.mobile.ui.theme.Eirs
import com.eirspay.mobile.ui.theme.EirsType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ItemStatusFilter(val label: String) { ALL("Any status"), ACTIVE("Active"), INACTIVE("Inactive") }

data class ItemsUiState(
    val query: String = "",
    val nature: NatureOfSupply? = null,
    val status: ItemStatusFilter = ItemStatusFilter.ALL,
    val eyebrow: String = "CATALOG",
    val items: List<CatalogItem> = emptyList(),
)

private data class ItemFilters(val query: String = "", val nature: NatureOfSupply? = null, val status: ItemStatusFilter = ItemStatusFilter.ALL)

@HiltViewModel
class ItemsViewModel @Inject constructor(
    observeCatalog: ObserveCatalogUseCase,
    private val setActive: SetCatalogItemActiveUseCase,
) : ViewModel() {
    private val filters = MutableStateFlow(ItemFilters())

    val state: StateFlow<ItemsUiState> = combine(observeCatalog(), filters) { cat, f ->
        val q = f.query.trim().lowercase()
        ItemsUiState(
            query = f.query, nature = f.nature, status = f.status,
            eyebrow = "CATALOG · ${cat.size} ITEMS · ${cat.count { it.active }} ACTIVE",
            items = cat.filter {
                (f.nature == null || it.nature == f.nature) &&
                    (f.status == ItemStatusFilter.ALL || (f.status == ItemStatusFilter.ACTIVE) == it.active) &&
                    (q.isEmpty() || "${it.code} ${it.description}".lowercase().contains(q))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemsUiState())

    fun onQuery(q: String) = filters.update { it.copy(query = q) }
    fun onNature(n: NatureOfSupply?) = filters.update { it.copy(nature = n) }
    fun onStatus(s: ItemStatusFilter) = filters.update { it.copy(status = s) }
    fun toggle(item: CatalogItem) { viewModelScope.launch { setActive(item.code, !item.active) } }
}

private val NatureFilters = listOf(null to "All", NatureOfSupply.GOODS to "Goods", NatureOfSupply.SERVICE to "Services")

@Composable
fun ItemsScreen(onBack: () -> Unit, onNewItem: () -> Unit, onEditItem: (code: String) -> Unit) {
    val vm: ItemsViewModel = hiltViewModel()
    val s by vm.state.collectAsStateWithLifecycle()
    ViewScaffold(
        header = {
            ViewHeader(
                s.eyebrow, onBack, title = { ViewTitle("Items ", "catalog") },
                trailing = { IconSquareButton(EirsIcons.Plus, onNewItem, filled = true, strokeWidth = 2.4f) },
            )
        },
        bodyPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
    ) {
        SearchField(s.query, vm::onQuery, "Code or description")
        ChipRail { NatureFilters.forEach { (n, label) -> EirsChip(label, s.nature == n, { vm.onNature(n) }) } }
        ChipRail { ItemStatusFilter.entries.forEach { st -> EirsChip(st.label, s.status == st, { vm.onStatus(st) }) } }
        s.items.forEach { item -> ItemCard(item, { onEditItem(item.code) }, { vm.toggle(item) }) }
        if (s.items.isEmpty()) EmptyState("No items match these filters.")
    }
}

@Composable
private fun ItemCard(item: CatalogItem, onOpen: () -> Unit, onToggle: () -> Unit) {
    val c = Eirs.colors
    ListCard(onClick = onOpen, alpha = if (item.active) 1f else 0.62f) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.description, style = EirsType.sans(15.sp, FontWeight.Medium))
                MonoText("${item.code} · HS ${item.hsCode.ifBlank { "—" }}", size = 11.sp, color = c.muted)
            }
            EirsSwitch(item.active, onToggle)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                NatureBadge(item.nature)
                TaxTag(item.taxCode.name)
                Text(item.unit, style = EirsType.sans(11.sp).copy(color = c.muted))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                MonoText(Fmt.f2(item.unitPrice), size = 14.sp, weight = FontWeight.Medium)
                Text("incl. tax " + Fmt.f2(item.grossPrice), style = EirsType.sans(10.sp).copy(color = c.muted))
            }
        }
    }
}

@Composable
internal fun NatureBadge(nature: NatureOfSupply) {
    val c = Eirs.colors
    if (nature == NatureOfSupply.GOODS) Badge(nature.displayName, c.tint1, c.brandDark) else Badge(nature.displayName, c.neutralChip, c.ink)
}

@Composable
private fun TaxTag(text: String) {
    val c = Eirs.colors
    Text(
        text, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(c.tint1).padding(horizontal = 6.dp, vertical = 3.dp),
        style = EirsType.mono(10.sp).copy(color = c.brandDark),
    )
}

