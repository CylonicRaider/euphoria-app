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
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.euphoria.xkcd.app.control.RoomController;
import io.euphoria.xkcd.app.data.Message;
import io.euphoria.xkcd.app.data.SessionView;
import io.euphoria.xkcd.app.impl.ui.MessageContainer;
import io.euphoria.xkcd.app.impl.ui.RoomUIImpl;
import io.euphoria.xkcd.app.impl.ui.MessageTree;

public class RoomActivity extends FragmentActivity {

    private static final String TAG_ROOM_CONTROLLER_FRAGMENT = RoomControllerFragment.class.getSimpleName();
    private static final Pattern ROOM_PATH_RE = Pattern.compile("^/room/(.*?)/?$", Pattern.CASE_INSENSITIVE);

    private RoomControllerFragment rcf;
    private RoomController roomController;

    private RoomUIImpl roomUI;
    private RecyclerView recyclerView;

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
        if (Intent.ACTION_VIEW.equals(i.getAction()) && euphoriaURI(i.getData()) && m.matches()) {
            roomUI = (RoomUIImpl) roomController.getManager().getRoomUI(m.group(1));
            Toast.makeText(this, roomUI.getRoomName(), Toast.LENGTH_SHORT).show();
            recyclerView.setAdapter(new CustomAdapter());
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
    private boolean euphoriaURI(Uri uri) {
        return uri.getHost().toLowerCase().equals("euphoria.io");
    }

    class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MessageViewHolder> {

        private Message testMsg = new Message(){
            @Override
            public String getID() {
                return "y";
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

        private MessageTree[] messages = new MessageTree[] {
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg),
                new MessageTree(testMsg)
        };

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            MessageContainer mc = (MessageContainer) inflater.inflate(R.layout.template_message, null);
            return new MessageViewHolder(mc);
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            MessageContainer mc = ((MessageContainer) holder.itemView);
            mc.recycle();
            mc.setMessage(messages[position]);
        }

        @Override
        public int getItemCount() {
            return messages.length;
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {

            public MessageViewHolder(MessageContainer mc) {
                super(mc);
            }
        }
    }
}
