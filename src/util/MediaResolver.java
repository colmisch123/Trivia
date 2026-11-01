package util;

import java.io.File;
import java.net.URL;

public class MediaResolver {
    public static File resolveMediaFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        String normalized = path.replace('\\', '/').trim();
        String fileName = new File(normalized).getName();

        // Direct candidate files on disk
        String[] candidatePaths = {
                normalized,
                "src/Resource/" + normalized,
                "Resource/" + normalized,
                "src/Resource/Halloween_trivia/" + fileName,
                "Resource/Halloween_trivia/" + fileName,
                "src/Resource/" + fileName,
                "Resource/" + fileName
        };

        for (String cp : candidatePaths) {
            File f = new File(cp);
            if (f.exists() && f.isFile()) {
                return f;
            }
        }

        // Fuzzy match: check if base name matches (e.g. F.jpeg -> F.jpg or m.jpg -> m.jpeg)
        String baseName = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        String[] dirs = {"src/Resource/Halloween_trivia", "Resource/Halloween_trivia", "src/Resource", "Resource"};
        for (String dirPath : dirs) {
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] matches = dir.listFiles((d, name) -> {
                    String n = name.toLowerCase();
                    String b = n.contains(".") ? n.substring(0, n.lastIndexOf('.')) : n;
                    return b.equalsIgnoreCase(baseName);
                });
                if (matches != null && matches.length > 0) {
                    return matches[0];
                }
            }
        }

        return null;
    }

    public static URL resolveMediaURL(String path) {
        File file = resolveMediaFile(path);
        if (file != null) {
            try {
                return file.toURI().toURL();
            } catch (Exception ignored) {
            }
        }

        if (path == null || path.isBlank()) {
            return null;
        }

        String normalized = path.replace('\\', '/').trim();
        String fileName = new File(normalized).getName();

        String[] resourceCandidates = {
                "/" + normalized,
                "/Resource/" + normalized,
                "/Resource/Halloween_trivia/" + fileName,
                "/Halloween_trivia/" + fileName,
                "/Resource/" + fileName,
                "/" + fileName,
                normalized
        };

        for (String rc : resourceCandidates) {
            URL url = MediaResolver.class.getResource(rc);
            if (url != null) {
                return url;
            }
        }

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            for (String rc : resourceCandidates) {
                String clRc = rc.startsWith("/") ? rc.substring(1) : rc;
                URL url = cl.getResource(clRc);
                if (url != null) {
                    return url;
                }
            }
        }

        return null;
    }
}
