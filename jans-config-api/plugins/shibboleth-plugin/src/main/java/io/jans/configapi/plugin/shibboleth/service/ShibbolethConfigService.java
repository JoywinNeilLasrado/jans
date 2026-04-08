package io.jans.configapi.plugin.shibboleth.service;

import io.jans.as.common.service.common.ApplicationFactory;
import io.jans.configapi.plugin.shibboleth.configuration.ShibbolethPluginConfigurationFactory;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginAppConf;
import io.jans.configapi.plugin.shibboleth.model.config.ShibbolethPluginConfiguration;
import io.jans.configapi.plugin.shibboleth.util.Constants;
import io.jans.orm.PersistenceEntryManager;
import io.jans.util.exception.InvalidConfigurationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@ApplicationScoped
public class ShibbolethConfigService {

    @Inject
    Logger logger;

    @Inject
    @Named(ApplicationFactory.PERSISTENCE_ENTRY_MANAGER_NAME)
    PersistenceEntryManager persistenceManager;

    @Inject
    ShibbolethPluginConfigurationFactory shibbolethPluginConfigurationFactory;

    // Config handling methods
    public ShibbolethPluginConfiguration findShibbolethPluginConfiguration() {
        final String dn = shibbolethPluginConfigurationFactory.getShibbolethPluginAppConfigurationDn();
        if (StringUtils.isBlank(dn)) {
            throw new InvalidConfigurationException("Shibboleth Configuration DN is undefined!");
        }

        logger.info(" dn:{}", dn);
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = persistenceManager.find(dn, ShibbolethPluginConfiguration.class, null);
        logger.info(" ShibbolethPluginConfiguration:{}", shibbolethPluginConfiguration);

        return shibbolethPluginConfiguration;
    }

    public void mergeShibbolethPluginAppConf(ShibbolethPluginAppConf shibbolethPluginAppConf) {
        shibbolethPluginAppConf.setRevision(shibbolethPluginAppConf.getRevision() + 1);
        persistenceManager.merge(shibbolethPluginAppConf);
    }

    public ShibbolethPluginAppConf find() {
        return getShibbolethPluginAppConf().getDynamicConf();
    }

    // Utility methods
    public String getTrustRelationshipDn() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String trustRelationshipDn = null;
        if (shibbolethPluginAppConf != null) {
            trustRelationshipDn = shibbolethPluginAppConf.getSamlTrustRelationshipDn();
        }
        return trustRelationshipDn;
    }

    public String getTrustedIdpDn() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String trustedIdpDn = null;
        if (shibbolethPluginConfiguration != null) {
            trustedIdpDn = shibbolethPluginConfiguration.getTrustedIdpDn();
        }
        return trustedIdpDn;
    }

    public boolean isSamlEnabled() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        logger.debug("shibbolethPluginAppConf.getDynamicConf():{}", shibbolethPluginAppConf.getDynamicConf());
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        boolean isSamlEnabled = false;
        if (shibbolethPluginConfiguration != null) {
            isSamlEnabled = shibbolethPluginConfiguration.isEnabled();
        }
        return isSamlEnabled;
    }

    public String getSelectedIdp() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String selectedIdp = null;
        if (shibbolethPluginConfiguration != null) {
            selectedIdp = shibbolethPluginConfiguration.getSelectedIdp();
        }
        return selectedIdp;
    }
   
    public String getIdpRootDir() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String idpRootDir = null;
        if (shibbolethPluginConfiguration != null) {
            idpRootDir = shibbolethPluginConfiguration.getIdpRootDir();
        }
        return idpRootDir;
    }

    public String getIdpMetadataDir() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration ShibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String idpMetadataDir = null;
        if (shibbolethPluginAppConf != null) {
            idpMetadataDir = shibbolethPluginAppConf.getIdpMetadataDir();
        }
        return idpMetadataDir;
    }

    public String getIdpMetadataTempDir() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String idpMetadataTempDir = null;
        if (shibbolethPluginConfiguration != null) {
            idpMetadataTempDir = shibbolethPluginConfiguration.getIdpMetadataTempDir();
        }
        return idpMetadataTempDir;
    }

    public String getIdpMetadataFilePattern() {
        return Constants.IDP_METADATA_FILE_PATTERN;
    }

    public String getIdpMetadataFile() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String idpMetadataFile = null;
        if (shibbolethPluginConfiguration != null) {
            idpMetadataFile = shibbolethPluginConfiguration.getIdpMetadataFile();
        }
        return idpMetadataFile;
    }

    public String getSpMetadataDir() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String spMetadataDir = null;
        if (shibbolethPluginConfiguration != null) {
            spMetadataDir = shibbolethPluginConfiguration.getSpMetadataDir();
        }
        return spMetadataDir;
    }

    public String getSpMetadataTempDir() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String spMetadataTempDir = null;
        if (shibbolethPluginConfiguration != null) {
            spMetadataTempDir = shibbolethPluginConfiguration.getSpMetadataTempDir();
        }
        return spMetadataTempDir;
    }

    public String getSpMetadataFilePattern() {
        return Constants.SP_METADATA_FILE_PATTERN;
    }

    public String getSpMetadataFile() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        String spMetadataFile = null;
        if (shibbolethPluginConfiguration != null) {
            spMetadataFile = shibbolethPluginConfiguration.getSpMetadataFile();
        }
        return spMetadataFile;
    }

    public boolean isIgnoreValidation() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        boolean ignoreValidation = false;
        if (shibbolethPluginConfiguration != null) {
            ignoreValidation = shibbolethPluginConfiguration.isIgnoreValidation();
        }
        return ignoreValidation;
    }
    
    
    public boolean isSetConfigDefaultValue() {
        final ShibbolethPluginAppConf shibbolethPluginAppConf = getShibbolethPluginAppConf();
        ShibbolethPluginConfiguration shibbolethPluginConfiguration = shibbolethPluginAppConf.getDynamicConf();
        boolean setConfigDefaultValue = false;
        if (shibbolethPluginConfiguration != null) {
            setConfigDefaultValue = shibbolethPluginConfiguration.isSetConfigDefaultValue();
        }
        return setConfigDefaultValue;
    }
   
    private ShibbolethPluginAppConf getShibbolethPluginAppConf() {
        ShibbolethPluginAppConf shibbolethPluginAppConf = findShibbolethPluginConfiguration();
        if (shibbolethPluginAppConf == null) {
            throw new InvalidConfigurationException("ShibbolethPluginAppConf is undefined!");
        }
        logger.debug("  ShibbolethPluginAppConf:{}", shibbolethPluginAppConf);
        return shibbolethPluginAppConf;
    }
}
