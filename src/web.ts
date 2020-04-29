import {WebPlugin} from '@capacitor/core';
import {AppUpdatePluginPlugin} from './definitions';

export class AppUpdatePluginWeb extends WebPlugin implements AppUpdatePluginPlugin {
    constructor() {
        super({
            name: 'AppUpdatePlugin',
            platforms: ['web']
        });
    }

    async echo(options: { value: string }): Promise<{ value: string }> {
        console.log('ECHO', options);
        return options;
    }

    // async getAppInfo(): Promise<{ value: string }> {
    //     console.log('ECHO', options);
    //     return options;
    // }
}

const AppUpdatePlugin = new AppUpdatePluginWeb();

export {AppUpdatePlugin};

import {registerWebPlugin} from '@capacitor/core';

registerWebPlugin(AppUpdatePlugin);
