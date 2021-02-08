package by.klimuk.mytimer1f;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static by.klimuk.mytimer1f.Conctants.*;

public class MyTimerFragment extends Fragment implements View.OnClickListener, Runnable {

    //Создаем переменную лога для наладки класса
    private final String LOG_TAG = "myLogs";
    // Log.d(LOG_TAG, "sdkfj");

    //Переменные таймера
    private int id; // идентификатор таймера
    private String name; // имя таймера
    private String message; // сообщение выдаваемое таймером по окончании отсчета
    private int duration; // время отсчитываемое таймером
    private  int time;// текущее время таймера
    //private int lostTime;// время оставшееся для отсчет после запуска или перезапуска.
    // Если lostTime < 0 - таймер в состоянии ожидания команды запуска
    // Если lostTime = 0 - таймер завершил отсчет и ждет сброса
    // Если lostTime > 0 - таймер в состоянии отсчета времени или паузы (зависит от переменной runTimer)
    private long startTime;// системное время запуска или перезапуска таймера
    private long timePause;// системное время нахождения таймера в состоянии Pause
    private long startPause;//системное время постановки таймера в Pause
    private long endPause;//системное время снятия таймера с Pause
    private boolean runTimer; //флаг работы таймера. Если идет отсчет runTimer == true

    // Вспомогательные элементы таймера
    private Handler handler;// handler для отсчета времени
    private Converter timeConvert;// объект для преобразования времени в различные форматы

    // переменные доступа к view элементам таймера
    private FrameLayout layoutMain;// корнеове View для таймера
    private FrameLayout layoutMainBack;// подсветка корневого слоя в пределах границы border
    private TextView tvName;// имя таймера в верхнем левом углу
    private TextView tvDur;// длительность таймера в верхнем правом углу
    private TextView tvMess;// сообщения таймера в центре
    private LinearLayout layoutBtn;// слой для добавления в него кнопок
    private ImageView ivSet;// значек настройки таймера

    // переменные кнопок
    private Button btnStart;
    private Button btnReset;
    private Button btnPause;
    private Button btnCont;
    // идентификаторы кнопок
    private final int BTN_START_ID = 1;
    private final int BTN_RESET_ID = 2;
    private final int BTN_PAUSE_ID = 3;
    private final int BTN_CONT_ID = 4;
    //названия кнопок
    private String BTN_START_NAME;
    private String BTN_RESET_NAME;
    private String BTN_PAUSE_NAME;
    private String BTN_CONT_NAME;

    public MyTimerFragment() {
        // Required empty public constructor
    }
    public MyTimerFragment(int _id, String _name, String _message, int _dur, int _time,
                           boolean _runTimer, long _startTime, long _timePause, long _startPause ) {
        setTimerId(_id);
        setName(_name);
        setMessage(_message);
        setDuration(_dur);
        setTime(_time);
        setRunTimer(_runTimer);
        setStartTime(_startTime);
        setTimePause(_timePause);
        setStartPause(_startPause);
        Log.d(LOG_TAG, "Constructor " + name);
    }

