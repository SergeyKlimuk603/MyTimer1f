package by.klimuk.mytimer1f;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static by.klimuk.mytimer1f.Conctants.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String LOG_TAG = "myLogs";
    //Log.d(LOG_TAG, "Создали активити");

    //настройки таймера по умолчанию
    private final int DEFAULT_ID = 0;
    private final String DEFAULT_NAME = "Default timer";
    private final String DEFAULT_MESSAGE = "Default message";
    private final int DEFAULT_DURATION = 10;

    //переменные доступа к view элементам
    LinearLayout timersList;//поле списка таймеров
    FrameLayout flMainMessBack;// поле подсветки основного сообщения
    FrameLayout flSoundOffBack;// поле подсветки кнопки октлючения звука
    TextView tvMainMess;//основное сообщение
    TextView btnSoundOff;//кнопка отключения звука
    TextView btnAdd;//кнопка добавления таймера

    //список таймеров
    HashMap<Integer, MyTimerFragment> timers;

    //проигрыватель звуковых файлов
    //MediaPlayer mp;

    //переменная онимации кнопок, если true - анимация включена
    private boolean animation = false;

    FragmentTransaction transaction;

//    MyTimerFragment t0;
//    MyTimerFragment t1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "MainActivit - onCreate");

        //создаем список для таймеров
        timers = new HashMap<Integer, MyTimerFragment>();
        //инициализация представлений
        initView();
        if (savedInstanceState == null) {
            Log.d(LOG_TAG, "MainActivit - onCreate savedInstanceState == null");
            //создаем новые таймеры
            createTimers();
        } else {
            Log.d(LOG_TAG, "MainActivit - onCreate savedInstanceState != null");
            //Log.d(LOG_TAG, "|||" + (((MyTimerFragment)getSupportFragmentManager().findFragmentByTag(TIMER_TAG + 0)).getName()));

            int timersAmount = savedInstanceState.getInt(TIMER_TAG);

            for (int i = 0; i < timersAmount; i++) {
                //получаем таг фрагмента по порядковому номеру записанному в Bundle
                String tag = savedInstanceState.getString(TIMER_TAG + i);
                Log.d(LOG_TAG, "??? tag" + i + " = " + tag);

                //получаем ссылку на новый фрагмент таймера по номеру тага
                MyTimerFragment t = ((MyTimerFragment)getSupportFragmentManager().
                        findFragmentByTag(tag));
                Log.d(LOG_TAG, "MainActivit - onCreate t.getTimerId() = " + t.getTimerId());
                timers.put(t.getTimerId(), t);
            }
            Log.d(LOG_TAG, "?MainActivit - onCreate timers.size() = " + timers.size());

        }
    }

    protected  void onSaveInstanceState(Bundle sis) {
        super.onSaveInstanceState(sis);
        Log.d(LOG_TAG, "MainActivity onSaveInstanceState");
        //определяем сколько таймеров содержится в списке (чтобы знать сколько их искать после поворота экрана)
        int timersAmount = timers.size();
        sis.putInt(TIMER_TAG, timersAmount);//Для простоты используем в качестве ключа константу TIMER_TAG не добавляя индекс.
        int i = 0;
        for (Map.Entry<Integer, MyTimerFragment> item : timers.entrySet()) {//перебираем список таймеров
            String s = item.getValue().getTag();//таг элемента
            //передаем таг в Bundle здесь (TIMER_TAG + i) - порядковый номер тага в списке таймеров
            // он не обязан совпадать с самим тагом таймера, - это нормально
            sis.putString(TIMER_TAG + i, s);//передаем таг в Bundle здесь (TIMER_TAG + i) может не совпадать с тагом таймера, это нормально
            Log.d(LOG_TAG, "|?|" + TIMER_TAG + i + " = " + s + ", timers.size() = " + timers.size());
            i++;//берем следующий элемент
        }
    }

    private void initView() {
        timersList = (LinearLayout) findViewById(R.id.timersList);
        flMainMessBack = (FrameLayout) findViewById(R.id.flMainMessBack);
        flSoundOffBack = (FrameLayout) findViewById(R.id.flSoundOffBack);
        tvMainMess = (TextView) findViewById(R.id.tvMainMess);
        btnSoundOff = (TextView) findViewById(R.id.btnSoundOff);
        btnSoundOff.setOnClickListener(this);
        btnAdd = (TextView) findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(this);
    }

    private void createTimers() {
        //создаем объекты для работы с файлом сохранения таймеров
        SharedPreferences sPref = getSharedPreferences(SAVE_FILE_NAME, MODE_PRIVATE);
        for(int i = 0; i < MAX_TIMERS_AMOUNT; i++) {
            //проверяем есть ли в записях таймер с id == i, если нет - пропускаем этот id
            int id = sPref.getInt(TIMER_ID + i, -1);
            if(id == -1) {continue;}
            String name = sPref.getString(TIMER_NAME + i, DEFAULT_NAME);
            String mess = sPref.getString(TIMER_MESSAGE + i, DEFAULT_MESSAGE);
            int dur = sPref.getInt(TIMER_DURATION + i, DEFAULT_DURATION);
            createTimer(i, name, mess, dur);
        }
    }

    //создаем и добавляем новый таймер в файл
    private void addNewTimer() {
        //ищем свободный id для нового таймера
        int freeId = findFreeId();
        Log.d(LOG_TAG, "Свободный id = " + freeId);
        //если свободного id нет, то выводим сообщение, что создано максимальное количество
        // таймеров и выходим из метода
        if(freeId >= MAX_TIMERS_AMOUNT) {
            Toast.makeText(this, getResources().getText(R.string.max_timers_amount) +
                    " = " + MAX_TIMERS_AMOUNT, Toast.LENGTH_LONG).show();
            return;
        }
        //создаем таймер с полученным свободным id
        createTimer(freeId, DEFAULT_NAME, DEFAULT_MESSAGE, DEFAULT_DURATION);
        //сохраняем новый таймер в файл
        saveTimer(timers.get(freeId));
    }

    //создаем новый таймер с заданным id
    private void createTimer(int _id, String _name, String _message, int _duration) {
        //создаем новый таймер
        MyTimerFragment t = new MyTimerFragment(_id, _name, _message, _duration);
        //помещаем его в список таймеров
        timers.put(_id, t);
        //добавляем новый таймер на экран
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(timersList.getId(), t, TIMER_TAG + _id);
        transaction.commit();
    }

    //ищем сободный id для таймера
    private int findFreeId() {
        //создаем объекты для работы с файлом сохранения таймеров
        SharedPreferences sPref = getSharedPreferences(SAVE_FILE_NAME, MODE_PRIVATE);
        //SharedPreferences.Editor editor = sPref.edit();
        for(int i = 0; i < MAX_TIMERS_AMOUNT; i++) {
            //считываем имеющиеся id из файла, если id отсутствует,
            //значит от свободен, метод возвращает номер этого id, для создания нового таймера
            int id = sPref.getInt(TIMER_ID + i, -1);
            if (id == -1) {
                return i;
            }
        }
        //если все id заняты перезаписываем таймер с id 100, даже если он существует
        return MAX_TIMERS_AMOUNT;
    }

    //сохранение настроек таймера в файл
    public void saveTimer(MyTimerFragment t) {
        SharedPreferences sPref = getSharedPreferences(SAVE_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putInt((TIMER_ID + t.getTimerId()), t.getTimerId());
        editor.putString((TIMER_NAME + t.getTimerId()), t.getName());
        editor.putString((TIMER_MESSAGE + t.getTimerId()), t.getMessage());
        editor.putInt((TIMER_DURATION + t.getTimerId()), t.getDuration());
        editor.apply();
    }

    //удаляем таймер
    public void delTimer(int _id) {
        //создаем объекты для работы с файлом сохранения таймеров
        SharedPreferences sPref = getSharedPreferences(SAVE_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sPref.edit();
        //удаляем таймер из файла
        editor.remove(TIMER_ID + _id);
        editor.remove(TIMER_NAME + _id);
        editor.remove(TIMER_MESSAGE + _id);
        editor.remove(TIMER_DURATION + _id);
        editor.apply();

        //удаляем таймер с экрана
        transaction = getSupportFragmentManager().beginTransaction();
        transaction.remove(timers.get(_id));
        transaction.commit();
        //удаляем таймер из списка
        timers.remove(_id);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAdd://нажата кнопка добавления таймера
                addNewTimer();//создать и добавить новый таймер
                break;
            case R.id.btnSoundOff://нажата кнопка выключения звука
                //stopSound();//выключить звук
        }
    }

    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "MainActivity - onStop");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "MainActivity - onDestroy");
        Log.d(LOG_TAG, " ");
    }
}