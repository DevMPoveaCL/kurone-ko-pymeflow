package com.kuroneko.pymeflow.application.port.out;

public interface BankProviderPort {
    ProviderSyncPage fetchStatements(ProviderSyncQuery query, ProviderAuth auth);
}