    //имена кнопок берем из ресурса strings
    private void initBtnNames() {
        BTN_START_NAME = getResources().getString(R.string.start);
        BTN_RESET_NAME = getResources().getString(R.string.reset);
        BTN_PAUSE_NAME = getResources().getString(R.string.pause);
        BTN_CONT_NAME = getResources().getString(R.string.cont);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(getActivity(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getActivity(), id, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
        am.cancel(pIntent);
        Log.d(LOG_TAG, "_________________________________ " +
                "MyTimerFragment onCreate() удалили AlarmManeger " + name);

        //если фракгмент пересоздается - восстанавливаем даные
        if (savedInstanceState != null) {
            Log.d(LOG_TAG, "_________________________________ savedInstanceState != null");
            id = savedInstanceState.getInt(TIMER_ID);
            name = savedInstanceState.getString(TIMER_NAME);
            message = savedInstanceState.getString(TIMER_MESSAGE);
            duration = savedInstanceState.getInt(TIMER_DURATION);
            time = savedInstanceState.getInt(TIMER_TIME);
            startTime = savedInstanceState.getLong(TIMER_START_TIME);
            timePause = savedInstanceState.getLong(TIMER_TIME_PAUSE);
            startPause = savedInstanceState.getLong(TIMER_START_PAUSE);
            runTimer = savedInstanceState.getBoolean(TIMER_RUN);
        } else {
            //Log.d(LOG_TAG, "_________________________________ MyTimerFragment onCreate() startTime = " + startTime);
            //Log.d(LOG_TAG, "_________________________________ MyTimerFragment onCreate() runTimer = " + runTimer);
            //проверяем был ли таймер в состоянии Pause. Если был, то добавляем к timePause время,
            //которое приложение было в неактивном состоянии и обновляем startPause, как бы
            //запуская новый цикл паузы.
            if (time > 0 && time != duration && runTimer == false) {
                timePause = timePause + (System.currentTimeMillis()- startPause);
                startPause = System.currentTimeMillis();
                //Log.d(LOG_TAG, "_________________________________ MyTimerFragment onCreate() timePause = " + timePause);
            }
            //вычисляем оставшееся для отсчета время
            time = timeLeft();
            //Log.d(LOG_TAG, "_________________________________ MyTimerFragment onCreate() time = " + time);
        }
        timeConvert = new Converter();
        handler = new Handler();
    }

    private int timeLeft() {
        //если startTime == 0 значит таймер еще не запускали и он сброшен, возвращаем уставку таймера
        if (startTime == 0) {
            return duration;
        }
        int _time = duration - ((int) ((System.currentTimeMillis() - startTime) -
                timePause) / 1000);
        // если _time <= 0, значит время вышло. Нужно смотреть значение переменной таймера runTimer
        // для определения состояния таймера
        if (_time <= 0) {
            _time = 0;
        }
        return _time;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onCreateView() " + name);
        View mainView = inflater.inflate(R.layout.fragment_my_timer, container, false);
        layoutMainBack = (FrameLayout) mainView.findViewById(R.id.flMainBack);
        tvName = (TextView) mainView.findViewById(R.id.tvName);
        tvMess = (TextView) mainView.findViewById(R.id.tvMess);
        tvDur = (TextView) mainView.findViewById(R.id.tvDur);
        layoutBtn = (LinearLayout) mainView.findViewById(R.id.layoutBtn);
        ivSet = (ImageView) mainView.findViewById(R.id.ivSet);// добавляем значек для вызова меню настроек таймера
        ivSet.setOnClickListener(this);//  добавляем слушателя к значку настройки таймера


        initBtnNames();
        createButtons();

        //инициализация таймера при различных условиях
        if (time == duration && runTimer == false){//таймер сброшен
            initTimer(name, btnStart, null, R.color.transparent);
            //reset();
        } else if (time > 0 & runTimer == false) {//таймер в состоянии паузы
            initTimer(timeConvert.intToStringTime(time), btnCont, btnReset, R.color.transparent);
            //pause();
        }  else if (time == 0 && runTimer == false) {//таймер завершил отсчет и ждет сброса
            initTimer(message, btnReset, null, R.color.background_main);
            //endTime();
        } else {//таймер работает
            initTimer(timeConvert.intToStringTime(time), btnPause, btnReset, R.color.transparent);
            handler.post(this);
            //cont();
        }

        // Inflate the layout for this fragment
        return mainView;
    }


    private void initTimer(String mess, Button btn1, Button btn2, Integer background) {
        layoutMainBack.setBackgroundResource(background);
        tvName.setText(name);
        tvDur.setText(timeConvert.intToStringTime(duration));
        tvMess.setText(mess);
        if (btn2 == null){
            addButtons(btn1);
        } else {
            addButtons(btn1, btn2);
        }
        //ни в коем случае нельзя добавлять сюда такие строчки
//        runTimer = false;
//        lostTime = -1;
    }

    public void onStart() {
        super.onStart();
    }

    //создание кнопок таймера
    private void createButtons() {
        //создаем LayoutParams кнопок для дальнейшего использования
        LinearLayout.LayoutParams btnLP = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnLP.weight = 1;//кнопки равномерно распределяются в области layoutBtn
        btnLP.gravity = Gravity.CENTER;// надписи на кнопках располагаются в центре

        // создаем сами кнопки
        // надо попробовать сделать это через цикл с использованием массива названий чтобы убрать
        // повторяемость кода
        btnStart = createButton(BTN_START_NAME, BTN_START_ID, btnLP, R.drawable.border);
        btnReset = createButton(BTN_RESET_NAME, BTN_RESET_ID, btnLP, R.drawable.border_solid);
        btnPause = createButton(BTN_PAUSE_NAME, BTN_PAUSE_ID, btnLP, R.drawable.border_solid);
        btnCont = createButton(BTN_CONT_NAME, BTN_CONT_ID, btnLP, R.drawable.border_solid);
    }

    //метод создания одной кнопки
    private Button createButton(String _name, int _id, ViewGroup.LayoutParams _lp,
                                int _background) {
        Button btn = new Button(getActivity());// новая кнопка
        btn.setId(_id);// id кнопки
        btn.setText(_name);// название кнопки
        btn.setLayoutParams(_lp);// параметры расположения кнопки
        btn.setTextColor(getActivity().getResources().getColor(R.color.textColor));// цвет текста кнопки
        btn.setBackgroundResource(_background);// рамка вокруг кнопки
        btn.setOnClickListener(this);//добавляем данный класс слушателем кнопки
        return btn;
    }

    //пересоздаем панель кнопок
    private void addButtons(Button... btns) {//получаем набор кнопок btns
        layoutBtn.removeAllViews();//удаляем старые кнопки
        //добавляем новые
        for (Button btn : btns) {
            layoutBtn.addView(btn);
        }
    }

    //обработка нажатия на кнопки
    public void onClick(View v) {
        switch (v.getId()) {
            case BTN_START_ID: //нажата кнопка Start
                start();
                break;
            case BTN_RESET_ID: //нажата кнопка Stop
                reset();
                break;
            case BTN_PAUSE_ID: //нажата кнопка Pause
                pause();
                break;
            case BTN_CONT_ID: //нажата кнопка Continue
                cont();
                break;
            case R.id.ivSet:
                callMenu();// вызвать меню таймера
                break;
            default:
                break;
        }
    }

    //запустить отсчет
    private void start() {
        addButtons(btnPause, btnReset);//обновляем кнопки
        runTimer = true;// таймер считает
        //lostTime = duration; //начало отсчета, оставшееся для отсчета время
        startTime = System.currentTimeMillis();// системное время при пуске или перезапуске таймера
        handler.post(this);//запускаем отсчет
        ivSet.setClickable(false);
        ivSet.setVisibility(View.GONE);
    }

    //сброс таймера
    private void reset() {
        runTimer = false;// останавливаем таймер
        //устанавливаем кнопку старт
        addButtons(btnStart);//обновляем кнопки
        //сбрасываем сообщение таймера вместо сообщения выводим имя
        tvMess.setText(name);
        //убираем фон
        layoutMainBack.setBackgroundResource(R.color.transparent);
        //таймер в ожидании комманды запуска
        time = duration;
        startTime = 0L;
        timePause = 0L;
        startPause = 0L;
        // сообщаем активности, что сбросили таймер. Это нужно для отключения сигнала, если сработавший
        // таймер был последним (определяем по сообщению таймера: если оно совпадает с тем, что
        // на общем экране - значит это последний сработавший таймер)
        ((MainActivity) getActivity()).timerReset(message);
        ivSet.setClickable(true);
        ivSet.setVisibility(View.VISIBLE);
    }

    //приостановить отсчет времени
    private void pause() {
        addButtons(btnCont, btnReset);//обновляем кнопки
        runTimer = false;// останавливаем таймер
        startPause = System.currentTimeMillis();
        //lostTime = time;// запоминаем оставшееся для отсчета время
        tvMess.setText(timeConvert.intToStringTime(time));//эта строчка нужна при повороте экрана, а может и нет
    }

    //продолжить отсчет времени
    private void cont() {
        //lostTime = time;// запоминаем оставшееся для отсчета время yfljkasjdf
        addButtons(btnPause, btnReset);//обновляем кнопки
        // продолжаем остчет времени
        endPause = System.currentTimeMillis();
        timePause = timePause + (endPause - startPause);
        runTimer = true;
        handler.post(this);
    }

    // вызываем меню таймера
    private void callMenu() {
        Intent intent = new Intent(getActivity(), TimerMenu.class);
        intent.putExtra(TIMER_NAME, name);// передаем текущее имя таймера
        intent.putExtra(TIMER_MESSAGE, message);// передаем текущее сообщение таймера
        intent.putExtra(TIMER_DURATION, duration);// передаем текущую уставку времени таймера
        startActivityForResult(intent, id);// вызываем меню настройки таймера с помощью, т.к. сам таймер такой функции не поддерживает
    }


    // отсчет времени
    public void run() {
        if (!runTimer) {//таймер остановлен, прекращаем отсчет
            return;
        }
        //Log.d(LOG_TAG, "-----------tik " + name + " -------------");
//        Log.d(LOG_TAG, "before time = " + time);
//        Log.d(LOG_TAG, "before startTime = " + startTime);
//        Log.d(LOG_TAG, "before timePause = " + timePause);
        // оставшееся текущее время
        time = timeLeft();
//        Log.d(LOG_TAG, "after time = " + time);
//        Log.d(LOG_TAG, "after startTime = " + startTime);
//        Log.d(LOG_TAG, "after timePause = " + timePause);
        tvMess.setText(timeConvert.intToStringTime(time));//выводим оставшееся текущее время на экран
        if (time <= 0){
            endTime();
            return;//время вышло
        }
        handler.postDelayed(this, 1000);
    }

    //отсчет закончен
    private void endTime() {
        runTimer = false;
        time = 0;// таймер завершил отсчет времени и ждет сброса
        addButtons(btnReset);//обновляем кнопки
        tvMess.setText(message);
        layoutMainBack.setBackgroundResource(R.color.background_main);// выделяем сработавший таймер фоном
        // сообщаем активности о завершении отсчета для включения сигнала
        // и передаем ей сообщение таймера для вывода его на общий экран сообщений
        ((MainActivity) getActivity()).timerEnd(message);
    }

    //метод принимает настройки из TimerMenu и меняет настройки соответствующего таймера
    //вызов TimerMenu происходит из самого таймера командой
    // activity.startActivityForResult(intent, id);
    // ответ приходит сюда, так как MyTimer не
    //является активностью и не поддерживает метода onActivityResult
    public  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // проверяем ответ настроек таймера с id записанным в requestCode
        if (resultCode == RESULT_OK) {
            name = data.getStringExtra(TIMER_NAME);// извлекаем имя таймера из интента
            message = data.getStringExtra(TIMER_MESSAGE);// извлекаем сообщение таймера из интента
            duration = data.getIntExtra(TIMER_DURATION, DEFAULT_DURATION);// извлекаем время таймера из интента
            initTimer(name, btnStart, null, R.color.transparent);
            ((MainActivity) getActivity()).saveTimer(this);//сохраняем настройки таймера в файл
            reset();
            return;
        }

        //удаляем таймер с id записанным в requestCode
        if(resultCode == TIMER_DELETE_RESULT) {
            ((MainActivity) getActivity()).delTimer(requestCode);//удаляем таймер по id
        }
    }

    //сохраняем переменные фрагмента при повороте устройства
    public void onSaveInstanceState(Bundle sis) {
        super.onSaveInstanceState(sis);
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onSaveInstanceState() " + name);
        sis.putInt(TIMER_ID, id);
        sis.putString(TIMER_NAME, name);
        sis.putString(TIMER_MESSAGE, message);
        sis.putInt(TIMER_DURATION, duration);
        sis.putInt(TIMER_TIME, time);
        sis.putLong(TIMER_START_TIME, startTime);
        sis.putLong(TIMER_TIME_PAUSE, timePause);
        sis.putLong(TIMER_START_PAUSE, startPause);
        sis.putBoolean(TIMER_RUN, runTimer);

    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onActivityCreated() " + name);
    }

    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onResume() " + name);
    }

