/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

package kr.co.enterprise1.workreportsql;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.mfp.adapter.api.AdaptersAPI;
import com.ibm.mfp.adapter.api.ConfigurationAPI;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
public class WorkReportSQLResource {
    /*
     * For more info on JAX-RS see https://jax-rs-spec.java.net/nonav/2.0-rev-a/apidocs/index.html
  */

    static Logger logger = Logger.getLogger(WorkReportSQLApplication.class.getName());

    @Context
    ConfigurationAPI configurationAPI;

    @Context
    AdaptersAPI adaptersAPI;

    public Connection getSQLConnection() throws SQLException {
        // Create a connection object to the database
        WorkReportSQLApplication app = adaptersAPI.getJaxRsApplication(WorkReportSQLApplication.class);
        return app.dataSource.getConnection();
    }

    @POST
    public Response createUser(@FormParam("userId") String userId,
                               @FormParam("userPw") String userPw
    ) throws SQLException {
        logger.info("userId = " + userId + ", userPw = " + userPw);
        Connection con = getSQLConnection();
        PreparedStatement insertUser =
                con.prepareStatement("INSERT INTO USER_INFO (USER_ID, USER_PW) VALUES (?,?)");

        try {
            insertUser.setString(1, userId);
            insertUser.setString(2, userPw);
            //            insertUser.setString(3, lastName);
            //            insertUser.setString(4, password);
            insertUser.executeUpdate();
            //Return a 200 OK
            return Response.ok().build();
        } catch (Exception violation) {
            //Trying to create a user that already exists
            return Response.status(Status.CONFLICT).entity(violation.getMessage()).build();
        } finally {
            //Close resources in all cases
            insertUser.close();
            con.close();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/{userId}")
    public Response getUser(@PathParam("userId") String userId) throws SQLException {
        logger.info("userId = " + userId);
        Connection con = getSQLConnection();
        String query = "SELECT * FROM USER_INFO WHERE USER_ID = ?";
        //PreparedStatement getUser = con.prepareStatement(query);
        PreparedStatement getUser =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        try {
            JSONObject result = new JSONObject();

            getUser.setString(1, userId);
            ResultSet data = getUser.executeQuery();

            if (data.first()) {
                String value = data.getString("USER_ID");
                result.put("userId", value);
/*
                result.put("firstName", data.getString("firstName"));
                result.put("lastName", data.getString("lastName"));
                result.put("password", data.getString("password"));
*/
                return Response.ok(result).build();
            } else {
                return Response.status(Status.NOT_FOUND).entity("User not found...").build();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            getUser.close();
            con.close();
            return Response.status(Status.NOT_FOUND).entity("User not found...").build();
        } finally {
            //Close resources in all cases
            getUser.close();
            con.close();
            logger.info("getUser.close(), con.close()");
        }
    }

    @GET
    @Produces("application/json")
    public Response getAllUsers() throws SQLException {
        JSONArray results = new JSONArray();
        Connection con = getSQLConnection();
        PreparedStatement getAllUsers = con.prepareStatement("SELECT * FROM users");
        ResultSet data = getAllUsers.executeQuery();

        while (data.next()) {
            JSONObject item = new JSONObject();
            item.put("userId", data.getString("userId"));
            item.put("firstName", data.getString("firstName"));
            item.put("lastName", data.getString("lastName"));
            item.put("password", data.getString("password"));

            results.add(item);
        }

        getAllUsers.close();
        con.close();

        return Response.ok(results).build();
    }

    @PUT
    @Path("/{userId}")
    public Response updateUser(@PathParam("userId") String userId,
                               @FormParam("firstName") String firstName,
                               @FormParam("lastName") String lastName,
                               @FormParam("password") String password)
            throws SQLException {
        Connection con = getSQLConnection();
        PreparedStatement getUser = con.prepareStatement("SELECT * FROM users WHERE userId = ?");

        try {
            getUser.setString(1, userId);
            ResultSet data = getUser.executeQuery();

            if (data.first()) {
                PreparedStatement updateUser = con.prepareStatement(
                        "UPDATE users SET firstName = ?, lastName = ?, password = ? WHERE userId = ?");

                updateUser.setString(1, firstName);
                updateUser.setString(2, lastName);
                updateUser.setString(3, password);
                updateUser.setString(4, userId);

                updateUser.executeUpdate();
                updateUser.close();
                return Response.ok().build();
            } else {
                return Response.status(Status.NOT_FOUND).entity("User not found...").build();
            }
        } finally {
            //Close resources in all cases
            getUser.close();
            con.close();
        }
    }

    @DELETE
    @Path("/{userId}")
    public Response deleteUser(@PathParam("userId") String userId) throws SQLException {
        Connection con = getSQLConnection();
        PreparedStatement getUser = con.prepareStatement("SELECT * FROM users WHERE userId = ?");

        try {
            getUser.setString(1, userId);
            ResultSet data = getUser.executeQuery();

            if (data.first()) {
                PreparedStatement deleteUser = con.prepareStatement("DELETE FROM users WHERE userId = ?");
                deleteUser.setString(1, userId);
                deleteUser.executeUpdate();
                deleteUser.close();
                return Response.ok().build();
            } else {
                return Response.status(Status.NOT_FOUND).entity("User not found...").build();
            }
        } finally {
            //Close resources in all cases
            getUser.close();
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
                "SELECT u.USER_ID, d.DEPT_NM from USER_INFO u, DEPT_INFO d WHERE u.DEPT_CD = d.DEPT_CD and USER_ID=? AND USER_PW=?";
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

                result.put("result", 1);
                result.put("msg", "");
                result.put("content", result2);

                return Response.ok(result).build();
            }
            //db에서 아무데이터가 안나오는경우??
            else {
                result.put("result", 0);
                result.put("msg", "로그인정보가 잘못되었습니다.");
                return Response.ok(result).build();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            checkLogin.close();
            con.close();
            result.put("result", 0);
            result.put("msg", "" + e.getMessage());
            return Response.ok(result).build();
        } finally {
            //Close resources in all cases
            checkLogin.close();
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
            object.put("result", 1);
            object.put("content", results);
            object.put("msg", "");

            return Response.ok(object).build();
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            checkLogin.close();
            con.close();

            object.put("result", 0);
            object.put("msg", " " + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            checkLogin.close();
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
            object.put("result", 1);
            object.put("content", results);
            object.put("msg", "");
            return Response.ok(object).build();
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            checkLogin.close();
            con.close();
            object.put("result", 0);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            checkLogin.close();
            con.close();
        }
    }

    //워킹데이 정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getWorkingDay")
    public Response getWorkingDay(@QueryParam("userId") String userId,
                                  @QueryParam("date") String date
    ) throws SQLException {

        Connection connection = getSQLConnection();
        PreparedStatement preparedStatement;
        String query;

        query = "SELECT"
                + " w.USER_ID,"
                + " w.USER_NM,"
                + " w.DEPT_NM,"
                + " to_char( w.WORK_YMD, 'YYYY-MM-DD')  WORK_YMD,"
                + " w.MCLS_CD,"
                + " w.DETAIL,"
                + " p.PROJ_CD,"
                + " p.PROJ_NM," // PROJ_NM
                + " to_char(w.S_TIME, 'hh24:mi') S_TIME,"
                + " to_char(w.E_TIME, 'hh24:mi') E_TIME,"
                + " to_char(w.EXTRA_TIME, 'hh24:mi') EXTRA_TIME,"
                + "to_char(w.UPD_TIME, 'yyyy-mm-dd hh24:mi') UPD_TIME"
                + " FROM WORK_DETAIL w, PROJ_INFO p "
                + "WHERE w.PRJ_CD = p.PROJ_CD "
                + "and w.USER_ID =? "
                + "and to_char(w.WORK_YMD,'yyyy-mm-dd') = ?";
        try {
            preparedStatement =
                    connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        } catch (Exception e) {
            preparedStatement = null;
            logger.info("error = " + e.getMessage());
        }
        JSONObject object = new JSONObject();
        JSONObject item = new JSONObject();
        JSONObject MCLS = new JSONObject();
        JSONObject PROJ = new JSONObject();

        try {
            preparedStatement.setString(1, userId);
            preparedStatement.setString(2, date);

            ResultSet data = preparedStatement.executeQuery();
            logger.info("userId = " + userId);
            logger.info("date = " + date);

            if (data.first()) {
                object.put("result", 1);

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
                object.put("content", item);

                object.put("msg", "");

                return Response.ok(object).build();
            } else {

                object.put("result", 0);
                object.put("msg", "정확한 날짜를 선택해주세요.");
                return Response.ok(object).build();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            preparedStatement.close();
            connection.close();
            object.put("result", 0);
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
                                     @FormParam("PRJ_CD") String PRJ_CD, @FormParam("S_TIME") String S_TIME,
                                     @FormParam("E_TIME") String E_TIME, @FormParam("USER_ID") String USER_ID,
                                     @FormParam("date") String date
    ) throws SQLException {

        Connection con = getSQLConnection();
        String query = "update WORK_DETAIL SET "
                + "LCLS_CD=?,"
                + "MCLS_CD=?,"
                + "DETAIL=?,"
                + "PRJ_CD=?,"
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

        //현재 시간 구하기
        String UPD_TIME = getCurrentTime();
        String s_time = makeSTime(date, S_TIME);
        String e_time = makeETime(date, S_TIME, E_TIME);

        PreparedStatement updateWorkingDay =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        JSONObject obj = new JSONObject();
        try {
            updateWorkingDay.setString(1, LCLS_CD);
            updateWorkingDay.setString(2, MCLS_CD);
            updateWorkingDay.setString(3, DETAIL);
            updateWorkingDay.setString(4, PRJ_CD);
            updateWorkingDay.setString(5, s_time);
            updateWorkingDay.setString(6, e_time);
            updateWorkingDay.setString(7, e_time);
            updateWorkingDay.setString(8, s_time);
            updateWorkingDay.setString(9, USER_ID);
            updateWorkingDay.setString(10, date);
            updateWorkingDay.setString(11, UPD_TIME);
            updateWorkingDay.setString(12, USER_ID);
            updateWorkingDay.setString(13, date);

            int cnt = updateWorkingDay.executeUpdate();

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
                        + "WHERE w.PRJ_CD = p.PROJ_CD "
                        + "and w.USER_ID =? "
                        + "and to_char(w.WORK_YMD,'yyyy-mm-dd') = ?";
                PreparedStatement getWorkingDay =
                        con.prepareStatement(query1, ResultSet.TYPE_SCROLL_INSENSITIVE,
                                ResultSet.CONCUR_READ_ONLY);

                JSONObject object = new JSONObject();
                JSONObject item = new JSONObject();
                JSONObject MCLS = new JSONObject();
                JSONObject PROJ = new JSONObject();
                try {
                    getWorkingDay.setString(1, USER_ID);
                    getWorkingDay.setString(2, date);

                    ResultSet data = getWorkingDay.executeQuery();

                    if (data.first()) {
                        object.put("result", 1);

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
                        object.put("content", item);

                        object.put("msg", "");

                        return Response.ok(object).build();
                    } else {

                        object.put("result", 0);
                        object.put("msg", "데이터가 없습니다.");
                        return Response.ok(object).build();
                    }
                } catch (Exception e) {
                    logger.info(e.getMessage());
                    logger.log(Level.INFO, e.getMessage(), e);
                    e.printStackTrace();
                    getWorkingDay.close();
                    con.close();
                    object.put("result", 0);
                    object.put("msg", "" + e.getMessage());
                    return Response.ok(object).build();
                } finally {
                    //Close resources in all cases
                    getWorkingDay.close();
                }
            } else {
                //변경 내역이 없음

            }

            //Return a 200 OK
            return Response.ok().build();
        } catch (Exception e) {
            //Trying to create a user that already exists
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            obj.put("result", 0);
            obj.put("msg", "" + e.getMessage());
            return Response.ok(obj).build();
        } finally {
            //Close resources in all cases
            updateWorkingDay.close();
            con.close();
        }
    }

    public void updateResult() {

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
                    res.put("result", 1);
                    res.put("msg", "");
                    return Response.ok(res).build();
                } else {
                    res.put("result", 0);
                    res.put("msg", "DB업데이드 오류");
                    return Response.ok(res).build();
                }
            } else {
                //현재비밀번호 틀림
                res.put("result", 0);
                res.put("msg", "현재비밀번호가 틀렸습니다.");

                return Response.ok(res).build();
            }
        } catch (Exception e) {
            //Trying to create a user that already exists
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();

            res.put("result", 0);
            res.put("msg", "" + e.getMessage());

            return Response.ok(res).build();
        } finally {
            //Close resources in all cases
            changePwd.close();
            con.close();
        }
    }

    //워킹데이 요약 정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getSummary")
    public Response getSummary(@QueryParam("DEPT_NM") String DEPT_NM) throws SQLException {

        Connection con = getSQLConnection();
        String query =
                "select  w.user_id, w.user_nm, p.proj_nm, w.mcls_cd, w.detail  from work_detail w, PROJ_INFO p where w.prj_cd=p.proj_cd "
                        + "and to_char(work_ymd,'yyyy-mm-dd')=to_char(sysdate,'yyyy-mm-dd') "
                        + "and w.dept_nm=?";
        PreparedStatement getWorkingDay =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

        JSONObject object = new JSONObject();

        try {
            getWorkingDay.setString(1, DEPT_NM);
            ResultSet data = getWorkingDay.executeQuery();
            JSONArray res = new JSONArray();
            Boolean flag = false;

            while (data.next()) {
                if (!flag) flag = true;
                JSONObject item = new JSONObject();

                item.put("USER_ID", data.getString(1));
                item.put("NAME", data.getString(2));
                item.put("PROJ_NM", data.getString(3));
                item.put("MCLS_CD", data.getString(4));
                item.put("DETAIL", data.getString(5));

                res.add(item);
            }

            if (flag) {
                object.put("result", 1);
                object.put("content", res);
                object.put("msg", "");

                return Response.ok(object).build();
            } else {
                object.put("result", 0);
                object.put("msg", "부서명을 정확하게 입력해주세요.");

                return Response.ok(object).build();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            getWorkingDay.close();
            con.close();

            object.put("result", 0);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            getWorkingDay.close();
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

                object.put("result", 1);
                object.put("content", con_object);
                object.put("msg", "");

                return Response.ok(object).build();
            } else {
                object.put("result", 0);
                object.put("msg", "데이터가 존재하지 않습니다.");

                return Response.ok(object).build();
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            logger.log(Level.INFO, e.getMessage(), e);
            e.printStackTrace();
            getWorkingDay.close();
            con.close();

            object.put("result", 0);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            getWorkingDay.close();
            con.close();
        }
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
            object.put("result", 1);
            object.put("content", arr);
            object.put("msg", "");
            return Response.ok(object).build();
        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
            object.put("result", 0);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            preparedStatement.close();
            con.close();
        }
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
            object.put("result", 1);
            object.put("content", array);
            object.put("msg", "");

        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
            object.put("result", 0);
            object.put("content", array);
            object.put("msg", e.getMessage());
            return Response.ok(object).build();
        } finally {
            preparedStatement.close();
            con.close();
        }
        return Response.ok(object).build();
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

            object.put("result", 1);
            object.put("content", contentsObj);
            object.put("msg", "");

        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
            object.put("result", 0);
            object.put("content", contentsObj);
            object.put("msg", e.getMessage());
            return Response.ok(object).build();
        }
        return Response.ok(object).build();
    }

