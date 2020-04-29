import { WebPlugin } from '@capacitor/core';
import { AppUpdatePluginPlugin } from './definitions';
export declare class AppUpdatePluginWeb extends WebPlugin implements AppUpdatePluginPlugin {
    constructor();
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
declare const AppUpdatePlugin: AppUpdatePluginWeb;
export { AppUpdatePlugin };
