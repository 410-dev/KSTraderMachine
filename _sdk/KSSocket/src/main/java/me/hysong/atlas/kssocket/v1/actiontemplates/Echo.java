package me.hysong.atlas.kssocket.v1.actiontemplates;

import me.hysong.atlas.kssocket.v1.KSSocket;

import java.io.IOException;
import java.io.Serializable;

public class Echo implements KSSocket.AcceptAction {
    @Override
    public Serializable run(KSSocket.Request request, Object decodedPayload) throws IOException {
        System.out.println(decodedPayload);
        return (Serializable) decodedPayload;
    }
}
