import java.io.IOException;
import java.rmi.Naming;
import java.util.logging.Logger;

public class ConcordiaServer {

  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("MyLog"), "ConcordiaServer.log");
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
