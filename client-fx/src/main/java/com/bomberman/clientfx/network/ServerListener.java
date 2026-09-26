package com.bomberman.clientfx.network;

import com.bomberman.common.message.NetworkMessage;

public interface ServerListener {

    void onMessage(NetworkMessage message);

    void onDisconnected();

    void onError(Throwable error);
}
