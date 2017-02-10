/*
 * Copyright Denys Lebediev
 */
package flatgui.samples.photoapp;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
public class FGPhotoList implements IFGPhotoList
{
    private static final String BASE_FOLDER = "baseFolder";
    private static final Set<String> ALLOWED_EXTENSIONS;
    static
    {
        Set<String> s = new HashSet<>();
        s.add("png");
        s.add("jpg");
        s.add("jpeg");
        s.add("bmp");
        ALLOWED_EXTENSIONS = Collections.unmodifiableSet(s);
    }

    private final String baseFolder_;

    public FGPhotoList()
    {
        baseFolder_ = System.getProperty(BASE_FOLDER, "D:\\PhotosPub");
    }

    @Override
    public List<String> getFolders()
    {
        File file = new File(baseFolder_);
        if (file.exists())
        {
            File[] pathFiles = file.listFiles(nextFile -> nextFile.isDirectory());
            return Arrays.stream(pathFiles)
                    .map(f -> f.getName())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getCoverPhoto(String folder)
    {
        List<String> listing = listPhotos(folder);
        return listing.size() > 0 ? listing.get(0) : null;
    }

    @Override
    public String getTitle(String folder)
    {
        return folder.replace("_", " ");
    }

    @Override
    public List<String> getPhotoURLs(String folder)
    {
        return listPhotos(folder);
    }

    private static boolean isExtensionAllowed(String fileName)
    {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex >= 0)
        {
            String extension = fileName.substring(dotIndex+1);
            extension = extension.toLowerCase();
            return ALLOWED_EXTENSIONS.contains(extension);
        }
        return false;
    }

    private List<String> listPhotos(String folder)
    {
        String folderPath = baseFolder_ +  File.separator +  folder;
        File file = new File(folderPath);
        if (file.exists())
        {
            File[] pathFiles = file.listFiles(nextFile -> !nextFile.isDirectory() && isExtensionAllowed(nextFile.getName()));
            List<String> list =  Arrays.stream(pathFiles)
                    .map(f -> "file:///" + folderPath + File.separator + f.getName())
                    .collect(Collectors.toList());
            return list;
        }
        return Collections.emptyList();
    }
}
