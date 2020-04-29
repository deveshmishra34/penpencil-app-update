import { WebPlugin } from '@capacitor/core';
import { AppInfoModal, AppUpdatePluginPlugin } from './definitions';
export declare class AppUpdatePluginWeb extends WebPlugin implements AppUpdatePluginPlugin {
    constructor();
    copyAndExtractFile(options: {
        fileName: string;
        updateVersion: string;
    }): Promise<{
        updateUrl: string;
        updateStatus: string;
    }>;
    downloadUpdate(options: {
        fileUrl: string;
        fileName: string;
    }): Promise<void>;
    getAppInfo(): Promise<AppInfoModal>;
    updatePref(options: {
        updateVersion: string;
        updateStatus: string;
    }): Promise<void>;
}
declare const AppUpdatePlugin: AppUpdatePluginWeb;
export { AppUpdatePlugin };
