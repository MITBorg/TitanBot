package mitb.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class contains the static method ({@link #shorten(URL)}) to shorten a
 * URL using the <tt>git.io</tt> service.
 *
 * <p>
 * Please note that this service works only with GitHub URLs. This library
 * is not affiliated with GitHub. Read the <a href="http://git.io/A7MF">GitHub
 * Terms of Service</a>.
 *
 * @author Andrea Burattin
 */
public class GitIO {

    // entry point of the shortening service
    protected static String SHORTENER_URL = "https://git.io/create";
    // base structure of the shortened URL
    protected static String SHORTENED_URL = "https://git.io/%s";

    /**
     * This static method can be used to obtain a short version of the provided
     * URL, using the <a href="http://git.io">Git.io</a> service.
     *
     * @param longURL the long URL to be shortened
     * @return the shortened version of the URL
     * @throws Exception
     */
    public static URL shorten(URL longURL) throws Exception {
        HttpURLConnection con = null;
        try {
            // set the connection
            URL shortener = new URL(SHORTENER_URL);
            con = (HttpURLConnection) shortener.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            // write the parameters
            try(DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.writeBytes("url=" + longURL);
                wr.flush();
            }

            if (con.getResponseCode() == 200) {
                // successfully response, read the shortened version of the URL
                return new URL(String.format(GitIO.SHORTENED_URL, GitIO.read(con.getInputStream())));
            } else {
                // some error occurred in the connection
                throw new Exception(GitIO.read(con.getErrorStream()));
            }
        } catch (IOException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * This method reads the available data from the provided input stream
     *
     * @param inputStream
     * @return a string representation of the read data
     */
    protected static String read(InputStream inputStream) {
        try(BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }
}