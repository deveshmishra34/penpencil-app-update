declare module "@capacitor/core" {
    interface PluginRegistry {
        AppUpdatePlugin: AppUpdatePluginPlugin;
    }
}
export interface AppUpdatePluginPlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
}
