package io.euphoria.xkcd.app;

import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;
import io.euphoria.xkcd.app.impl.ui.RootMessageListAdapter;

public class RoomActivity extends FragmentActivity {

    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();
    private static final Pattern ROOM_PATH_RE = Pattern.compile("^/room/(.*?)/?$", Pattern.CASE_INSENSITIVE);

    private RoomControllerFragment rcf;
    private RoomController roomController;

    private RoomUIImpl roomUI;
    private RecyclerView recyclerView;
    private RootMessageListAdapter rmla;

    // TODO debugging test message
    private Message testMsg = new Message(){
        @Override
        public String getID() {
            return "x";
        }

        @Override
        public String getParent() {
            return null;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }

        @Override
        public SessionView getSender() {
            return new SessionView() {
                @Override
                public String getSessionID() {
                    return "bot:porpoise";
                }

                @Override
                public String getAgentID() {
                    return "x";
                }

                @Override
                public String getName() {
                    return "TellBot";
                }

                @Override
                public boolean isStaff() {
                    return false;
                }

                @Override
                public boolean isManager() {
                    return false;
                }
            };
        }

        @Override
        public String getContent() {
            return "Test message for testing porpoises. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin sed purus vel leo porta pulvinar a quis ex. Integer vestibulum. ";
        }

        @Override
        public boolean isTruncated() {
            return false;
        }
    };
    private Message testSubMsg = new Message(){
        @Override
        public String getID() {
            return "y";
        }

        @Override
        public String getParent() {
            return "x";
        }

        @Override
        public long getTimestamp() {
            return 1;
        }

        @Override
        public SessionView getSender() {
            return new SessionView() {
                @Override
                public String getSessionID() {
                    return "bot:porpoise";
                }

                @Override
                public String getAgentID() {
                    return "x";
                }

                @Override
                public String getName() {
                    return "TellBot";
                }

                @Override
                public boolean isStaff() {
                    return false;
                }

                @Override
                public boolean isManager() {
                    return false;
                }
            };
        }

        @Override
        public String getContent() {
            return "Test sub message for testing porpoises. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin sed purus vel leo porta pulvinar a quis ex. Integer vestibulum. ";
        }

        @Override
        public boolean isTruncated() {
            return false;
        }
    };
    private Message testSubSubMsg = new Message(){
        @Override
        public String getID() {
            return "z";
        }

        @Override
        public String getParent() {
            return "y";
        }

        @Override
        public long getTimestamp() {
            return 2;
        }

        @Override
        public SessionView getSender() {
            return new SessionView() {
                @Override
                public String getSessionID() {
                    return "bot:porpoise";
                }

                @Override
                public String getAgentID() {
                    return "x";
                }

                @Override
                public String getName() {
                    return "TellBot";
                }

                @Override
                public boolean isStaff() {
                    return false;
                }

                @Override
                public boolean isManager() {
                    return false;
                }
            };
        }

        @Override
        public String getContent() {
            return "Test sub message for testing porpoises. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin sed purus vel leo porta pulvinar a quis ex. Integer vestibulum. ";
        }

        @Override
        public boolean isTruncated() {
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);

        FragmentManager fm = getSupportFragmentManager();
        rcf = (RoomControllerFragment) fm.findFragmentByTag(TAG_ROOM_CONTROLLER_FRAGMENT);

        // create the fragment and data the first time
        if (rcf == null) {
            // add the fragment
            rcf = new RoomControllerFragment();
            fm.beginTransaction().add(rcf, TAG_ROOM_CONTROLLER_FRAGMENT).commit();
            fm.executePendingTransactions();
        }
        roomController = rcf.getRoomController();

        recyclerView = (RecyclerView) findViewById(R.id.message_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = getIntent();
        Matcher m = ROOM_PATH_RE.matcher(i.getData().getPath());
        if (Intent.ACTION_VIEW.equals(i.getAction()) && isEuphoriaURI(i.getData()) && m.matches()) {
            roomUI = (RoomUIImpl) roomController.getManager().getRoomUI(m.group(1));
            rmla = new RootMessageListAdapter();
            // TODO remove test messages
            rmla.add(testMsg);
            rmla.add(testSubMsg);
            rmla.add(testSubSubMsg);
            recyclerView.setAdapter(rmla);
        } else {
            Intent chooserIntent = new Intent(this, MainActivity.class);
            startActivity(chooserIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        roomController.shutdown();
    }

    /**
     * Checks if an URI describes a room of euphoria
     *
     * @param uri URI to check
     * @return True if URI describes an euphoria room
     */
    private boolean isEuphoriaURI(Uri uri) {
        return uri.getHost().toLowerCase().equals("euphoria.io");
    }
}
