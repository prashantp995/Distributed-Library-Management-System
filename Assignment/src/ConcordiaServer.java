import java.io.IOException;
import java.rmi.Naming;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.SimpleLayout;

public class ConcordiaServer {

  public static void main(String args[]) throws IOException {
    final Logger logger = Logger.getLogger(ConcordiaServer.class);
    SimpleLayout layout = new SimpleLayout();
    RollingFileAppender appender = new RollingFileAppender(layout, "Assignment/logs/test.log", true);
    logger.addAppender(appender);

    String registryURL;
    try {

      int RMIPortNum = 8080;
      Utilities.startRegistry(RMIPortNum);
      LibraryRemoteServiceImpl exportedObj = new LibraryRemoteServiceImpl();
      System.setProperty("java.rmi.server.hostname", "localhost");
      registryURL = "rmi://localhost:" + RMIPortNum + "/findItem";
      Naming.rebind(registryURL, exportedObj);
      logger.info("Server registered.  Registry currently contains:");
      Utilities.listRegistry(registryURL);
      logger.info("Server ready.");
    } catch (Exception re) {
      logger.info("Exception in HelloServer.main: " + re);
    }
  }


}