    public void onPause() {
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onPause() " + name);
        super.onPause();
    }

    public void onStop() {
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onStop() " + name);
        super.onStop();
    }

    public void onDestroyView() {
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onDestroyView() " + name);
        super.onDestroyView();
    }

    public void onDestroy() {
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onDestroy() " + name);

        if (runTimer) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(getActivity(), id, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager am = (AlarmManager) getActivity().getSystemService(ALARM_SERVICE);
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + ((time + 1) * 1000),
                    pIntent);
            Log.d(LOG_TAG, "_________________________________ MyTimerFragment onDestroy() установили AlarmManeger " + name);
        }
        runTimer = false;
        super.onDestroy();

    }

    public void onDetach() {
        Log.d(LOG_TAG, "_________________________________ MyTimerFragment onDetach() " + name);
        super.onDetach();
    }









    //______Геттеры и Сеттеры___________________________________________________________________________

    public int getTimerId() {
        return id;
    }

    public void setTimerId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        if (duration < 0) {
            this.duration = 0;
            return;
        }
        if (duration > 359999){
            this.duration = 359999;
            return;
        }
        this.duration = duration;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getTimePause() {
        return timePause;
    }

    public void setTimePause(long timePause) {
        this.timePause = timePause;
    }

    public long getStartPause() {
        return startPause;
    }

    public void setStartPause(long startPause) {
        this.startPause = startPause;
    }

    public boolean isRunTimer() {
        return runTimer;
    }

    public void setRunTimer(boolean runTimer) {
        this.runTimer = runTimer;
    }

}