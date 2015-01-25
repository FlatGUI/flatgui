/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core;

import clojure.lang.Var;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGClientAppLoader
{
    private static final String REGISTER_FN_NAME = "register-container";

    public static void initialize(String... source)
    {
        for (String src : source)
        {
            File srcFile = new File(src);

            String content = null;
            try
            {
                content = new Scanner(srcFile).useDelimiter("\\Z").next();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            initializeFromContent(content);
        }
    }

    public static void initializeFromContent(String content)
    {
        // Ensure RT class is initialized before compiler
        System.out.println(clojure.lang.RT.byteCast(1));

        try
        {
            clojure.lang.Compiler.load(new StringReader(content));
        }
        catch(Exception ex)
        {
            System.out.println("Unable to parse source content: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void registerContainer(String clientNs, String clientContainerVarName, String containerName)
    {
        Var containerVar = clojure.lang.RT.var(clientNs, clientContainerVarName);
        Var registerFn = clojure.lang.RT.var(FGModule.FG_CORE_NAMESPACE, REGISTER_FN_NAME);
        registerFn.invoke(containerName, containerVar.get());
    }
}
