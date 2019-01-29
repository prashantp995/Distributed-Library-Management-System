import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

public class ConcordiaServer {

  public static void main(String args[]) throws IOException {

    Logger logger = Utilities.setupLogger(Logger.getLogger("CONServerlog"), "ConcordiaServer.log");
    String registryURL;
    try {
      int RMIPortNum = 8080;
      ConcordiaRemoteServiceImpl exportedObj = new ConcordiaRemoteServiceImpl();
      Registry registry =
          LocateRegistry.createRegistry(RMIPortNum);
      registry.bind("CON", exportedObj);
      System.out.println("Server Started");
      logger.info("Server ready.");
      System.out.println(exportedObj.provideCount());
    } catch (Exception re) {
      logger.info("Exception " + re);
    } finally {

    }
  }


}
