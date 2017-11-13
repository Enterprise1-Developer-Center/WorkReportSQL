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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.OAuthSecurity;

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
    String query = "SELECT COUNT(*) from USER_INFO WHERE USER_ID=? AND USER_PW=?";
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd (EEE)", Locale.KOREAN);

        if (data.getInt(1) == 1) {
          result2.put("date",sdf.format(date).toString());
          result.put("result", 1);
          result.put("msg","");
          result.put("content",result2);

          return Response.ok(result).build();
        }
        //로그인 실패(아이디 비밀번호 틀림 / 공백)
        else {
          result.put("result", 0);
          result.put("msg","로그인정보가 잘못되었습니다.");
          return Response.ok(result).build();
        }
      }
      //db에서 아무데이터가 안나오는경우??
      else {
        result.put("result",0);
        result.put("msg","데이터가 없습니다.");
        return Response.ok(result).build();
      }
    } catch (Exception e) {
      logger.info(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
      e.printStackTrace();
      checkLogin.close();
      con.close();
      result.put("result",0);
      result.put("msg",""+e.getMessage());
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
        "SELECT L.LCLS_NM, L.LCLS_CD, M.MCLS_NM, M.MCLS_CD FROM WORK_MCLASS M, WORK_LCLASS L WHERE M.LCLS_CD=L.LCLS_CD";
    PreparedStatement checkLogin =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    JSONArray results = new JSONArray();
    JSONObject object = new JSONObject();

    try {
      ResultSet data = checkLogin.executeQuery();

      while (data.next()) {
        JSONObject item = new JSONObject();
        item.put("LCLS_NM", data.getString(1));
        item.put("LCLS_CD", data.getInt(2));
        item.put("MCLS_NM", data.getString(3));
        item.put("MCLS_CD", data.getInt(4));
        results.add(item);
      }
      object.put("result",1);
      object.put("content",results);
      object.put("msg","");


      return Response.ok(object).build();

    } catch (Exception e) {
      logger.info(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
      e.printStackTrace();
      checkLogin.close();
      con.close();

      object.put("result",0);
      object.put("msg"," "+e.getMessage());
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
  public Response getProjects() throws SQLException {

    Connection con = getSQLConnection();
    String query = "SELECT PROJ_CD, PROJ_NM FROM PROJ_INFO";
    PreparedStatement checkLogin =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    JSONArray results = new JSONArray();
    JSONObject object = new JSONObject();


    try {
      ResultSet data = checkLogin.executeQuery();
      while (data.next()) {
        JSONObject item = new JSONObject();
        item.put("PROJ_CD", data.getInt(1));
        item.put("PROJ_NM", data.getString(2));
        results.add(item);
      }
      object.put("result",1);
      object.put("content",results);
      object.put("msg","");
      return Response.ok(object).build();
    } catch (Exception e) {
      logger.info(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
      e.printStackTrace();
      checkLogin.close();
      con.close();
      object.put("result",0);
      object.put("msg",""+e.getMessage());
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

    Connection con = getSQLConnection();
    String query = "SELECT USER_ID, "
        + "USER_NM,"
        + "DEPT_NM,"
        + "to_char( WORK_YMD, 'YYYY-MM-DD')  WORK_YMD, "
        + "MCLS_CD,"
        + "DETAIL, "
        + "PROJ_CD,"
        + "PROJ_INFO.PROJ_NM PROJ_NM, "
        + "to_char(S_TIME, 'yyyy-mm-dd hh24:mi') S_TIME, "
        + "to_char(E_TIME, 'yyyy-mm-dd hh24:mi') E_TIME, "
        + "to_char(EXTRA_TIME, 'hh24:mi') EXTRA_TIME, "
        + "to_char(UPD_TIME, 'yyyy-mm-dd hh24:mi') UPD_TIME "
        + "FROM WORK_DETAIL, PROJ_INFO  "
        + "WHERE WORK_DETAIL.PRJ_CD = PROJ_INFO.PROJ_CD "
        + "and USER_ID =? and to_char(WORK_YMD,'yyyy-mm-dd') = ?";
    PreparedStatement getWorkingDay =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    JSONObject object = new JSONObject();
    JSONObject item = new JSONObject();
    JSONObject MCLS = new JSONObject();
    JSONObject PROJ = new JSONObject();
    try {
      getWorkingDay.setString(1, userId);
      getWorkingDay.setString(2, date);

      ResultSet data = getWorkingDay.executeQuery();

      if (data.first()) {
        object.put("result",1);

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
        object.put("content",item);

        object.put("msg","");

        return Response.ok(object).build();
      } else {

        object.put("result",0);
        object.put("msg","데이터가 없습니다.");
        return Response.ok(object).build();
      }
    } catch (Exception e) {
      logger.info(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
      e.printStackTrace();
      getWorkingDay.close();
      con.close();
      object.put("result",0);
      object.put("msg",""+e.getMessage());
      return Response.ok(object).build();

    } finally {
      //Close resources in all cases
      getWorkingDay.close();
      con.close();
    }
  }

  //워킹데이 수정
  @POST
  @Produces("application/json")
  @Path("/updateWorkingDay")
  public Response updateWorkingDay(@FormParam("LCLS_CD") String LCLS_CD,
      @FormParam("MCLS_CD") String MCLS_CD, @FormParam("DETAIL") String DETAIL,
      @FormParam("PRJ_CD") String PRJ_CD, @FormParam("S_TIME") String S_TIME,
      @FormParam("E_TIME") String E_TIME, @FormParam("UPD_TIME") String UPD_TIME,
      @FormParam("USER_ID") String USER_ID, @FormParam("date") String date
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

    PreparedStatement updateWorkingDay =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    JSONObject obj = new JSONObject();
    try {
      updateWorkingDay.setString(1, LCLS_CD);
      updateWorkingDay.setString(2, MCLS_CD);
      updateWorkingDay.setString(3, DETAIL);
      updateWorkingDay.setString(4, PRJ_CD);
      updateWorkingDay.setString(5, S_TIME);
      updateWorkingDay.setString(6, E_TIME);
      updateWorkingDay.setString(7, E_TIME);
      updateWorkingDay.setString(8, S_TIME);
      updateWorkingDay.setString(9, USER_ID);
      updateWorkingDay.setString(10, date);
      updateWorkingDay.setString(11, UPD_TIME);
      updateWorkingDay.setString(12, USER_ID);
      updateWorkingDay.setString(13, date);

      int cnt = updateWorkingDay.executeUpdate();

      if (cnt > 0) {
        //업데이트 성공
        String query1 = "SELECT USER_ID, "
            + "USER_NM,"
            + "DEPT_NM,"
            + "to_char( WORK_YMD, 'YYYY-MM-DD')  WORK_YMD, "
            + "MCLS_CD,"
            + "DETAIL, "
            + "PROJ_CD,"
            + "PROJ_INFO.PROJ_NM PROJ_NM, "
            + "to_char(S_TIME, 'yyyy-mm-dd hh24:mi') S_TIME, "
            + "to_char(E_TIME, 'yyyy-mm-dd hh24:mi') E_TIME, "
            + "to_char(EXTRA_TIME, 'hh24:mi') EXTRA_TIME, "
            + "to_char(UPD_TIME, 'yyyy-mm-dd hh24:mi') UPD_TIME "
            + "FROM WORK_DETAIL, PROJ_INFO  "
            + "WHERE WORK_DETAIL.PRJ_CD = PROJ_INFO.PROJ_CD "
            + "and USER_ID =? and to_char(WORK_YMD,'yyyy-mm-dd') = ?";
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
            object.put("result",1);

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
            object.put("content",item);

            object.put("msg","");

            return Response.ok(object).build();
          } else {

            object.put("result",0);
            object.put("msg","데이터가 없습니다.");
            return Response.ok(object).build();
          }
        } catch (Exception e) {
          logger.info(e.getMessage());
          logger.log(Level.INFO, e.getMessage(), e);
          e.printStackTrace();
          getWorkingDay.close();
          con.close();
          object.put("result",0);
          object.put("msg",""+e.getMessage());
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
      obj.put("result",0);
      obj.put("msg",""+e.getMessage());
      return Response.ok(obj).build();
    } finally {
      //Close resources in all cases
      updateWorkingDay.close();
      con.close();
    }
  }

  public void updateResult(){

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

      if (!newPwd.equals(newPwdConfirm)) {
        res.put("result",0);
        res.put("msg", "비밀번호 확인 불일치.");
        return Response.ok(res).build();
      }
      if (newPwd.equals(curPwd)) {
        res.put("result",0);
        res.put("msg", "기존 비밀번호와 동일합니다.");
        return Response.ok(res).build();
      }

      changePwd.setString(1, newPwd);
      changePwd.setString(2, userId);
      changePwd.setString(3, curPwd);

      int cnt = changePwd.executeUpdate();

      if (cnt > 0) {
        //업데이트 성공
        res.put("result", 1);
        res.put("msg","");
        return Response.ok(res).build();
      } else {
        //변경 내역이 없음 => 현재비밀번호 틀림
        res.put("result",0);
        res.put("msg", "현재비밀번호가 틀렸습니다.");

        return Response.ok(res).build();
      }
    } catch (Exception e) {
      //Trying to create a user that already exists
      logger.info(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
      e.printStackTrace();

      res.put("result",0);
      res.put("msg", ""+e.getMessage());

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
  public Response getSummary() throws SQLException {

    Connection con = getSQLConnection();
    String query = "select w.user_nm, p.proj_nm, w.mcls_cd, w.detail  from work_detail w, PROJ_INFO p where w.prj_cd=p.proj_cd "
        + "and to_char(work_ymd,'yyyy-mm-dd')=to_char(sysdate,'yyyy-mm-dd')";
    PreparedStatement getWorkingDay =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);


    JSONObject object = new JSONObject();

    try {
      ResultSet data = getWorkingDay.executeQuery();
      JSONArray res = new JSONArray();
      while(data.next()){
        JSONObject item = new JSONObject();
        item.put("NAME",data.getString(1));
        item.put("PROJ_NM",data.getString(2));
        item.put("MCLS_CD",data.getString(3));
        item.put("DETAIL",data.getString(4));

        res.add(item);

      }

      object.put("result",1);
      object.put("content", res);
      object.put("msg","");

      return Response.ok(object).build();


    } catch (Exception e) {
      logger.info(e.getMessage());
      logger.log(Level.INFO, e.getMessage(), e);
      e.printStackTrace();
      getWorkingDay.close();
      con.close();

      object.put("result",0);
      object.put("msg",""+e.getMessage());
      return Response.ok(object).build();
    } finally {
      //Close resources in all cases
      getWorkingDay.close();
      con.close();
    }
  }


}