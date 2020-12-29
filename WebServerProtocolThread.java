import java.net.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.io.*;

public class WebServerProtocolThread extends Thread {

    private Socket socket = null;

    public WebServerProtocolThread(Socket socket) {
        super("WebServerProtocolThread");
        this.socket = socket;
    }

    public void run() {

        try (
                // create output streams for the socket to send response through
                // and create input stream to read request from socket
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                DataOutputStream bod = new DataOutputStream(socket.getOutputStream());
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {

            // initializing both objects needed to process request and respond
            Response response = new Response();
            WebServerProtocol wsp = new WebServerProtocol();

            // the initial time the connection was opened
            long startTime = System.currentTimeMillis();

            // this while loop times the user input
            // if there is no user input within 5 seconds of opening the connect
            // a 408 status response is sent and the connection is closed
            while((System.currentTimeMillis() - startTime < 5000)) {

                // checks to see if user has entered input
                //if(in.ready()) {
                if(in.ready()) {
                    // takes user input request
                    /*String request = in.readLine().trim() + " ";
                    int contentLengthInt = 0;
                    while(true) {
                        String s = in.readLine();
                        String contentLength = "Content-Length: ";
                        if (s.startsWith(contentLength)) {
                            contentLengthInt = Integer.parseInt(s.substring(contentLength.length()));
                        }
                        if (s.length() == 0) {
                            break;
                        }
                        request += s + " ";
                    }
                    char[] params = new char[contentLengthInt];
                    in.read(params);
                    String parm = new String(params);
                    System.out.println("Parm: " + parm);
                    request += parm;*/

                    String request = "";
                    String line = in.readLine();
                    while(!line.equals("")) {
                        request = request + "\n" + line;
                        line = in.readLine();

                        if(line.equals("") && request.indexOf("POST") != -1 && in.ready()) {
                            line = "\n" + in.readLine();
                            request += line;
                            //System.out.println(request);
                            break;
                        }
                    }

                    // processes the request and sets the response object's fields accordingly
                    response = wsp.processInput(request, PartialHTTP1Server.SERVER_PORT);

                    // sends the status line and headers to output stream and flushes stream
                    response.toString(out);
                    out.flush();

                    // not really sure if the if statement is necessary
                    // sends the body of the response to output stream and flushes stream
                    if(response.allow && response.body != null) {
                        bod.write(response.body);
                    }

                    // additional formatting for response and flushing output streams
                    out.write("\r\n\r\n");
                    out.flush();
                    bod.flush();

                    // makes the thread wait a quarter second before closing all streams and socket
                    try {
                        sleep(250);
                    } catch (Exception e) {
                        System.err.println("Error: Could not force the current thread to sleep");
                    }

                    // closes all input/output streams and the socket
                    out.close();
                    bod.close();
                    socket.close();
                    return;
                }
            }
            // sends 408 response if no request is received in 5 seconds
            out.write("HTTP/1.0 408 Request Timeout\r\n\r\n");
            out.flush();
            bod.flush();

            // makes the thread wait a quarter second before closing all streams and socket
            try {
                sleep(250);
            } catch (Exception e) {
                // not sure what to do here
                System.out.println("something went wrong");
            }

            // closes all input/output streams and the socket
            in.close();
            bod.close();
            out.close();
            socket.close();

        } catch (IOException e) {
            System.out.print("HTTP/1.0 500 Internal Server Error\r\n\r\n");
        }
    }

}
