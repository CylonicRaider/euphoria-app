# Euphoria App Control Flow

1.  UI gets instantiated.
2.  UI immediately instantiates `RoomController`.
3.  UI delivers a room joining event (delayed until now if necessary) to `RoomController`.
4.  `RoomController` (lazily) starts a background `Service` to maintain the connections.
5.  `RoomController` binds to the service and requests a connection to the room from above.
6.  The service grants the connection request.
7.  `RoomController` forwards events between both endpoints.
8.  If the user intentionally closes the room, the controller terminates the connection immediately.
9.  Destruction of the `Activity` is propagated to the `RoomController.shutdown` method.
10. If the `Activity` is destroyed without further notice, a temporary event is assumed, and the connection(s) are
    held open for some time.
11. If the activity is restored within the timeout, its `RoomController` binds to the service again.
12. Otherwise, the connection(s) are closed.
13. When no connections are left, the background service terminates itself.
14. Service shutdown is propagated to the `ConnectionManager.shutdown` method.
