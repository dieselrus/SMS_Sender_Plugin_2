package ru.dsoft38.sms_sender_plugin_2;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * Created by user on 25.03.2015.
 */
public class SendSMSService extends Service {

    // максимальное количество отправляемых сообщений для этого сервиса
    final String LOG_TAG = "Send SMS Service";
    private int maxSMSIndex = 100;

    // Флаги для отправки и доставки SMS
    String SENT_SMS_FLAG = "SENT_SMS";
    String DELIVER_SMS_FLAG = "DELIVER_SMS";

    PendingIntent sentPIn = null;
    PendingIntent deliverPIn = null;

    // Список телефонных номеров и текст сообщения
    String[] numList = null;
    String smsText = null;

    private int currentSMSNumberIndex = 0;

    // Для передачи данных обратно приложению
    Intent intentApp;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");

        Intent sentIn = new Intent(SENT_SMS_FLAG);
        sentPIn = PendingIntent.getBroadcast(this, 0, sentIn, 0);

        Intent deliverIn = new Intent(DELIVER_SMS_FLAG);
        deliverPIn = PendingIntent.getBroadcast(this, 0, deliverIn, 0);

        // Для передачи данных обратно приложению
        intentApp = new Intent("SMSSender");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        // Регистрация на оповещения об отправке и доставке СМС
        registerReceiver(sentReceiver, new IntentFilter(SENT_SMS_FLAG));
        registerReceiver(deliverReceiver, new IntentFilter(DELIVER_SMS_FLAG));

        sendDataToApp("SMSSenderServiceStatus", "servicestatus", "start");

        // Получаем переданные номера телефонов и текст СМС
        numList     = (String[])intent.getExtras().get("numberList");
        smsText     = (String)intent.getExtras().get("smsText");
        maxSMSIndex = numList.length;

        // Отправляем СМС
        sendSMS();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        // Уведомляем главное окно о том что сервиз остановлен (закрылся)
        sendDataToApp("SMSSenderServiceStatus", "servicestatus", "stop");
        // отмена регистрации на оповещение отправки и доставка СМС
        unregisterReceiver(sentReceiver);
        unregisterReceiver(deliverReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    void sendSMS() {
        // Завершаем сервис если отправили максимальное количество СМС (-1 потому что индекс в массиве начинается с 0)
        //if (currentSMSNumberIndex >= maxSMSIndex - 1)
        //        stopSelf();

        if ( currentSMSNumberIndex >= maxSMSIndex )
            return;

        if ( numList[currentSMSNumberIndex] == null | smsText == null )
            return;

        SmsManager smsManager = SmsManager.getDefault();
        // отправляем сообщение
        Log.d(LOG_TAG, "Отправляется сообщение №" + String.valueOf(currentSMSNumberIndex + 1) + " из " + String.valueOf(maxSMSIndex));

        // Удаляем не нужные символы
        String num = numList[currentSMSNumberIndex].replace("-", "").replace(";", "").replace(" ", "").trim();

        // Проверяем длину номера 11 символов или 12, если с +
        if (num.length() == 11 || (num.substring(0, 1).equals("+") && num.length() == 12)) {
            Log.d(LOG_TAG, "Отправляется");
            smsManager.sendTextMessage(num, null, smsText, sentPIn, deliverPIn);
            //smsManager.sendTextMessage("5556", null, smsText, null, null);
        }

        // Оповещаем приложение об отправке СМС
        sendDataToApp("SMSSenderSMSCount", "smscount", String.valueOf(currentSMSNumberIndex + 1));

    }

    BroadcastReceiver sentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent in) {
            //currentSMSNumberIndex++;

            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    // sent SMS message successfully;
                    //Toast toast = Toast.makeText(getApplicationContext(),"Сообщение отправлено!", Toast.LENGTH_SHORT);
                    //toast.show();
                    Log.d(LOG_TAG, "Сообщение отправлено!");
                    //currentSMSNumberIndex++;
                    //sendSMS();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF :
                    Log.d(LOG_TAG, "Телефонный модуль выключен!");
                    //sendSMS();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU :
                    Log.d(LOG_TAG, "Возникла проблема, связанная с форматом PDU (protocol description unit)!");
                    //sendSMS();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Log.d(LOG_TAG, "При отправке возникли неизвестные проблемы!");
                    //sendSMS();
                    break;
                default:
                    // sent SMS message failed
                    Log.d(LOG_TAG, "Сообщение не отправлено!");
                    break;
            }
        }
    };

    BroadcastReceiver deliverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent in) {
            // SMS delivered actions

            // Увеличиваем счетчик для номеров телефонов в списке
            currentSMSNumberIndex++;

            // В зависимости от ответа о доставке СМС выводим лог. Запускаем отправку следующего СМС (проверить как будет если номер отключен)
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    // sent SMS message successfully;
                    //Toast toast = Toast.makeText(getApplicationContext(), "Сообщение доставлено!", Toast.LENGTH_SHORT);
                    //toast.show();
                    Log.d(LOG_TAG, "Сообщение доставлено!");
                    sendSMS();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF :
                    Log.d(LOG_TAG, "!Телефонный модуль выключен!");
                    sendSMS();
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU :
                    Log.d(LOG_TAG, "!Возникла проблема, связанная с форматом PDU (protocol description unit)!");
                    sendSMS();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Log.d(LOG_TAG, "!При отправке возникли неизвестные проблемы!");
                    sendSMS();
                    break;
                default:
                    // sent SMS message failed
                    Log.d(LOG_TAG, "Сообщение не доставлено!");
                    sendSMS();
                    break;
            }

            // Завершаем сервис если отправили максимальное количество СМС )
            if (currentSMSNumberIndex >= maxSMSIndex) {
                sendDataToApp("SMSSenderServiceStatus", "endtask", "end");
                stopSelf();
            }
        }
    };

    // Отправка широковещательного сообщения
    private void sendDataToApp(String action, String name,String value){
        intentApp.setAction(action);
        intentApp.removeExtra(name);
        intentApp.putExtra(name, value);
        sendBroadcast(intentApp);
    }
}
