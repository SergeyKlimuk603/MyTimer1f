package by.klimuk.mytimer1f;

import android.content.SharedPreferences;

abstract class Conctants {
    //глобальные переменные проекта
    public static final String TIMER_ID = "id";
    public static final String TIMER_NAME = "name";
    public static final String TIMER_MESSAGE = "message";
    public static final String TIMER_DURATION = "duration";
    public static final String TIMER_TIME = "time";
    public static final String TIMER_START_TIME = "startTime";
    public static final String TIMER_LOST_TIME = "lostTime";
    public static final String TIMER_RUN = "runTimer";
    public static final String TIMER_TAG = "timerTag";
    public static final int TIMER_DELETE_RESULT = -51;//переменная используется чтобы сообщить об удалении таймера из настроек. Значение взято от балды
    public static final String SAVE_FILE_NAME = "myTimer1";
    public static final int MAX_TIMERS_AMOUNT = 10;//максимальное количество таймеров
    public static final String TIMER_FRAGMENT_ID = "fragmentId";

    //настройки таймера по умолчанию
    public static final int DEFAULT_ID = 0;
    public static final String DEFAULT_NAME = "Default timer";
    public static final String DEFAULT_MESSAGE = "Default message";
    public static final int DEFAULT_DURATION = 10;


}
