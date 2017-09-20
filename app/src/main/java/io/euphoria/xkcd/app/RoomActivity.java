package io.euphoria.xkcd.app;

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.InputBar;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;
import io.euphoria.xkcd.app.impl.ui.MessageListAdapter;

public class RoomActivity extends FragmentActivity {

    // Tag for finding RoomControllerFragment
    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();
    private static final Pattern ROOM_PATH_RE = Pattern.compile("^/room/(.*?)/?$", Pattern.CASE_INSENSITIVE);

    private RoomControllerFragment rcf;
    private RoomController roomController;

    private RoomUIImpl roomUI;
    private RecyclerView recyclerView;
    private InputBar inputBar;
    private MessageListAdapter rmla;

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

        // Get RoomControllerFragment
        FragmentManager fm = getSupportFragmentManager();
        rcf = (RoomControllerFragment) fm.findFragmentByTag(TAG_ROOM_CONTROLLER_FRAGMENT);

        // create the fragment and data the first time
        if (rcf == null) {
            // add the fragment
            rcf = new RoomControllerFragment();
            fm.beginTransaction().add(rcf, TAG_ROOM_CONTROLLER_FRAGMENT).commit();
            fm.executePendingTransactions();
        }
        // Acquire RoomController
        roomController = rcf.getRoomController();

        // View setup
        recyclerView = (RecyclerView) findViewById(R.id.message_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inputBar = (InputBar) inflater.inflate(R.layout.input_bar, null);
        FrameLayout topLvlEntryWrp = (FrameLayout) findViewById(R.id.top_lvl_entry);
        inputBar.init();
        topLvlEntryWrp.addView(inputBar);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = getIntent();
        // Check passed path with ROOM_PATH_RE, if it doesn't match return to the MainActivity
        Matcher m = ROOM_PATH_RE.matcher(i.getData().getPath());
        if (Intent.ACTION_VIEW.equals(i.getAction()) && isEuphoriaURI(i.getData()) && m.matches()) {
            roomUI = (RoomUIImpl) roomController.getManager().getRoomUI(m.group(1));
            rmla = new MessageListAdapter();
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
