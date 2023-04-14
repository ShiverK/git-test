package me.sihang.backend;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class localTest {

    @Test
    public void test() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timeStr = "2022-07-20 08:00:00";
        Date date = dateFormat.parse(timeStr);
        long ts = date.getTime();
        String s = String.valueOf(ts/1000);
        System.out.println(s);
    }


}
