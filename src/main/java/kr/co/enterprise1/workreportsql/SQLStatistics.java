package kr.co.enterprise1.workreportsql;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.mfp.adapter.api.AdaptersAPI;
import com.ibm.mfp.adapter.api.ConfigurationAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.Calendar;

/**
 * Created by jaeho on 2018. 1. 9
 */
@Path("/")
public class SQLStatistics {
    private final static AdapterLog Log = new AdapterLog(SQLStatistics.class.getName());
    @Context
    ConfigurationAPI configurationAPI;

    @Context
    AdaptersAPI adaptersAPI;

    public Connection getSQLConnection() throws SQLException {
        // Create a connection object to the database
        WorkReportSQLApplication app = adaptersAPI.getJaxRsApplication(WorkReportSQLApplication.class);
        return app.dataSource.getConnection();
    }

    @GET
    @Produces("application/json")
    @Path("/getCreateDbYear")
    public Response getCreateDbYear() throws SQLException {

        JSONObject object = new JSONObject();
        JSONObject contentsObj = new JSONObject();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int minYear = year - 3;
        int maxYear = year + 50;

        try {
            contentsObj.put("year", year);
            contentsObj.put("minYear", minYear);
            contentsObj.put("maxYear", maxYear);

            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", contentsObj);
            object.put("msg", "");

        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            object.put("result", Constants.RESULT_FAILURE);
            object.put("content", contentsObj);
            object.put("msg", e.getMessage());
            return Response.ok(object).build();
        }
        return Response.ok(object).build();
    }

