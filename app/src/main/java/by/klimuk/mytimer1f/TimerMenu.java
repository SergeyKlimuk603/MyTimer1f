package by.klimuk.mytimer1f;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import static by.klimuk.mytimer1f.Conctants.*;

public class TimerMenu extends Activity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {

    private final String LOG_TAG = "myLog";
    //Log.d(LOG_TAG, "Создали активити");

    // переменные доступа к view элементам меню
    private EditText etName;// поле имени таймера
    private EditText etMess;// поле сообщения таймера
    private TextView tvDur;// поле длительности таймера
    private SeekBar sbHour;// ползунок часов
    private SeekBar sbMin;// ползунок минут
    private SeekBar sbSec;// ползунок секунд
    private Button btnSave;// кнопка сохранения настроек
    private Button btnDelete;// кнопка удаления таймера

    //получаем данные таймера
    Intent intent;

    //данные таймера в меню настроек, их будем менять
    private int duration;

    // объект для преобразования времени в различные форматы
    private Converter timeConvert;

    // id диалога удаления таймера
    private final int DELETE_DIALOG_ID = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_menu);
        //получаем доступ к представлениям
        initView();
        //получаем данные от таймера
        intent = getIntent();
        // объект для преобразования времени в различные форматы
        timeConvert = new Converter();
        // заполняем поля настроек данными из таймера
        initSetting();
    }

    //инициализация Views
    private void initView() {
        etName = (EditText) findViewById(R.id.etName);
        etMess = (EditText) findViewById(R.id.etMess);
        tvDur = (TextView) findViewById(R.id.tvDur);
        sbHour = (SeekBar) findViewById(R.id.sbHour);
        sbMin = (SeekBar) findViewById(R.id.sbMin);
        sbSec = (SeekBar) findViewById(R.id.sbSec);
        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);
        btnDelete = (Button) findViewById(R.id.btnDel);
        btnDelete.setOnClickListener(this);
    }

    //заполняем поля настроек данными из таймера
    private void initSetting() {
        etName.setText(intent.getStringExtra(TIMER_NAME));// выводим имя таймера
        etMess.setText(intent.getStringExtra(TIMER_MESSAGE));// выводим сообщение
        duration = intent.getIntExtra(TIMER_DURATION, 10);//Получаем значение уставки таймера в секундах
        tvDur.setText(timeConvert.intToStringTime(duration));//выводим длительность таймера
        //задаем значения расположения ползунков задания времени в соответствии с длительностью таймера
        sbHour.setProgress(timeConvert.hour);
        sbHour.setOnSeekBarChangeListener(this);
        sbMin.setProgress(timeConvert.min);
        sbMin.setOnSeekBarChangeListener(this);
        sbSec.setProgress(timeConvert.sec);
        sbSec.setOnSeekBarChangeListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSave:
                save();
                break;
            case R.id.btnDel:
                //отображаем диалог удаления таймера
                showDialog(DELETE_DIALOG_ID);
                break;
        }
    }

    private void save() {
        Intent intent = new Intent();
        intent.putExtra(TIMER_NAME, etName.getText().toString());//новое имя таймера
        intent.putExtra(TIMER_MESSAGE, etMess.getText().toString());//новое сообщение таймера
        duration = timeConvert.toSeconds();//преобразуем время в секунды
        intent.putExtra(TIMER_DURATION, duration);//новое время таймера
        setResult(RESULT_OK, intent);//передаем настройки в MainActivity
        finish();//закрываем меню настроек таймера
    }

    //создаем диалог для подтверждения удаления таймера
    protected Dialog onCreateDialog(int dialogId) {
        AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        //заголовок диалога
        deleteDialog.setTitle(getResources().getString(R.string.do_you_want_to_del) + " "
                + etName.getText() + "?");
        //кнопка положительного ответа
        deleteDialog.setPositiveButton(R.string.yes, dialogListener);
        //кнопка отрицательного ответа
        deleteDialog.setNegativeButton(R.string.no, dialogListener);
        return deleteDialog.create();
    }

    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    deleteTimer();//удаляем таймер
                    finish();//закрываем меню настроек
                    break;
                case Dialog.BUTTON_NEGATIVE://просто выходим из диалога удаления таймера
                    break;
            }
        }
    };

    //отправляем MainActivity запрос на удаление таймера
    private  void deleteTimer() {
        setResult(TIMER_DELETE_RESULT);//отправка сообщения об удалении таймера
        finish();//закрываем меню таймера
    }

    //настраиваем с помощью seekBar время работы таймера и выводим его на экран
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sbHour:
                timeConvert.hour = progress;
                break;
            case R.id.sbMin:
                timeConvert.min = progress;
                break;
            case R.id.sbSec:
                timeConvert.sec = progress;
                break;
        }
        tvDur.setText(timeConvert.intToStringTime());
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }


}