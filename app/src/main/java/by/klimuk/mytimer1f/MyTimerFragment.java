package by.klimuk.mytimer1f;

import android.app.Activity;
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

import static by.klimuk.mytimer1f.Conctants.TIMER_DURATION;
import static by.klimuk.mytimer1f.Conctants.TIMER_ID;
import static by.klimuk.mytimer1f.Conctants.TIMER_LOST_TIME;
import static by.klimuk.mytimer1f.Conctants.TIMER_MESSAGE;
import static by.klimuk.mytimer1f.Conctants.TIMER_NAME;
import static by.klimuk.mytimer1f.Conctants.TIMER_RUN;
import static by.klimuk.mytimer1f.Conctants.TIMER_START_TIME;
import static by.klimuk.mytimer1f.Conctants.TIMER_TIME;


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
    private int lostTime;// время оставшееся для отсчет после запуска или перезапуска.
    // Если lostTime < 0 - таймер в состоянии ожидания команды запуска
    // Если lostTime = 0 - таймер завершил отсчет и ждет сброса
    // Если lostTime > 0 - таймер в состоянии отсчета времени или паузы (зависит от переменной runTimer)
    private long startTime;// системное время запуска или перезапуска таймера
    private boolean runTimer; //флаг работы таймера. Если идет отсчет runTimer == true

    // Вспомогательные элементы таймера
    private Handler handler;// handler для отсчета времени
    private Activity activity; // ссылка на вызывающую таймер активность
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
        Log.d(LOG_TAG, "frag" + id + " defConstructor");
        // Required empty public constructor
    }
    public MyTimerFragment(int _id, String _name, String _message,
                           int _dur) {
        setTimerId(_id);
        setName(_name);
        setMessage(_message);
        setDuration(_dur);
        lostTime = -1;
        Log.d(LOG_TAG, "frag" + id + " Constructor");
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
        //если фракгмент пересоздается - восстанавливаем даные
        if (savedInstanceState != null) {
            id = savedInstanceState.getInt(TIMER_ID);
            name = savedInstanceState.getString(TIMER_NAME);
            message = savedInstanceState.getString(TIMER_MESSAGE);
            duration = savedInstanceState.getInt(TIMER_DURATION);
            time = savedInstanceState.getInt(TIMER_TIME);
            lostTime = savedInstanceState.getInt(TIMER_LOST_TIME);
            startTime = savedInstanceState.getLong(TIMER_START_TIME);
            runTimer = savedInstanceState.getBoolean(TIMER_RUN);
        }
        timeConvert = new Converter();
        handler = new Handler();
        Log.d(LOG_TAG, "   \n" + "frag" + id + " onCreate" + "id = " + id +
                ", name = " + name + ", message = " + message + ", duration = " + duration);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(LOG_TAG, "frag" + id + " onCreateView");
        View mainView = inflater.inflate(R.layout.fragment_my_timer, container, false);
        layoutMainBack = (FrameLayout) mainView.findViewById(R.id.flMainBack);
        tvName = (TextView) mainView.findViewById(R.id.tvName);
        tvMess = (TextView) mainView.findViewById(R.id.tvMess);
        tvDur = (TextView) mainView.findViewById(R.id.tvDur);
        layoutBtn = (LinearLayout) mainView.findViewById(R.id.layoutBtn);

        tvName.setText(name);// выводим имя таймера на экран
        //преобразуем время duration в удобный для отображения формат и выводим на экран в правом верхнем углу таймера
        tvDur.setText(timeConvert.intToStringTime(duration));

        initBtnNames();
        createButtons();

        Log.d(LOG_TAG, "Я здесь");


        if (lostTime == -1){//таймер сброшен
            addButtons(btnStart);
            tvMess.setText(name);
        } else if (lostTime > 0 & runTimer == false) {//таймер в состоянии паузы
            addButtons(btnCont, btnReset);
            tvMess.setText(timeConvert.intToStringTime(lostTime));
        }  else if (lostTime == 0 & runTimer == true) {//таймер завершил отсчет и ждет сброса
            addButtons(btnReset);
            tvMess.setText(message);
            layoutMainBack.setBackgroundResource(R.color.background_main);// выделяем сработавший таймер фоном
        } else {//таймер работает
            addButtons(btnPause, btnReset);
            tvMess.setText(timeConvert.intToStringTime(time));
            handler.post(this);
        }

        // Inflate the layout for this fragment
        return mainView;
    }

    public void onStart() {
        super.onStart();

        //преобразуем время duration в удобный для отображения формат и выводим на экран в правом верхнем углу таймера

        Log.d(LOG_TAG, "frag" + id + " onStart");
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
        Button btn = new Button(activity);// новая кнопка
        btn.setId(_id);// id кнопки
        btn.setText(_name);// название кнопки
        btn.setLayoutParams(_lp);// параметры расположения кнопки
        btn.setTextColor(activity.getResources().getColor(R.color.textColor));// цвет текста кнопки
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
                //callMenu();// вызвать меню таймера
                break;
            default:
                break;
        }
    }

    //запустить отсчет
    private void start() {
        addButtons(btnPause, btnReset);//обновляем кнопки
        runTimer = true;// таймер считает
        lostTime = duration; //начало отсчета, оставшееся для отсчета время
        startTime = System.currentTimeMillis();// системное время при пуске или перезапуске таймера
        handler.post(this);//запускаем отсчет
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
        lostTime = -1;
        // сообщаем активности, что сбросили таймер. Это нужно для отключения сигнала, если сработавший
        // таймер был последним (определяем по сообщению таймера: если оно совпадает с тем, что
        // на общем экране - значит это последний сработавший таймер)
        //activity.timerReset(message);
    }

    //приостановить отсчет времени
    private void pause() {
        addButtons(btnCont, btnReset);//обновляем кнопки
        runTimer = false;// останавливаем таймер
        lostTime = time;// запоминаем оставшееся для отсчета время
    }

    //продолжить отсчет времени
    private void cont() {
        addButtons(btnPause, btnReset);//обновляем кнопки
        // продолжаем остчет времени
        startTime = System.currentTimeMillis();// системное время при пуске или перезапуске таймера
        runTimer = true;
        run();
    }


    // отсчет времени
    public void run() {
        Log.d(LOG_TAG, "___________________________tik_______________________");
        if (!runTimer) return;//таймер остановлен, прекращаем отсчет
        time = lostTime - ((int) (System.currentTimeMillis() - startTime) / 1000);// оставшееся текущее время
        tvMess.setText(timeConvert.intToStringTime(time));//выводим оставшееся текущее время на экран
        if (time <= 0){
            endTime();
            return;//время вышло
        }
        handler.postDelayed(this, 1000);
    }

    //отсчет закончен
    private void endTime() {
        addButtons(btnReset);//обновляем кнопки
        tvMess.setText(message);
        layoutMainBack.setBackgroundResource(R.color.background_main);// выделяем сработавший таймер фоном
        lostTime = 0;// таймер завершил отсчет времени и ждет сброса
        // сообщаем активности о завершении отсчета для включения сигнала
        // и передаем ей сообщение таймера для вывода его на общий экран сообщений
        //activity.timerEnd(message);
    }

    //настройка таймера (запускаем при создании таймера или при изменении его настроек)
    void initSetting(String _name, String _message, int _duration) {
        setName(_name);// установка имени таймера
        setMessage(_message);// установка сообщения таймера
        setDuration(_duration);// установка длительности таймера
        //заполняем таймер начальными значениями надписей и кнопками
        tvName.setText(name);// выводим имя таймера на экран
        //преобразуем время duration в удобный для отображения формат и выводим на экран в правом верхнем углу таймера
        tvDur.setText(timeConvert.intToStringTime(duration));
    }









    //сохраняем переменные фрагмента при повороте устройства
    public void onSaveInstanceState(Bundle sis) {
        super.onSaveInstanceState(sis);
        Log.d(LOG_TAG, "frag" + id + " onSaveInstanceState");
        sis.putInt(TIMER_ID, id);
        sis.putString(TIMER_NAME, name);
        sis.putString(TIMER_MESSAGE, message);
        sis.putInt(TIMER_DURATION, duration);
        sis.putInt(TIMER_TIME, time);
        sis.putInt(TIMER_LOST_TIME, lostTime);
        sis.putLong(TIMER_START_TIME, startTime);
        sis.putBoolean(TIMER_RUN, runTimer);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
        Log.d(LOG_TAG, "frag" + id + " onAttach");
    }


    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(LOG_TAG, "frag" + id + " onActivityCreated");
    }



    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "frag" + id + " onResume");
    }

    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "frag" + id + " onPause");
    }

    public void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "frag" + id + " onStop");
    }

    public void onDestroyView() {
        super.onDestroyView();
        Log.d(LOG_TAG, "frag" + id + " onDestroyView");
    }

    public void onDestroy() {
        super.onDestroy();
        runTimer = false;
        Log.d(LOG_TAG, "frag" + id + " onDestroy");
    }

    public void onDetach() {
        super.onDetach();
        Log.d(LOG_TAG, "frag" + id + " onDetach");
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
        this.duration = duration;
    }
}