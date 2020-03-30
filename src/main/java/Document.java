public class Document {
    private String contentType;
    private byte[] data;

    public String getContentType() {
        return contentType;
    }

    public byte[] getData() {
        return data;
    }

    public Document(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }
}
