import java.io.PrintWriter;
import java.net.HttpCookie;
import java.util.HashMap;

/**
 * Holds the information required in an HTTP response message
 */
public class Response {
    /**
     * Status code for response
     */
    public int statusCode;

    /**
     * Status message for the response
     */
    public String statusLine;

    /**
     * Indicates whether the body of a file should be shown in the HTTP response
     */
    public boolean allow = true;

    /**
     * Content encoding used to compress the media type
     */
    public String encoding;

    /**
     * Holds the length of the file content
     */
    public String length;

    /**
     * Type and subtype of the file
     */
    public String type;

    /**
     * Indicates the date of expiration for the response
     */
    public String expires;

    /**
     * The date the file accessed was last modified
     */
    public String lastModified;

    /**
     * The date a request specifies to compare to lastModified
     */
    public String modifiedSince;

    /**
     * Encodes the file content in bytes
     */
    public byte[] body;

    /**
     * Holds key (status code, message) key-value pairs
     */
    public HashMap<Integer, String> responseCode = new HashMap<>();

    /**
     * Indicates the system to print response out to 
     */
    public PrintWriter out;

    /**
     * Indicates if a Request was a POST request
     */
    public boolean isPost = false;

    /**
     * Response cookie
     * */
    public HttpCookie lasttime;

    /**
     * Constructor for response that populates responseCode Hashtable
     */
    public Response(){
        responseCode.put(304, "Not Modified");
        responseCode.put(400, "Bad Request");
        responseCode.put(403, "Forbidden");
        responseCode.put(404, "Not Found");
        responseCode.put(408, "Request Timeout");
        responseCode.put(500, "Internal Server Error");
        responseCode.put(501, "Not Implemented");
        responseCode.put(503, "Service Unavailable");
        responseCode.put(505, "HTTP Version Not Supported");
        responseCode.put(411, "Length Required");
        responseCode.put(405, "Method Not Allowed");
        responseCode.put(204, "No Content");
    }

    /**
     * Sets out
     * @param out PrintWriter system to print response out to 
     */
    public void setOut(PrintWriter out) {
        this.out = out;
    }

    /**
     * Sets statusCode
     * @param statusCode Status Code for the HTTP response 
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Sets statusLine
     * @param statusLine Status message for the HTTP response
     */
    public void setStatusLine(String statusLine) {
        this.statusLine = "HTTP/1.0 " + statusLine;
    }

    /**
     * Sets allow
     * @param allow Specifies whether content should be printed in HTTP response
     */
    public void setAllow(boolean allow) {
        this.allow = allow;
    }

    /**
     * Sets encoding
     * @param encoding Encoding type for the file
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Sets length 
     * @param length Length of the file 
     */
    public void setLength(String length) {
        this.length = length;
    }

    /**
     * Sets type
     * @param type Type and subtype of a file separated by "/" character
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Sets expires
     * @param expires Expiration date of file 
     */
    public void setExpires(String expires) {
        this.expires = expires;
    }

    /**
     * Sets lastModified
     * @param lastModified Date the file was last modified
     */
    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Sets modifiedSince
     * @param modifiedSince Date the client specifies for when the file was last modified
     */
    public void setModifiedSince(String modifiedSince) {
        this.modifiedSince = modifiedSince;
    }

    /**
     * Sets body
     * @param body Encodes the file content in bytes
     */
    public void setBody(byte[] body) {
        this.body = body;
    }

    /**
     * Prints out the HTTP response based on status code and file content
     * @param out PrintWriter system to print out HTTP response to
     */
    public void toString(PrintWriter out) {
        //if it's a post request, send it to the PostRequest String method
        if (this.isPost) { postToString(out); return; }

        //Handles responses with status codes other than 200
        if (this.responseCode.containsKey(this.statusCode)) {
            out.write("HTTP/1.0 " + statusCode + " " + responseCode.get(statusCode));
            //Not modified status code requires expires date
            if(this.statusCode == 304) {
                out.write("\r\nExpires: Sat, 21 Jul 2021 11:00:00 GMT\r\n");
            }
            out.write("\r\n\r\n");
            return;
        }
        //Handles responses with 200 status code
        out.write("HTTP/1.0 200 OK\r\n");
        out.write("Content-Type: " + this.type + "\r\n");
        out.write("Content-Length: " + this.length + "\r\n");
        out.write("Last-Modified: " + this.lastModified + "\r\n");
        out.write("Content-Encoding: " + this.encoding + "\r\n");
        out.write("Allow: GET, POST, HEAD" + "\r\n");
        out.write("Expires: Sat, 21 Jul 2021 11:00:00 GMT\r\n\r\n");

    }

    /**
     * Prints out the HTTP response based on status code and file content for  POST request
     * @param out PrintWriter system to print out HTTP response to
     */
    public void postToString(PrintWriter out) {

        if (this.responseCode.containsKey(this.statusCode)) {
            out.write("HTTP/1.0 " + statusCode + " " + responseCode.get(statusCode));
            out.write("\r\n\r\n");
            return;
        }
        out.write("HTTP/1.0 200 OK\r\n");
        out.write("Content-Length: "+ this.length+ "\r\n");
        out.write("Content-Type: " + this.type+ "\r\n");
        out.write("Allow: GET, POST, HEAD" + "\r\n");
        out.write("Expires: Sat, 21 Jul 2021 11:00:00 GMT\r\n\r\n");
    }
}
