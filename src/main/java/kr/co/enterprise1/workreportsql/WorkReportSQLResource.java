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

@Path("/")
public class WorkReportSQLResource {
    /*
     * For more info on JAX-RS see https://jax-rs-spec.java.net/nonav/2.0-rev-a/apidocs/index.html
  */
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

    @POST
    public Response createUser(@FormParam("userId") String userId,
                               @FormParam("userPw") String userPw
    ) throws SQLException {
        Log.d("userId = " + userId + ", userPw = " + userPw);
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
        Log.d("userId = " + userId);
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
            Log.d(e.getMessage(), e);
            getUser.close();
            con.close();
            return Response.status(Status.NOT_FOUND).entity("User not found...").build();
        } finally {
            //Close resources in all cases
            getUser.close();
            con.close();
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
}