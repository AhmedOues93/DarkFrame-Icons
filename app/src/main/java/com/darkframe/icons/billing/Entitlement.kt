package com.darkframe.icons.billing

enum class Entitlement { FREE, PRO }

interface EntitlementProvider {
    fun current(): Entitlement
}

/** V1 safe default. Play Billing implementation will replace this provider; never grants fake Pro. */
class LocalFreeEntitlementProvider : EntitlementProvider {
    override fun current() = Entitlement.FREE
}
