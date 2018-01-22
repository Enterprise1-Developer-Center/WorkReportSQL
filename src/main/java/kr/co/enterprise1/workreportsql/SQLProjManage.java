package kr.co.enterprise1.workreportsql;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.mfp.adapter.api.AdaptersAPI;
import com.ibm.mfp.adapter.api.ConfigurationAPI;
import org.apache.http.util.TextUtils;

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
public class SQLProjManage {
    private final static AdapterLog Log = new AdapterLog(SQLProjManage.class.getName());
    @Context
    ConfigurationAPI configurationAPI;

    @Context
    AdaptersAPI adaptersAPI;

    public Connection getSQLConnection() throws SQLException {
        // Create a connection object to the database
        WorkReportSQLApplication app = adaptersAPI.getJaxRsApplication(WorkReportSQLApplication.class);
        return app.dataSource.getConnection();
    }

    //프로젝트 정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getProjects2")
    public Response getProjects2(@QueryParam("DEPT_CD") String deptCD) throws SQLException {

        Connection con = getSQLConnection();
        String query = "SELECT * FROM PROJ_INFO WHERE PROJ_CD != 0";

        PreparedStatement preparedStatement =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);

        JSONArray results = new JSONArray();
        JSONObject object = new JSONObject();

        try {
            ResultSet data = preparedStatement.executeQuery();
            while (data.next()) {
                JSONObject item = new JSONObject();
                item.put("PROJ_CD", data.getString(1));
                item.put("PROJ_NM", data.getString(2));
                item.put("DEPT_NM", data.getString(8));
                item.put("PROJ_SDATE", data.getString(9));
                item.put("PROJ_EDATE", data.getString(10));
                item.put("DEPT_CD", data.getString(11));

                results.add(item);
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", results);
            object.put("msg", "");
            return Response.ok(object).build();
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            preparedStatement.close();
            con.close();
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            preparedStatement.close();
            con.close();
        }
    }

    //부서정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getDepts")
    public Response getDepts() throws SQLException {

        Connection con = getSQLConnection();
        String query = "SELECT * FROM DEPT_INFO";

        PreparedStatement preparedStatement =
                con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);

        JSONArray results = new JSONArray();
        JSONObject object = new JSONObject();

        try {
            ResultSet data = preparedStatement.executeQuery();
            while (data.next()) {
                JSONObject item = new JSONObject();
                item.put("DEPT_CD", data.getString(1));
                item.put("DEPT_NM", data.getString(2));

                results.add(item);
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", results);
            object.put("msg", "");
            return Response.ok(object).build();
        } catch (Exception e) {
            Log.d(e.getMessage(), e);
            preparedStatement.close();
            con.close();
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            preparedStatement.close();
            con.close();
        }
    }

