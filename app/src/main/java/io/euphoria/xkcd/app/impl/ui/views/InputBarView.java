package io.euphoria.xkcd.app.impl.ui.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import io.euphoria.xkcd.app.R;
import io.euphoria.xkcd.app.impl.ui.views.MessageListAdapter.InputBarDirection;

import static io.euphoria.xkcd.app.impl.ui.UIUtils.COLOR_SENDER_LIGHTNESS;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.hslToRgbInt;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.nickColor;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setColoredBackground;
import static io.euphoria.xkcd.app.impl.ui.UIUtils.setEnterKeyListener;

public class InputBarView extends BaseMessageView {

    public interface NickChangeListener {

        boolean onChangeNick(InputBarView view);

    }

    public interface SubmitListener {

        boolean onSubmit(InputBarView view);

    }

    private final MarginLayoutParams defaultLayoutParams;
    private String lastNick;
    private EditText nickEntry;
    private EditText messageEntry;
    private NickChangeListener nickChangeListener;
    private SubmitListener submitListener;

    public InputBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        defaultLayoutParams = BaseMessageView.getDefaultMargins(context, attrs);
        lastNick = "";
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        nickEntry = findViewById(R.id.nick_entry);
        messageEntry = findViewById(R.id.message_entry);
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
                // Recolor background
                int color = defaultColor;
                if (s.length() != 0)
                    color = nickColor(s.toString());
                setColoredBackground(nickEntry, R.drawable.bg_nick, color);
            }
        });
        setEnterKeyListener(nickEntry, EditorInfo.IME_ACTION_NEXT, new Runnable() {
            @Override
            public void run() {
                messageEntry.requestFocus();
            }
        });
        nickEntry.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) doNickChange();
            }
        });
        setEnterKeyListener(messageEntry, EditorInfo.IME_ACTION_SEND, new Runnable() {
            @Override
            public void run() {
                if (submitListener == null || submitListener.onSubmit(InputBarView.this))
                    messageEntry.setText("");
            }
        });
        setColoredBackground(nickEntry, R.drawable.bg_nick, defaultColor);
    }

    @Override
    protected void updateDisplay() {
        // TODO state restoration should go here
    }

    @Override
    public void recycle() {
        super.recycle();
        // TODO state saving should go here
    }

    public String getLastNick() {
        return lastNick;
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

    public NickChangeListener getNickChangeListener() {
        return nickChangeListener;
    }

    public void setNickChangeListener(NickChangeListener nickChangeListener) {
        this.nickChangeListener = nickChangeListener;
    }

    public String getNickText() {
        return nickEntry.getText().toString();
    }

    public String getMessageText() {
        return messageEntry.getText().toString();
    }

    public void setIndent(int indent) {
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp == null) {
            lp = new MarginLayoutParams(defaultLayoutParams);
            setLayoutParams(lp);
        }
        BaseMessageView.setMarginForIndent(getContext(), lp, indent);
    }

    public boolean requestEntryFocus() {
        if (nickEntry.getText().length() == 0) {
            return nickEntry.requestFocus();
        } else {
            return messageEntry.requestFocus();
        }
    }

    public boolean mayNavigateInput(InputBarDirection dir) {
        String text = messageEntry.getText().toString();
        if (dir == InputBarDirection.ROOT || text.isEmpty()) return true;
        int index = messageEntry.getSelectionStart(), indexEnd = messageEntry.getSelectionEnd();
        return index == indexEnd && (dir == InputBarDirection.UP || dir == InputBarDirection.DOWN) &&
                !text.contains("\n");
    }

    private void doNickChange() {
        // Strip whitespace
        Editable nickEditor = nickEntry.getText();
        String newNick = nickEditor.toString();
        String trimmedNick = newNick.trim();
        if (!newNick.equals(trimmedNick)) {
            nickEditor.replace(0, newNick.length(), trimmedNick);
        }
        // Invoke listener
        if (nickChangeListener != null && !nickChangeListener.onChangeNick(this)) {
            nickEntry.setText(lastNick);
        } else {
            lastNick = nickEntry.getText().toString();
        }
    }

}
