package com.dsd.as3;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class Utilities {

  private static final String LOG_DIR = "C:/DSD/Git/DSD_Assignment_COMP_6231/Assignment/logs/";

  public static void startRegistry(int RMIPortNum)
      throws RemoteException {
    try {
      Registry registry = LocateRegistry.getRegistry(RMIPortNum);
      registry.list();
    } catch (RemoteException e) {
      System.out.println("RMI registry cannot be located at port " + RMIPortNum);
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      System.out.println("RMI registry created at port " + RMIPortNum);
    }
  }

  public static void listRegistry(String registryURL)
      throws RemoteException, MalformedURLException {
    System.out.println("Registry " + registryURL + " contains: ");
    String[] names = Naming.list(registryURL);
    for (int i = 0; i < names.length; i++) {
      System.out.println(names[i]);
    }
  }

  public static Logger setupLogger(Logger logger, String fileName, boolean showlogsInConsole)
      throws IOException {

    FileHandler fh;

    try {
      if (!showlogsInConsole) {
        logger.setUseParentHandlers(false);
      }
      fh = new FileHandler(LOG_DIR + fileName, true);
      logger.addHandler(fh);
      SimpleFormatter formatter = new SimpleFormatter();
      fh.setFormatter(formatter);

    } catch (SecurityException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return logger;
  }

  public static void closeLoggerHandlers(Logger logger) {
    for (Handler h : logger.getHandlers()) {
      h.close();   //must call h.close or a .LCK file will remain.
    }
  }

  public static Logger getLogger(String username) {
    Logger logger = null;
    try {
      logger = Utilities
          .setupLogger(Logger.getLogger(username + "log"), username + ".log", true);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return logger;
  }

  public static boolean validateItemIdAndNumberOfDays(String itemId, int numberOfDays) {
    return UserClient.validateItemId(itemId) && numberOfDays > 0;
  }


  public static boolean validateUserName(String username) {
    return username.length() == 8 &&
        (username.startsWith("CONU")
            || username.startsWith("MCGU")
            || username.startsWith("MONU") || username.startsWith("CONM") || username
            .startsWith("MCGM") || username.startsWith("MONM"));
  }


  public static int getResponseFromClient(Logger logger) {

    Registry registry = null;
    try {
      registry = LocateRegistry.getRegistry(LibConstants.CLIENT_PORT);
      CallbackClientInterface obj = (CallbackClientInterface) registry
          .lookup(LibConstants.CLIENT_REG);
      return obj.askForWaitingList();
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (NotBoundException e) {
      e.printStackTrace();
    }

    return 0;
  }


}
