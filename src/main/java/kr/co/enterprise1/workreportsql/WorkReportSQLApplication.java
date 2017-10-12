/*
 *    Licensed Materials - Property of IBM
 *    5725-I43 (C) Copyright IBM Corp. 2015, 2016. All Rights Reserved.
 *    US Government Users Restricted Rights - Use, duplication or
 *    disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*/

package kr.co.enterprise1.workreportsql;

import java.util.logging.Logger;

import com.ibm.mfp.adapter.api.ConfigurationAPI;
import com.ibm.mfp.adapter.api.MFPJAXRSApplication;
import org.apache.commons.dbcp.BasicDataSource;

import javax.ws.rs.core.Context;

public class WorkReportSQLApplication extends MFPJAXRSApplication {

    static Logger logger = Logger.getLogger(WorkReportSQLApplication.class.getName());

    public BasicDataSource dataSource = null;

    @Context
    ConfigurationAPI configurationAPI;

    @Override
    protected void init() throws Exception {
        logger.info("Adapter initialized!");

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUrl(configurationAPI.getPropertyValue("DB_url"));
        dataSource.setUsername(configurationAPI.getPropertyValue("DB_username"));
        dataSource.setPassword(configurationAPI.getPropertyValue("DB_password"));
    }

    protected void destroy() throws Exception {
        logger.info("Adapter destroyed!");
    }

    @Override
    protected String getPackageToScan() {
        //The package of this class will be scanned (recursively) to find JAX-RS resources.
        return getClass().getPackage().getName();
    }
}
