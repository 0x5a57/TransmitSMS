package com.example.transmitsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    IntentFilter filter;
    SmsReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        receiver = new SmsReceiver();
        registerReceiver(receiver, filter);//注册广播接收器

        TextView textView = findViewById(R.id.textView4);
        textView.setText(getActionMessage());

        EditText keywordEdit = findViewById(R.id.editText);
        keywordEdit.setText(getMessageKeyword());

        EditText phoneEdit = findViewById(R.id.editText2);
        StringBuilder phones = new StringBuilder();
        for (String phone : getPhones()) {
            phones.append(phone).append("#");
        }
        phoneEdit.setText(phones);
    }

    /**
     * 保存按钮执行的函数
     * 会把关键字和电话号码保存到本activity的配置文件中
     * @param view
     */
    public void saveButton(View view) {
        EditText keywordEdit = findViewById(R.id.editText);
        String messageKeyword = keywordEdit.getText().toString();
        EditText phoneEdit = findViewById(R.id.editText2);
        String[] phones = phoneEdit.getText().toString().split("#");

        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("messageKeyword", messageKeyword);
        editor.putStringSet("phones", new HashSet<>(Arrays.asList(phones)));
        editor.apply();

        TextView textView = findViewById(R.id.textView4);
        textView.setText(getActionMessage());
    }

    /**
     * 从本activity的配置中读取短信关键字和要转发的号码
     * 然后构造成一个字符串，用于展示
     */
    public StringBuilder getActionMessage() {
        String messageKeyword = getMessageKeyword();
        HashSet<String> phones = getPhones();

        StringBuilder actionMessage;
        if (messageKeyword.equals("")) {
            actionMessage = new StringBuilder("将把所有短信转发至：");
        } else {
            actionMessage = new StringBuilder("将把含有 " + messageKeyword + " 的短信转发至：");
        }
        for (String phone : phones) {
            actionMessage.append(phone).append(" ");
        }
        actionMessage.append("。");
        return actionMessage;
    }

    /**
     * 从本activity的配置文件中获取关键字
     * @return 返回关键字字符串
     */
    public String getMessageKeyword() {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getString("messageKeyword", "");
    }

    /**
     * 从本activity的配置文件中获取电话号码
     * @return 返回一个HashSet，保存了电话号码
     */
    public HashSet<String> getPhones() {
        SharedPreferences sharedPref = MainActivity.this.getPreferences(Context.MODE_PRIVATE);
        return (HashSet<String>) sharedPref.getStringSet("phones", null);
    }

    /**
     * 广播接收器
     * 重写onReceive函数，执行我们自己的动作
     */
    public class SmsReceiver extends BroadcastReceiver {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            StringBuilder content = new StringBuilder();//用于存储短信内容
            String sender = null;//存储短信发送方手机号
            Bundle bundle = intent.getExtras();//通过getExtras()方法获取短信内容
            String format = intent.getStringExtra("format");
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");//根据pdus关键字获取短信字节数组，数组内的每个元素都是一条短信
                for (Object object : pdus) {
                    SmsMessage message = SmsMessage.createFromPdu((byte[]) object, format);//将字节数组转化为Message对象
                    sender = message.getOriginatingAddress();//获取短信手机号
                    content.append(message.getMessageBody());//获取短信内容


                    TextView textView = findViewById(R.id.textView);
                    String messageBody = message.getMessageBody();
                    String messageKeyword = getMessageKeyword();
                    HashSet<String> phones = getPhones();
                    SmsManager smsManager = SmsManager.getDefault();

                    // 设置转发情况，如果关键字为空就转发全部短信
                    // 如果有关键字，则转发包含关键字的短信
                    if (messageKeyword.equals("")) {
                        sendMessageByPhones(smsManager, messageBody, phones);
                        textView.setText(messageBody);
                    } else if (messageBody.contains(messageKeyword)) {
                        sendMessageByPhones(smsManager, messageBody, phones);
                        textView.setText(messageBody);
                    }
                }
            }

        }
    }

    /**
     * 发送短信
     * @param smsManager
     * @param messageBody
     * @param phones
     */
    public void sendMessageByPhones(SmsManager smsManager, String messageBody, HashSet<String> phones) {
        for (String phone : phones) {
            smsManager.sendTextMessage(phone, null, messageBody, null, null);
        }
    }
}
