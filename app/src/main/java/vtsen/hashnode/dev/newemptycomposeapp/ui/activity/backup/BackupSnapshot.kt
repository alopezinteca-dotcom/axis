package vtsen.hashnode.dev.newemptycomposeapp.ui.activity.backup

import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.data.TravelStopEntity
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.BillingPeriod
import vtsen.hashnode.dev.newemptycomposeapp.ui.activity.kpi.CalendarOverridesStore

data class BackupSnapshot(
    val createdAtMillis: Long,
    val billingPeriod: BillingPeriod?,
    val holidays: List<CalendarOverridesStore.Holiday>,
    val vacations: List<CalendarOverridesStore.Vacation>,
    val travels: List<TravelEntity>,
    val stops: List<TravelStopEntity>
)
