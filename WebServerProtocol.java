import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.*;

/**
 * Handles processes related to interpretting HTTP requests
 */
public class WebServerProtocol {

    /**
     * Array of supported commands
     */
    String[] supportedCommandsArray = {"GET", "POST", "HEAD"};
    /**
     * List of supported commands
     */
    List<String> supportedCommands = Arrays.asList(supportedCommandsArray);

    /**
     * Array of unsupported commands
     */
    String[] unsupportedCommandsArray = {"PUT", "LINK", "UNLINK", "DELETE"};

    /**
     * List of unsupported commands
     */
    List<String> unsupportedCommands = Arrays.asList(unsupportedCommandsArray);

    /**
     * Array of subtypes of "text" type 
     */
    String[] textArray = {"html", "plain"};
    /**
     * List of subtypes of "text" type
     */
    List<String> text = Arrays.asList(textArray);

    /**
     * Array of subtypes of "image" type 
     */
    String[] imageArray = {"gif", "jpeg", "png"};
    /**
     * List of subtypes of "image" type
     */
    List<String> image = Arrays.asList(imageArray);

    /**
     * Array of subtypes of "application" type 
     */
    String[] applicationArray = {"octet-stream", "pdf", "x-gzip", "zip"};
    /**
     * List of subtypes of "application" type
     */
    List<String>  application = Arrays.asList(applicationArray);

    /**
     * Type of file requested
     */
    String type = "";

    /**
     * Processes the client's request 
     * @param theInput Client's HTTP request
     * @param port Port the server is listening on 
     * @return Response object with HTTP response information 
     */
    public Response processInput(String theInput, int port) {
        //response to be returned 
        Response response = new Response();

        //Command to carry out
        String command = "";
        //Resource to access
        String resource = "";
        //HTTP Version to verify
        String HTTPversion = "";
        //Used to verify If-Modified-Since header
        String additionalHeader= "";
        //Date specified by client 
        String conditionDate = "";
        //Date the file was last modified
        String modifiedDate = "";

        //tokenizes input using space and newline as delimeters
        StringTokenizer input = new StringTokenizer(theInput, " \n");

        //makes sure there are at least 3 tokens to look at
        if (input.countTokens() < 3) {
            response.setStatusCode(400);
            return response;
        }

        //populates REQUIRED request components
        command = input.nextToken().trim();
        resource = input.nextToken().trim();
        HTTPversion = input.nextToken().trim();

        //makes sure the resource is properly formatted
        if(resource.indexOf("/") != 0 && !command.equals("POST")) {
            response.setStatusCode(400);
            return response;
        }

        //checks for an "If-Modified-Since" header
        //gets the modified date in string form
        if(input.hasMoreTokens() && !command.equals("POST")){
            additionalHeader=input.nextToken();
            //makes sure the additional header is If-Modified-Since
            if(!additionalHeader.equals("If-Modified-Since:")){
                response.setStatusCode(400);
                return response;
            }
            //Constructs the conditioned modified date string from request
            StringBuilder br = new StringBuilder();
            while(input.hasMoreTokens()){
                br.append(input.nextToken() + " ");
            }
            conditionDate = br.toString().trim();
        }

        //checks if version is acceptable
        //returns appropriate response if it isn't
        response.setStatusCode(checkVersionFormat(HTTPversion));
        if(response.statusCode!=200) {
            return response;
        }

        //checks if command is acceptable
        //returns appropriate response if it isn't
        response.setStatusCode(checkCommandFormat(command));
        if(response.statusCode!=200) {
            return response;
        }

        // outsources implementation to a method that handles post requests
        if (command.indexOf("POST") != -1) {
            return processPostRequest(theInput, port);
        }

        //sets "Allow" to false if it is a head request
        if(command.equals("HEAD")) response.setAllow(false);

        //gets resource information
        String extension = "";
        int period = resource.indexOf(".");
        //if the file type is not specified
        if (period == -1) {
            type = "application";
            extension = "octet-stream";
        }
        //if the file type is specified
        else {
            extension = resource.substring(period + 1);

            if(text.contains(extension)) {
                type = "text";
            }
            else if(image.contains(extension)) {
                type = "image";
            }
            else if(application.contains(extension)) {
                type = "application";
            }
            else {
                type = "application";
                extension = "octet-stream";
            }
        }

        //fetch required file
        File file = null;
        try {
            //gets the file
            file = new File(resource.substring(resource.indexOf("/") + 1));

            //checks if the file exists
            if(!file.exists()) {
                response.setStatusCode(404);
                return response;
            }

            //checks if the file is a directory
            if(file.isDirectory()) {
                response.setStatusCode(404);
                return response;
            }

            // format for the lastModified string
            SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

            //lastModified time of file
            modifiedDate = sdf.format(file.lastModified());
            response.setLastModified(modifiedDate + " GMT");
            response.setLength("" + file.length());
        } catch(NullPointerException e) {
            //if the file is not found
            response.setStatusCode(404);
        }

        //checks if the file is readable
        if(!Files.isReadable(file.toPath())) {
            response.setStatusCode(403);
            return response;
        }

        //checks If-Modified-Since condition if needed
        if(additionalHeader.equals("If-Modified-Since:") && !command.equals("HEAD") && !isModified(conditionDate, modifiedDate)){
            response.setStatusCode(304);
            return response;
        }

        //handle 200 status code responses
        response.setStatusCode(200);
        response.setType(type + "/" + extension);
        response.setEncoding("identity");

        //extract body of the file
        byte[] body;
        //reads resource for body of Response
        try {
            body = Files.readAllBytes(file.toPath());
        } catch(Exception e) {
            body = null;
        }
        response.setBody(body);

        return response;
    }

