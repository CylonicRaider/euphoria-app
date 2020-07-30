package io.euphoria.xkcd.app.ui.event

/** Created by Xyzzy on 2017-02-26.  */ /* Event encapsulating the intent to change to another nick */
interface NewNickEvent : UIEvent {
    /* The nickname to change to */
    val newNick: String?
}
