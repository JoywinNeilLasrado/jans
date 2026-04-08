package io.jans.configapi.plugin.shibboleth.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.jans.as.model.configuration.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShibbolethPluginConfiguration implements Configuration {

    private String applicationName;
    private boolean enabled;

    private String trustRelationshipDn;

    private String idpRootDir;
    private String spMetadataDir;
    private String spMetadataTempDir;
    private String spMetadataFile;

    private boolean setDefaultValue;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getTrustRelationshipDn() {
        return trustRelationshipDn;
    }

    public void setTrustRelationshipDn(String trustRelationshipDn) {
        this.trustRelationshipDn = trustRelationshipDn;
    }

    public String getIdpRootDir() {
        return idpRootDir;
    }

    public void setIdpRootDir(String idpRootDir) {
        this.idpRootDir = idpRootDir;
    }

    public String getSpMetadataDir() {
        return spMetadataDir;
    }

    public void setSpMetadataDir(String spMetadataDir) {
        this.spMetadataDir = spMetadataDir;
    }

    public String getSpMetadataTempDir() {
        return spMetadataTempDir;
    }

    public void setSpMetadataTempDir(String spMetadataTempDir) {
        this.spMetadataTempDir = spMetadataTempDir;
    }

    public String getSpMetadataFile() {
        return spMetadataFile;
    }

    public void setSpMetadataFile(String spMetadataFile) {
        this.spMetadataFile = spMetadataFile;
    }

    public boolean isSetDefaultValue() {
        return setDefaultValue;
    }

    public void setSetDefaultValue(boolean setDefaultValue) {
        this.setDefaultValue = setDefaultValue;
    }

    @Override
    public String toString() {
        return "ShibbolethPluginConfiguration [applicationName=" + applicationName + ", enabled=" + enabled
                + ", trustRelationshipDn=" + trustRelationshipDn + ", idpRootDir=" + idpRootDir + ", spMetadataDir="
                + spMetadataDir + ", spMetadataTempDir=" + spMetadataTempDir + ", spMetadataFile=" + spMetadataFile
                + ", setDefaultValue=" + setDefaultValue + "]";
    }  
    
}
