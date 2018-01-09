package kr.co.enterprise1.workreportsql;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.mfp.adapter.api.AdaptersAPI;
import com.ibm.mfp.adapter.api.ConfigurationAPI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * Created by jaeho on 2018. 1. 9
 */
@Path("/")
public class SQLTeamReport {

    private final static AdapterLog Log = new AdapterLog(SQLTeamReport.class.getName());
    @Context
    ConfigurationAPI configurationAPI;

    @Context
    AdaptersAPI adaptersAPI;

    public Connection getSQLConnection() throws SQLException {
        // Create a connection object to the database
        WorkReportSQLApplication app = adaptersAPI.getJaxRsApplication(WorkReportSQLApplication.class);
        return app.dataSource.getConnection();
    }


    //워킹데이 요약 정보 가져오기
    @GET
    @Produces("application/json")
    @Path("/getSummary")
    public Response getSummary(@QueryParam("DEPT_NM") String DEPT_NM) throws SQLException {

        Connection con = getSQLConnection();
        String query = "SELECT W.USER_ID\n" +
                "     , A.USER_NM\n" +
                "     , P.PROJ_NM\n" +
                "     , W.MCLS_CD\n" +
                "     , W.DETAIL\n" +
                "  FROM USER_INFO A\n" +
                "     , DEPT_INFO B\n" +
                "     , WORK_DETAIL W\n" +
                "     , PROJ_INFO P\n" +
                " WHERE A.USER_ID = W.USER_ID\n" +
                "   AND A.DEPT_CD = B.DEPT_CD\n" +
                "   AND W.PROJ_CD = P.PROJ_CD\n" +
                "   AND TO_CHAR(W.WORK_YMD, 'YYYYMMDD') = TO_CHAR(SYSDATE, 'YYYYMMDD')\n" +
                "   AND B.DEPT_NM =?\n";
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
                object.put("result", Constants.RESULT_SUCCESS);
                object.put("content", res);
                object.put("msg", "");

                return Response.ok(object).build();
            } else {
                object.put("result", Constants.RESULT_FAILURE);
                object.put("msg", "부서명을 정확하게 입력해주세요.");

                return Response.ok(object).build();
            }
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

}
