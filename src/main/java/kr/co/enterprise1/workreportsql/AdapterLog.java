package kr.co.enterprise1.workreportsql;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jaeho on 2018. 1. 9
 */
public class AdapterLog {
    private Logger logger;

    public AdapterLog(String className) {
        logger = Logger.getLogger(className);
    }


    public void d(String msg) {
        logger.info(msg);
        logger.log(Level.ALL, msg);
    }

    public void d(String msg, Throwable throwable) {
        logger.log(Level.ALL, msg, throwable);
    }
}
