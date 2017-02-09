/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core.awt;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

/**
 * @author Denis Lebedev
 */
public class FGImageLoader implements IFGImageLoader
{
    // TODO cache

    @Override
    public Image getImage(String url) throws IOException
    {
        if (url.startsWith(CLASSPATH_PROTOCOL))
        {
            return ImageIO.read(ClassLoader.getSystemResource(url.substring(CLASSPATH_PROTOCOL.length())));
        }
        else
        {
            return ImageIO.read(new URL(url));
        }
    }

    public static Dimension getImageSize(String url)
    {
        if (url == null)
        {
            return null;
        }

        try (ImageInputStream is = ImageIO.createImageInputStream(new File(new URL(url).toURI())))
        {
            final Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
            if (readers.hasNext())
            {
                ImageReader reader = readers.next();
                try
                {
                    reader.setInput(is);
                    return new Dimension(reader.getWidth(0), reader.getHeight(0));
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                finally
                {
                    reader.dispose();
                }
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
