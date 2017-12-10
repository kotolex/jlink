package com.kotolex.services;

import java.io.PrintStream;

/**
 * Инкапсулирет работу с System.out, используя только нужные мне методы, позволяет избежать вызова статиков в классах
 *
 * @author kotolex
 * @version 1.0
 */
public class SimpleConsole {
    /** Стрим для работы со стандартным выводом*/
    private PrintStream writer;
    /** Хранит время начала отсчета, для определения времени, затраченного на определенные действия */
    private long startTime;

    public SimpleConsole() {
        writer = System.out;
        startCount();
    }

    /** Начинает отсчет времени*/
    public void startCount() {
        startTime = System.currentTimeMillis();
    }

    public void println(String text) {
        writer.println(text);
    }

    public void print(String text) {
        writer.print(text);
    }

    /** Выводит время от начала отсчета до завершения */
    public void printTime() {
        println("Time elapsed: " + time() + " sec.");
    }

    /** Возвращает время о начала отсчета до текущего момента, с точностью до тысячных секунды */
    private double time() {
        return ((double) (System.currentTimeMillis() - startTime)) / 1000;
    }
}
