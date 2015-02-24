package com.hibo.imitateime;

import java.util.ArrayList;
import java.util.List;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class ImiateIME extends InputMethodService implements KeyboardView.OnKeyboardActionListener 
{
	private final String TAG = ImiateIME.class.getSimpleName();
	private SkbView mSkbView;
	private SoftKeyboard mQWERTYKeyboard;
	private SoftKeyboard mSymbolsKeyboard;
	private SoftKeyboard mCurrKeyboard;
	private StringBuilder mInputContents = new StringBuilder();
	private boolean mPredictionOn = false;
	private boolean mCompletionOn = false;
	private String mWordSeparators;
	public static final int KEYCODE_SWITCHER = Keyboard.KEYCODE_ALT -1;
	public static final int KEYCODE_VOICE = KEYCODE_SWITCHER -1;
	
	@Override
	public void onInitializeInterface() {
		super.onInitializeInterface();
		mQWERTYKeyboard = new SoftKeyboard(this, R.xml.qwerty);
		mSymbolsKeyboard = new SoftKeyboard(this, R.xml.symbols);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mWordSeparators = getResources().getString(R.string.word_separators);
	}

	@Override
	public View onCreateInputView() {
		mSkbView = (SkbView) getLayoutInflater().inflate(R.layout.skb_view, null);
		mSkbView.setOnKeyboardActionListener(this);
		mSkbView.setKeyboard(mQWERTYKeyboard);
		return mSkbView;
	}

	@Override
	public View onCreateCandidatesView() {
		Log.d(TAG, "onCreateCandidatesView");
		return super.onCreateCandidatesView();
	}
	
	@Override
	public void onStartInput(EditorInfo attribute, boolean restarting) {
		super.onStartInput(attribute, restarting);
//		Log.d(TAG, "onStartInput EditorInfo type:"+attribute.inputType);
		mInputContents.setLength(0);
		mPredictionOn = false;
		
		switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) 
		{
		case EditorInfo.TYPE_CLASS_TEXT:
			mPredictionOn = true;
			mCurrKeyboard = mQWERTYKeyboard;
			
			if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                Log.e(TAG, " TYPE_TEXT_FLAG_AUTO_COMPLETE");
                mPredictionOn = false;
                mCompletionOn = isFullscreenMode();
            }
			break;
		default:
			mCurrKeyboard = mQWERTYKeyboard;
			break;
		}
		updateShiftKeyState(attribute);
	}
	
	private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null && mSkbView != null && mQWERTYKeyboard == mSkbView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
//            mSkbView.setShifted(mCapsLock || caps != 0);
        }
    }
	
	  /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mInputContents.length() > 0) {
            inputConnection.commitText(mInputContents, mInputContents.length());
            mInputContents.setLength(0);
            updateCandidates();
        }
    }
	
	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
//		Log.i(TAG, "onKey primaryCode="+primaryCode);
		if (isWordSeparator(primaryCode)) {
            // Handle separator
            if (mInputContents.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
        }
		else
		{
			switch(primaryCode)
			{
			case Keyboard.KEYCODE_DELETE:
				handleBackspace();
				break;
			case Keyboard.KEYCODE_SHIFT:
				handleShift();
				break;
			case Keyboard.KEYCODE_MODE_CHANGE:
				if(null != mSkbView)
					handleModeChanged();
				break;
			case KEYCODE_SWITCHER:
				Log.d(TAG, "KEYCODE_SWITCHER ");
				break;
			case KEYCODE_VOICE:
				Log.d(TAG, "KEYCODE_VOICE ");
				break;
			default:
				handleCharacter(primaryCode, keyCodes);
				break;
			}
		}
	}

	@Override
	public void onFinishInput() {
		super.onFinishInput();
		mInputContents.setLength(0);
	}
	
	private void handleBackspace() 
	{
		final int length = mInputContents.length();
		Log.e(TAG, "before length="+length);
		if(length > 1)
		{
			mInputContents.delete(length-1, length);
			getCurrentInputConnection().setComposingText(mInputContents, 1);
		}
		else if(length > 0)
		{
			mInputContents.setLength(0);
			getCurrentInputConnection().commitText("", 1);
		}
		else
		{
			keyDownUp(KeyEvent.KEYCODE_DEL);
		}
	}
	
	private void keyDownUp(int keyEventCode) {
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}
	
	private void handleShift() {
        if (mSkbView == null) {
            return;
        }
        
	}

	private void handleCharacter(int primaryCode, int[] keyCodes) {
		Log.d(TAG, "handleCharacter mPredictionOn="+mPredictionOn);
		if (isInputViewShown()) {
            if (mSkbView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
                Log.d(TAG, "primaryCode="+primaryCode);
            }
        }
		if(isAlphabet(primaryCode) && mPredictionOn)
		{
			mInputContents.append((char)primaryCode);
			Log.i(TAG, " mInputContents content= "+mInputContents.toString());
			getCurrentInputConnection().setComposingText(mInputContents, 1);
			updateShiftKeyState(getCurrentInputEditorInfo());
			updateCandidates();
		}
		else
		{
			getCurrentInputConnection().commitText(String.valueOf((char)primaryCode), 1);
		}
	}
	
	private void handleModeChanged()
	{
		Keyboard current = mSkbView.getKeyboard();
        if (current == mSymbolsKeyboard) {
            current = mQWERTYKeyboard;
        } else {
            current = mSymbolsKeyboard;
        }
        mSkbView.setKeyboard(current);
        if (current == mSymbolsKeyboard) {
            current.setShifted(false);
        }
	}

	private void updateCandidates() {
        if (!mCompletionOn) {
            if (mInputContents.length() > 0) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(mInputContents.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }
	
	public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
//        if (mCandidateView != null) {
//            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
//        }
    }
	private boolean isAlphabet(int code)
	{
//		if(Character.isLetter(code))
			return Character.isLetter(code) ? true : false;
	}
	
	private String getWordSeparators() {
        return mWordSeparators;
    }
	
	public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }
	
	/**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
            	Log.e(TAG, "enter ");
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }
    
	@Override
	public void onPress(int arg0) {
	}

	@Override
	public void onRelease(int arg0) {
	}

	@Override
	public void onText(CharSequence arg0) {
	}

	@Override
	public void swipeDown() {
	}

	@Override
	public void swipeLeft() {
	}

	@Override
	public void swipeRight() {
	}

	@Override
	public void swipeUp() {
	}
}
