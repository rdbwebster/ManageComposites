package com.managesoa;

import java.io.Console;
import javax.naming.Context;
import java.util.*;
import oracle.soa.management.facade.Locator;
import oracle.soa.management.facade.LocatorFactory;
import oracle.soa.management.facade.Composite;
import oracle.soa.management.util.CompositeFilter;

/*
 * ManageComposites is a commandline utility that will list and optionally delete
 * all Oracle SOA Composite instances that are in a faulted state.
 * This utilty provides an method of removing instances that are not processed
 * by the SOA Purge Scripts.
 * 
 * the utility accepts 1 of 3 different commands:
 * listFaulted - List all composites along with a count of the faulted instances for each composite.
 * removeFaulted - Remove all faulted instances for all composites
 * interactive - Processes 1 composite at a time prompting the operator for a confirmation to remove faulted instances.
 * 
 * The following 4 dedendencies must be on the classpath at runtime:
 * 
 * $FMWHOME/oracle_common/soa/modules/oracle.soa.mgmt_11.1.1/soa-infra-mgmt.jar
 * $FMWHOME/oracle_common/webservices/wsclient_extended.jar
 * $FMWHOME/oracle_common/modules/oracle.fabriccommon_11.1.1/fabric-common.jar
 * $FMWHOME/oracle_common/modules/wlthint3client.jar
 * 
 * @Author Bob Webster Mar 2014
 */
public class ManageComposites {
    
 public static void main(String[] args) {

 String host = null;
 String port = null;
 String partition = null;
 String command = null;
 String userid = null;
 String password = null;
 Console consoleReader = null;
         
 if(args.length !=6)
 {
     System.out.println("Usage:    java com.managesoa.ManageComposites [listFaulted | removeFaulted | Interactive] SoaPartition  wlsSoaSvrHost, wlsSoaSvcPort, userid, password");
     System.out.println("");
     System.out.println("Example:  java com.managesoa.ManageComposites listFaulted default localhost 8001 weblogic password");
     
     return;
 }     


 command = args[0];
 partition = args[1];
 host = args[2];
 port = args[3];
 userid = args[4];
 password = args[5];
 
 // Connect to input keyboard if interactive mode requested.
 if(command.equals("interactive")) {
         consoleReader = System.console();    
         if(consoleReader == null) {
            System.out.println("Console not available for interactive mode. Exiting");   
            
            return;
         }
 }
 
 
 try {
     
     Hashtable jndiProps = new Hashtable();
     
     jndiProps.put(Context.PROVIDER_URL,"t3://" + host + ":" + port + "/soa-infra");
     jndiProps.put(Context.INITIAL_CONTEXT_FACTORY,"weblogic.jndi.WLInitialContextFactory");
     jndiProps.put(Context.SECURITY_PRINCIPAL, userid);
     jndiProps.put(Context.SECURITY_CREDENTIALS, password);
 //    jndiProps.put("dedicated.connection", "true");

     System.out.println("Connecting to t3://" + host + ":" + port + "/soa-infra");
     Locator locator = LocatorFactory.createLocator(jndiProps);
          
     // Get a list of all composites 
     CompositeFilter filter = new CompositeFilter();
     filter.setPartition(partition);
     
     // List of composites and their instance info
     ArrayList<Composite> compositeList = (ArrayList<Composite>) locator.getComposites(filter, true);
     
     // Build a list of composites with faulted instances
     ArrayList<Composite> faultedCompositeList = new ArrayList<Composite>(compositeList.size());
     for(Composite c : compositeList) {
         
         if(c.getFaultCount() > 0)
             faultedCompositeList.add(c);
     }
    
    System.out.println("Located " + faultedCompositeList.size() + " composites with faulted instances.");
  
     if(faultedCompositeList.size() > 0) {
            // List and or Purge all faulted instances 
            for(Composite c : faultedCompositeList) {
                if(command.equalsIgnoreCase("listFaulted"))
                {
                    System.out.println("Composite " +  c.getCompositeDN() + " has " + c.getFaultCount() + " faulted instances.");
                }
                else if(command.equalsIgnoreCase("removeFaulted")) {
                        System.out.println("Removing " + c.getFaultCount() + " faulted instances for composite " + c.getCompositeDN());
                        locator.purgeInstance(c.getCompositeDN().toString());
                     } 
                     else if(command.equalsIgnoreCase("interactive")) {
                                System.out.println("Composite " +  c.getCompositeDN() + " has " + c.getFaultCount() + " faulted instances.");
                                System.out.print("Remove faulted instances Y(es), N(o): ");
                               
                                String inputLine = consoleReader.readLine();
                                          
                                 if(inputLine.startsWith("Y") | inputLine.startsWith("y")) {
                                       System.out.println("Removing " + c.getFaultCount() + " faulted instances for composite " + c.getCompositeDN());
                                       locator.purgeInstance(c.getCompositeDN().toString());
                                    } else
                                       System.out.println("Skipped.");                                 
                           }
                           else System.out.println("Invalid command, must be either listFaulted, removeFaulted or interactive");
            }
     }
     else
         System.out.println("No composites with faulted instances were located.");
         
     
    System.out.println("Done.");
     
     } catch (Exception e) {
        System.out.println("Exception " + e.getMessage());
        e.printStackTrace();
        }      
     }
}
 