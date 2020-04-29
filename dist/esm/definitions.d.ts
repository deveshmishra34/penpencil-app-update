import { PluginListenerHandle } from "@capacitor/core";
declare module "@capacitor/core" {
    interface PluginRegistry {
        AppUpdatePlugin: AppUpdatePluginPlugin;
    }
}
export interface AppUpdatePluginPlugin {
    /**
     * Get device info
     */
    getAppInfo(): Promise<AppInfoModal>;
    /**
     * Update pref
     */
    updatePref(options: {
        updateVersion: string;
        updateStatus: string;
    }): Promise<void>;
    /**
     * Download update
     */
    downloadUpdate(options: {
        fileUrl: string;
        fileName: string;
    }): Promise<void>;
    /**
     * Copy and extract update
     */
    copyAndExtractFile(options: {
        fileName: string;
        updateVersion: string;
    }): Promise<{
        updateUrl: string;
        updateStatus: string;
    }>;
    /**
     * Listen for is updated downloaded
     */
    addListener(eventName: 'appUpdateDownloaded', listenerFunc: () => void): PluginListenerHandle;
    /**
     * Listen for is updated live
     */
    addListener(eventName: 'appUpdateLive', listenerFunc: () => void): PluginListenerHandle;
}
export interface AppInfoModal {
    binaryVersionCode: number;
    binaryVersionName: string;
    bundleName: string;
    bundleVersion: string;
    currentVersion: string;
    deviceId: string;
    updateVersion: string;
    updateUrl: string;
    updateStatus: string;
    version: number;
    deviceInfo: DeviceInfo;
}
interface DeviceInfo {
    brand: string;
    display: string;
    hardware: string;
    manufacturer: string;
    model: string;
    platform: string;
    platformVersion: string;
}
export {};
