package com.doubletimegames.GodotGooglePlayBilling

import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.doubletimegames.GodotGooglePlayBilling.utils.*
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot


class GodotGooglePlayBilling(godot: Godot) :
    GodotPlugin(godot),
    PurchasesUpdatedListener,
    BillingClientStateListener,
    AcknowledgePurchaseResponseListener,
    PurchasesResponseListener,
    ConsumeResponseListener,
    ProductDetailsResponseListener {

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

    private lateinit var billingClient: BillingClient
    private var logLevel = 0
    private var logTag = "GodotGooglePlayBilling"
    private val productDetailsHashMap = HashMap<String, ProductDetails>()
    private var latestToken = ""

    private fun log(message: String) {
        if (logLevel > 0) {
            Log.i(logTag, message)
        }
    }

    @UsedByGodot
    fun setLogLevel(level: Int) {
        logLevel = level
    }

    @UsedByGodot
    fun setLogTag(tag: String) {
        logTag = tag
    }

    @UsedByGodot
    fun startConnection() {
        val activity = godot.getActivity()
        if (activity == null || activity.applicationContext == null) {
            throw Error("Godot Activity is not defined")
        }

        billingClient = BillingClient
            .newBuilder(activity.applicationContext)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .setListener(this)
            .build()

        billingClient.startConnection(this)
    }

    @UsedByGodot
    fun endConnection() {
        billingClient.endConnection()
    }

    @UsedByGodot
    fun isReady(): Boolean {
        return billingClient.isReady
    }

    @UsedByGodot
    fun getConnectionState(): Int {
        return billingClient.connectionState
    }

    override fun getPluginSignals(): Set<SignalInfo> {
        val signals: MutableSet<SignalInfo> = mutableSetOf()

        // Core
        signals.add(SignalInfo("connected"))
        signals.add(SignalInfo("disconnected"))
        signals.add(SignalInfo("resume"))
        signals.add(
            SignalInfo(
                "connect_error",
                Int::class.javaObjectType,
                String::class.javaObjectType,
            )
        )

        // Purchases
        signals.add(SignalInfo("purchases_updated", Int::class.javaObjectType, String::class.javaObjectType, Object::class.javaObjectType)) // Debug message, Error code, purchases[]
        signals.add(SignalInfo("purchase_error", Int::class.javaObjectType, String::class.javaObjectType)) // Response code, Debug message

        // Queries
        signals.add(SignalInfo("query_purchases", Int::class.javaObjectType, String::class.javaObjectType, Object::class.javaObjectType))
        signals.add(SignalInfo("query_product_details", Int::class.javaObjectType, String::class.javaObjectType, Object::class.javaObjectType))

        // Acknowledgement / Consumption
        signals.add(
            SignalInfo(
                "purchase_acknowledged",
                Int::class.javaObjectType,
                String::class.javaObjectType,
                String::class.javaObjectType
            )
        )
        signals.add(
            SignalInfo(
                "purchase_consumed",
                Int::class.javaObjectType,
                String::class.javaObjectType,
                String::class.javaObjectType
            )
        )

        return signals
    }

    @UsedByGodot
    fun queryProductDetails(productIds: Array<String?>, productType: String?) {
        if (productIds.isEmpty()) {
            return
        }

        val products: MutableList<QueryProductDetailsParams.Product> = ArrayList()

        for (id in productIds) {
            products.add(
                QueryProductDetailsParams.Product.newBuilder().setProductId(id!!)
                    .setProductType(
                        productType!!
                    ).build()
            )
        }
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        billingClient.queryProductDetailsAsync(queryProductDetailsParams, this)
    }

    @UsedByGodot
    fun purchase(productId: String?, productType: String?) {
        this.log("Purchasing")
        if (!productDetailsHashMap.containsKey(productId)) {
            emitSignal(
                "purchase_error",
                BillingClient.BillingResponseCode.ERROR,
                "Product $productId does not exist. Have you called queryProductDetails?"
            )
            return
        }

        val activity = godot.getActivity()

        if (activity == null) {
            emitSignal(
                "purchase_error",
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                "Not connected to Google Play Billing services"
            )
            return
        }

        val productDetails = productDetailsHashMap[productId] ?: return
        val productDetailsParamsBuilder = ProductDetailsParams
            .newBuilder()
            .setProductDetails(productDetails)

        if (productType == "subs") {
            if (productDetails.subscriptionOfferDetails == null) {
                return
            }

            productDetailsParamsBuilder.setOfferToken(productDetails.subscriptionOfferDetails!![0].offerToken)
        }

        val productDetailsParams = productDetailsParamsBuilder.build()

        val billingFlowParams = BillingFlowParams
            .newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .setObfuscatedAccountId("")
            .setObfuscatedProfileId("")
            .build()

        val billingResult =
            billingClient.launchBillingFlow(activity, billingFlowParams)

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            this.log("Purchase Error! " + billingResult.responseCode + " " + billingResult.debugMessage);
            emitSignal(
                "purchase_error",
                billingResult.responseCode,
                billingResult.debugMessage
            );
        }
    }

    @UsedByGodot
    fun acknowledgePurchase(purchaseToken: String) {
        val acknowledgePurchaseParams =
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        latestToken = purchaseToken
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
    }

    @UsedByGodot
    fun queryPurchases(productType: String?) {
        val queryPurchasesParams = QueryPurchasesParams.newBuilder().setProductType(
            productType!!
        ).build()
        billingClient.queryPurchasesAsync(queryPurchasesParams, this)
    }

    @UsedByGodot
    fun consume(purchaseToken: String?) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken!!).build()
        billingClient.consumeAsync(consumeParams, this)
    }

    /**
     * Event Listeners
     */

    override fun onMainResume() {
        try {
            emitSignal("resume")
        } catch (e: IllegalArgumentException) {
            this.log(e.toString())
        }
    }

    override fun onBillingServiceDisconnected() {
        emitSignal("disconnected")
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            this.log("Connected!")
            emitSignal("connected")
        } else {
            this.log("Connect Error!")
            emitSignal("connect_error", billingResult.responseCode, billingResult.debugMessage)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, list: MutableList<Purchase>?) {
          this.log("onAcknowledgePurchaseResponse: ${billingResult.responseCode} - ${billingResult.debugMessage} - $list")

        emitSignal(
            "purchases_updated",
            billingResult.responseCode,
            billingResult.debugMessage,
            purchaseListToDictionaryArray(if (list.isNullOrEmpty()) emptyList() else list)
        );
    }

    override fun onProductDetailsResponse(
        billingResult: BillingResult,
        list: List<ProductDetails>
    ) {
        this.log("onAcknowledgePurchaseResponse: ${billingResult.responseCode} - ${billingResult.debugMessage} - $list")

        if (list != null) {
            for (productDetails in list) {
                productDetailsHashMap[productDetails.productId] = productDetails
            }
        }

        emitSignal(
            "query_product_details",
            billingResult.responseCode,
            billingResult.debugMessage,
            productDetailsListToDictionaryArray(list)
        )
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        this.log("onAcknowledgePurchaseResponse: ${billingResult.responseCode} - ${billingResult.debugMessage} - $latestToken")

        emitSignal("purchase_acknowledged", billingResult.responseCode, billingResult.debugMessage, latestToken)

        latestToken = ""
    }

    override fun onConsumeResponse(billingResult: BillingResult, purchaseToken: String) {
        this.log("onConsumeResponse: ${billingResult.responseCode} - ${billingResult.debugMessage} - $purchaseToken")

        emitSignal("purchase_consumed", billingResult.responseCode, billingResult.debugMessage, purchaseToken)
    }

    override fun onQueryPurchasesResponse(billingResult: BillingResult, list: List<Purchase>) {
        this.log("onQueryPurchasesResponse: ${billingResult.responseCode} - ${billingResult.debugMessage} - $list")

        emitSignal(
            "query_purchases",
            billingResult.responseCode,
            billingResult.debugMessage,
            purchaseListToDictionaryArray(list)
        )
    }
}
