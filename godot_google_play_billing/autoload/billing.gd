extends Node

enum ResponseCode {
	FEATURE_NOT_SUPPORTED = -2,
	SERVICE_DISCONNECTED = -1,
	OK = 0,
	USER_CANCELED = 1,
	SERVICE_UNAVAILABLE = 2,
	BILLING_UNAVAILABLE = 3,
	ITEM_UNAVAILABLE = 4,
	DEVELOPER_ERROR = 5,
	ERROR = 6,
	ITEM_ALREADY_OWNED = 7,
	NETWORK_ERROR = 12,
}

enum ConnectionState {
	CLOSED = 3,
	CONNECTED = 2,
	CONNECTING = 1,
	DISCONNECTED = 0
}

const CONSUMABLE_ITEMS: PackedStringArray = []
const NON_CONSUMABLE_ITEMS: PackedStringArray = []
const SUBSCRIPTION_ITEMS: PackedStringArray = []
const LIBRARY_NAME: StringName = "GodotGooglePlayBilling"

var _library: Object = null
var _consume: Array[Array] = []
var _product_details: Array[ProductDetails] = []
var _purchases: Array[Purchase] = []

func Log(message: String):
	print("[%s]: %s" % [LIBRARY_NAME, message])
	
func LogError(message: String):
	printerr("[%s]: %s" % [LIBRARY_NAME, message])

func _ready() -> void:
	if not Engine.has_singleton(LIBRARY_NAME):
		Log("Can not find library")
		return
	Log("Library found")
	_library = Engine.get_singleton(LIBRARY_NAME)
	
	_library.setLogTag(LIBRARY_NAME + "Remote")
	_library.setLogLevel(1)
	
	_connect_signals()
	startConnection()
	
	await _library.connected

func queryProductDetails(product_ids: PackedStringArray, product_type: String) -> void:
	Log("Querying product details! Product IDs: %s Product Type: %s" % [product_ids, product_type])
	_library.queryProductDetails(product_ids, product_type)
	
func queryPurchases(product_type: String) -> void:
	Log("Querying purchases! Product Type: %s" % product_type)
	_library.queryPurchases(product_type)
	
func purchase(productId: String, productType: String) -> void:
	Log("Requesting purchasing %s." % productId)
	_library.purchase(productId, productType)

func acknowledgePurchase(purchase_token: String) -> void:
	Log("Acknowledging purchase! Purchase Token: %s" % purchase_token)
	_library.acknowledgePurchase(purchase_token)

func consumePurchase(purchase_token: String) -> void:
	Log("Consuming! Purchase Token: %s" % purchase_token)
	_library.consume(purchase_token)
	
func startConnection() -> void:
	Log("Starting connection.")
	_library.startConnection()

func endConnection() -> void:
	Log("Ending connection.")
	_library.endConnection()

func isReady() -> bool:
	Log("Returning is ready.")
	return _library.isReady()

func getConnectionState() -> ConnectionState:
	Log("Returning connection state.")
	return _library.getConnectionState()

func _connect_signals() -> void:
	# Core
	_library.connect("connected", _on_connected)
	_library.connect("disconnected", _on_disconnected)
	_library.connect("resume", _on_resume)
	_library.connect("connect_error", _on_connect_error)
	
	# Purchases
	_library.connect("purchases_updated", _on_purchases_updated)
	_library.connect("purchase_error", _on_purchase_error)
	
	# Queries
	_library.connect("query_product_details", _on_query_product_details)
	_library.connect("query_purchases", _on_query_purchases)
	
	# Acknowledged / Consumed
	_library.connect("purchase_acknowledged", _on_purchase_acknowledged)
	_library.connect("purchase_consumed", _on_purchase_consumed)

func _handle_purchase(purchase_token: String) -> void:
	Log("Handling purchase! Purchase token: %s" % purchase_token)
	match purchase_token:
		"token":
			pass

func _handle_consume(purchase_token: String) -> void:
	Log("Handling consume! Purchase token: %s" % purchase_token)
	var quantity: int = 0
	for i: int in range(_consume.size() - 1, 0, -1):
		if _consume[i][0] == purchase_token:
			quantity = _consume[i][1]
			_consume.remove_at(i)
			break
	match purchase_token:
		"token":
			pass # You can do something like `money += 1000 * quantity`
	
###
### Listeners
###

### Core
func _on_connected() -> void:
	Log("Connected!")

func _on_disconnected() -> void:
	Log("Disconnected")
	
func _on_resume() -> void:
	Log("Resumed")
	
func _on_connect_error(responseCode: ResponseCode, debugMessage: String) -> void:
	Log("Connect error: Message: %s - responseCode: %s" % [debugMessage, responseCode])

### Purchases
func _on_purchases_updated(responseCode: ResponseCode, debugMessage: String, purchases: Array) -> void:
	if (responseCode != ResponseCode.OK):
		LogError("Purchases Updated Error - Error code: %s - Debug Message: %s" % [responseCode, debugMessage])
		return
	
	Log("Purchases: %s" % purchases)
	for data: Dictionary in purchases:
		_purchases.append(Purchase.new().deserialize(data))
	for purchase: Purchase in _purchases:
		if purchase.is_acknowledged or purchase.state != purchase.PurchaseState.PURCHASED:
			continue
		if purchase.token in CONSUMABLE_ITEMS:
			_consume.append([purchase.token, purchase.quantity])
			consumePurchase(purchase.token)
		elif purchase.token in NON_CONSUMABLE_ITEMS or purchase.token in SUBSCRIPTION_ITEMS:
			acknowledgePurchase(purchase.token)

func _on_purchase_error(responseCode: ResponseCode, debugMessage: String) -> void:
	LogError("Purchase error: Error Code: %s - Debug Message: %s" % [responseCode, debugMessage])
	
### Queries
func _on_query_product_details(responseCode: ResponseCode, debugMessage: String, product_details: Array) -> void:
	if (responseCode != ResponseCode.OK):
		Log("Query Product Details error: Error Code: %s - Debug Message: %s" % [responseCode, debugMessage])
		return
		
	Log("Product Details: %s" % str(product_details))
	
	if product_details.is_empty():
		return
	for data: Dictionary in product_details:
		_product_details.append(ProductDetails.new().deserialize(data))
		
	Log("Product details size: %s" % _product_details.size())

func _on_query_purchases(responseCode: ResponseCode, debugMessage: String, purchases: Array) -> void:
	if (responseCode != ResponseCode.OK):
		LogError("Query Purchases error: Error Code: %s - Debug Message: %s" % [responseCode, debugMessage])
		return
		
	Log("Product Details: %s" % str(purchases))
	
	if purchases.is_empty():
		return
	
	for purchase: Dictionary in purchases:
		var object: Purchase = Purchase.new().deserialize(purchase)
		if not object in _purchases:
			_purchases.append(object)
	Log("Purchases size: %s" % _purchases.size())
	
### Acknowledged / Consumed
func _on_purchase_acknowledged(responseCode: ResponseCode, debugMessage: String, purchase_token: String) -> void:
	Log("Purcase Acknowledged error: Error Code: %s - Debug Message: %s" % [responseCode, debugMessage])
	if responseCode != ResponseCode.OK:
		return
	_handle_purchase(purchase_token)

func _on_purchase_consumed(responseCode: ResponseCode, debugMessage: String, purchase_token: String) -> void:
	Log("Purchase Consumed error: Error Code: %s - Debug Message: %s" % [responseCode, debugMessage])
	if responseCode != ResponseCode.OK:
		return
	_handle_consume(purchase_token)
