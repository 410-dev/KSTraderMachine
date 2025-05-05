package me.hysong.atlas.utils;

import me.hysong.atlas.interfaces.KSDeepSystemCommunicator;

import java.io.*;
import java.util.HashMap;

public class MFS1 {

    private static final HashMap<String, String> pathCache_VtoR = new HashMap<>();
    private static final HashMap<String, String> pathCache_RtoV = new HashMap<>();

    public static String realPath(String virtualPath) {
        KSDeepSystemCommunicator com = KSHostTool.getSystemCommunicator();
        if (pathCache_VtoR.containsKey(virtualPath)) {
            return pathCache_VtoR.get(virtualPath);
        } else {
            String realPath = com.toRealPath(virtualPath);
            if (realPath != null && !realPath.isEmpty()) {
                pathCache_VtoR.put(virtualPath, realPath);
            }
            return realPath;
        }
    }

    public static String virtualPath(String realPath) {
        KSDeepSystemCommunicator com = KSHostTool.getSystemCommunicator();
        if (pathCache_RtoV.containsKey(realPath)) {
            return pathCache_RtoV.get(realPath);
        } else {
            String virtualPath = com.toVirtualPath(realPath);
            if (virtualPath != null && !virtualPath.isEmpty()) {
                pathCache_RtoV.put(realPath, virtualPath);
            }
            return virtualPath;
        }
    }

    public static boolean mkdirs(String virtualPath) {
        if (virtualPath == null || virtualPath.isEmpty()) {
            return false;
        }
        String realPath = realPath(virtualPath);
        if (realPath == null || realPath.isEmpty()) {
            return false;
        }
        return new File(realPath).mkdirs();
    }

    public static boolean mkdir(String virtualPath) {
        if (virtualPath == null || virtualPath.isEmpty()) {
            return false;
        }
        String realPath = realPath(virtualPath);
        if (realPath == null || realPath.isEmpty()) {
            return false;
        }
        return new File(realPath).mkdir();
    }

    public static boolean write(String virtualPath, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(realPath(virtualPath)));
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean append(String virtualPath, String content) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(realPath(virtualPath), true));
            writer.write(content);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean write(String virtualPath, byte[] content) {
        try {
            FileOutputStream fos = new FileOutputStream(realPath(virtualPath));
            fos.write(content);
            fos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean write(String virtualPath, Object o) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(realPath(virtualPath)));
            oos.writeObject(o);
            oos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String readString(String virtualPath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(realPath(virtualPath)));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] readBytes(String virtualPath) {
        try {
            FileInputStream fis = new FileInputStream(realPath(virtualPath));
            byte[] bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object readObject(String virtualPath) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(realPath(virtualPath)));
            Object obj = ois.readObject();
            ois.close();
            return obj;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static HashMap<String, Boolean> listContent(String virtualPath, boolean includeHidden) {
        File file = new File(realPath(virtualPath));
        HashMap<String, Boolean> map = new HashMap<>();
        if (file.exists() && file.isDirectory()) {
            if (!file.canRead()) {
                return map;
            }
            File[] files = file.listFiles();
            if (files == null) {
                return map;
            }
            for (File f : files) {
                if (includeHidden || !f.isHidden()) {
                    map.put(f.getName(), f.isDirectory());
                }
            }
        }
        return map;
    }

    public static HashMap<String, Boolean> listContentRecursively(String virtualPath, boolean includeHidden) {
        File file = new File(realPath(virtualPath));
        HashMap<String, Boolean> map = new HashMap<>();
        if (file.exists() && file.isDirectory()) {
            if (!file.canRead()) {
                return map;
            }
            File[] files = file.listFiles();
            if (files == null) {
                return map;
            }
            for (File f : files) {
                if (includeHidden || !f.isHidden()) {
                    map.put(virtualPath(f.getAbsolutePath()), f.isDirectory());
                }
                if (f.isDirectory()) {
                    map.putAll(listContentRecursively(virtualPath(f.getAbsolutePath()), includeHidden));
                }
            }
        }
        return map;
    }

    public static String[] listDirectories(String virtualPath, boolean recursive) {
        HashMap<String, Boolean> map = recursive ? listContentRecursively(virtualPath, false) : listContent(virtualPath, false);
        return map.entrySet().stream()
                .filter(entry -> entry.getValue())
                .map(entry -> entry.getKey())
                .toArray(String[]::new);
    }

    public static String[] listFiles(String virtualPath, boolean recursive) {
        HashMap<String, Boolean> map = recursive ? listContentRecursively(virtualPath, false) : listContent(virtualPath, false);
        return map.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(entry -> entry.getKey())
                .toArray(String[]::new);
    }

    public static boolean delete(String virtualPath) {
        File file = new File(realPath(virtualPath));
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }

    public static boolean deleteRecursively(String virtualPath) {
        File file = new File(realPath(virtualPath));
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteRecursively(virtualPath(f.getAbsolutePath()));
                }
            }
            return file.delete();
        }
        return false;
    }

    public static boolean rename(String oldVirtualPath, String newVirtualPath) {
        File oldFile = new File(realPath(oldVirtualPath));
        File newFile = new File(realPath(newVirtualPath));
        if (oldFile.exists()) {
            return oldFile.renameTo(newFile);
        }
        return false;
    }

    public static boolean copy(String sourceVirtualPath, String destVirtualPath) {
        File sourceFile = new File(realPath(sourceVirtualPath));
        File destFile = new File(realPath(destVirtualPath));
        if (sourceFile.exists()) {
            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
