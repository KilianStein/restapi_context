package org.bonitasoft.rest.context;

import java.util.logging.Logger

import org.bonitasoft.engine.api.ProcessAPI
import org.bonitasoft.engine.bpm.data.ArchivedDataNotFoundException
import org.bonitasoft.engine.bpm.data.DataNotFoundException
import org.bonitasoft.engine.bpm.flownode.ActivityInstance
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstance
import org.bonitasoft.engine.bpm.flownode.ArchivedActivityInstanceSearchDescriptor
import org.bonitasoft.engine.bpm.parameter.ParameterCriterion
import org.bonitasoft.engine.bpm.parameter.ParameterInstance
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.exception.RetrieveException
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser


/* ******************************************************************************** */
/*                                                                                                                                                                  */
/* RestContextPilot                                                                                                                                    */
/*                                                                                                                                                                  */
/*  return the pilot to control what the REST API has to return                                                           */
/*                                                                                                                                                                  */
/* Pilot And ContextCaseId                                                                                                                      */
/*  theses two class must know each other, but their role is different :                                            */
/*     PILOT control the final result. The pilot is initialized from a String, and to find this string     */
/*       (it could be save in a local / global variable name, a parameters) then the Pilot need         */
/*      to get information from the ContextRestCaseid                                                                          */
/*   CONTEXTCASEID : The context Case Id keep all information about the caseId. It retrieve         */
/*      these information from the URL, from a caseId, a taskId or a contentStorageid                     */
/*                                                                                                                                                                  */
/*                                                                                                                                                                  */
/* ******************************************************************************** */

public class RestContextPilot {

    private static Logger logger = Logger.getLogger("org.bonitasoft.rest.context.RestContextPilot");

    /** describe the current analysis */
    String analysisString;

    String errorMessage;

    Map<String,Object> pilotDataMap;

    Object contextDataOrigin =null;
    String contextDataSt = null;

    /**
     * after the decodeParameters, we get the cont
     */
    RestContextCaseId contextCaseId;

    RestContextTrackPerformance trackPerformance;

    boolean isAllowAllVariables;
    boolean isPilotDetected=false;


    public void setContextCaseId(RestContextCaseId contextCaseId, RestContextTrackPerformance trackPerformance) {
        this.contextCaseId = contextCaseId;
        this.trackPerformance = trackPerformance;
    }
    /**
     * decode the pilot
     * @param contextCaseId
     */
    public void decodeParameters() {
        analysisString="";
        errorMessage=null;
        pilotDataMap=null;

        this.isPilotDetected= false;

        if (contextCaseId.taskInstanceId !=null) {
            try {
                contextDataOrigin = contextCaseId.processAPI.getActivityDataInstance("context", contextCaseId.taskInstanceId);
                contextDataSt = contextDataOrigin.getValue();
                this.isPilotDetected= true;
                analysisString+="ActivityDataInstance[context] value="+contextDataOrigin.getValue();
            } catch(DataNotFoundException dnte )
            // Ok, no worry, let's try different options
            {}
        }

        // first level is special because the case may be archived. Then, no need to search all the hierarchy (if we have a hierary, that's mean the case is alive)
        if (contextDataOrigin == null)
            searchContextData(contextCaseId.processInstanceId , contextCaseId.archivedProcessInstance,  contextCaseId.processDefinitionId );


        if (contextDataOrigin == null && contextCaseId.processInstanceRoot!=null)
            searchContextData(contextCaseId.processInstanceRoot.getId() , null, contextCaseId.processInstanceRoot.getProcessDefinitionId() );



        //  still null and a parent process ? Look here

        // no data were given : create a default one
        if (contextDataOrigin == null)
        {
            contextDataSt = "{ \"*\" : \"all\" }";
            analysisString+="Default value="+contextDataSt;
        }

        isAllowAllVariables=false;

        final JSONParser jsonParser = new JSONParser();
        final JSONObject pilotData = (JSONObject) jsonParser.parse(contextDataSt);

        if (pilotData == null ) {
            errorMessage= "The JSON information is missing : "+analysisString;
        }
        else if (! pilotData instanceof Map)
        {
            errorMessage=  "Local Variable [context] must be a MAP";
        }
        else {
            pilotDataMap = (Map<String,Object>) pilotData;

            // for optimisation, we calculated now the isAllowAllVariables
            for (Object varNameIt : pilotDataMap.keySet())
            {
                if ("*".equals(varNameIt))
                {
                    isAllowAllVariables=true;
                }
            }
        }
        contextCaseId.log( analysisString);
    }

    public String getAnalysisString()
    { return  analysisString; }
    ;

    public boolean isPilotDetected()
    { return this.isPilotDetected; }


    /**
     * different getter
     * @return
     */
    public  Map<String,Object> getPilotDataMap()
    {
        return pilotDataMap;
    }

