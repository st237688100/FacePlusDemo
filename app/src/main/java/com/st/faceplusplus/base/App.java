package com.st.faceplusplus.base;

import android.app.Application;

import com.facebook.stetho.Stetho;


public class App extends Application {
    private static App app;

    public static App getInstance() {
        return app;
    }

    public App() {
        app = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(
                                Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(
                                Stetho.defaultInspectorModulesProvider(this))
                        .build());
    }

}
