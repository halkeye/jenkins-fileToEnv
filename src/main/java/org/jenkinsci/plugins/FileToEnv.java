package org.jenkinsci.plugins;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.EnvironmentContributingAction;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class FileToEnv extends Builder {
    private final String fileName;
    private final String envName;
    private String fileContents;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public FileToEnv(String fileName, String envName){ 
        this.fileName = fileName;
        this.envName = envName;
    }
    
    public String getFileName() { return fileName; }
    public String getEnvName() { return envName; }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException {
        //String fileName = getDescriptor().getFileName();
        //String envName = getDescriptor().getEnvName();

        listener.getLogger().println("The fileName of the test is: "+ fileName);
        listener.getLogger().println("The envName of the test is: "+ envName);
			
        try {
            EnvVars vars = build.getEnvironment(listener);

            if (vars == null) { return false; }
                    
            FilePath filePath = new FilePath(build.getWorkspace(), vars.expand(fileName));       

            if (filePath == null) {
                listener.getLogger().println("Unable to load file - "+ vars.expand(fileName));
                return false;
            }
            
            fileContents = filePath.readToString();
            listener.getLogger().println("Contents: "+ fileContents);
            vars.put(envName, fileContents);

            EnvironmentContributingAction environmentAction = new EnvironmentContributingAction() {
                public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
                    System.err.println("Adding " + envName + " with " + fileContents);
                    env.put(envName, fileContents);
                }
                public String getDisplayName() { return null; }
                public String getIconFileName() { return null; }
                public String getUrlName() { return null; }
            };

            System.err.println("Adding");
            build.getActions().add(environmentAction);
            System.err.println(" DoneAdding");
            
            vars = build.getEnvironment(listener);
            System.err.println(vars.expand("Success env ${LAST_GIT_CHANGELOG}"));

        } catch (IOException ex) {
            listener.getLogger().println("IOException " + ex);
            // FIXME
            return false;
        }
        
        listener.getLogger().println("Success");

        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    // https://wiki.jenkins-ci.org/display/~martino/2011/10/27/The+JenkinsPluginTotallySimpelGuide
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
    {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private String fileName;
        private String envName;
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            
            fileName = formData.getString("fileName");
            envName = formData.getString("envName");

            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Set ENV from File";
        }

        public String getFileName() {
            return fileName;
        }
        
        public String getEnvName() {
            return envName;
        }
    }
}