    /**
     * Checks if the HTTP version is supported by server
     * @param version version inputted by the client's request
     * @return status code based on input
     */
    public int checkVersionFormat(String version) {
        //check if there is a "/" character
        if (!version.contains("/")){
            return 400;
        }
        //get string of version number
        String versionString = version.substring(version.indexOf("/") + 1);
        double versionNum = 0.0;
        //convert version number to a double
        try {
            versionNum = Double.parseDouble(versionString);
        } catch(Exception e){
            return 400;
        }
        //check if version is supported
        if (versionNum > 1.0 || versionNum <= 0){
            return 505;
        }
        //version is acceptable
        return 200;
    }

    /**
     * Checks if the command is supported
     * @param command inputted by client making request
     * @return status code based on input
     */
    public int checkCommandFormat(String command) {
        //checks if the command is supported
        if(supportedCommands.contains(command)) {
            return 200;
        }
        //checks if the command is in list of unimplemented commands
        else if(unsupportedCommands.contains(command)) {
            return 501;
        }
        //the command is nonsense
        return 400;

    }

    /**
     * Compares the condition date specified by the client with the last modified date of the resource
     * @param conditionDate date specified in client's request
     * @param lastModified date the file was last modified
     * @return true if the modified condition is met or the modified condition isn't a date
     */
    public boolean isModified(String conditionDate, String lastModified) {
        //obtains the condition date string without the time zone
        if(conditionDate.indexOf(" GMT") != -1) {
            conditionDate = conditionDate.substring(0, conditionDate.indexOf(" GMT"));
        }
        //set both dates to the same time zone for comparison
        SimpleDateFormat sdf = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ZZZ");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        SimpleDateFormat sdf2 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss");
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date conDate;
        Date modDate;
        //parse condition date to Date
        try {
            conDate = sdf2.parse(conditionDate);
        } catch (ParseException e) {
            return true;
        }
        //parse modified date to Date
        try {
            modDate = sdf2.parse(lastModified);
        } catch (ParseException e) {
            //not a valid If-Modified-Since date, ignore the date altogether
            return true;
        }
        //if the modified date is after or the same time as the condition date
        if(modDate.compareTo(conDate)>=0) {
            return true;
        }
        //the modified date was before the condition date
        return false;
    }

    /**
     * Processes post requests ONLY 
     * @param theInput request that the client sends
     * @param port given port the Server is listening on 
     * @return Response object that contains the information about the HTTP response
     */
    public Response processPostRequest(String theInput, int port) {

        System.out.println(theInput);

        /*
        NOTES ON DELIMETER
            Gets each "word" of the post request (i.e. "POST", "/cgi_bin/basic.cgi", and "HTTP/1.0" would
            all be different tokens even though they are on the same line). This works because
            we deliminate by space characters. The input from the jar file Tester doesn't consider different
            lines to be separated by a newline character, so I modified the code so that we also deliminate
            by space. We could probably remove the newline character from the delimiter altogether, but I
            left it in since we did something similar for processRequest().
        */
        StringTokenizer input = new StringTokenizer(theInput, " \n");
        //creates new Response object to return
        Response returnResponse = new Response();

        //Notes that the response is for a post request
        returnResponse.isPost=true;

        //e.g. /cgi_bin/upcase.cgi
        String CGI_PATH = "";
        //e.g me@mycomputer.
        String FROM = "";
        //will hold the User-Agent
        String USER_AGENT = "";
        //will hold the Content-Type
        String CONTENT_TYPE = "";
        //will hold the Content-Length from the REQUEST
        String CONTENT_LENGTH = "";
        //will hold the NAME-VALUE pairs from the REQUEST
        String NAME_VALUE = "";

        // gets each line from the request
        while (input.hasMoreTokens()) {
            String temp = input.nextToken().trim();
            if(temp.equals("POST")){
                CGI_PATH = input.nextToken().trim();
            } else if (temp.equals("From:")) {
                FROM = input.nextToken().trim();
            } else if (temp.equals("User-Agent:")) {
                USER_AGENT = input.nextToken().trim();
            } else if (temp.equals("Content-Type:")) {
                CONTENT_TYPE = input.nextToken().trim();
            } else if (temp.equals("Content-Length:")) {
                CONTENT_LENGTH = input.nextToken().trim();
            } else if(!temp.equals("HTTP/1.0")){
                NAME_VALUE = temp;
            }
        }

        NAME_VALUE = decodeMessage(NAME_VALUE);

        //checks CONTENT_LENGTH format as specified by the request
        if(CONTENT_LENGTH.equals("")) {
            //makes sure that CONTENT_LENGTH was specified in the request
            returnResponse.setStatusCode(411);
            return returnResponse;
        } else {
            //makes sure CONTENT_LENGTH specified is an integer
            try {
                Integer.parseInt(CONTENT_LENGTH);
            } catch (NumberFormatException e) {
                returnResponse.setStatusCode(411);
                return returnResponse;
            }
        }

        //Makes sure CONTENT_TYPE is specified by the user
        if (CONTENT_TYPE.equals("")) {
            returnResponse.setStatusCode(500);
            return returnResponse;
        }

        //Makes sure the selected file ends with .cgi
        if(!CGI_PATH.substring(CGI_PATH.length()-4, CGI_PATH.length()).equals(".cgi")){
            returnResponse.setStatusCode(405);
            return returnResponse;
        }

        //command with request specified parameters to be used for the cgi execution
        String[] commands = {"/bin/bash", "-c", "echo \""+ NAME_VALUE + "\" | " + CGI_PATH.substring(1)};

        //executes the command and processes the output of the .cgi code
        try {
            //starts a process builder to run the desired command
            ProcessBuilder builder = new ProcessBuilder(commands);

            //sets the environment variables according to the request
            //As the client specified in the request
            builder.environment().put("CONTENT_LENGTH", CONTENT_LENGTH);
            //As the client specified in the request
            builder.environment().put("SCRIPT_NAME", CGI_PATH);
            //As the client specified in the request
            builder.environment().put("HTTP_FROM", FROM);
            //As the client specified in the request
            builder.environment().put("HTTP_USER_AGENT", USER_AGENT);
            //This will get the host name of the server
            InetAddress ip;
            String hostname="";
            try {
                ip = InetAddress.getLocalHost();
                hostname = ip.getHostName();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            builder.environment().put("SERVER_NAME", hostname);
            //This sets the server port as found in the PartialHTTP1Server class
            builder.environment().put("SERVER_PORT", Integer.toString(port));

            Process proc = builder.start();
            //reads the output of the cgi command
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            //takes the output of the cgi execution and combines into one string
            String s = null;
            String bodyString ="";
            while((s=stdInput.readLine()) != null) {
                //bodyString+= s.trim()+"\n";
                bodyString += s;
            }

            /*if(CGI_PATH.equals("/cgi_bin/param.cgi")) {
                System.out.println("name: " + NAME_VALUE);
                System.out.println();
                System.out.println("body: " + bodyString);
            }*/

            //if there is no output from the CGI, special status code is required
            //Note that this is not a failure but should still only print out
            //"HTTP/1.0 204 No Content"
            if (bodyString.length()==0) {
                returnResponse.setStatusCode(204);
                return returnResponse;
            } else {
                //sets the Content-Length for the RESPONSE object
                returnResponse.setLength(""+bodyString.length());
            }

            //sets the Content-Type for the RESPONSE object
            //ALWAYS "text/html" according to the test cases
            returnResponse.setType("text/html");

            //converts the body of the cgi output into a byte array so we can output the body
            //in the WebServerProtocolThread class
            byte[] bodyArray = bodyString.getBytes();
            returnResponse.setBody(bodyArray);

        } catch (Exception e) {
            e.printStackTrace();
        }

        returnResponse.setStatusCode(200);
        return returnResponse;
    }

    //method for decoding the encoded message included at the end of the POST header
    /*public String decodeMessage(String encodedMessage){
        char encodingCharacters[] = {'!', '*', '\'', '(', ')', ';', ':', '@', '$', '+', ',', '/', '?', '#', '[', ']', ' '};
        StringBuilder sb = new StringBuilder(encodedMessage);
        for(int i = 0; i<encodedMessage.length()-2;i++){    //length-1 to avoid out-of-bounds error
            if(encodedMessage.charAt(i) == '!') {
                for (int j = 0; j < encodingCharacters.length; j++) {
                    if (encodedMessage.charAt(i+1) == encodingCharacters[j]) {    //checks if the character after "!" is another special character
                        sb.deleteCharAt(i);    //deletes the "!"
                        encodedMessage = sb.toString();
                    }
                }
            }
        }
        return encodedMessage;
    }*/

    public String decodeMessage(String encodedMessage) {

        //ArrayList<String> encodingCharacters = new ArrayList<String>(Arrays.asList("!", "*", "\'", "(", ")", ";", ":", "@", "$", "+", ",", "/", "?", "#", "[", "]", " "));
        String encodingCharacters = "!*\"();:@$+,/?#[] ";

        String decoded = "";

        for(int i=0; i<encodedMessage.length(); i++) {
            if(i+1<encodedMessage.length()) {
                if(encodedMessage.charAt(i) == '!' && encodingCharacters.indexOf(encodedMessage.charAt(i+1)) != -1) {
                    decoded += encodedMessage.charAt(i+1);
                    i += 1;
                    continue;
                }
            }
            decoded += encodedMessage.charAt(i);
        }

        //System.out.println("ENCODED: " + encodedMessage);
        //System.out.println("DECODED: " + decoded);

        return decoded;
    }

    public String decodeDate(){
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedDate = myDateObj.format(myFormatObj);
        System.out.printf("Formatted date+time %s \n",formattedDate);

        //String encodedDateTime = URLEncoder.encode(formattedDate, "UTF-8");
        //System.out.printf("URL encoded date-time %s \n",encodedDateTime);
        //String decodedDateTime = URLDecoder.decode(encodedDateTime, "UTF-8");
        //System.out.printf("URL decoded date-time %s \n",decodedDateTime);

        //return encodedDateTime;
        return "";
    }


}
