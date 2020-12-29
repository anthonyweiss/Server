import java.io.*;
import java.net.*;

public class WebServerClient {

    public static BufferedReader reader;

    public static void main(String[] args) throws IOException {

        if (args.length != 2) {
            System.err.println(
                    "Usage: java EchoClient <host name> <port number>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);

        try (
                Socket kkSocket = new Socket(hostName, portNumber);
                PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(kkSocket.getInputStream()));
        ) {
            reader = in;
            BufferedReader stdIn =
                    new BufferedReader(new InputStreamReader(System.in));


            String fromUser;

            fromUser = stdIn.readLine();
            String send = fromUser;
            if (fromUser != null) {

                fromUser = stdIn.readLine();
                if(fromUser != null) {
                    send = send + "\n" + fromUser;
                }
            }
            out.println(send);

            String fromServer = in.readLine();

            while (fromServer != null) {
                System.out.println(fromServer);
                fromServer = in.readLine();
            }

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            //System.err.println("Couldn't get I/O for the connection to " +
            // hostName);
            System.out.println(reader.readLine());
            System.exit(1);
        }
    }
}
