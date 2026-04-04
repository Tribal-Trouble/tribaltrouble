package com.oddlabs.http;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * <code>MultiPartFormOutputStream</code> is used to write
 * "multipart/form-data" to a <code>java.net.URLConnection</code> for
 * POSTing.  This is primarily for file uploading to HTTP servers.
 *
 * @since JDK1.3
 *
 * <a href="http://forum.java.sun.com/thread.jsp?forum=31&thread=451245">...</a>
 *
 */

public class MultiPartFormOutputStream {

    /**
     * The line end characters.
     */
    private static final String NEWLINE = "\r\n";

    /**
     * The boundary prefix.
     */
    private static final String PREFIX = "--";

    /**
     * The output stream to write to.
     */
    private final @NonNull DataOutputStream out;

    /**
     * The multipart boundary string.
     */
    private final @NonNull String boundary;

    /**
     * Creates a new <code>MultiPartFormOutputStream</code> object using
     * the specified output stream and boundary.  The boundary is required
     * to be created before using this method, as described in the
     * description for the <code>getContentType(String)</code> method.
     * The boundary is only checked for <code>null</code> or empty string,
     * but it is recommended to be at least 6 characters.  (Or use the
     * static createBoundary() method to create one.)
     *
     * @param os       the output stream
     * @param boundary the boundary
     * @see #createBoundary()
     * @see #getContentType(String)
     */
    public MultiPartFormOutputStream(@NonNull OutputStream os, @NonNull String boundary) {
        if (os == null) {
            throw new IllegalArgumentException("Output stream is required.");
        }
        if (boundary == null || boundary.isEmpty()) {
            throw new IllegalArgumentException("Boundary stream is required.");
        }
        this.out = new DataOutputStream(os);
        this.boundary = boundary;
    }

