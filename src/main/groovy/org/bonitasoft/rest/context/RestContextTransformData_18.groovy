package org.bonitasoft.rest.context;

import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/* ******************************************************************************** */
/*                                                                                                                                                                  */
/* RestContextTransformData    for 1.8                                                                                                                                    */
/*                                                                                                                                                                  */
/*  For an Data and the pilot, calcul the result to send.         */
/*                                                                                                                                                                  */
/* ******************************************************************************** */


/* -------------------------------------------------------------------------------- */
/*                                                                                  */
/*  class ContextCaseId                                                             */
/*                                                                                  */
/* -------------------------------------------------------------------------------- */

public class RestContextTransformData_18
{
    private static Logger logger = Logger.getLogger("org.bonitasoft.rest.context.RestContextTransformData_18");

    static DateTimeFormatter dtfDay = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    static DateTimeFormatter dtfHour = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    static DateTimeFormatter dtfHourAbsolute = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String getTimeFromJDK18( String varName, Object data, String translatorAction )
    {
        logger.info("Transform ["+varName+"] class["+data.getClass().getName()+"] action["+translatorAction+"]");
        if (data.getClass().getName().equals("java.time.LocalDate"))
        {
            java.time.LocalDate dataLocalDate = (java.time.LocalDate) data;
            return dtfDay.format( dataLocalDate );
        }
        if (data.getClass().getName().equals("java.time.OffsetDateTime"))
        {
            java.time.OffsetDateTime dataLocalDate = (java.time.OffsetDateTime) data;

            if ("absolute".equals(translatorAction))
                return dtfHourAbsolute.format(dataLocalDate);
            return dtfHour.format(dataLocalDate);
        }
        if (data.getClass().getName().equals("java.time.LocalDateTime"))
        {
            java.time.LocalDateTime dataLocalDate = (java.time.LocalDateTime) data;
            if ("absolute".equals(translatorAction))
                return dtfHourAbsolute.format(dataLocalDate);
            return dtfHour.format(dataLocalDate);
        }
        return null;
    }
}