    //프로젝트 추가
    @POST
    @Produces("application/json")
    @Path("/addProject")
    public Response addProject(@FormParam("PROJ_CD") String proj_cd,
                               @FormParam("PROJ_NM") String proj_nm, @FormParam("PROJ_SDATE") String proj_sdate,
                               @FormParam("PROJ_EDATE") String proj_edate, @FormParam("DEPT_CD") String dept_cd
    ) throws SQLException {
        if (TextUtils.isBlank(proj_cd) || TextUtils.isBlank(proj_nm) || TextUtils.isBlank(proj_sdate) || TextUtils.isBlank(proj_edate) || TextUtils.isBlank(dept_cd)) {
            JSONObject result = new JSONObject();
            result.put("result", Constants.RESULT_FAILURE);
            result.put("msg", "모두 입력하셔야 합니다");
            return Response.ok(result).build();
        }

        String query = "MERGE INTO PROJ_INFO P\n" +
                "USING DUAL\n" +
                "   ON (P.PROJ_CD = '" + proj_cd + "')\n" +
                "WHEN MATCHED THEN\n" +
                "     UPDATE SET\n" +
                "        PROJ_NM = '" + proj_nm + "'\n" +
                "      , REMARK = 'TEST2'\n" +
                "      , DEPT_NM = 'TEST3'\n" +
                "      , PROJ_SDATE = '" + proj_sdate + "'\n" +
                "      , PROJ_EDATE = '" + proj_edate + "'\n" +
                "      , DEPT_CD = '" + dept_cd + "'\n" +
                "      , UPD_ID = 'TEST4'\n" +
                "      , UPD_DTM = SYSDATE\n" +
                "WHEN NOT MATCHED THEN\n" +
                "     INSERT (\n" +
                "                PROJ_CD\n" +
                "              , PROJ_NM\n" +
                "              , REMARK\n" +
                "              , DEPT_NM\n" +
                "              , PROJ_SDATE\n" +
                "              , PROJ_EDATE\n" +
                "              , DEPT_CD\n" +
                "              , UPD_ID\n" +
                "              , UPD_DTM\n" +
                "              , CRE_ID\n" +
                "              , CRE_DTM\n" +
                "            )\n" +
                "     VALUES (\n" +
                "                '" + proj_cd + "'\n" +
                "              , '" + proj_nm + "'\n" +
                "              , 'TEST2'\n" +
                "              , 'TEST3'\n" +
                "              , '" + proj_sdate + "'\n" +
                "              , '" + proj_edate + "'\n" +
                "              , '" + dept_cd + "'\n" +
                "              , 'TEST4'\n" +
                "              , SYSDATE\n" +
                "              , 'TEST4'\n" +
                "              , SYSDATE\n" +
                "             )";

        Connection connection = getSQLConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        JSONObject result = new JSONObject();
        try {
            preparedStatement.execute();
            result.put("result", Constants.RESULT_SUCCESS);
            result.put("msg", "프로젝트 추가되었습니다.");
            return Response.ok(result).build();
        } catch (Exception e) {
            //Trying to create a user that already exists
            Log.d(e.getMessage(), e);
            result.put("result", Constants.RESULT_FAILURE);
            result.put("msg", "" + e.getMessage());
            return Response.ok(result).build();
        } finally {
            //Close resources in all cases
            preparedStatement.close();
            connection.close();
        }
    }

    //프로젝트 수정
    @POST
    @Produces("application/json")
    @Path("/editProject")
    public Response editProject(@FormParam("PROJ_CD") String proj_cd,
                                @FormParam("PROJ_NM") String proj_nm, @FormParam("PROJ_SDATE") String proj_sdate,
                                @FormParam("PROJ_EDATE") String proj_edate, @FormParam("DEPT_CD") String dept_cd
    ) throws SQLException {
        if (TextUtils.isBlank(proj_cd) || TextUtils.isBlank(proj_nm) || TextUtils.isBlank(proj_sdate) || TextUtils.isBlank(proj_edate) || TextUtils.isBlank(dept_cd)) {
            JSONObject result = new JSONObject();
            result.put("result", Constants.RESULT_FAILURE);
            result.put("msg", "모두 입력하셔야 합니다");
            return Response.ok(result).build();
        }

        String query = "MERGE INTO PROJ_INFO P\n" +
                "USING DUAL\n" +
                "   ON (P.PROJ_CD = '" + proj_cd + "')\n" +
                "WHEN MATCHED THEN\n" +
                "     UPDATE SET\n" +
                "        PROJ_NM = '" + proj_nm + "'\n" +
                "      , REMARK = 'TEST2'\n" +
                "      , DEPT_NM = 'TEST3'\n" +
                "      , PROJ_SDATE = '" + proj_sdate + "'\n" +
                "      , PROJ_EDATE = '" + proj_edate + "'\n" +
                "      , DEPT_CD = '" + dept_cd + "'\n" +
                "      , UPD_ID = 'TEST4'\n" +
                "      , UPD_DTM = SYSDATE\n" +
                "WHEN NOT MATCHED THEN\n" +
                "     INSERT (\n" +
                "                PROJ_CD\n" +
                "              , PROJ_NM\n" +
                "              , REMARK\n" +
                "              , DEPT_NM\n" +
                "              , PROJ_SDATE\n" +
                "              , PROJ_EDATE\n" +
                "              , DEPT_CD\n" +
                "              , UPD_ID\n" +
                "              , UPD_DTM\n" +
                "              , CRE_ID\n" +
                "              , CRE_DTM\n" +
                "            )\n" +
                "     VALUES (\n" +
                "                '" + proj_cd + "'\n" +
                "              , '" + proj_nm + "'\n" +
                "              , 'TEST2'\n" +
                "              , 'TEST3'\n" +
                "              , '" + proj_sdate + "'\n" +
                "              , '" + proj_edate + "'\n" +
                "              , '" + dept_cd + "'\n" +
                "              , 'TEST4'\n" +
                "              , SYSDATE\n" +
                "              , 'TEST4'\n" +
                "              , SYSDATE\n" +
                "             )";

        Connection connection = getSQLConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        JSONObject result = new JSONObject();
        try {
            preparedStatement.executeUpdate();
            result.put("result", Constants.RESULT_SUCCESS);
            result.put("msg", "프로젝트가 수정되었습니다.");
            return Response.ok(result).build();
        } catch (Exception e) {
            //Trying to create a user that already exists
            Log.d(e.getMessage(), e);
            result.put("result", Constants.RESULT_FAILURE);
            result.put("msg", "" + e.getMessage());
            return Response.ok(result).build();
        } finally {
            //Close resources in all cases
            preparedStatement.close();
            connection.close();
        }
    }

