package by.klimuk.mytimer1f;

class Converter {
    //класс разделяющий непрерывное время таймера на часы, минуты и секунды и обратно
    //TODO Нужно преобразовать этот класс в класс Time где будут происходьть все работы с
    // преобразованием времени

    int sec;
    int min;
    int hour;

    //Разделяем время на часы, минуты и секунды и преобразуем итог в формат String для отбражения
    // на экране
    String intToStringTime(Integer time) {
        splitTime(time);
        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    //если параметр времени не передан, то преобразуем текущее время конвертера
    String intToStringTime() {
        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    //Разделяем время на часы, минуты и секунды
    void splitTime(int time) {
        sec = time % 60;
        min = (time % 3600) / 60;
        hour = time / 3600;
    }

    //приводим время из формата часы, минуты, секунды к формату в секуднах
    int toSeconds() {
        int seconds = hour * 3600 + min * 60 + sec;
        return seconds;
    }
}
