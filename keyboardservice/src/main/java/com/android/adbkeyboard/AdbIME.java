package com.android.adbkeyboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import java.io.*;

import android.os.Environment;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.Toast;

import com.android.adbkeyboard.hook.WechatDatabaseHook;

import de.robv.android.xposed.XposedBridge;

public class AdbIME extends InputMethodService {
    private String IME_MESSAGE = "ADB_INPUT_TEXT";
    private String IME_CHARS = "ADB_INPUT_CHARS";
    private String IME_KEYCODE = "ADB_INPUT_CODE";
    private String IME_EDITORCODE = "ADB_EDITOR_CODE";
    private String IME_MESSAGE_B64 = "ADB_INPUT_B64";
    private String IME_SDCARDTEXTFILE = "ADB_INPUT_SF";

    private String IME_ADBHOOKSF = "ADB_HOOK_SF";

    private BroadcastReceiver mReceiver = null;

    final String ACTION_EMU_IME_ACTION = "android.intent.action.EMU_IME_ACTION";
    final String ACTION_EMU_IME_NOTICE = "android.intent.action.EMU_IME_NOTICE";

    @Override
    public View onCreateInputView() {
        View mInputView = getLayoutInflater().inflate(R.layout.view, null);

        if (mReceiver == null) {
            IntentFilter filter = new IntentFilter(IME_MESSAGE);
            filter.addAction(IME_CHARS);
            filter.addAction(IME_KEYCODE);
            filter.addAction(IME_EDITORCODE);
            filter.addAction(IME_MESSAGE_B64);
            filter.addAction(IME_SDCARDTEXTFILE);
            filter.addAction(ACTION_EMU_IME_ACTION);

            filter.addAction(IME_ADBHOOKSF);

            mReceiver = new AdbReceiver();
            registerReceiver(mReceiver, filter);
        }

        return mInputView;
    }