    //프로젝트 제거
    @POST
    @Produces("application/json")
    @Path("/delProject")
    public Response delProject(@FormParam("PROJ_CD") String projCode) throws SQLException {
        Log.d("delProject(" + projCode + ")");
        Connection connection = getSQLConnection();
        String query = "DELETE FROM PROJ_INFO WHERE PROJ_CD = ?";

        PreparedStatement preparedStatement =
                connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);

        JSONObject object = new JSONObject();

        try {
            preparedStatement.setString(1, projCode);
            // ResultSet data =
            preparedStatement.executeQuery();
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("msg", "프로젝트가 삭제되었습니다.");
            return Response.ok(object).build();
        } catch (Exception e) {
            object.put("result", Constants.RESULT_FAILURE);
            object.put("msg", "" + e.getMessage());
            return Response.ok(object).build();
        } finally {
            //Close resources in all cases
            preparedStatement.close();
            connection.close();
        }
    }

    //프로젝트 상제 정보 가져오기, 투입인원 현황
    @GET
    @Produces("application/json")
    @Path("/getEmployees")
    public Response getEmployees() throws SQLException {

        Connection connection = getSQLConnection();
        String sql = "SELECT PD.*, UI.USER_NM, PI.PROJ_NM FROM PROJ_DETAIL PD, USER_INFO UI, PROJ_INFO PI WHERE PD.USER_ID = UI.USER_ID AND PD.PROJ_CD = PI.PROJ_CD";

        PreparedStatement preparedStatement =
                connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                        ResultSet.CONCUR_READ_ONLY);

        JSONArray results = new JSONArray();
        JSONObject object = new JSONObject();

        try {
            ResultSet data = preparedStatement.executeQuery();
            while (data.next()) {
                JSONObject item = new JSONObject();
                item.put("PROJ_CD", data.getString(1));
                item.put("USER_ID", data.getString(2));
                item.put("USER_SDATE", data.getString(3));
                item.put("USER_EDATE", data.getString(4));
                item.put("LCLS_CD", data.getString(9));
                item.put("MCLS_CD", data.getString(10));
                item.put("USER_NM", data.getString(11));
                item.put("PROJ_NM", data.getString(12));

                results.add(item);
            }
            object.put("result", Constants.RESULT_SUCCESS);
            object.put("content", results);
            object.put("msg", "");
            return Response.ok(object).build();
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

    //프로젝트 상제 정보 가져오기, 투입인원 현황
    @GET
    @Produces("application/json")
    @Path("/getUserTypes")
    public Response getUserTypes() throws SQLException {
        JSONArray jsonArray = new JSONArray();
        JSONObject contents = new JSONObject();
        contents.put("TYPE_CD", "1");
        contents.put("TYPE_NM", "정규");
        jsonArray.add(contents);
        contents = new JSONObject();
        contents.put("TYPE_CD", "2");
        contents.put("TYPE_NM", "외부");
        jsonArray.add(contents);
        JSONObject object = new JSONObject();

        object.put("result", Constants.RESULT_SUCCESS);
        object.put("content", jsonArray);
        object.put("msg", "");
        return Response.ok(object).build();
    }
}
