package com.aptasystems.kakapo;

import android.content.Context;

import com.aptasystems.kakapo.entities.Models;
import com.goterl.lazycode.lazysodium.LazySodiumAndroid;
import com.goterl.lazycode.lazysodium.SodiumAndroid;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.requery.Persistable;
import io.requery.TransactionListener;
import io.requery.android.sqlite.DatabaseSource;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.EntityStateListener;
import io.requery.sql.StatementListener;
import io.requery.sql.TableCreationMode;
import io.requery.util.function.Supplier;
import kakapo.client.retrofit.RetrofitService;
import kakapo.client.retrofit.RetrofitServiceGenerator;
import kakapo.crypto.ICryptoService;
import kakapo.crypto.LibSodiumCrypto;

@Module
public class KakapoModule {

    private KakapoApplication _application;

    public KakapoModule(KakapoApplication application) {
        _application = application;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return _application;
    }

    @Provides
    @Singleton
    public KakapoApplication provideApplication() {
        return _application;
    }

    @Provides
    public EventBus provideEventBus() {
        return EventBus.getDefault();
    }

    @Provides
    @Singleton
    public ICryptoService provideCryptoService() {
        return new LibSodiumCrypto(new LazySodiumAndroid(new SodiumAndroid()));
    }

    @Provides
    @Singleton
    public RetrofitService provideRetrofitService() {
        String baseUrl = _application.getString(R.string.kakapo_server_base_url);
        return RetrofitServiceGenerator.createService(RetrofitService.class,
                baseUrl,
                BuildConfig.DEBUG);
    }

    @Provides
    @Singleton
    public EntityDataStore<Persistable> provideEntityStore() {

        String databaseName = _application.getString(R.string.local_database_name);
        DatabaseSource source = new DatabaseSource(_application, Models.DEFAULT, databaseName, 3);
        if (BuildConfig.DEBUG) {
            source.setTableCreationMode(TableCreationMode.DROP_CREATE);
        }
        Configuration configuration = source.getConfiguration();

        // This is really kludgy, but I'm not sure how else to do this. All I want to do is set
        // the entity cache to null so that the cache is turned off, but since the configuration is
        // immutable, we have to create a new one. That's fine, but we want to use the values from
        // the database source. So we're essentially cloning the configuration, minus the entity
        // cache. GROSS.
        ConfigurationBuilder builder = new ConfigurationBuilder(configuration.getConnectionProvider(),
                configuration.getModel())
                .setBatchUpdateSize(configuration.getBatchUpdateSize())
                .setColumnTransformer(configuration.getColumnTransformer())
                .setEntityCache(null)
                .setMapping(configuration.getMapping())
                .setPlatform(configuration.getPlatform())
                .setQuoteColumnNames(configuration.getQuoteColumnNames())
                .setQuoteTableNames(configuration.getQuoteColumnNames())
                .setStatementCacheSize(configuration.getStatementCacheSize())
                .setTableTransformer(configuration.getTableTransformer())
                .setTransactionIsolation(configuration.getTransactionIsolation())
                .setTransactionMode(configuration.getTransactionMode())
                .setWriteExecutor(configuration.getWriteExecutor());
        for (EntityStateListener listener : configuration.getEntityStateListeners()) {
            builder.addEntityStateListener(listener);
        }
        for (StatementListener listener : configuration.getStatementListeners()) {
            builder.addStatementListener(listener);
        }
        for (Supplier<TransactionListener> listener : configuration.getTransactionListenerFactories()) {
            builder.addTransactionListenerFactory(listener);
        }

        return new EntityDataStore<>(builder.build());
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
