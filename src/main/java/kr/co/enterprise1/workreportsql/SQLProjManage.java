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
}
