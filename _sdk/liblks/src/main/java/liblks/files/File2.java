package liblks.files;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class File2 extends File {

    private String toUniversalPath(String nativePath) {
        return nativePath.replace(File.separator, "/");
    }

    private String toNativePath(String universalPath) {
        return universalPath.replace("/", File.separator);
    }

    public File2(String pathname) {
        super(pathname.replace(File.separator, "/"));
    }

    public File2(String parent, String child) {
        super(parent.replace(File.separator, "/"), child.replace(File.separator, "/"));
    }

    public File2 child(String path) {
        return new File2(this.getAbsolutePath(), path);
    }

    public File2 parent() {
        return parent(1);
    }

    public File2 parent(int depth) {
        File2 ptr = this;
        for (int i = 0; i < depth; i++) {
            ptr = new File2(ptr.getParent());
        }
        return ptr;
    }

    public ArrayList<String> children() {
        ArrayList<String> files = new ArrayList<>();
        File[] listed = this.listFiles();
        if (listed == null) return files;

        for (File f : listed) {
            files.add(f.getName());
        }
        return files;
    }

    public ArrayList<String> childrenFiles() {
        ArrayList<String> result = children();
        result.removeIf(s -> new File(s).isDirectory());
        return result;
    }

    public ArrayList<String> childrenDirectories() {
        ArrayList<String> result = children();
        result.removeIf(s -> new File(s).isFile());
        return result;
    }

    public void writeString(boolean createParentDirectory, String content, boolean append) throws IOException {
        if (createParentDirectory) {
            if (!parent().exists()) {
                parent().mkdirs();
            }
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(this, append));
        bw.write(content);
        bw.close();
    }

    public void appendString(boolean createParentDirectory, String appendingContent) throws IOException {
        writeString(createParentDirectory, appendingContent, true);
    }

    public void writeString(boolean createParentDirectory, String content) throws IOException {
        writeString(createParentDirectory, content, false);
    }

    public String readString() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(this));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line).append("\n");
            line = br.readLine();
        }
        br.close();
        return sb.toString();
    }
}
