#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(AppUpdatePlugin, "AppUpdatePlugin",
           CAP_PLUGIN_METHOD(echo, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(getAppInfo, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(updatePref, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(downloadUpdate, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(copyAndExtractFile, CAPPluginReturnPromise);
)
