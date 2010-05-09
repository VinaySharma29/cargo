/*
 * ========================================================================
 * 
 * Copyright 2005-2010 Vincent Massol.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * ========================================================================
 */
package org.codehaus.cargo.container.jboss;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.internal.AntContainerExecutorThread;
import org.codehaus.cargo.container.jboss.internal.AbstractJBoss5xInstalledLocalContainer;
import org.codehaus.cargo.container.property.GeneralPropertySet;

/**
 * JBoss 6.x series container implementation.
 * 
 * @version $Id: JBoss5xInstalledLocalContainer.java 1705 2008-09-02 13:14:55Z adriana $
 */
public class JBoss6xInstalledLocalContainer extends AbstractJBoss5xInstalledLocalContainer
{
    /**
     * JBoss 6.x series unique id.
     */
    public static final String ID = "jboss6x";

    /**
     * {@inheritDoc}
     * @see AbstractJBossInstalledLocalContainer#AbstractJBossInstalledLocalContainer(org.codehaus.cargo.container.configuration.LocalConfiguration)
     */
    public JBoss6xInstalledLocalContainer(LocalConfiguration configuration)
    {
        super(configuration);
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.Container#getId()
     */
    public String getId()
    {
        return ID;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.Container#getName()
     */
    public String getName()
    {
        return "JBoss " + getVersion("6x");
    }
    
    /**
     * {@inheritDoc}
     */
    protected void doStop(Java java) throws Exception
    {
        Path classPath = java.createClasspath();
        classPath.createPathElement().setLocation(new File(getHome(), "bin/shutdown.jar"));
        java.setClassname("org.jboss.Shutdown");

        java.createArg().setValue(
            "--server=service:jmx:rmi:///jndi/rmi://" 
                + getConfiguration().getPropertyValue(GeneralPropertySet.HOSTNAME) + ":"
                + getConfiguration().getPropertyValue(GeneralPropertySet.RMI_PORT) + "/jmxrmi");
        
        String jbossUser = getConfiguration().getPropertyValue(JBossPropertySet.JBOSS_USER);
        String jbossPassword = getConfiguration().getPropertyValue(JBossPropertySet.JBOSS_PASSWORD);
        if (jbossUser != null)
        {
            java.createArg().setValue("--user=" + jbossUser);
            if (jbossPassword != null)
            {
                java.createArg().setValue("--password=" + jbossPassword);
            }
        }

        AntContainerExecutorThread jbossRunner = new AntContainerExecutorThread(java);
        jbossRunner.start();

        jbossRunner.join(5000L);
        if (jbossRunner.isAlive())
        {
            getLogger().warn("Stopping server, not finished after 5 seconds.", 
                    this.getClass().getName());
        }
        // Sleep some extra time to fully ensure JBoss is stopped before giving back the control
        // to the user.
        Thread.sleep(5000L);
        
        BuildException buildException = jbossRunner.getBuildException();
        if (null != buildException)
        {
            // Stopping failed: in cargo code: Java returned: 1
            // Can't get much more information if Java is forked :(
            getLogger().warn("Stopping failed: " + buildException.getMessage(), 
                    this.getClass().getName());
            throw buildException;
        }
    }
}