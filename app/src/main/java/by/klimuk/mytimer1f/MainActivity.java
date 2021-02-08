package by.klimuk.mytimer1f;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import static by.klimuk.mytimer1f.Conctants.*;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String LOG_TAG = "myLogs";
    //Log.d(LOG_TAG, "Создали активити");

    //переменные доступа к view элементам
    LinearLayout timersList;//поле списка таймеров
    FrameLayout flMainMessBack;// поле подсветки основного сообщения
    FrameLayout flSoundOffBack;// поле подсветки кнопки октлючения звука
    TextView tvMainMess;//основное сообщение
    TextView btnSoundOff;//кнопка отключения звука
    TextView btnAdd;//кнопка добавления таймера

    //переменная состояния главного экрана, используется при повороте экрана
    private static int mainState;

    //список таймеров
    private HashMap<Integer, MyTimerFragment> timers;

    //константы состояния главного экрана
    public static final int MAIN_STATE_REST = 0;
    public static final int MAIN_STATE_ALARM = 1;
    public static final int MAIN_STATE_WAIT = 2;

    //проигрыватель звуковых файлов
    public static MediaPlayer mp;

    //переменная онимации кнопок, если true - анимация включена
    private boolean animation = false;

    FragmentTransaction transaction;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "---MainActivity onCreate()");

        //создаем список для таймеров
        timers = new HashMap<Integer, MyTimerFragment>();
        //инициализация представлений
        initView();
        if (savedInstanceState == null) {
            SharedPreferences sPref = getSharedPreferences(SAVE_FILE_NAME, MODE_PRIVATE);
            mainState = sPref.getInt(MAIN_MESS_STATE, MAIN_STATE_REST);
            tvMainMess.setText(sPref.getString(MAIN_MESS, DEFAULT_MESSAGE));
            //создаем новые таймеры
            createTimers(sPref);
        } else {
            Log.d(LOG_TAG, "---MainActivity savedInstanceState() != null");
            //определяем количество таймеров
            int timersAmount = savedInstanceState.getInt(TIMER_TAG);//Для простоты используем в
            // качестве ключа константу TIMER_TAG не добавляя индекс.

            for (int i = 0; i < timersAmount; i++) {
                //получаем таг фрагмента по порядковому номеру записанному в Bundle
                String tag = savedInstanceState.getString(TIMER_TAG + i);

                //получаем ссылку на новый фрагмент таймера по номеру тага
                MyTimerFragment t = ((MyTimerFragment)getSupportFragmentManager().
                        findFragmentByTag(tag));
                timers.put(t.getTimerId(), t);
            }

            //эту переменную сделали статической, поэтому при повороте ее передавать не нужно
            //mainState = savedInstanceState.getInt(MAIN_STATE, MAIN_STATE_REST);
            tvMainMess.setText(savedInstanceState.getString(MAIN_MESS));
        }
        if (mainState == MAIN_STATE_ALARM) {
            frameLayoutAnim(flSoundOffBack);
            flMainMessBack.setBackgroundResource(R.drawable.border_solid);
        } else if (mainState == MAIN_STATE_WAIT) {
            flMainMessBack.setBackgroundResource(R.drawable.border_solid);
        }
    }

    protected  void onSaveInstanceState(Bundle sis) {
        Log.d(LOG_TAG, "---MainActivity onSaveInstanceState()");
        super.onSaveInstanceState(sis);
        //определяем сколько таймеров содержится в списке (чтобы знать сколько их искать после поворота экрана)
        int timersAmount = timers.size();
        sis.putInt(TIMER_TAG, timersAmount);//Для простоты используем в качестве ключа константу TIMER_TAG не добавляя индекс.
        int i = 0;
        for (Map.Entry<Integer, MyTimerFragment> item : timers.entrySet()) {//перебираем список таймеров
            String s = item.getValue().getTag();//таг элемента
            //передаем таг в Bundle здесь (TIMER_TAG + i) - порядковый номер тага в списке таймеров
            // он не обязан совпадать с самим тагом таймера, - это нормально
            sis.putString(TIMER_TAG + i, s);//передаем таг в Bundle здесь (TIMER_TAG + i) может не совпадать с тагом таймера, это нормально
            i++;//берем следующий элемент
        }

        //сохраняем текст отображаемый в главном сообщении
        sis.putString(MAIN_MESS, tvMainMess.getText().toString());
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

    private void createTimers(SharedPreferences _sPref) {
        //создаем объекты для работы с файлом сохранения таймеров

        for(int i = 0; i < MAX_TIMERS_AMOUNT; i++) {
            //проверяем есть ли в записях таймер с id == i, если нет - пропускаем этот id
            int id = _sPref.getInt(TIMER_ID + i, -1);
            if(id == -1) {continue;}
            String name = _sPref.getString(TIMER_NAME + i, DEFAULT_NAME);
            String mess = _sPref.getString(TIMER_MESSAGE + i, DEFAULT_MESSAGE);
            int dur = _sPref.getInt(TIMER_DURATION + i, DEFAULT_DURATION);
            int _time = _sPref.getInt(TIMER_TIME + i, -1);//считываем время таймера
            boolean _runTimer = _sPref.getBoolean(TIMER_RUN + i, false);
            long _startTime = _sPref.getLong(TIMER_START_TIME + i, 0L);
            long _timePause = _sPref.getLong(TIMER_TIME_PAUSE + i, 0L);
            long _startPause = _sPref.getLong(TIMER_START_PAUSE + i, 0L);

            createTimer(i, name, mess, dur, _time, _runTimer,  _startTime, _timePause, _startPause);
        }
    }

    //создаем и добавляем новый таймер в файл
    private void addNewTimer() {
        //ищем свободный id для нового таймера
        int freeId = findFreeId();
        //если свободного id нет, то выводим сообщение, что создано максимальное количество
        // таймеров и выходим из метода
        if(freeId >= MAX_TIMERS_AMOUNT) {
            Toast.makeText(this, getResources().getText(R.string.max_timers_amount) +
                    " = " + MAX_TIMERS_AMOUNT, Toast.LENGTH_LONG).show();
            return;
        }
        //создаем таймер с полученным свободным id
        createTimer(freeId, DEFAULT_NAME, DEFAULT_MESSAGE, DEFAULT_DURATION, DEFAULT_DURATION,
                false, 0L, 0L, 0L);
        //сохраняем новый таймер в файл
        saveTimer(timers.get(freeId));
    }

    //создаем новый таймер с заданным id
    private void createTimer(int _id, String _name, String _message, int _duration, int _time,
                             boolean _runTimer, long _startTime, long _timePause,
                             long _startPause) {
        //создаем новый таймер
        MyTimerFragment t = new MyTimerFragment(_id, _name, _message, _duration, _time,  _runTimer,
                _startTime, _timePause, _startPause);
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
        editor.remove(TIMER_RUN + _id);
        //editor.remove(TIMER_TIME + _id);
        editor.remove(TIMER_START_TIME + _id);
        editor.remove(TIMER_TIME_PAUSE + _id);
        editor.remove(TIMER_START_PAUSE + _id);
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
                stopSound();//выключить звук
        }
    }

    public void timerEnd(String _message) {
        //подсвечиваем панель главного сообщения
        flMainMessBack.setBackgroundResource(R.drawable.border_solid);
        //выводим сообщение таймера на панель главного сообщения
        tvMainMess.setText(_message);
        //меняем состояние главного экрана
        mainState = MAIN_STATE_ALARM;
        //включаем сигнал и анимацию
        alarmStart();
    }

    // сообщаем активности, что сбросили таймер. Это нужно для отключения сигнала, если сработавший
    // таймер был последним (определяем по сообщению таймера: если оно совпадает с тем, что
    // на общем экране - значит это последний сработавший таймер)
    public void timerReset(String _message) {
        //выключаем звук
        stopSound();
        //Если сообщения таймера и главное сообщение совпадают, сбрасываем подсветку главного сообщения
        String mess = tvMainMess.getText().toString();
        if (mess.equals(_message)) {
            //убираем фон
            flMainMessBack.setBackgroundColor(Color.BLACK);
            //удаляем сообщение
            tvMainMess.setText(getResources().getText(R.string.no_message));
            mainState = MAIN_STATE_REST;
        }
    }

    //начать воспроизведение сигнала
    private void playSound() {
        //включаем сигнал
        if (mp != null) {
            resetPlayer();
        }
        //создаем новый музыкальный проигрыватель
        mp = MediaPlayer.create(this, R.raw.music);
        mp.start();//начинаем воспроизведение
        //присваиваем слушателя, который определяет окончание композиции
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopSound();
            }
        });
    }

    private void alarmStart() {
        //запускаем звук
        playSound();
        //включаем пульсацию кнопки выключения сигнала SoundOff
        frameLayoutAnim(flSoundOffBack);
        mainState = MAIN_STATE_ALARM;
    }

    //остановить воспроизведение сигнала
    private void stopSound() {
        animation = false;//запретить анимацию кнопки выключения звука
        flSoundOffBack.setBackgroundColor(Color.BLACK);//убираем фон кнопки выключения звука
        resetPlayer();//выключаем звук и освобождаем ресурсы проигрывателя
        if (mainState ==  MAIN_STATE_ALARM) {
            mainState = MAIN_STATE_WAIT;
        }
    }

    private void resetPlayer() {
        if (mp != null) {
            mp.release();//освобождаем ресурсы старого музыкального проигрывателя
        }
            mp = null;//обнуляем ссылку чтобы отвязаться от старого плеера
    }

    private void frameLayoutAnim(FrameLayout fl) {
        if(animation) {return;}//если анимация уже запущена выходим из метода
        animation = true;//разрешаем анимацию
        //создаем анимацию в другом потоке
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //если анимация запрещена устанавливаем черный фон и покидаем метод
                if(animation == false) {
                    fl.setBackgroundColor(Color.BLACK);
                    return;
                }
                //создаем и начинаем анимацию
                Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.alpha);
                fl.setBackgroundResource(R.drawable.border_solid);//фон анимации
                fl.startAnimation(anim);
                handler.postDelayed(this, 2000);
            }
        });
    }

    public void onStart() {
        Log.d(LOG_TAG, "---MainActivity onStart()");
        super.onStart();
    }

    public void onResume() {
        Log.d(LOG_TAG, "---MainActivity onResume()");
        super.onResume();
    }

    public void onPause() {
        Log.d(LOG_TAG, "---MainActivity onPause()");
        super.onPause();
    }

    public void onStop() {
        Log.d(LOG_TAG, "---MainActivity onStop()");
        super.onStop();
    }

    public void onDestroy() {
        Log.d(LOG_TAG, "---MainActivity onDestroy()");
        animation = false;
        new Saver().start();
        super.onDestroy();
    }

    private class Saver extends Thread {
        public void run() {
            //Log.d(LOG_TAG, "---MainActivity Saver run() start");
            SharedPreferences sPref = getSharedPreferences(SAVE_FILE_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = sPref.edit();
            for (Map.Entry<Integer, MyTimerFragment> item : timers.entrySet()) {//перебираем список таймеров
                int id = item.getValue().getTimerId();
                editor.putBoolean(TIMER_RUN + id, item.getValue().isRunTimer());
                editor.putInt(TIMER_TIME + id, item.getValue().getTime());
                editor.putLong(TIMER_START_TIME + id, item.getValue().getStartTime());
                editor.putLong(TIMER_TIME_PAUSE + id, item.getValue().getTimePause());
                editor.putLong(TIMER_START_PAUSE + id, item.getValue().getStartPause());
            }
            editor.putInt(MAIN_MESS_STATE, mainState);
            editor.putString(MAIN_MESS, tvMainMess.getText().toString());
            editor.apply();
            //Log.d(LOG_TAG, "---MainActivity Saver run() stop");
        }
    }
}