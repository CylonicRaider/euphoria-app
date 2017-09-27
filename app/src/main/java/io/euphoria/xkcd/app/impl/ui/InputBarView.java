package io.euphoria.xkcd.app.impl.ui;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;

import io.euphoria.xkcd.app.R;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_LIGHTNESS;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.nickColor;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setEnterKeyListener;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setRoundedRectBackground;

public class InputBarView extends RelativeLayout {

    public interface SubmitListener {

        boolean onSubmit(InputBarView view);

    }

    private EditText nickEntry;
    private EditText messageEntry;
    private MessageTree tree;
    private SubmitListener submitListener;

    public InputBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        nickEntry = (EditText) findViewById(R.id.nick_entry);
        messageEntry = (EditText) findViewById(R.id.message_entry);
        // Color nick-background with unsaturated nickname color as default
        final int defaultColor = hslToRgbInt(0, 0, COLOR_SENDER_LIGHTNESS);
        nickEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // NOP
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // NOP
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Strip whitespace
                if (!s.toString().equals(s.toString().trim())) {
                    s.replace(0, s.length(), s.toString().trim());
                }
                // Recolor background
                int color = defaultColor;
                if (s.length() != 0)
                    color = nickColor(s.toString());
                setRoundedRectBackground(nickEntry, color);
            }
        });
        setEnterKeyListener(nickEntry, EditorInfo.IME_ACTION_NEXT, new Runnable() {
            @Override
            public void run() {
                messageEntry.requestFocus();
            }
        });
        setEnterKeyListener(messageEntry, EditorInfo.IME_ACTION_SEND, new Runnable() {
            @Override
            public void run() {
                if (submitListener == null || submitListener.onSubmit(InputBarView.this))
                    messageEntry.setText("");
            }
        });
        setRoundedRectBackground(nickEntry, defaultColor);
    }

    public MessageTree getTree() {
        return tree;
    }

    void setTree(MessageTree tree) {
        this.tree = tree;
    }

    public EditText getNickEntry() {
        return nickEntry;
    }

    public EditText getMessageEntry() {
        return messageEntry;
    }

    public SubmitListener getSubmitListener() {
        return submitListener;
    }

    public void setSubmitListener(SubmitListener submitListener) {
        this.submitListener = submitListener;
    }

    public String getNick() {
        return nickEntry.getText().toString();
    }

    public String getMessage() {
        return messageEntry.getText().toString();
    }

    public void setIndent(int indent) {
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp == null) {
            // HACK: Ignoring anything defined in the XML file...
            lp = new MarginLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            setLayoutParams(lp);
        }
        int margin = MessageView.computeIndentWidth(getContext(), indent);
        lp.setMargins(margin, 0, 0, 0);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            lp.setMarginStart(margin);
        }
    }

    public boolean requestEntryFocus() {
        if (nickEntry.getText().length() == 0) {
            return nickEntry.requestFocus();
        } else {
            return messageEntry.requestFocus();
        }
    }

}
