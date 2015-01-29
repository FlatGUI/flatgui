/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core2;

import java.io.StringReader;

/**
 * @author Denis Lebedev
 */
public class FGTemplate implements IFGTemplate
{
    private final String containerNamespace_;
    private final String containerVarName_;

    public FGTemplate(String sourceCode, String containerNamespace, String containerVarName) throws Exception
    {
        containerNamespace_ = containerNamespace;
        containerVarName_ = containerVarName;

        // Ensure RT class is initialized before compiler
        System.out.println(clojure.lang.RT.byteCast(1));

        clojure.lang.Compiler.load(new StringReader(sourceCode));
    }

    @Override
    public String getContainerNamespace()
    {
        return containerNamespace_;
    }

    @Override
    public String getContainerVarName()
    {
        return containerVarName_;
    }
}
