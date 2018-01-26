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

import static org.apache.http.HttpHeaders.FROM;

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
  public Response getDetailOperationRate(@QueryParam("YEAR") int year,
      @QueryParam("DEPT_CD") int code)
      throws SQLException {
    final Connection connection = getSQLConnection();
    final String sql =
        "SELECT * FROM TABLE(WORK_STRU.WORK_STRU_TB('" + year + "', '" + code + "'))";
    final PreparedStatement preparedStatement =
        connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
            ResultSet.CONCUR_READ_ONLY);
    final ResultSet resultSet = preparedStatement.executeQuery();
    final JSONObject resultJson = new JSONObject();
    final JSONArray items = new JSONArray();
    boolean isResult = false;
    while (resultSet.next()) {
      isResult = true;
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

    if (isResult) {
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
  public Response getSummaryTotal(@QueryParam("DEPT_CD") String dept_cd,
      @QueryParam("YEAR") int year) throws SQLException {
    Connection con = getSQLConnection();
    String query = "WITH DET_TMP AS( \n"
        + "SELECT A.LCLS_CD, \n"
        + "COUNT(1) \n"
        + "AS CNT \n"
        + "FROM WORK_DETAIL A, USER_INFO B \n"
        + "WHERE A.USER_ID = B.USER_ID \n"
        + "AND A.WORK_YMD \n"
        + "BETWEEN ? || '0101' \n"
        + "AND ? || '1231' \n"
        + "AND B.DEPT_CD = ? \n"
        + "GROUP BY A.LCLS_CD)\n"
        + "SELECT L.LCLS_NM, L.LCLS_CD, NVL(D.CNT, 0 ) \n"
        + "AS CNT \n"
        + "FROM WORK_LCLASS L, DET_TMP D \n"
        + "WHERE L.LCLS_CD = D.LCLS_CD(+) \n"
        + "ORDER BY L.LCLS_CD";

    // 1 : YEAR
    // 2 : YEAR
    // 3 : DEPT_CD
    PreparedStatement preparedStatement =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
    JSONObject object = new JSONObject();
    try {
      preparedStatement.setString(1, String.valueOf(year));
      preparedStatement.setString(2, String.valueOf(year));
      preparedStatement.setString(3, dept_cd);
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

  /**
   * 통계 > 가동률 > 연간 가동률
   *
   * @param code 부서 코드
   * @param year 연도
   * @return Response
   * @throws SQLException
   */
  @GET
  @Produces("application/json")
  @Path("/getYearOperationRate")
  public Response getYearOperatingRatio(@QueryParam("YEAR") int year,
      @QueryParam("DEPT_CD") int code)
      throws SQLException {

    Connection con = getSQLConnection();
    String query = "SELECT * FROM TABLE(WORK_STRU.MONTH_STRU_TB('" + year + "', '" + code + "'))";

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

  /**
   * 통계 > 가동률 > 현재 가동률
   *
   * @param code 부서 코드
   * @param year 연도
   * @return Response
   * @throws SQLException
   */
  @GET
  @Produces("application/json")
  @Path("/getCurrentOperationRate")
  public Response getCurrentOperatingRatio(@QueryParam("YEAR") int year,
      @QueryParam("DEPT_CD") int code)
      throws SQLException {

    final Connection con = getSQLConnection();
    final String query =
        "SELECT * FROM TABLE(WORK_STRU.MAN_STRU_TB('" + year + "', '" + code + "'))";

    final PreparedStatement preparedStatement =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    JSONObject object = new JSONObject();

    try {
      ResultSet data = preparedStatement.executeQuery();
      JSONArray items = new JSONArray();

      boolean flag = false;
      while (data.next()) {
        flag = true;
        JSONObject item = new JSONObject();
        item.put("user_id", data.getString(1));
        item.put("user_nm", data.getString(2));
        item.put("man_rate", data.getString(3));
        item.put("tot_rate", data.getString(4));
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
      object.put("result", Constants.RESULT_FAILURE);
      object.put("msg", "" + e.getMessage());
      return Response.ok(object).build();
    } finally {
      //Close resources in all cases
      preparedStatement.close();
      con.close();
    }
  }

  @POST
  @Produces("application/json")
  @Path("/createWorkCalendarDb")
  public Response createWorkCalendarDb(@FormParam("YEAR") int year,
      @FormParam("USER_ID") String userId) throws SQLException {
    String query = "{call MOBILE.INSERT_YEAR_WORK_DAY_SP(?,?)}";
    userId = "SYSTEM";
    JSONObject object = new JSONObject();
    try {
      Connection connection = getSQLConnection();
      CallableStatement cstmt = connection.prepareCall(query);
      cstmt.setString(1, String.valueOf(year));
      cstmt.setString(2, userId);
      final boolean isExecuted = cstmt.execute();
      Log.d("isExecuted = " + isExecuted);
      object.put("result", Constants.RESULT_SUCCESS);
      object.put("msg", year + " WORK_CALENDAR를 생성하였습니다.");
    } catch (Exception e) {
      e.printStackTrace();
      object.put("result", Constants.RESULT_FAILURE);
      object.put("msg", e.getMessage());
    }

    return Response.ok(object).build();
  }

  //통계 > 가동률 > 현재 가동률
  @GET
  @Produces("application/json")
  @Path("/getHolidays")
  public Response getHolidays(@QueryParam("YEAR") int year)
      throws SQLException {
    Log.d("year = " + year);
    final Connection con = getSQLConnection();
    final String query =
        "SELECT * FROM HOLIDAY WHERE WORK_YMD LIKE '"+year+"%'";
    Log.d("query = " + query);
    final PreparedStatement preparedStatement =
        con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

    JSONObject object = new JSONObject();

    try {
      //preparedStatement.setString(1, String.valueOf(year));
      ResultSet data = preparedStatement.executeQuery();
      JSONArray items = new JSONArray();

      boolean flag = false;
      while (data.next()) {
        flag = true;
        JSONObject item = new JSONObject();
        item.put("WORK_YMD", data.getString(1));
        item.put("HOLIDAY_NM", data.getString(2));
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
      object.put("result", Constants.RESULT_FAILURE);
      object.put("msg", "" + e.getMessage());
      return Response.ok(object).build();
    } finally {
      //Close resources in all cases
      preparedStatement.close();
      con.close();
    }
  }
}
