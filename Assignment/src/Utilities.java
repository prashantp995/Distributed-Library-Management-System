import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.SimpleLayout;

public class Utilities {

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

  public static Logger setupLogger(Logger logger, String fileName) throws IOException {
    SimpleLayout layout = new SimpleLayout();
    RollingFileAppender appender = new RollingFileAppender(layout, fileName, true);
    logger.addAppender(appender);
    return logger;
  }


}
