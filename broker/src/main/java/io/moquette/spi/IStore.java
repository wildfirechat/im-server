package io.moquette.spi;


public interface IStore {

    IMessagesStore messagesStore();

    ISessionsStore sessionsStore();

    void close();

}
