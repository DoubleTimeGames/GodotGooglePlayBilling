@tool
extends EditorPlugin

var _export_plugin: AndroidExportPlugin = null

var pluginName = "[GodotGooglePlayBilling]"

func _enter_tree() -> void:
	_export_plugin = AndroidExportPlugin.new()
	add_export_plugin(_export_plugin)
	add_autoload_singleton("GodotGooglePlayBilling", "res://addons/godot_google_play_billing/autoload/billing.gd")

func _exit_tree() -> void:
	remove_export_plugin(_export_plugin)
	remove_autoload_singleton("GodotGooglePlayBilling")
	_export_plugin = null

class AndroidExportPlugin extends EditorExportPlugin:
	var _plugin_name: String = "GodotGooglePlayBilling"

	func _supports_platform(platform: EditorExportPlatform) -> bool:
		if platform is EditorExportPlatformAndroid:
			return true
		return false

	func _get_android_libraries(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		if debug:
			return PackedStringArray(["res://addons/godot_google_play_billing/GodotGooglePlayBilling-debug.aar"])
		else:
			return PackedStringArray(["res://addons/godot_google_play_billing/GodotGooglePlayBilling-release.aar"])

	func _get_android_dependencies(platform: EditorExportPlatform, debug: bool) -> PackedStringArray:
		return PackedStringArray(['com.android.billingclient:billing:7.1.1'])

	func _get_name() -> String:
		return _plugin_name