    public String getActionFromVariable( String varName )
    {
        Object varObj = pilotDataMap.get( varName );
        if (varObj==null)
            return varObj;
        return varObj.toString();
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }


    /**
     *
     */
    private void searchContextData( Long processInstanceId , Long archivedProcessInstanceId,  Long processDefinitionId )
    {
        if (processInstanceId !=null)
        {
            try
            {
                contextDataOrigin = contextCaseId.processAPI.getProcessDataInstance("globalcontext", processInstanceId);
                this.isPilotDetected= true;

                contextDataSt = contextDataOrigin.getValue();
                analysisString+="ProcessDataInstance[globalcontext] value="+contextDataOrigin.getValue();
                return;
            } catch(DataNotFoundException dnte )
            // ok, maybe no context where given ?
            {}
        }
        if (archivedProcessInstanceId!=null)
        {
            try
            {
                contextDataOrigin =  contextCaseId.processAPI.getArchivedProcessDataInstance("globalcontext",archivedProcessInstanceId.getSourceObjectId());
                contextDataSt = contextDataOrigin.getValue();
                this.isPilotDetected= true;

                analysisString+="ArchivedProcessDataInstance[globalcontext] value="+contextDataOrigin.getValue();
                return;
            } catch(ArchivedDataNotFoundException dnte )
            // still Ok, search after
            {}
        }


        // maybe a parameters exist
        if (processDefinitionId!=null)
        {
            List<ParameterInstance> listParameters = contextCaseId.processAPI.getParameterInstances(processDefinitionId, 0, 500, ParameterCriterion.NAME_ASC);
            if (listParameters!=null)
                for (ParameterInstance parameter : listParameters)
            {
                if ( "paramcontext".equalsIgnoreCase( parameter.getName() ))
                {
                    contextDataOrigin = parameter;
                    contextDataSt = parameter.getValue();
                    this.isPilotDetected= true;

                    analysisString+= "Parameters [paramcontext]";
                    return;
                }
            }
        }
    }

    /* ********************************************************************************* */
    /*                                                                                                                                                                   */
    /* Access                                                                                                                                                      */
    /*                                                                                                                                                                  */
    /*  the pilot control the access to any variable (processvariable, BDM, document, parameters)   */
    /* from the name, the control is done.                                                                                                   */
    /*                                                                                                                                                                  */
    /* ******************************************************************************** */

    /** Access */
    public boolean isAllowVariableName(String varName )
    {
        contextCaseId.log("context.isAllowVariableName["+varName+"] ? AllowAllVariables="+isAllowAllVariables);
        if (isAllowAllVariables)
            return true;

        // call a static method because this method is copy/paste in the GroovySecurity
        return checkPermission( varName, pilotDataMap);
    }
    /* -------------------------------------------------------------------------------- */
    /*                                                                                  */
    /*  CheckPermission                                                                 */
    /*                                                                                  */
    /*  This methode is copy/paste in the groovy security script                        */
    /* -------------------------------------------------------------------------------- */


