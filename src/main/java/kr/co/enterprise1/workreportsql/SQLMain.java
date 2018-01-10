package kr.co.enterprise1.workreportsql;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.mfp.adapter.api.AdaptersAPI;
import com.ibm.mfp.adapter.api.ConfigurationAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by jaeho on 2018. 1. 9
 */
@Path("/")
public class SQLMain {
    private final static AdapterLog Log = new AdapterLog(SQLMain.class.getName());
    @Context
    ConfigurationAPI configurationAPI;

    @Context
    AdaptersAPI adaptersAPI;

    public Connection getSQLConnection() throws SQLException {
        // Create a connection object to the database
        WorkReportSQLApplication app = adaptersAPI.getJaxRsApplication(WorkReportSQLApplication.class);
        return app.dataSource.getConnection();
    }

    //워킹데이 정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getWorkingDay")
    public Response getWorkingDay(@QueryParam("userId") String userId,
                                  @QueryParam("date") String date
    ) throws SQLException {
        Log.d("userId = " + userId + ", date = " + date);

        String query = "SELECT\n" +
                " w.USER_ID,\n" +
                " a.USER_NM,\n" +
                " (SELECT DEPT_NM FROM DEPT_INFO WHERE DEPT_CD = a.DEPT_CD) AS DEPT_NM,\n" +
                " to_char( w.WORK_YMD, 'YYYY-MM-DD')  WORK_YMD,\n" +
                " w.MCLS_CD,\n" +
                " w.DETAIL,\n" +
                " p.PROJ_CD,\n" +
                " p.PROJ_NM,\n" +
                " to_char(w.S_TIME, 'hh24:mi') S_TIME,\n" +
                " to_char(w.E_TIME, 'hh24:mi') E_TIME,\n" +
                " to_char(w.EXTRA_TIME, 'hh24:mi') EXTRA_TIME,\n" +
                " to_char(w.UPD_TIME, 'yyyy-mm-dd hh24:mi') UPD_TIME\n" +
                " FROM USER_INFO a, WORK_DETAIL w, PROJ_INFO p\n" +
                "WHERE a.USER_ID = w.USER_ID\n" +
                "  AND w.PROJ_CD = p.PROJ_CD\n" +
                "  AND w.USER_ID =?\n" +
                "  AND to_char(w.WORK_YMD,'yyyy-mm-dd') = ?";
        Connection connection = getSQLConnection();
        PreparedStatement preparedStatement;
        preparedStatement =
                connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONObject object = new JSONObject();
        JSONObject item = new JSONObject();
        JSONObject MCLS = new JSONObject();
        JSONObject PROJ = new JSONObject();

        try {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, date);

            ResultSet data = preparedStatement.executeQuery();
            if (data.first()) {
                item.put("WORK_YMD", data.getString("WORK_YMD"));
                item.put("DEPT_NM", data.getString("DEPT_NM"));
                item.put("USER_ID", data.getString("USER_ID"));
                item.put("USER_NM", data.getString("USER_NM"));

                PROJ.put("PROJ_CD", data.getString("PROJ_CD"));
                PROJ.put("PROJ_NM", data.getString("PROJ_NM"));
                item.put("PROJ", PROJ);

                MCLS.put("MCLS_CD", data.getString("MCLS_CD"));
                MCLS.put("DETAIL", data.getString("DETAIL"));
                item.put("MCLS", MCLS);

                item.put("S_TIME", data.getString("S_TIME"));
                item.put("E_TIME", data.getString("E_TIME"));
                item.put("EXTRA_TIME", data.getString("EXTRA_TIME"));
                item.put("UPD_TIME", data.getString("UPD_TIME"));

                object.put("result", Constants.RESULT_SUCCESS);
                object.put("content", item);
                object.put("msg", "");
                return Response.ok(object).build();
            } else {
                object.put("result", Constants.RESULT_FAILURE);
                object.put("msg", "정확한 날짜를 선택해주세요.");
                return Response.ok(object).build();
            }
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            preparedStatement.close();
            connection.close();
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            preparedStatement.close();
            connection.close();
        }
    }

    //워킹데이 수정
    @POST
    @Produces("application/json")
    @Path("/updateWorkingDay")
    public Response updateWorkingDay(@FormParam("LCLS_CD") String LCLS_CD,
                                     @FormParam("MCLS_CD") String MCLS_CD, @FormParam("DETAIL") String DETAIL,
                                     @FormParam("PROJ_CD") String PROJ_CD, @FormParam("S_TIME") String S_TIME,
                                     @FormParam("E_TIME") String E_TIME, @FormParam("USER_ID") String USER_ID,
                                     @FormParam("date") String date
    ) throws SQLException {


        String query = "update WORK_DETAIL SET "
                + "LCLS_CD=?,"
                + "MCLS_CD=?,"
                + "DETAIL=?,"
                + "PROJ_CD=?,"
                + "S_TIME=to_date(?,'yyyy-mm-dd HH24:MI'),"
                + "E_TIME=to_date(?,'yyyy-mm-dd HH24:MI'),"
                + "EXTRA_TIME = "
                + "(select to_date(SUBSTR(diff, 12, 5),'HH24:MI') "
                + "from "
                + "(select NUMTODSINTERVAL (to_date(?,'yyyy-mm-dd HH24:MI') - to_date(?,'yyyy-mm-dd HH24:MI'), 'DAY') diff from WORK_DETAIL where USER_ID=? and to_char(WORK_YMD,'yyyy-mm-dd') = ?)),"
                + "UPD_TIME=to_date(?,'yyyy-mm-dd HH24:MI')"
                + "where USER_ID=? and to_char(WORK_YMD,'yyyy-mm-dd') = ?";

        //1:대구분 코드, 2:구분 코드, 3:상세내용, 4:프로젝트코드, 5:초과근무 시작시간, 6:초과근무 종료시간, 7:초과근무 종료시간, 8:초과근무 시작시간, 9:유저ID, 10:조회 날짜
        //11:수정시간, 12:유저ID, 13:조회날짜

        Connection con = getSQLConnection();
        PreparedStatement preparedStatement_1 =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        JSONObject obj = new JSONObject();

        //현재 시간 구하기
        String UPD_TIME = TimeUtils.getCurrentTime();
        String s_time = TimeUtils.makeSTime(date, S_TIME);
        String e_time = TimeUtils.makeETime(date, S_TIME, E_TIME);

        try {
            preparedStatement_1.setString(1, LCLS_CD);
            preparedStatement_1.setString(2, MCLS_CD);
            preparedStatement_1.setString(3, DETAIL);
            preparedStatement_1.setString(4, PROJ_CD);
            preparedStatement_1.setString(5, s_time);
            preparedStatement_1.setString(6, e_time);
            preparedStatement_1.setString(7, e_time);
            preparedStatement_1.setString(8, s_time);
            preparedStatement_1.setString(9, USER_ID);
            preparedStatement_1.setString(10, date);
            preparedStatement_1.setString(11, UPD_TIME);
            preparedStatement_1.setString(12, USER_ID);
            preparedStatement_1.setString(13, date);

            int cnt = preparedStatement_1.executeUpdate();

            if (cnt > 0) {
                //업데이트 성공
                String query1 = "SELECT w.USER_ID,"
                        + " w.USER_NM,"
                        + " w.DEPT_NM,"
                        + " to_char( w.WORK_YMD, 'YYYY-MM-DD')  WORK_YMD,"
                        + " w.MCLS_CD,"
                        + " w.DETAIL,"
                        + " p.PROJ_CD,"
                        + " p.PROJ_NM PROJ_NM,"
                        + " to_char(w.S_TIME, 'hh24:mi') S_TIME,"
                        + " to_char(w.E_TIME, 'hh24:mi') E_TIME,"
                        + " to_char(w.EXTRA_TIME, 'hh24:mi') EXTRA_TIME,"
                        + "to_char(w.UPD_TIME, 'yyyy-mm-dd hh24:mi') UPD_TIME"
                        + " FROM WORK_DETAIL w, PROJ_INFO p "
                        + "WHERE w.PROJ_CD = p.PROJ_CD "
                        + "and w.USER_ID =? "
                        + "and to_char(w.WORK_YMD,'yyyy-mm-dd') = ?";
                PreparedStatement preparedStatement_2 =
                        con.prepareStatement(query1, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                ResultSet.CONCUR_READ_ONLY);

                JSONObject object = new JSONObject();
                JSONObject item = new JSONObject();
                JSONObject MCLS = new JSONObject();
                JSONObject PROJ = new JSONObject();
                try {
                    preparedStatement_2.setString(1, USER_ID);
                    preparedStatement_2.setString(2, date);

                    ResultSet data = preparedStatement_2.executeQuery();

                    if (data.first()) {


                        item.put("WORK_YMD", data.getString("WORK_YMD"));
                        item.put("DEPT_NM", data.getString("DEPT_NM"));
                        item.put("USER_ID", data.getString("USER_ID"));
                        item.put("USER_NM", data.getString("USER_NM"));

                        PROJ.put("PROJ_CD", data.getString("PROJ_CD"));
                        PROJ.put("PROJ_NM", data.getString("PROJ_NM"));
                        item.put("PROJ", PROJ);

                        MCLS.put("MCLS_CD", data.getString("MCLS_CD"));
                        MCLS.put("DETAIL", data.getString("DETAIL"));
                        item.put("MCLS", MCLS);

                        item.put("S_TIME", data.getString("S_TIME"));
                        item.put("E_TIME", data.getString("E_TIME"));
                        item.put("EXTRA_TIME", data.getString("EXTRA_TIME"));
                        item.put("UPD_TIME", data.getString("UPD_TIME"));

                        object.put("result", Constants.RESULT_SUCCESS);
                        object.put("content", item);
                        object.put("msg", "");

                        return Response.ok(object).build();
                    } else {

                        object.put("result", Constants.RESULT_FAILURE);
                        object.put("msg", "데이터가 없습니다.");
                        return Response.ok(object).build();
                    }
                } catch (Exception e) {
                    Log.d(e.getMessage(), e);
                    preparedStatement_2.close();
                    con.close();
                    object.put("result", Constants.RESULT_FAILURE);
                    object.put("msg", "" + e.getMessage());
                    return Response.ok(object).build();
                } finally {
                    //Close resources in all cases
                    preparedStatement_2.close();
                }
            } else {
                //변경 내역이 없음

            }

            //Return a 200 OK
            return Response.ok().build();
        } catch (Exception e) {
            //Trying to create a user that already exists
            Log.d(e.getMessage(), e);
            obj.put("result", Constants.RESULT_FAILURE);
            obj.put("msg", "" + e.getMessage());
            return Response.ok(obj).build();
        } finally {
            //Close resources in all cases
            preparedStatement_1.close();
            con.close();
        }
    }

    //구분코드 가져오기
    @GET
    @Produces("application/json")
    @Path("/getCode")
    public Response getCode() throws SQLException {

        Connection con = getSQLConnection();
        String query =
                "SELECT L.LCLS_NM, L.LCLS_CD, M.MCLS_NM, M.MCLS_CD, M.REMARK FROM WORK_MCLASS M, WORK_LCLASS L WHERE M.LCLS_CD=L.LCLS_CD";
        PreparedStatement checkLogin =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONArray results = new JSONArray();
        JSONObject object = new JSONObject();

        try {
            ResultSet data = checkLogin.executeQuery();

            while (data.next()) {
                JSONObject item = new JSONObject();
                item.put("LCLS_NM", data.getString(1));
                item.put("LCLS_CD", data.getString(2));
                item.put("MCLS_NM", data.getString(3));
                item.put("MCLS_CD", data.getString(4));
                item.put("REMARK", data.getString(5));
                results.add(item);
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", results);
            object.put("msg", "");

            return Response.ok(object).build();
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            checkLogin.close();
            con.close();

            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", " " + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            checkLogin.close();
            con.close();
        }
    }

    //성공:1  / 실패:0
    //msg:""   msg:로그인정보가 잘못되었습니다.
    //로그인
    @GET
    @Produces("application/json")
    @Path("/login")
    public Response checkLogin(@QueryParam("userId") String userId,
                               @QueryParam("userPw") String userPw
    ) throws SQLException {
        Connection con = getSQLConnection();
        String query =
                "SELECT u.USER_ID, d.DEPT_NM, d.DEPT_CD from USER_INFO u, DEPT_INFO d WHERE u.DEPT_CD = d.DEPT_CD and USER_ID=? AND USER_PW=?";
        PreparedStatement checkLogin =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONObject result = new JSONObject();
        JSONObject result2 = new JSONObject();

        try {
            checkLogin.setString(1, userId);
            checkLogin.setString(2, userPw);
            ResultSet data = checkLogin.executeQuery();

            if (data.first()) {
                //로그인 성공
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREAN);

                result2.put("date", sdf.format(date).toString());
                result2.put("USER_ID", data.getString(1));
                result2.put("DEPT_NM", data.getString(2));
                result2.put("DEPT_CD", data.getInt(3));

                result.put("result", Constants.RESULT_SUCCESS);
                result.put("msg", "");
                result.put("content", result2);

                return Response.ok(result).build();
            }
            //db에서 아무데이터가 안나오는경우??
            else {
                result.put("result", Constants.RESULT_FAILURE);
                result.put("msg", "로그인정보가 잘못되었습니다.");
                return Response.ok(result).build();
            }
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            checkLogin.close();
            con.close();
            result.put("result", Constants.RESULT_FAILURE);
            result.put("msg", "" + e.getMessage());
            return Response.ok(result).build();
        } finally {
            //Close resources in all cases
            checkLogin.close();
            con.close();
        }
    }

    //비밀번호 수정
    @POST
    @Produces("application/json")
    @Path("/changePwd")
    public Response changePwd(@FormParam("userId") String userId, @FormParam("curPwd") String curPwd,
                              @FormParam("newPwd") String newPwd, @FormParam("newPwdConfirm") String newPwdConfirm)
            throws SQLException {

        Connection con = getSQLConnection();
        String query = "update USER_INFO set USER_PW=? where user_id=? and user_pw=?";

        PreparedStatement changePwd =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONObject res = new JSONObject();

        try {

            changePwd.setString(1, curPwd);
            changePwd.setString(2, userId);
            changePwd.setString(3, curPwd);

            int cnt = changePwd.executeUpdate();

            if (cnt > 0) {
                //현재 비밀번호 일치
                if (!newPwd.equals(newPwdConfirm)) {
                    res.put("result", 0);
                    res.put("msg", "비밀번호 확인 불일치.");
                    return Response.ok(res).build();
                }
                if (newPwd.equals(curPwd)) {
                    res.put("result", 0);
                    res.put("msg", "기존 비밀번호와 동일합니다.");
                    return Response.ok(res).build();
                }

                changePwd.setString(1, newPwd);
                int cnt2 = changePwd.executeUpdate();
                if (cnt2 > 0) {
                    res.put("result", Constants.RESULT_SUCCESS);
                    res.put("msg", "");
                    return Response.ok(res).build();
                } else {
                    res.put("result", Constants.RESULT_FAILURE);
                    res.put("msg", "DB업데이드 오류");
                    return Response.ok(res).build();
                }
            } else {
                //현재비밀번호 틀림
                res.put("result", Constants.RESULT_FAILURE);
                res.put("msg", "현재비밀번호가 틀렸습니다.");

                return Response.ok(res).build();
            }
        } catch (Exception e) {
            //Trying to create a user that already exists
            Log.d(e.getMessage(), e);
            res.put("result", Constants.RESULT_FAILURE);
            res.put("msg", "" + e.getMessage());
            return Response.ok(res).build();
        } finally {
            //Close resources in all cases
            changePwd.close();
            con.close();
        }
    }

    //프로젝트 정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getProjects")
    public Response getProjects(@QueryParam("DEPT_NM") String DEPT_NM) throws SQLException {

        Connection con = getSQLConnection();
        String query = "SELECT PROJ_CD, PROJ_NM, dept_nm FROM PROJ_INFO";
        String query_dept =
                "SELECT PROJ_CD, PROJ_NM, dept_nm  FROM PROJ_INFO where dept_nm=? or dept_nm='0'";

        PreparedStatement checkLogin =
                DEPT_NM == null ?
                        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                ResultSet.CONCUR_READ_ONLY) :
                        con.prepareStatement(query_dept, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                ResultSet.CONCUR_READ_ONLY);

        JSONArray results = new JSONArray();
        JSONObject object = new JSONObject();

        if (DEPT_NM != null) {
            checkLogin.setString(1, DEPT_NM);
        }
        try {
            ResultSet data = checkLogin.executeQuery();
            while (data.next()) {
                JSONObject item = new JSONObject();
                item.put("PROJ_CD", data.getString(1));
                item.put("PROJ_NM", data.getString(2));
                item.put("DEPT_NM", data.getString(3));

                results.add(item);
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", results);
            object.put("msg", "");
            return Response.ok(object).build();
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            checkLogin.close();
            con.close();
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            checkLogin.close();
            con.close();
        }
    }


}
