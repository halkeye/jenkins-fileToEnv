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
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class FileToEnv extends Builder {
    private final String fileName;
    private final String envName;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public FileToEnv(String fileName, String envName){ 
        this.fileName = fileName;
        this.envName = envName;
    }
    
    public String getFileName() { return fileName; }
    public String getEnvName() { return envName; }


    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
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
            
            String fileContents = filePath.readToString();
            vars.put(envName, fileContents);

            
            EnvAction envData = build.getAction(EnvAction.class);
            if (envData != null) {
                envData.add(envName, fileContents);
            }
        } catch (InterruptedException ex) {
            listener.getLogger().println("InterruptedException " + ex);
        } catch (IOException ex) {
            listener.getLogger().println("IOException " + ex);
            // FIXME
            return false;
        }
        
        listener.getLogger().println("Success env");

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
    
    private static class EnvAction implements EnvironmentContributingAction {
        private transient Map<String,String> data = new HashMap<String,String>();

        private void add(String key, String value) {
            if (data==null) return;
            data.put(key, value);
        }

        public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
            if (data!=null) env.putAll(data);
        }

        public String getIconFileName() { return null; }
        public String getDisplayName() { return null; }
        public String getUrlName() { return null; }
    }
}

