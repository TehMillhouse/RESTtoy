import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Random;

import static java.lang.Math.max;

@WebServlet(name = "DocumentServlet", urlPatterns = "/storage/documents/*")
public class DocumentServlet extends HttpServlet {

    static private Random rng = new Random();
    // if Java had tuples, I'd use a HashMap<String, (String, byte[])>, but alas...
    private HashMap<String, byte[]> documents = new HashMap<>();
    private HashMap<String, String> contentTypes = new HashMap<>();

    static private String newUUID() {
        StringBuilder uuid = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            int idx = rng.nextInt(26 + 10); // letters + numbers
            if (idx < 10) {
                uuid.append(idx);
            } else {
                uuid.append((char) (((int) 'A') + (idx - 10)));
            }
        }
        return uuid.toString();
    }

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
        if (req.getPathInfo() != null) {
            // user is trying to POST to /storage/documents/SOMETHING
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        byte[] data = readContent(req);
        String contentType = req.getContentType();
        String uuid = newUUID();
        this.contentTypes.put(uuid, contentType);
        this.documents.put(uuid, data);

        resp.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = resp.getWriter();
        out.write(uuid + '\n');
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // user is trying to GET /storage/documents, which only accepts POST
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        String uuid = req.getPathInfo().substring(1);  // strip '/' at beginning
        if (!this.documents.containsKey(uuid)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }

        resp.setContentType(this.contentTypes.get(uuid));
        byte[] doc = this.documents.get(uuid);
        resp.setContentLength(doc.length);

        BufferedOutputStream out = new BufferedOutputStream(resp.getOutputStream());
        out.write(doc);

    }
}
