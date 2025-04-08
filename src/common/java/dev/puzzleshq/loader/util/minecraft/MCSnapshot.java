package dev.puzzleshq.loader.util.minecraft;

import dev.puzzleshq.loader.util.Version;

import java.util.regex.Pattern;

public class MCSnapshot extends Version {

    public static final Pattern MCSValidator = Pattern.compile("^[0-9]+w[0-9]+[a-zA-Z_]+$");

    int week;
    int year;
    String extra;

    public MCSnapshot(int year, int week, String extra) {
        super(year, week, 0, Type.BETA);

        this.week = week;
        this.year = year;
        this.extra = extra;
    }

    public static MCSnapshot parse(String s) {
        int year = Integer.parseInt(s.replaceFirst("w", "§").split("§")[0]);
        String last = s.replaceFirst("w", "§").split("§")[1];
        int week = Integer.parseInt(last.replaceAll("[a-zA-Z_]", ""));
        String extra = last.replaceAll("[0-9]", "");

        return new MCSnapshot(year, week, extra);
    }

    public int getWeek() {
        return week;
    }

    public int getYear() {
        return year;
    }

    public String getExtra() {
        return extra;
    }

}