    public void onDestroy() {
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    void commit(String paramString) {
        InputConnection localInputConnection = getCurrentInputConnection();
        if ((paramString.indexOf("\b") != -1) || (paramString.indexOf("#ENTER#") != -1)) {

            paramString = paramString.replace("#ENTER#", "\f");

            for (int i = 0; i < paramString.length(); i++) {
                String str = paramString.substring(i, i + 1);
                if (str.equals("\b")) {
                    localInputConnection.deleteSurroundingText(1, 0);
                }

                if (str.equals("\f")) {
                    long l = SystemClock.uptimeMillis();
                    localInputConnection.sendKeyEvent(new KeyEvent(l, l, 0, 66, 0, 0, 0, 0, 6));
                    localInputConnection.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), l, 1, 66, 0, 0, 0, 0, 6));
                } else {
                    localInputConnection.commitText(str, 1);
                }

            }
        } else {

            getCurrentInputConnection().commitText(paramString, 1);

        }
    }

    void composing(String paramString) {
        getCurrentInputConnection().setComposingText(paramString, 1);
    }

    class AdbReceiver extends BroadcastReceiver {
        public String readExternal(Context context, String filename) {
            try {
                StringBuilder sb = new StringBuilder("");
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {


                    filename = "/sdcard" + File.separator + filename;
                    //打开文件输入流
                    FileInputStream inputStream = new FileInputStream(filename);
                    File fi = new File(filename);
                    int length = (int) fi.length();
                    byte[] buffer = new byte[length];
                    int len = inputStream.read(buffer);
                    //读取文件内容
                    while (len > 0) {
                        sb.append(new String(buffer, 0, len, "UTF-8"));

                        //继续将数据放到buffer中
                        len = inputStream.read(buffer);
                    }
                    //关闭输入流
                    inputStream.close();
                }
                return sb.toString();
            } catch (Exception anyerror) {
                return anyerror.getMessage();

            }

        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(IME_MESSAGE)) {
                String msg = intent.getStringExtra("msg");
                if (msg != null) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null)
                        ic.commitText(msg, 1);
                }
            }

            if (intent.getAction().equals(IME_MESSAGE_B64)) {
                String data = intent.getStringExtra("msg");

                byte[] b64 = Base64.decode(data, Base64.DEFAULT);
                String msg = "NOT SUPPORTED";
                try {
                    msg = new String(b64, "UTF-8");
                } catch (Exception e) {

                }

                if (msg != null) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null)
                        ic.commitText(msg, 1);
                }
            }

            if (intent.getAction().equals(IME_CHARS)) {
                int[] chars = intent.getIntArrayExtra("chars");
                if (chars != null) {
                    String msg = new String(chars, 0, chars.length);
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null)
                        ic.commitText(msg, 1);
                }
            }
            if (intent.getAction().equals(IME_SDCARDTEXTFILE)) {
                String data = intent.getStringExtra("msg");
                String msg = readExternal(context, data);
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(msg, 1);
                    long l = SystemClock.uptimeMillis();
                    ic.sendKeyEvent(new KeyEvent(l, l, 0, 66, 0, 0, 0, 0, 6));
                    ic.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), l, 1, 66, 0, 0, 0, 0, 6));
                    //Toast.makeText("auto complete");
                    //ic.commitText("complete",1);
                }
            }
            if (intent.getAction().equals(IME_KEYCODE)) {
                int code = intent.getIntExtra("code", -1);
                if (code != -1) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null)
                        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
                }
            }

            if (intent.getAction().equals(IME_EDITORCODE)) {
                int code = intent.getIntExtra("code", -1);
                if (code != -1) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null)
                        ic.performEditorAction(code);
                }
            }
            if (intent.getAction().equals(ACTION_EMU_IME_ACTION)) {
                InputConnection ic = getCurrentInputConnection();
                //ic.commitText("leidian ime",1);
                String newcontext = "";
                try {


                    int i = intent.getIntExtra("actionId", -1);
                    newcontext = intent.getStringExtra("text");
                    //ic.commitText(i+"" ,1);
                    // ic.commitText(newcontext,1);
                    switch (i) {
                        case 1:
                            AdbIME.this.composing(newcontext);
                            return;
                        case 8:
                            //ic.commitText(newcontext, 1);
                            long l = SystemClock.uptimeMillis();
                            ic.sendKeyEvent(new KeyEvent(l, l, 0, 66, 0, 0, 0, 0, 6));
                            ic.sendKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), l, 1, 66, 0, 0, 0, 0, 6));

                    }
                } catch (Exception paramAnonymousContext) {
                    paramAnonymousContext.printStackTrace();
                    return;
                }
                AdbIME.this.commit(newcontext);
                return;

            }//ACTION_EMU_IME_ACTION
            if (intent.getAction().equals(IME_ADBHOOKSF)) {
                String datamsg = intent.getStringExtra("file");
                String msg = readExternal(context, datamsg);

                String datasql = intent.getStringExtra("sql");
                String sql = readExternal(context, datasql);
                boolean SelectSuccess = false;
                for (int fileindex = 0; fileindex < WechatDatabaseHook.INSTANCE.getWechatdbs().size(); fileindex++) {
                    if (SelectSuccess) {
                        break;
                    }
                    if (WechatDatabaseHook.INSTANCE.getWechatdbs().get(fileindex).getDbpath().contains(msg)) {
                        try {
                            android.database.Cursor cur = ((android.database.sqlite.SQLiteDatabase) WechatDatabaseHook.INSTANCE.getWechatdbs().get(fileindex).getDb()).rawQuery(sql, null);
                            SelectSuccess = true;
                            XposedBridge.log("查询成功");

                        }
                        catch (Exception AnyError)
                        {
                             XposedBridge.log(AnyError.getMessage());
                        }

                    }
                }
                if (SelectSuccess==false)
                {
                    XposedBridge.log("查询失败或没有SQLiteDataBase");
                }


            }//fun dend
        }//onreceive

    }//receiver


}//inputservice

