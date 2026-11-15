package com.arin.civicreport.ui

object Routes {
    const val HOME = "home"
    const val TYPES = "types"
    const val CAPTURE = "capture"
    const val AUTHORITIES = "authorities"
    const val COMPOSE = "compose"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val REPORT_DETAIL = "report/{reportId}"

    fun reportDetail(id: String) = "report/$id"
}
