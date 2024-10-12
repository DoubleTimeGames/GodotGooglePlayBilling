package com.doubletimegames.GodotGooglePlayBilling.utils

import com.android.billingclient.api.AccountIdentifiers
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.OneTimePurchaseOfferDetails
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PendingPurchaseUpdate

import org.godotengine.godot.Dictionary


fun purchaseToDictionary(purchase: Purchase): Dictionary {
    val data = Dictionary()
    if (purchase.accountIdentifiers != null) {
        data["account_identifiers"] =
            accountIdentifiersToDictionary(purchase.accountIdentifiers) // dictionary
    }
    if (purchase.pendingPurchaseUpdate != null) {
        data["pending_purchase_update"] =
            pendingPurchaseUpdateToDictionary(purchase.pendingPurchaseUpdate) // dictionary
    }
    data["products"] = purchase.products.toTypedArray() // string array
    data["token"] = purchase.purchaseToken // string
    data["state"] = purchase.purchaseState // int
    data["is_auto_renewing"] = purchase.isAutoRenewing // bool
    data["original_json"] = purchase.originalJson // string
    data["package_name"] = purchase.packageName // string
    data["order_id"] = purchase.orderId // string
    data["developer_payload"] = purchase.developerPayload // string
    data["quantity"] = purchase.quantity // int
    data["is_acknowledged"] = purchase.isAcknowledged // bool
    data["signature"] = purchase.signature // string
    return data
}

fun accountIdentifiersToDictionary(accountIdentifiers: AccountIdentifiers?): Dictionary {
    val data = Dictionary()
    data["obfuscated_account_id"] = accountIdentifiers!!.obfuscatedAccountId // string
    data["obfuscated_profile_id"] = accountIdentifiers.obfuscatedProfileId // string
    return data
}

fun pendingPurchaseUpdateToDictionary(pendingPurchaseUpdate: PendingPurchaseUpdate?): Dictionary {
    val data = Dictionary()
    data["token"] = pendingPurchaseUpdate!!.purchaseToken // string
    data["products"] = pendingPurchaseUpdate.products.toTypedArray() // string array
    return data
}

fun productDetailsToDictionary(productDetails: ProductDetails): Dictionary {
    val data = Dictionary()
    data["id"] = productDetails.productId // string
    data["type"] = productDetails.productType // string
    data["description"] = productDetails.description // string
    data["name"] = productDetails.name // string
    data["title"] = productDetails.title // string
    if (productDetails.oneTimePurchaseOfferDetails != null) {
        data["one_time_purchase_offer_details"] =
            oneTimePurchaseOfferDetailsToDictionary(productDetails.oneTimePurchaseOfferDetails) // dictionary
    }
    if (productDetails.subscriptionOfferDetails != null) {
        data["subscription_offer_details"] =
            subscriptionOfferDetailsListToDictionaryArray(productDetails.subscriptionOfferDetails) // array
    }
    return data
}

fun oneTimePurchaseOfferDetailsToDictionary(oneTimePurchaseOfferDetails: OneTimePurchaseOfferDetails?): Dictionary {
    val data = Dictionary()
    data["currency_code"] = oneTimePurchaseOfferDetails!!.priceCurrencyCode // string
    data["formatted_price"] = oneTimePurchaseOfferDetails.formattedPrice // string
    data["price_amount"] = oneTimePurchaseOfferDetails.priceAmountMicros // int
    return data
}

fun subscriptionOfferDetailsToDictionary(details: SubscriptionOfferDetails): Dictionary {
    val data = Dictionary()
    data["id"] = details.offerId // string
    data["tags"] = details.offerTags.toTypedArray() // string array
    data["token"] = details.offerToken // string
    data["base_plan_id"] = details.basePlanId // string
    data["pricing_phases"] =
        details.pricingPhases.pricingPhaseList.toTypedArray() // string array
    if (details.installmentPlanDetails != null) {
        data["installment_plan_commitment_payments_count"] = details.installmentPlanDetails!!
            .installmentPlanCommitmentPaymentsCount // int
        data["subsequent_installment_plan_commitment_payments_count"] =
            details.installmentPlanDetails!!
                .subsequentInstallmentPlanCommitmentPaymentsCount // int
    }
    return data
}

fun productDetailsListToDictionaryArray(productDetails: List<ProductDetails>): Array<Any?> {
    val data: MutableList<Dictionary> = ArrayList()
    for (details in productDetails) {
        data.add(productDetailsToDictionary(details))
    }
    return data.toTypedArray()
}

fun subscriptionOfferDetailsListToDictionaryArray(subscriptionOfferDetails: List<SubscriptionOfferDetails>?): Array<Any?> {
    val data: MutableList<Dictionary> = ArrayList()
    for (details in subscriptionOfferDetails!!) {
        data.add(subscriptionOfferDetailsToDictionary(details))
    }
    return data.toTypedArray()
}

fun purchaseListToDictionaryArray(purchases: List<Purchase>): Array<Any?>? {
    val list: MutableList<Dictionary> = ArrayList()
    for (purchase in purchases) {
        list.add(purchaseToDictionary(purchase))
    }
    return list.toTypedArray()
}