    private boolean checkPermission(String varName, Map<String,Object> pilotDataMap)
    {

        String permissionControl = pilotDataMap.getAt( varName );
        if (permissionControl==null)
        {
            contextCaseId.log("isAllowVariableName["+varName+"] found["+permissionControl+"]");
            return false;
        }

        return checkPermissionString(varName, permissionControl);
    }
    /**
     * this method can be call from a general varName, or for a more complex structure
     * @param varName for information only
     * @param permissionControl  the permission string, a set of "actor:XXX; etc...
     * @return
     */
    public boolean checkPermissionString(String varName, String permissionControl)
    {
        String analysis="isAllowVariableName["+varName+"] found["+permissionControl+"]";
        Map<String,Object> trackSubOperation = trackPerformance.startSubOperation("checkPermissionString["+varName+"]");

        boolean allowAccess=false;
        boolean isOnlyFormatData=true;

        long processInstanceId =-1;
        long processDefinitionId=-1;
        if (contextCaseId.processInstance!=null) {
            processInstanceId   = contextCaseId.processInstance.getId();
            processDefinitionId = contextCaseId.processInstance.getProcessDefinitionId();
        }
        if (contextCaseId.archivedProcessInstance!=null) {
            processInstanceId   = contextCaseId.archivedProcessInstance.getId();
            processDefinitionId = contextCaseId.archivedProcessInstance.getProcessDefinitionId();
        }
        analysis +="processInstanceId["+processInstanceId+"] processDefinitionId["+processDefinitionId+"]";

        if (permissionControl==null)
        {
            analysis += "; NoPermission, yes";
            allowAccess=true;
        }
        else
        {
            // permission is a set of info separate by a ;
            StringTokenizer st = new StringTokenizer( permissionControl, ";");

            while ( st.hasMoreTokens() && ( !  allowAccess) )
            {
                String onePermission= st.nextToken();
                analysis+=";["+onePermission+"]";
                if ("data".equals(onePermission) || ("public".equals(onePermission)))
                {
                    analysis+=":publicAccess";
                    allowAccess=true;
                    isOnlyFormatData=false;
                }
                else if ("admin".equals( onePermission ))
                {
                    allowAccess= contextCaseId.isAdministratorUser();
                    analysis+=":admin ? "+allowAccess;
                }
                else if ("initiator".equals( onePermission ))
                {
                    isOnlyFormatData=false;
                    analysis+=":initiator ? ";
                    if  ((contextCaseId.processInstance != null) && (contextCaseId.processInstance.getStartedBy() == contextCaseId.userId))
                    {
                        analysis+="yes;";
                        allowAccess=true;
                    } else if ( (contextCaseId.archivedProcessInstance != null) && (contextCaseId.archivedProcessInstance.getStartedBy() == userId))
                    {
                        analysis+="yes;";
                        allowAccess=true;
                    }
                    else
                        analysis+="no;";
                }
                else if (onePermission.startsWith("task:"))
                {
                    isOnlyFormatData=false;
                    String taskName = onePermission.substring("task:".length());
                    analysis+=":taskName["+taskName+"] ";
                    SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0,100);
                    searchOptions.filter( ActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId)
                    searchOptions.filter( ActivityInstanceSearchDescriptor.NAME,taskName);
                    SearchResult<ActivityInstance> searchResult = contextCaseId.processAPI.searchActivities(searchOptions.done());
                    for (ActivityInstance activity : searchResult.getResult())
                    {
                        if (contextCaseId.processAPI.canExecuteTask(activity.getId(), contextCaseId.userId))
                        {
                            analysis+=" yes taskid["+activity.getId()+"]";
                            allowAccess=true;
                        }
                    }
                    // same with archived task
                    searchOptions = new SearchOptionsBuilder(0,100);
                    searchOptions.filter( ArchivedActivityInstanceSearchDescriptor.PARENT_PROCESS_INSTANCE_ID, processInstanceId)
                    searchOptions.filter( ArchivedActivityInstanceSearchDescriptor.NAME,taskName);
                    SearchResult<ArchivedActivityInstance> searchResultArchived = contextCaseId.processAPI.searchArchivedActivities(searchOptions.done());
                    for (ArchivedActivityInstance archivedActivity : searchResultArchived.getResult())
                    {
                        if (archivedActivity.getExecutedBy()== contextCaseId.userId)
                        {
                            analysis+=" yesArchived taskid["+archivedActivity.getId()+"]";
                            allowAccess=true;
                        }
                    }
                }
                else if (onePermission.startsWith("actor:"))
                {
                    isOnlyFormatData=false;
                    String actorName = onePermission.substring("actor:".length());
                    analysis+=":actorName["+actorName+"] ";
                    Boolean isPart = isPartOfActor( actorName, processDefinitionId );
                    if (isPart==null)
                        analysis+=":Actor not found;"
                    else if (isPart)
                    {
                        analysis+="part of actor";
                        allowAccess=true;
                    }
                }
                else if (onePermission.startsWith("format:"))
                {
                    // format
                }
                else if ("data".equals( onePermission))
                {
                    // data
                }


            } // end loop on permission
        } // end permission==null

        if (isOnlyFormatData)
        {
            analysis+=";OnlyAFormatData - equals to public";
            allowAccess=true;
        }
        contextCaseId.log("RestContextPilot.checkPermissionString : analysis: "+analysis+" RESULT="+allowAccess);
        trackPerformance.endSubOperation( trackSubOperation);

        return allowAccess;
    }

    /**
     * Actor Cache
     *
     */
    private Map<String,Boolean> cacheActor= new HashMap<String,Boolean>();

    /**
     * isPart : return True (part of ) False (not part of ) NULL (actor unknown
     * @param actorName
     * @return
     */
    public Boolean isPartOfActor(String actorName, long processDefinitionId)
    {
        if (cacheActor.containsKey( actorName ))
        {
            return  cacheActor.get( actorName );
        }

        // calculated and save it
        int startIndex=0;
        try
        {
            List<Long> listUsersId = contextCaseId.processAPI.getUserIdsForActor(processDefinitionId, actorName, startIndex, 1000);

            while (listUsersId.size()>0)
            {
                // logger.info("getUserIdsForActor start at "+startIndex+" found "+listUsersId.size()+" record")
                startIndex+= 1000;
                if (listUsersId.contains( contextCaseId.userId ))
                {
                    cacheActor.put(actorName, Boolean.TRUE );
                    return true;
                }
                else
                    listUsersId = contextCaseId.processAPI.getUserIdsForActor(processDefinitionId, actorName, startIndex, 1000);
            }
        }catch (RetrieveException e){
            cacheActor.put(actorName,null);
            return null;
            // analysis+=":Actor not found;";
        }
        cacheActor.put(actorName, Boolean.FALSE )
        return false;
    }


}