package com.aptasystems.kakapo;

import android.content.Context;

import com.aptasystems.kakapo.entities.Models;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.requery.Persistable;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.sql.Configuration;
import io.requery.sql.EntityDataStore;
import io.requery.sql.TableCreationMode;
import kakapo.client.retrofit.RetrofitService;
import kakapo.client.retrofit.RetrofitServiceGenerator;
import kakapo.crypto.PGPEncryptionService;
import kakapo.crypto.SecretKeyEncryptionService;

@Module
public class KakapoModule {

    private Context _context;

    public KakapoModule(Context context) {
        _context = context;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return _context;
    }

    @Provides
    public EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    public PGPEncryptionService providePgpEncryptionService() {
        return new PGPEncryptionService();
    }

    @Provides
    @Singleton
    public SecretKeyEncryptionService provideSecretKeyEncryptionService() {
        return new SecretKeyEncryptionService();
    }

    @Provides
    @Singleton
    public RetrofitService provideRetrofitService() {
        String baseUrl = _context.getString(R.string.kakapo_server_base_url);
        return RetrofitServiceGenerator.createService(RetrofitService.class,
                baseUrl,
                BuildConfig.DEBUG);
    }

    @Provides
    @Singleton
    public EntityDataStore<Persistable> provideEntityStore() {
        String databaseName = _context.getString(R.string.local_database_name);
        DatabaseSource source = new DatabaseSource(_context, Models.DEFAULT, databaseName, 2);
        source.setLoggingEnabled(true);
        if (BuildConfig.DEBUG) {
            source.setTableCreationMode(TableCreationMode.CREATE_NOT_EXISTS);
        }
        Configuration configuration = source.getConfiguration();
        return new EntityDataStore<>(configuration);
    }

//    @Provides
//    @Singleton
//    public ReactiveEntityStore<Persistable> provideReactiveEntityStore()
//    {
//        String databaseName = _context.getString(R.string.local_database_name);
//        DatabaseSource source = new DatabaseSource(_context, Models.DEFAULT, databaseName, 1);
//        source.setLoggingEnabled(true);
//        if (BuildConfig.DEBUG) {
//            // use this in development mode to drop and recreate the tables on every upgrade
//            source.setTableCreationMode(TableCreationMode.DROP_CREATE);
//        }
//        Configuration configuration = source.getConfiguration();
//        return ReactiveSupport.toReactiveStore(new EntityDataStore<Persistable>(configuration));
//    }

}
