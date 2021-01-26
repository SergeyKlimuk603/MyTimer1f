package by.klimuk.mytimer1f;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String LOG_TAG = "myLog";
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
    //HashMap<Integer, MyTimer> timers;

    //проигрыватель звуковых файлов
    //MediaPlayer mp;

    //переменная онимации кнопок, если true - анимация включена
    private boolean animation = false;

    FragmentTransaction transaction;

    FrameLayout fl;
    FrameLayout fll;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //создаем музыкальный проигрыватель
        //mp = MediaPlayer.create(this, R.raw.music);
        //инициализация представлений
        initView();
        if (savedInstanceState == null) {
            MyTimerFragment fragment = new MyTimerFragment(5, "Sergey", "Hello!!!", 100);
            MyTimerFragment fragment1 = new MyTimerFragment(10, "Vera", "Hi", 5);
            transaction = getSupportFragmentManager().beginTransaction();

            //fl = new FrameLayout(this);
            //timersList.addView(fl);
            transaction.add(timersList.getId(), fragment);
            transaction.add(timersList.getId(), fragment1);
            transaction.commit();
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



    @Override
    public void onClick(View v) {

    }


}