    @POST
    @Produces("application/json")
    @Path("/createWorkCalendarDb")
    public Response createWorkCalendarDb(@FormParam("YEAR") int year, @FormParam("DEPT_NM") String deptNm) throws SQLException {

        JSONObject object = new JSONObject();

        try {
            object.put("result", 1);
            object.put("content", year + ", " + deptNm);
            object.put("msg", year + " WORK_CALENDAR를 생성하였습니다.");

        } catch (Exception e) {
            logger.info(e.getMessage());
            e.printStackTrace();
            object.put("result", 0);
            object.put("content", null);
            object.put("msg", e.getMessage());
            return Response.ok(object).build();
        }
        return Response.ok(object).build();
    }

    //현재 날짜 시간 구하는 함수
    public String getCurrentTime() {
        Calendar cur = Calendar.getInstance();  // 현재 날짜/시간 등의 각종 정보 얻기
        String result =
                "" + cur.get(Calendar.YEAR) + "-" + (cur.get(Calendar.MONTH) + 1) + "-" + cur.get(
                        Calendar.DAY_OF_MONTH) + " " + cur.get(Calendar.HOUR_OF_DAY) + ":"
                        + cur.get(Calendar.MINUTE);

        return result;
    }

    //초과 근무 시작 시간에 년 월 일 붙이기
    public String makeSTime(String date, String time) {

        String result = date + " " + time;
        return result;
    }

    //초과 근무 종료시간에 년 월 일 붙이기 -> 하루지나서 까지 일때 -> 하루 + 1
    public String makeETime(String date, String stime, String etime) {
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