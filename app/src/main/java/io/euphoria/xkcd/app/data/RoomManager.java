package io.euphoria.xkcd.app.data;

/* Created by Xyzzy on 2017-02-24. */

/* The entry point for this submodule */
public interface RoomManager {

    /* Get room for name
     *
     * If there is no Room for the name as of yet, a new one is created.
     */
    Room getRoom(String name);

}
