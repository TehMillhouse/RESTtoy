import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Random;

@WebServlet(name = "DocumentServlet", urlPatterns = "/storage/documents/*")
public class DocumentServlet extends HttpServlet {

    static private Random rng = new Random();
    private HashMap<String, Document> documents = new HashMap<>();

    /**
     * Generates a 20 characters long alphanumeric UUID
     */
    static private String newUUID() {
        StringBuilder uuid = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            int idx = rng.nextInt(26 + 10); // letters + numbers
            if (idx < 10) {
                uuid.append(idx);
            } else {
                uuid.append((char) (((int) 'A') + idx - 10));
            }
        }
        return uuid.toString();
    }

    /**
     *  On input validation: validating the preconditions for the server routes (is the UUID there?
     *  does it correspond to an existing document?) is done in helper methods which set the
     *  HTTP status code in case of an error. Unfortunately, HttpServletResponse.sendError does
     *  *not* actually send an error immediately (and escape via exception), so I have to do that myself.
     *
     *  Using exceptions for control flow is generally frowned upon, but IMO, it's worth it in this case,
     *  as this condenses the if-then-else-cascade of input validation into
     *  a row of assertions, which is much easier to read.
     */

    /**
     * If the first argument is `true`, ensures the UUID parameter is present in the request URL,
     * otherwise ensures the UUID parameter is NOT present.
     * @param shouldBePresent whether the URL should conform to the pattern /storage/documents/SOMETHING or /storage/documents
     * @throws IllegalArgumentException If the condition required by the first argument is NOT fulfilled.
     *   In this case, the HTTP status code is already set
     */
    static private void assertUuidParameter(boolean shouldBePresent, HttpServletRequest req, HttpServletResponse resp)
            throws IOException, IllegalArgumentException {
        if ((req.getPathInfo() != null) != shouldBePresent) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            throw new IllegalArgumentException();
        }
    }

    /**
     * Asserts that the requested document is actually present on the server, and returns its UUID
     * @throws IllegalArgumentException If the document can't be found. In this case, HTTP status code 404 is already set.
     */
    private String assertAndGetExistingUuid(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, IllegalArgumentException {
        String uuid = req.getPathInfo().substring(1);  // strip '/' at beginning
        if (!this.documents.containsKey(uuid)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            throw new IllegalArgumentException();
        }
        return uuid;
    }

    /**
     * Read the request's Content as uninterpreted byte array
     */
    static private byte[] readContent(HttpServletRequest req) throws IOException {
        // since "documents don't need to be persisted cross server shutdown"
        // I'm assuming every file will fit in a single allocation in memory
        int total = req.getContentLength();
        byte[] content = new byte[total];
        int read = 0;
        int available = 0;
        // apparently, InputStream.read(byte[], offset, length) doesn't simply call down to memcpy,
        // but instead opts to *read the bytes one at a time*, so I guess we're slapping
        // a BufferedInputStream around this. This does double the in-transit memory footprint
        // of a document, but the alternative would be reimplementing what InputStream *should* be doing.
        BufferedInputStream is = new BufferedInputStream(req.getInputStream(), total);
        while (read < total) {
            // ensure Blocking read, rather than burning CPU time
            available = Math.max(is.available(), 1);
            is.read(content, read, available);
            // This loop would also be a good place to add timeout logic
            read += available;
        }
        return content;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            assertUuidParameter(false, req, resp);

            byte[] data = readContent(req);
            String contentType = req.getContentType();
            String uuid = newUUID();
            Document newDoc = new Document(contentType, data);
            this.documents.put(uuid, newDoc);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            PrintWriter out = resp.getWriter();
            out.write(uuid);
        } catch(IllegalArgumentException alreadyHandled) {}
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            assertUuidParameter(true, req, resp);
            String uuid = assertAndGetExistingUuid(req, resp);

            // judgment call: DRY principle
            // technically, I could factor out the code common to doPut and doPost,
            // however, both methods are small, might have to be modified independently,
            // and factoring out the code doesn't lead to a huge saving of code duplication
            // ==> prefer repeating code in this case
            byte[] data = readContent(req);
            String contentType = req.getContentType();
            Document newDoc = new Document(contentType, data);
            this.documents.put(uuid, newDoc);

            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch(IllegalArgumentException alreadyHandled) {}
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        try {
            assertUuidParameter(true, req, resp);
            String uuid = assertAndGetExistingUuid(req, resp);

            Document doc = this.documents.get(uuid);
            resp.setContentType(doc.getContentType());
            byte[] data = doc.getData();
            resp.setContentLength(data.length);

            BufferedOutputStream out = new BufferedOutputStream(resp.getOutputStream());
            out.write(data);
            out.flush();
        } catch(IllegalArgumentException alreadyHandled) {}
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            assertUuidParameter(true, req, resp);
            String uuid = assertAndGetExistingUuid(req, resp);

            this.documents.remove(uuid);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch(IllegalArgumentException alreadyHandled) {}
    }
}
