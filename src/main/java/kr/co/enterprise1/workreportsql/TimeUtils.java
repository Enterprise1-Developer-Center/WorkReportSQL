package kr.co.enterprise1.workreportsql;

import java.util.Calendar;

/**
 * Created by jaeho on 2018. 1. 9
 */
public class TimeUtils {
    //현재 날짜 시간 구하는 함수
    public static String getCurrentTime() {
        Calendar cur = Calendar.getInstance();  // 현재 날짜/시간 등의 각종 정보 얻기
        String result =
                "" + cur.get(Calendar.YEAR) + "-" + (cur.get(Calendar.MONTH) + 1) + "-" + cur.get(
                        Calendar.DAY_OF_MONTH) + " " + cur.get(Calendar.HOUR_OF_DAY) + ":"
                        + cur.get(Calendar.MINUTE);

        return result;
    }

    //초과 근무 시작 시간에 년 월 일 붙이기
    public static String makeSTime(String date, String time) {

        String result = date + " " + time;
        return result;
    }

    //초과 근무 종료시간에 년 월 일 붙이기 -> 하루지나서 까지 일때 -> 하루 + 1
    public static String makeETime(String date, String stime, String etime) {
        String result = "";
        String[] dateArr = date.split("-");
        Calendar cal = Calendar.getInstance();

        cal.set(Integer.parseInt(dateArr[0]), Integer.parseInt(dateArr[1]) - 1,
                Integer.parseInt(dateArr[2]));

        int s_time_hour = Integer.parseInt(stime.split(":")[0]);
        int e_time_hour = Integer.parseInt(etime.split(":")[0]);

        if (s_time_hour > e_time_hour) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        result = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(
                Calendar.DAY_OF_MONTH) + " " + etime;

        return result;
    }
}