    /**
     * Writes an boolean field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, boolean value) throws java.io.IOException {
        writeField(name, Boolean.toString(value));
    }

    /**
     * Writes an double field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, double value) throws java.io.IOException {
        writeField(name, Double.toString(value));
    }

    /**
     * Writes an float field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, float value) throws java.io.IOException {
        writeField(name, Float.toString(value));
    }

    /**
     * Writes an long field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, long value) throws java.io.IOException {
        writeField(name, Long.toString(value));
    }

    /**
     * Writes an int field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, int value) throws java.io.IOException {
        writeField(name, Integer.toString(value));
    }

    /**
     * Writes an short field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, short value) throws java.io.IOException {
        writeField(name, Short.toString(value));
    }

    /**
     * Writes an char field value.
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, char value) throws java.io.IOException {
        writeField(name, Character.toString(value));
    }

    /**
     * Writes an string field value.  If the value is null, an empty string
     * is sent ("").
     *
     * @param name  the field name (required)
     * @param value the field value
     * @throws java.io.IOException on input/output errors
     */
    public void writeField(@NonNull String name, @Nullable String value) throws java.io.IOException {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        if (value == null) {
            value = "";
        }
		/*
		   --boundary\r\n
		   Content-Disposition: form-data; name="<fieldName>"\r\n
		   \r\n
		   <value>\r\n
		 */
        // write boundary
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        // write content header
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"");
        out.writeBytes(NEWLINE);
        out.writeBytes(NEWLINE);
        // write content
        out.writeBytes(value);
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
     * Writes a file's contents.  If the file is null, does not exists, or
     * is a directory, a <code>java.lang.IllegalArgumentException</code>
     * will be thrown.
     *
     * @param name     the field name
     * @param mimeType the file content type (optional, recommended)
     * @param file     the file (the file must exist)
     * @throws java.io.IOException on input/output errors
     */
    public void writeFile(String name, String mimeType, @NonNull File file) throws java.io.IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("File does not exist.");
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("File cannot be a directory.");
        }
        writeFile(name, mimeType, file.getCanonicalPath(), new FileInputStream(file));
    }

    private void writeFileHeader(String name, @Nullable String mimeType, String fileName) throws java.io.IOException {
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(NEWLINE);
        // write content header
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"");
        out.writeBytes(NEWLINE);
        if (mimeType != null) {
            out.writeBytes("Content-Type: " + mimeType);
            out.writeBytes(NEWLINE);
        }
        out.writeBytes(NEWLINE);
    }

    /**
     * Writes a input stream's contents.  If the input stream is null, a
     * <code>java.lang.IllegalArgumentException</code> will be thrown.
     *
     * @param name     the field name
     * @param mimeType the file content type (optional, recommended)
     * @param fileName the file name (required)
     * @param is       the input stream
     * @throws java.io.IOException on input/output errors
     */
    public void writeFile(String name, String mimeType, @NonNull String fileName, @NonNull InputStream is) throws java.io.IOException {
        if (is == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
		/*
		   --boundary\r\n
		   Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
		   Content-Type: <mime-type>\r\n
		   \r\n
		   <file-data>\r\n
		 */
        // write boundary
        writeFileHeader(name, mimeType, fileName);
        // write content
        byte[] data = new byte[1024];
        int r = 0;
        while ((r = is.read(data, 0, data.length)) != -1) {
            out.write(data, 0, r);
        }
        // close input stream, but ignore any possible exception for it
        try {
            is.close();
        } catch (IOException _) {
        }
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
     * Writes the given bytes.  The bytes are assumed to be the contents
     * of a file, and will be sent as such.  If the data is null, a
     * <code>java.lang.IllegalArgumentException</code> will be thrown.
     *
     * @param name     the field name
     * @param mimeType the file content type (optional, recommended)
     * @param fileName the file name (required)
     * @param data     the file data
     * @throws java.io.IOException on input/output errors
     */
    public void writeFile(String name, String mimeType, @NonNull String fileName, byte @NonNull [] data) throws java.io.IOException {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null.");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty.");
        }
		/*
		   --boundary\r\n
		   Content-Disposition: form-data; name="<fieldName>"; filename="<filename>"\r\n
		   Content-Type: <mime-type>\r\n
		   \r\n
		   <file-data>\r\n
		 */
        // write boundary
        writeFileHeader(name, mimeType, fileName);
        // write content
        out.write(data, 0, data.length);
        out.writeBytes(NEWLINE);
        out.flush();
    }

    /**
     * Flushes the stream.  Actually, this method does nothing, as the only
     * write methods are highly specialized and automatically flush.
     *
     * @throws java.io.IOException on input/output errors
     */
    public void flush() throws java.io.IOException {
        // out.flush();
    }

    /**
     * Closes the stream.
     * <p>
     * <b>NOTE:</b> This method <b>MUST</b> be called to finalize the
     * multipart stream.
     *
     * @throws java.io.IOException on input/output errors
     */
    public void close() throws java.io.IOException {
        // write final boundary
        out.writeBytes(PREFIX);
        out.writeBytes(boundary);
        out.writeBytes(PREFIX);
        out.writeBytes(NEWLINE);
        out.flush();
        out.close();
    }

    /**
     * Gets the multipart boundary string being used by this stream.
     *
     * @return the boundary
     */
    public @NonNull String getBoundary() {
        return this.boundary;
    }

    /**
     * Creates a new <code>java.net.URLConnection</code> object from the
     * specified <code>java.net.URL</code>.  This is a convenience method
     * which will set the <code>doInput</code>, <code>doOutput</code>,
     * <code>useCaches</code> and <code>defaultUseCaches</code> fields to
     * the appropriate settings in the correct order.
     *
     * @return a <code>java.net.URLConnection</code> object for the URL
     * @throws java.io.IOException on input/output errors
     */
    public static @NonNull URLConnection createConnection(@NonNull URL url) throws java.io.IOException {
        URLConnection urlConn = url.openConnection();
        if (urlConn instanceof HttpURLConnection httpConn) {
            httpConn.setRequestMethod("POST");
        }
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setDefaultUseCaches(false);
        return urlConn;
    }

    /**
     * Creates a multipart boundary string by concatenating 20 hyphens (-)
     * and the hexadecimal (base-16) representation of the current time in
     * milliseconds.
     *
     * @return a multipart boundary string
     * @see #getContentType(String)
     */
    public static @NonNull String createBoundary() {
        return "--------------------" + Long.toString(System.currentTimeMillis(), 16);
    }

    /**
     * Gets the content type string suitable for the
     * <code>java.net.URLConnection</code> which includes the multipart
     * boundary string.  <br />
     * <br />
     * This method is static because, due to the nature of the
     * <code>java.net.URLConnection</code> class, once the output stream
     * for the connection is acquired, it's too late to set the content
     * type (or any other request parameter).  So one has to create a
     * multipart boundary string first before using this class, such as
     * with the <code>createBoundary()</code> method.
     *
     * @param boundary the boundary string
     * @return the content type string
     * @see #createBoundary()
     */
    public static @NonNull String getContentType(String boundary) {
        return "multipart/form-data; boundary=" + boundary;
    }

    //Test method
    void main() throws Exception {
        URL url = URI.create("http://www.domain.com/webems/upload.do").toURL();
        //--create a boundary string
        String boundary = MultiPartFormOutputStream.createBoundary();
        URLConnection urlConn = MultiPartFormOutputStream.createConnection(url);
        urlConn.setRequestProperty("Accept", "*/*");
        urlConn.setRequestProperty("Content-Type", MultiPartFormOutputStream.getContentType(boundary));
        //--set some other request headers...
        urlConn.setRequestProperty("Connection", "Keep-Alive");
        urlConn.setRequestProperty("Cache-Control", "no-cache");
        //--no need to connect cuz getOutputStream() does it
        MultiPartFormOutputStream out = new MultiPartFormOutputStream(urlConn.getOutputStream(), boundary);
        //--write a text field element
        out.writeField("myText", "text field text");
        //--upload a file
        out.writeFile("myFile", "text/plain", new File("C:\\test.txt"));
        //--can also write bytes directly
        //out.writeFile("myFile", "text/plain", "C:\\test.txt",
        //"This is some file text.".getBytes("ASCII"));
        out.close();
        //--read response from server
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        String line = "";
        while ((line = in.readLine()) != null) {
            IO.println(line);
        }
        in.close();
    }
}
