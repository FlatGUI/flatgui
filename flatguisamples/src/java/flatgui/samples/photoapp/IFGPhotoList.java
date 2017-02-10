/*
 * Copyright Denys Lebediev
 */
package flatgui.samples.photoapp;

import java.util.List;

/**
 * @author Denis Lebedev
 */
public interface IFGPhotoList
{
    List<String> getFolders();

    String getCoverPhoto(String folder);

    String getTitle(String folder);

    List<String> getPhotoURLs(String folder);
}
