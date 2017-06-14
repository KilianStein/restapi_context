package org.bonitasoft.rest.context;

import java.util.logging.Logger;

/* -------------------------------------------------------------------------------- */
/*                                                                                  */
/*  class PerformanceTrace                                                          */
/*                                                                                  */
/* -------------------------------------------------------------------------------- */
class RestContextTrackPerformance {


    private static Logger logger = Logger.getLogger(RestContextTrackPerformance.class.getName());

    private List<Map<String,Object>> listOperations = new ArrayList<Map<String,Object>>();
    private List<Map<String,Object>> listSubOperations = new ArrayList<Map<String,Object>>();
    long totalTime=0;
    public void addMarker(String operation) {
        long currentTime= System.currentTimeMillis();
        Map<String,Object> oneOperation = new HashMap<String,Object>();
        oneOperation.put("t",System.currentTimeMillis());
        oneOperation.put("n",operation);
        listOperations.add( oneOperation );
    }


    /**
     *
     * @param subOperation
     * @param timeSubOperation
     */
    public void addSubOperation(String subOperationName, long timeSubOperation) {
        Map<String,Object> oneOperation = new HashMap<String,Object>();
        oneOperation.put("t",timeSubOperation);
        oneOperation.put("n",subOperationName);
        listSubOperations.add( oneOperation );
    }

    /**
     * start the subPperation. Return an object, which has to be given a the endSubOperation
     * @param subOperationName
     * @return
     */
    public Map<String,Object> startSubOperation(String subOperationName) {
        Map<String,Object> oneOperation = new HashMap<String,Object>();
        long currentTime= System.currentTimeMillis();
        oneOperation.put("s",currentTime);
        oneOperation.put("n",subOperationName);
        return oneOperation;
    }
    /**
     * end the siboperation. Calculate the time and register it
     * @param subOperation
     */
    public void  endSubOperation(Map<String,Object> subOperation) {
        long currentTime= System.currentTimeMillis();
        long startTime = (Long) subOperation.get( "s");

        subOperation.put("t",currentTime - startTime );
        listSubOperations.add( subOperation );
    }




    public String trace() {
        String result="";


        Collections.sort(listSubOperations, new Comparator<Map<String,Object>>() {
                    public int compare(Map<String,Object> s1,
                            Map<String,Object> s2) {
                        Long t1= Long.valueOf( s1.get("t") );
                        Long t2= Long.valueOf( s2.get("t") );
                        // in first the BIGGER time
                        return t2.compareTo( t1 );
                    }
                });

        result+=" SUB_OPERATION:";
        // first sub operation
        for (int i=1;i<listSubOperations.size();i++) {
            result+= listSubOperations.get( i ).get("n")+":"+listSubOperations.get( i ).get("t")+" ms,";
        }

        result+="### MAIN_MARKER:";
        // then MAIN
        for (int i=1;i<listOperations.size();i++) {
            String time = ((long) listOperations.get( i ).get("t")) - ((long)listOperations.get( i-1 ).get("t"));
            result+= listOperations.get( i ).get("n")+":"+time+" ms,";
        }
        totalTime = ((long) listOperations.get( listOperations.size()-1 ).get("t")) - ((long)listOperations.get( 0 ).get("t"));
        result+="Total "+totalTime+" ms";

        return result;
    }
    public long getTotalTime() {
        return totalTime;
    }
}
