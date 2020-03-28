import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Random;

import static java.lang.Math.max;

@WebServlet(name = "DocumentServlet", urlPatterns = "/storage/documents/*")
public class DocumentServlet extends HttpServlet {

    static private Random rng = new Random();

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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // since "documents don't need to be persisted cross server shutdown"
        // I'm assuming every file will fit in a single allocation in memory
        int total = req.getContentLength();
        byte[] doc = new byte[total];
        int read = 0;
        int available = 0;
        // I'm not using a BufferedReader for this, since the OS is keeping a buffer for all the data
        // that has been received anyways.
        InputStream is = req.getInputStream();
        while (read < total) {
            // ensure Blocking read, rather than burning CPU time
            available = Math.max(is.available(), 1);
            is.read(doc, read, available);
            // This loop would also be a good place to add timeout logic
            read += available;
        }

        String uuid = newUUID();

        resp.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = resp.getWriter();
        out.write(uuid + '\n');
        // super.doPost(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getPathInfo() == null) {
            // user is trying to GET /storage/documents, which only accepts POST
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }

        PrintWriter out = resp.getWriter();
        out.println("HERE BE DRAGONS!\n");
        out.println(req.getPathInfo() + "\n");
    }
}
