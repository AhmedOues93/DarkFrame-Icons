package com.darkframe.icons.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The billing rules, written as the situations they protect someone from.
 *
 * This is the one part of DarkFrame where a bug takes money or takes away something paid for, and it
 * is also the only part of billing testable without a Play connection — which is why the decision
 * lives in a pure object instead of inside the client callbacks.
 */
class EntitlementPolicyTest {

    @Test
    fun `a completed purchase grants Pro`() {
        val decision = EntitlementPolicy.decide(
            queryCompleted = true,
            status = PurchaseStatus.OWNED,
            acknowledged = true,
            cached = Entitlement.FREE,
        )
        assertEquals(Entitlement.PRO, decision.entitlement)
        assertFalse(decision.acknowledge)
    }

    @Test
    fun `an unacknowledged purchase is acknowledged`() {
        // Not tidiness: Play refunds an unacknowledged purchase after three days and takes the
        // entitlement with it, so missing this would quietly un-buy someone's Pro.
        val decision = EntitlementPolicy.decide(
            queryCompleted = true,
            status = PurchaseStatus.OWNED,
            acknowledged = false,
            cached = Entitlement.PRO,
        )
        assertEquals(Entitlement.PRO, decision.entitlement)
        assertTrue(decision.acknowledge)
    }

    @Test
    fun `opening Pro without a connection does not take Pro away`() {
        // The case that matters most: someone who paid, on a train.
        val decision = EntitlementPolicy.decide(
            queryCompleted = false,
            status = PurchaseStatus.NONE,
            acknowledged = false,
            cached = Entitlement.PRO,
            problem = BillingProblem.NETWORK,
        )
        assertEquals(Entitlement.PRO, decision.entitlement)
        assertEquals(BillingProblem.NETWORK, decision.problem)
    }

    @Test
    fun `a failed query does not grant Pro either`() {
        val decision = EntitlementPolicy.decide(
            queryCompleted = false,
            status = PurchaseStatus.OWNED,
            acknowledged = false,
            cached = Entitlement.FREE,
        )
        assertEquals(Entitlement.FREE, decision.entitlement)
        assertFalse("an unanswered query must never acknowledge", decision.acknowledge)
    }

    @Test
    fun `a pending payment neither grants nor revokes`() {
        // Cash and carrier payments sit in PENDING for minutes to days. Granting gives the product
        // away; revoking takes it from someone who already had it and is buying something else.
        val fromFree = EntitlementPolicy.decide(
            queryCompleted = true,
            status = PurchaseStatus.PENDING,
            acknowledged = false,
            cached = Entitlement.FREE,
        )
        assertEquals(Entitlement.FREE, fromFree.entitlement)
        assertTrue(fromFree.pending)

        val fromPro = EntitlementPolicy.decide(
            queryCompleted = true,
            status = PurchaseStatus.PENDING,
            acknowledged = false,
            cached = Entitlement.PRO,
        )
        assertEquals(Entitlement.PRO, fromPro.entitlement)
        assertTrue(fromPro.pending)
    }

    @Test
    fun `a pending payment is never acknowledged`() {
        val decision = EntitlementPolicy.decide(
            queryCompleted = true,
            status = PurchaseStatus.PENDING,
            acknowledged = false,
            cached = Entitlement.FREE,
        )
        assertFalse(decision.acknowledge)
    }

    @Test
    fun `an answered query with no purchase revokes`() {
        // Refunds and chargebacks are real, and honouring them is the price of an entitlement that
        // works offline the rest of the time.
        val decision = EntitlementPolicy.decide(
            queryCompleted = true,
            status = PurchaseStatus.NONE,
            acknowledged = false,
            cached = Entitlement.PRO,
        )
        assertEquals(Entitlement.FREE, decision.entitlement)
        assertFalse(decision.pending)
    }

    @Test
    fun `backing out of checkout is not reported as a problem`() {
        assertFalse(EntitlementPolicy.isWorthReporting(BillingProblem.CANCELLED))
        // Already-owned is answered by the follow-up query, so telling the user about it would be
        // reporting an error immediately before granting them the thing.
        assertFalse(EntitlementPolicy.isWorthReporting(BillingProblem.ALREADY_OWNED))
        assertFalse(EntitlementPolicy.isWorthReporting(null))
    }

    @Test
    fun `everything the user did not choose is reported`() {
        listOf(
            BillingProblem.NETWORK,
            BillingProblem.UNAVAILABLE,
            BillingProblem.ITEM_UNAVAILABLE,
            BillingProblem.UNKNOWN,
        ).forEach { problem ->
            assertTrue("$problem should be reported", EntitlementPolicy.isWorthReporting(problem))
        }
    }

    @Test
    fun `no path grants Pro without a completed owned purchase`() {
        // Exhaustive over the inputs, because "no fake Pro" is a product promise and a single
        // missed branch is how one appears.
        for (completed in listOf(true, false)) {
            for (status in PurchaseStatus.entries) {
                for (acknowledged in listOf(true, false)) {
                    val decision = EntitlementPolicy.decide(
                        queryCompleted = completed,
                        status = status,
                        acknowledged = acknowledged,
                        cached = Entitlement.FREE,
                    )
                    val grantsPro = decision.entitlement == Entitlement.PRO
                    val earned = completed && status == PurchaseStatus.OWNED
                    assertEquals(
                        "completed=$completed status=$status granted=$grantsPro",
                        earned,
                        grantsPro,
                    )
                }
            }
        }
    }
}
