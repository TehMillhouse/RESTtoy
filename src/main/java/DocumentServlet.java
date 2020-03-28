import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;

@WebServlet(name = "DocumentServlet", urlPatterns = "/storage/documents/*")
public class DocumentServlet extends HttpServlet {

    static String newUUID() {
        //TODO
        return null;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // if I were expecting to persist these files, I'd make sure to use getContentLengthLong,
        // and only read / write chunks at a time, but since "documents don't need to be persisted
        // across server shutdown", I'm assuming every file will fit in a single allocation in memory
        int total = req.getContentLength();
        byte[] doc = new byte[total];
        int read = 0;
        int available = 0;
        // I'm not using a BufferedReader for this, since the OS is keeping a buffer for all the data
        // that has been received anyways.
        InputStream is = req.getInputStream();
        while (read < total) {
            available = is.available();
            is.read(doc, read, available);  // trickle-fill our buffer.
            // This loop would also be a good place to add timeout logic

            read += available;
        }

        resp.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = resp.getWriter();
        out.write(doc[0]);
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