    @GET
    @Produces("application/json")
    @Path("/getAvailableStatisticsYear")
    public Response getAvailableStatisticsYear() throws SQLException {
        Connection con = getSQLConnection();
        String query = "SELECT DISTINCT SUBSTR(TO_CHAR(WORK_YMD,'YYYYMMDD'),0,4) AS YEAR" +
                "        FROM WORK_DETAIL" +
                "        ORDER BY 1";

        PreparedStatement preparedStatement =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONArray array = new JSONArray();
        JSONObject object = new JSONObject();
        try {
            ResultSet data = preparedStatement.executeQuery();
            while (data.next()) {
                array.add(data.getString(1));
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", array);
            object.put("msg", "");

        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            object.put("result", Constants.RESULT_FAILURE);
            object.put("content", array);
            object.put("msg", e.getMessage());
            return Response.ok(object).build();
        } finally {
            preparedStatement.close();
            con.close();
        }
        return Response.ok(object).build();
    }

    //가동률 통계표 가져오기
    @GET
    @Produces("application/json")
    @Path("/getDetailOperationRate")
    public Response getDetailOperationRate(@QueryParam("DEPT_NM") String DEPT_NM, @QueryParam("YEAR") int year)
            throws SQLException {

        Connection connection = getSQLConnection();
        String query = "SELECT * FROM TABLE(WORK_STRU.WORK_STRU_TB('" + year + "'))";
        PreparedStatement preparedStatement =
                connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        ResultSet resultSet = preparedStatement.executeQuery();
        JSONObject resultJson = new JSONObject();
        JSONArray items = new JSONArray();
        boolean isResultSet = false;
        while (resultSet.next()) {
            isResultSet = true;
            JSONObject item = new JSONObject();
            item.put("user_id", resultSet.getString(1));
            item.put("user_nm", resultSet.getString(2));

            item.put("m1", resultSet.getString(3));
            item.put("m2", resultSet.getString(4));
            item.put("m3", resultSet.getString(5));
            item.put("m4", resultSet.getString(6));
            item.put("m5", resultSet.getString(7));
            item.put("m6", resultSet.getString(8));
            item.put("m7", resultSet.getString(9));
            item.put("m8", resultSet.getString(10));
            item.put("m9", resultSet.getString(11));
            item.put("m10", resultSet.getString(12));
            item.put("m11", resultSet.getString(13));
            item.put("m12", resultSet.getString(14));

            item.put("nowsum", resultSet.getString(15));
            item.put("totsum", resultSet.getString(16));
            items.add(item);
        }

        if (isResultSet) {
            resultJson.put("result", Constants.RESULT_SUCCESS);
            resultJson.put("content", items);
            resultJson.put("msg", "");
        } else {
            resultJson.put("result", Constants.RESULT_FAILURE);
            resultJson.put("content", items);
            resultJson.put("msg", "데이터가 존재하지 않습니다.");
        }

        connection.close();
        preparedStatement.close();

        return Response.ok(resultJson).build();
    }

    // 요약 집계
    @GET
    @Produces("application/json")
    @Path("/getSummaryTotal")
    public Response getSummaryTotal(@QueryParam("DEPT_NM") String dept_nm, @QueryParam("YEAR") int year) throws SQLException {
        Connection con = getSQLConnection();
        String query = "WITH DET_TMP AS(" +
                "        SELECT A.LCLS_CD" +
                "              , COUNT(1) AS CNT" +
                "          FROM WORK_DETAIL A" +
                "             , USER_INFO B" +
                "         WHERE A.USER_ID = B.USER_ID" +
                "           AND A.WORK_YMD BETWEEN '" + year + "' || '0101' AND '" + year + "' || '1231'" +
                "           AND B.DEPT_CD = '2'" +
                "         GROUP BY A.LCLS_CD" +
                "        ) " +
                "SELECT L.LCLS_NM" +
                "     , L.LCLS_CD" +
                "     , NVL(D.CNT, 0 ) AS CNT" +
                "  FROM WORK_LCLASS L" +
                "      , DET_TMP D" +
                " WHERE L.LCLS_CD = D.LCLS_CD(+)" +
                " ORDER BY L.LCLS_CD";
        PreparedStatement preparedStatement =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        JSONObject object = new JSONObject();
        try {
            ResultSet data = preparedStatement.executeQuery();
            JSONObject item;
            JSONArray arr = new JSONArray();
            while (data.next()) {
                item = new JSONObject();
                item.put("name", data.getString(1));
                item.put("value", data.getInt(3));
                arr.add(item);
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", arr);
            object.put("msg", "");
            return Response.ok(object).build();
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            preparedStatement.close();
            con.close();
        }
    }

    //가동률 통계표 가져오기
    @GET
    @Produces("application/json")
    @Path("/getOperRatio")
    public Response getOperRatio(@QueryParam("DEPT_NM") String DEPT_NM, @QueryParam("YEAR") int YEAR)
            throws SQLException {

        Connection con = getSQLConnection();
        String query = "select * from OPER_RATIO_BS";
        PreparedStatement getWorkingDay =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONObject object = new JSONObject();
        JSONObject con_object = new JSONObject();
        try {
            ResultSet data = getWorkingDay.executeQuery();
            JSONArray res = new JSONArray();
            Boolean flag = false;
            float JAN = 0, FEB = 0, MAR = 0, APR = 0, MAY = 0, JUN = 0, JUL = 0, AUG = 0, SEP = 0, OCT =
                    0, NOV = 0, DEC = 0, CUR_OPR = 0, YEAR_OPR = 0;
            int cnt = 0;
            JSONObject item = new JSONObject();
            item.put("USER_ID", "ID");
            item.put("USER_NM", "이름");
            item.put("JAN", "1월");
            item.put("FEB", "2월");
            item.put("MAR", "3월");
            item.put("APR", "4월");
            item.put("MAY", "5월");
            item.put("JUN", "6월");
            item.put("JUL", "7월");
            item.put("AUG", "8월");
            item.put("SEP", "9월");
            item.put("OCT", "10월");
            item.put("NOV", "11월");
            item.put("DEC", "12월");
            item.put("CUR_OPR", "현재 가동률");
            item.put("YEAR_OPR", "년간 가동률");
            con_object.put("header", item);

            while (data.next()) {
                if (!flag) flag = true;
                item = new JSONObject();
                cnt++;
                //JAN  FEB        MAR        APR        MAY        JUN        JUL        AUG        SEP        OCT        NOV        DEC    CUR_OPR   YEAR_OPR
                item.put("USER_ID", data.getString(1));
                item.put("USER_NM", data.getString(16));

                item.put("JAN", data.getFloat(2));
                JAN += data.getFloat(2);

                item.put("FEB", data.getFloat(3));
                FEB += data.getFloat(3);

                item.put("MAR", data.getFloat(4));
                MAR += data.getFloat(4);

                item.put("APR", data.getFloat(5));
                APR += data.getFloat(5);

                item.put("MAY", data.getFloat(6));
                MAY += data.getFloat(6);

                item.put("JUN", data.getFloat(7));
                JUN += data.getFloat(7);

                item.put("JUL", data.getFloat(8));
                JUL += data.getFloat(8);

                item.put("AUG", data.getFloat(9));
                AUG += data.getFloat(9);

                item.put("SEP", data.getFloat(10));
                SEP += data.getFloat(10);

                item.put("OCT", data.getFloat(11));
                OCT += data.getFloat(11);

                item.put("NOV", data.getFloat(12));
                NOV += data.getFloat(12);

                item.put("DEC", data.getFloat(13));
                DEC += data.getFloat(13);

                item.put("CUR_OPR", data.getFloat(14));
                CUR_OPR += data.getFloat(14);

                item.put("YEAR_OPR", data.getFloat(15));
                YEAR_OPR += data.getFloat(15);

                res.add(item);
            }

            if (flag) {
                con_object.put("op_ratio_list", res);

                item = new JSONObject();
                item.put("USER_NM", "집계");
                item.put("JAN", Math.round(JAN / cnt * 10) / 10.0);
                item.put("FEB", Math.round(FEB / cnt * 10) / 10.0);
                item.put("MAR", Math.round(MAR / cnt * 10) / 10.0);
                item.put("APR", Math.round(APR / cnt * 10) / 10.0);
                item.put("MAY", Math.round(MAY / cnt * 10) / 10.0);
                item.put("JUN", Math.round(JUN / cnt * 10) / 10.0);
                item.put("JUL", Math.round(JUL / cnt * 10) / 10.0);
                item.put("AUG", Math.round(AUG / cnt * 10) / 10.0);
                item.put("SEP", Math.round(SEP / cnt * 10) / 10.0);
                item.put("OCT", Math.round(OCT / cnt * 10) / 10.0);
                item.put("NOV", Math.round(NOV / cnt * 10) / 10.0);
                item.put("DEC", Math.round(DEC / cnt * 10) / 10.0);
                item.put("CUR_OPR", Math.round(CUR_OPR / cnt * 10) / 10.0);
                item.put("YEAR_OPR", Math.round(YEAR_OPR / cnt * 10) / 10.0);
                con_object.put("total", item);

                object.put("result", Constants.RESULT_SUCCESS);
                object.put("content", con_object);
                object.put("msg", "");

                return Response.ok(object).build();
            } else {
                object.put("result", Constants.RESULT_FAILURE);
                object.put("msg", "데이터가 존재하지 않습니다.");

                return Response.ok(object).build();
            }
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            e.printStackTrace();
            getWorkingDay.close();
            con.close();

            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            getWorkingDay.close();
            con.close();
        }
    }

    //연간 가동률
    @GET
    @Produces("application/json")
    @Path("/getYearOperatingRatio")
    public Response getYearOperatingRatio(@QueryParam("DEPT_NM") String deptNm, @QueryParam("YEAR") int year)
            throws SQLException {

        Connection con = getSQLConnection();
        String query = "WITH MON_TMP AS (  \n" +
                "        SELECT A.MON\n" +
                "             , D_CNT-H_CNT AS W_DAY\n" +
                "          FROM (  \n" +
                "                SELECT SUBSTR(WORK_YMD, 5,2) AS MON, COUNT(1) AS D_CNT\n" +
                "                  FROM WORK_CALENDAR\n" +
                "                 WHERE WORK_YMD BETWEEN '" + year + "' || '0101' AND '" + year + "' || '1231'\n" +
                "                GROUP BY SUBSTR(WORK_YMD, 5,2)\n" +
                "               ) A,\n" +
                "                (\n" +
                "                SELECT SUBSTR(WORK_YMD, 5,2) AS MON, COUNT(1) AS H_CNT\n" +
                "                  FROM HOLIDAY\n" +
                "                 WHERE WORK_YMD BETWEEN '" + year + "' || '0101' AND '" + year + "' || '1231'\n" +
                "                GROUP BY SUBSTR(WORK_YMD, 5,2)\n" +
                "               ) B\n" +
                "          WHERE A.MON = B.MON\n" +
                "    ),\n" +
                "    USER_TMP AS(\n" +
                "    SELECT  USER_ID\n" +
                "          , TO_CHAR(WORK_YMD, 'MM') AS MON\n" +
                "          , COUNT(1) AS W_CNT\n" +
                "     FROM WORK_DETAIL\n" +
                "    WHERE TO_CHAR(WORK_YMD, 'YYYYMMDD') BETWEEN '" + year + "' || '0101' AND '" + year + "' || '1231'\n" +
                "     AND LCLS_CD = '1' AND MCLS_CD = '11'\n" +
                "     GROUP BY USER_ID, TO_CHAR(WORK_YMD, 'MM')\n" +
                "    )\n" +
                "    SELECT MON\n" +
                "         , ROUND(SUM(W_CNT)/SUM(W_DAY) * 100, 1) AS MON_RATE\n" +
                "         , AVG(ROUND(SUM(W_CNT)/SUM(W_DAY) * 100, 1)) OVER() AS TOT_RATE\n" +
                "      FROM (\n" +
                "                SELECT A.USER_ID\n" +
                "                     , A.MON\n" +
                "                     , A.W_CNT\n" +
                "                     , B.W_DAY\n" +
                "                  FROM USER_TMP A\n" +
                "                     , MON_TMP B\n" +
                "                 WHERE A.MON = B.MON\n" +
                "            )\n" +
                "      GROUP BY MON\n" +
                "      ORDER BY MON";

        PreparedStatement getWorkingDay =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONObject object = new JSONObject();

        try {
            ResultSet data = getWorkingDay.executeQuery();
            JSONArray items = new JSONArray();
            boolean flag = false;
            while (data.next()) {
                flag = true;
                JSONObject item = new JSONObject();
                item.put("mon", data.getString(1));
                item.put("mon_rate", data.getString(2));
                item.put("tot_rate", data.getString(3));
                items.add(item);
            }
            if (flag) {
                object.put("result", Constants.RESULT_SUCCESS);
                object.put("content", items);
                object.put("msg", "Great!");
            } else {
                object.put("result", Constants.RESULT_FAILURE);
                object.put("msg", "데이터가 존재하지 않습니다.");
            }

            return Response.ok(object).build();
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            getWorkingDay.close();
            con.close();

            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            getWorkingDay.close();
            con.close();
        }
    }

    @POST
    @Produces("application/json")
    @Path("/createWorkCalendarDb")
    public Response createWorkCalendarDb(@FormParam("YEAR") int year, @FormParam("USER_ID") String userId) throws SQLException {
        String query = "{call MOBILE.INSERT_YEAR_WORK_DAY_SP(?,?)}";
        userId = "SYSTEM";
        JSONObject object = new JSONObject();
        try {
            Connection connection = getSQLConnection();
            CallableStatement cstmt = connection.prepareCall(query);
            cstmt.setString(1, String.valueOf(year));
            cstmt.setString(2, userId);
            cstmt.execute();

            object.put("result", Constants.RESULT_SUCCESS);
            object.put("msg", year + " WORK_CALENDAR를 생성하였습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", e.getMessage());
        }

        return Response.ok(object).build();
    }

}
