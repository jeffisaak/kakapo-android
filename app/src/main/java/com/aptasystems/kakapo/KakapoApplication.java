package com.aptasystems.kakapo;

import android.app.Application;
import android.content.Context;

import io.reactivex.plugins.RxJavaPlugins;

public class KakapoApplication extends Application {

    private KakapoComponent _kakapoComponent;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        RxJavaPlugins.setErrorHandler(e -> { });

        _kakapoComponent = DaggerKakapoComponent.builder()
                .kakapoModule(new KakapoModule(this))
                .build();

    }

    public KakapoComponent getKakapoComponent() {
        return _kakapoComponent;
    }

}
