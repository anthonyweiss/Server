import java.net.*;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.io.*;

public class PartialHTTP1Server {

    public static int SERVER_PORT;
    public HttpCookie lasttime = new HttpCookie("website", "javapointers");

    public static void main(String[] args) throws IOException {


        // checks to see if a port number is included in arguments
        if (args.length != 1) {
            System.err.println("Usage: java PartialHTTP1Server.java <port number>");
            System.exit(1);
        }

        // captures port number and converts it to an int
        int portNumber = Integer.parseInt(args[0]);
        SERVER_PORT = portNumber;

        try (
                // attempts to open a new server socket on given port
                ServerSocket serverSocket = new ServerSocket(portNumber);

        ) {
            // create a thread pool to limit number of connections and manage threads
            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(5, 50, 5000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1)) {

            };
            // while loop that always runs to continuously accept connections
            while(true) {

                // attempts to accept client connection
                Socket clientSocket = serverSocket.accept();

                try {
                    // attempts to create a new thread to handle client connection
                    threadPool.execute(new WebServerProtocolThread(clientSocket));
                }
                // catches exception when thread cannot be created 
                catch (RejectedExecutionException e) {

                    // sends client 503 response
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.print("HTTP/1.0 503 Service Unavaiable\r\n");
                    out.close();
                }
            }

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }
}
