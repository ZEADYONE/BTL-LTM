package com.bomberman.client.state;

@FunctionalInterface
public interface ClientStateListener {

    void onClientStateChanged();
}
