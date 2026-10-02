package com.darkframe.icons.billing

enum class Entitlement { FREE, PRO }

interface EntitlementProvider {
    fun current(): Entitlement
}
