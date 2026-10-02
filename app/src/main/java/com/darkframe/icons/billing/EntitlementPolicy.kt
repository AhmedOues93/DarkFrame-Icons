package com.darkframe.icons.billing

/** What Google Play says the user's standing with the Pro product is. */
enum class PurchaseStatus {
    /** No purchase of the product at all. */
    NONE,

    /** A purchase exists but has not completed — a cash or carrier payment still being settled. */
    PENDING,

    /** A completed purchase. */
    OWNED,
}

/** Why a billing operation did not produce an answer. Separate from "the answer was no". */
enum class BillingProblem {
    /** Play Billing is not available on this device or build at all. */
    UNAVAILABLE,

    /** Transient: no network, or Play could not be reached. */
    NETWORK,

    /** The user backed out of checkout. Not a problem to report. */
    CANCELLED,

    /** Play says the product is already owned, which [PurchaseStatus.OWNED] will confirm. */
    ALREADY_OWNED,

    /** The product id is not configured, or not available in this country. */
    ITEM_UNAVAILABLE,

    /** Anything else. */
    UNKNOWN,
}

/**
 * What to do about the user's entitlement, and whether to say anything.
 *
 * @param entitlement what the app should treat the user as having, now.
 * @param acknowledge whether Play is still waiting for an acknowledgement.
 * @param problem the thing that went wrong, if the caller should mention it.
 */
data class EntitlementDecision(
    val entitlement: Entitlement,
    val acknowledge: Boolean,
    val problem: BillingProblem? = null,
    /** True when the decision is a pending purchase the user should know is in progress. */
    val pending: Boolean = false,
)

/**
 * The rules for turning a Play query into an entitlement.
 *
 * Pure, and separate from [PlayBillingManager], because this is where a billing bug actually costs
 * somebody something and it is the one part of billing that can be tested without a Play connection.
 * Three of the four rules exist because of ways the naive version is wrong:
 *
 *  - **A failed query must not revoke.** Opening the Pro screen on a train would otherwise take Pro
 *    away from someone who paid for it. A query that did not complete is not an answer, so the cached
 *    entitlement stands.
 *  - **A pending purchase must neither grant nor revoke.** Cash and carrier payments sit in
 *    `PENDING` for minutes to days. Granting would give the product away; revoking would take it from
 *    someone who already had it and happens to be buying something else.
 *  - **An answered query with no purchase must revoke.** Refunds and chargebacks are real, and
 *    honouring them is the price of a one-time purchase that works offline the rest of the time.
 */
object EntitlementPolicy {

    /**
     * @param queryCompleted whether Play actually answered. False for every failure, including
     *   offline.
     * @param status what the answer said, meaningless when [queryCompleted] is false.
     * @param acknowledged whether the owned purchase has already been acknowledged.
     * @param cached what the app last stored, which is what an unanswered query falls back to.
     */
    fun decide(
        queryCompleted: Boolean,
        status: PurchaseStatus,
        acknowledged: Boolean,
        cached: Entitlement,
        problem: BillingProblem? = null,
    ): EntitlementDecision {
        if (!queryCompleted) {
            return EntitlementDecision(
                entitlement = cached,
                acknowledge = false,
                problem = problem ?: BillingProblem.NETWORK,
            )
        }
        return when (status) {
            PurchaseStatus.OWNED -> EntitlementDecision(
                entitlement = Entitlement.PRO,
                // Play revokes an unacknowledged purchase after three days, so this is not optional
                // tidiness: missing it refunds the user and removes their Pro.
                acknowledge = !acknowledged,
            )

            PurchaseStatus.PENDING -> EntitlementDecision(
                entitlement = cached,
                acknowledge = false,
                pending = true,
            )

            PurchaseStatus.NONE -> EntitlementDecision(
                entitlement = Entitlement.FREE,
                acknowledge = false,
            )
        }
    }

    /**
     * Whether a problem is worth telling the user about.
     *
     * Backing out of checkout is the user's own action and needs no message; everything else is
     * something they did not choose and may want to retry.
     */
    fun isWorthReporting(problem: BillingProblem?): Boolean =
        problem != null && problem != BillingProblem.CANCELLED &&
            problem != BillingProblem.ALREADY_OWNED
}
