package io.jans.configapi.plugin.shibboleth.service;


import io.jans.as.common.service.OrganizationService;
import io.jans.as.common.util.AttributeConstants;
import io.jans.configapi.configuration.ConfigurationFactory;
import io.jans.configapi.plugin.shibboleth.model.ShibbolethIdpConfiguration;
import io.jans.configapi.plugin.shibboleth.model.ShibbolethIdpConfigurationProperties;
import io.jans.configapi.plugin.shibboleth.model.TrustRelationship;
import io.jans.configapi.plugin.shibboleth.model.TrustedServiceProvider;

import io.jans.configapi.util.ApiConstants;
import io.jans.model.SearchRequest;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SortOrder;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ShibbolethService {

    private static final String SHIBBOLETH_IDP_CONFIG_DN = "ou=shibboleth-idp,ou=configuration,o=jans";
    private static final String SHIBBOLETH_TR_CONFIG_DN = "inum=%s,ou=trustRelationships,%s";

    @Inject
    private Logger logger;
    
    @Inject
    ConfigurationFactory configurationFactory;
    
    @Inject
    OrganizationService organizationService;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;
    


    public ShibbolethIdpConfiguration getConfiguration() {
        logger.debug("Fetching Shibboleth IDP configuration");
        
        try {
            return persistenceEntryManager.find(ShibbolethIdpConfiguration.class, SHIBBOLETH_IDP_CONFIG_DN);
        } catch (Exception e) {
            logger.warn("Shibboleth configuration not found, returning defaults");
            return createDefaultConfiguration();
        }
    }

    public void updateConfiguration(ShibbolethIdpConfiguration configuration) {
        logger.info("Updating Shibboleth IDP configuration");
        
        configuration.setRevision(configuration.getRevision() + 1);
        
        if (persistenceEntryManager.contains(SHIBBOLETH_IDP_CONFIG_DN, ShibbolethIdpConfiguration.class)) {
            persistenceEntryManager.merge(configuration);
        } else {
            configuration.setDn(SHIBBOLETH_IDP_CONFIG_DN);
            persistenceEntryManager.persist(configuration);
        }
    }    

    public String generateInumForNewRelationship() {
        String newInum = null;
        String newDn = null;
        do {
            newInum = UUID.randomUUID().toString();
            newDn = getDnForTrustRelationship(newInum);
        } while (containsRelationship(newDn));

        return newInum;
    }
    
    public String getDnForTrustRelationship(String inum) {
        String orgDn = organizationService.getDnForOrganization();
        if (StringHelper.isEmpty(inum)) {
            return String.format("ou=trustRelationships,%s", orgDn);
        }
        return String.format(SHIBBOLETH_TR_CONFIG_DN, inum, orgDn);
    }
    
    public boolean containsRelationship(String dn) {
        return persistenceEntryManager.contains(dn, TrustRelationship.class);
    }
    
    public TrustRelationship getTrustRelationshipByDn(String dn) {
        try {
            return persistenceEntryManager.find(TrustRelationship.class, dn);
        } catch (Exception e) {
            logger.warn("", e);
            return null;
        }
    }
    

    public TrustRelationship getTrustRelationshipByInum(String inum) {
        TrustRelationship result = null;
        try {
            result = persistenceEntryManager.find(TrustRelationship.class, getDnForTrustRelationship(inum));
        } catch (Exception ex) {
            logger.error("Failed to load TrustRelationship entry", ex);
        }
        return result;
    }


    public List<TrustRelationship> getAllTrustRelationshipByDisplayName(String name) {
        logger.info("Search TrustRelationship with name:{}", name);

        String[] targetArray = new String[] { name };
        Filter displayNameFilter = Filter.createEqualityFilter(AttributeConstants.DISPLAY_NAME, targetArray);
        logger.debug("Search TrustRelationship with displayNameFilter:{}", displayNameFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class,
                displayNameFilter);
    }
    
    
    public List<TrustRelationship> searchTrustRelationship(String pattern, int sizeLimit) {

        logger.debug("Search TrustRelationship with pattern:{}, sizeLimit:{}", pattern, sizeLimit);

        String[] targetArray = new String[] { pattern };
        Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null, targetArray,
                null);
        Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null, targetArray,
                null);
        Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
        Filter searchFilter = Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter);

        logger.debug("Search TrustRelationship with searchFilter:{}", searchFilter);
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, searchFilter, sizeLimit);
    }

    public List<TrustRelationship> getAllTrustRelationship(int sizeLimit) {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null, sizeLimit);
    }

    public List<TrustRelationship> getAllTrustRelationship() {
        return persistenceEntryManager.findEntries(getDnForTrustRelationship(null), TrustRelationship.class, null);
    }

    public PagedResult<TrustRelationship> getTrustRelationship(SearchRequest searchRequest) {
        logger.debug("Search TrustRelationship with searchRequest:{}", searchRequest);

        Filter searchFilter = null;
        List<Filter> filters = new ArrayList<>();
        if (searchRequest.getFilterAssertionValue() != null && !searchRequest.getFilterAssertionValue().isEmpty()) {

            for (String assertionValue : searchRequest.getFilterAssertionValue()) {
                String[] targetArray = new String[] { assertionValue };
                Filter displayNameFilter = Filter.createSubstringFilter(AttributeConstants.DISPLAY_NAME, null,
                        targetArray, null);
                Filter descriptionFilter = Filter.createSubstringFilter(AttributeConstants.DESCRIPTION, null,
                        targetArray, null);
                Filter inumFilter = Filter.createSubstringFilter(AttributeConstants.INUM, null, targetArray, null);
                filters.add(Filter.createORFilter(displayNameFilter, descriptionFilter, inumFilter));
            }
            searchFilter = Filter.createORFilter(filters);
        }

        logger.trace("TrustRelationship pattern searchFilter:{}", searchFilter);
        List<Filter> fieldValueFilters = new ArrayList<>();
        if (searchRequest.getFieldValueMap() != null && !searchRequest.getFieldValueMap().isEmpty()) {
            for (Map.Entry<String, String> entry : searchRequest.getFieldValueMap().entrySet()) {
                Filter dataFilter = Filter.createEqualityFilter(entry.getKey(), entry.getValue());
                logger.trace("TrustRelationship dataFilter:{}", dataFilter);
                fieldValueFilters.add(Filter.createANDFilter(dataFilter));
            }
            searchFilter = Filter.createANDFilter(Filter.createORFilter(filters),
                    Filter.createANDFilter(fieldValueFilters));
        }

        logger.debug("TrustRelationship searchFilter:{}", searchFilter);

        return persistenceEntryManager.findPagedEntries(getDnForTrustRelationship(null), TrustRelationship.class, searchFilter, null,
                searchRequest.getSortBy(), SortOrder.getByValue(searchRequest.getSortOrder()),
                searchRequest.getStartIndex(), searchRequest.getCount(), searchRequest.getMaxCount());

    }
    
    public int getRecordMaxCount() {
        logger.trace(" MaxCount details - ApiAppConfiguration.MaxCount():{}, DEFAULT_MAX_COUNT:{} ",
                configurationFactory.getApiAppConfiguration().getMaxCount(), ApiConstants.DEFAULT_MAX_COUNT);
        return (configurationFactory.getApiAppConfiguration().getMaxCount() > 0
                ? configurationFactory.getApiAppConfiguration().getMaxCount()
                : ApiConstants.DEFAULT_MAX_COUNT);
    }


    
    public TrustRelationship addTrustRelationship(TrustRelationship trustRelationship, InputStream file)
            throws IOException {
        logger.info("Add new trustRelationship:{}, file:{}", trustRelationship, file);

        setTrustRelationshipDefaultValue(trustRelationship, false);
        persistenceEntryManager.persist(trustRelationship);

        if (file != null && file.available() > 0) {
            saveSpMetaDataFileSourceTypeFile(trustRelationship, file);
        }else {
            //trustRelationship.setSpMetaDataFN(null);
        }
        
        persistenceEntryManager.merge(trustRelationship);
        logger.info("After saving new trustRelationship:{}", trustRelationship);
        return getTrustRelationshipByInum(trustRelationship.getInum());
    }

    private TrustRelationship setTrustRelationshipDefaultValue(TrustRelationship trustRelationship, boolean update) {
        logger.debug("trustRelationship:{}, update:{}",trustRelationship, update);
        return trustRelationship;
    }
    
    private boolean saveSpMetaDataFileSourceTypeFile(TrustRelationship trustRelationship, InputStream file) {

        log.debug("saveSpMetadataFileSourceTypeFile(). trustRelationship: {} . file: {}",trustRelationship,file);
    
        final String spMetadataFileName = getSpNewMetadataFileName(trustRelationship);
        trustRelationship.setSpMetaDataFN(spMetadataFileName);
        InputStream targetStream = file;
        final String metadataFilePath  = samlIdpService.saveMetadataFile(
                samlConfigService.getSpMetadataDir(),spMetadataFileName,Constants.SP_MODULE,targetStream);
        log.debug("targetStream: {}, spMetadataDir: {}, spMetadataFileName: {}",targetStream,samlConfigService.getSpMetadataDir(),spMetadataFileName);
        if(StringHelper.isNotEmpty(metadataFilePath)) {
            trustRelationship.setSpMetaDataFN(metadataFilePath);
            log.debug("SP Metadata file ' {} ' saved.",spMetadataFileName);
            return true;
        }else {
            log.error("Failed to save SP metadata file for TrustRelationship ' {} '",trustRelationship.getInum());
            return false;
        }
    }
    
    public String getSpNewMetadataFileName(TrustRelationship trustRel) {
        return getSpNewMetadataFileName(trustRel.getInum());
    }

    public String getSpNewMetadataFileName(String inum) {
        logger.info("Generate SP Metadata FileName with inum:{}",inum);
        String relationshipInum = StringHelper.removePunctuation(inum);
        logger.info("inum after remove punctuation is:{}",relationshipInum);
        
        //TO-DO LAter
        //return String.format(samlConfigService.getSpMetadataFilePattern(), relationshipInum);
    }
    
    //----------------

    public List<TrustedServiceProvider> getTrustedServiceProviders() {
        logger.debug("Fetching trusted service providers");
        
        ShibbolethIdpConfiguration config = getConfiguration();
        if (config != null && config.getShibbolethIdpProperties() != null) {
            return config.getShibbolethIdpProperties().getTrustedServiceProviders();
        }
        return new ArrayList<>();
    }

    public TrustedServiceProvider getTrustedServiceProvider(String entityId) {
        logger.debug("Fetching trusted service provider: {}", entityId);
        
        List<TrustedServiceProvider> providers = getTrustedServiceProviders();
        for (TrustedServiceProvider provider : providers) {
            if (provider.getEntityId().equals(entityId)) {
                return provider;
            }
        }
        return null;
    }

    public void addTrustedServiceProvider(TrustedServiceProvider serviceProvider) {
        logger.info("Adding trusted service provider: {}", serviceProvider.getEntityId());
        
        ShibbolethIdpConfiguration config = getConfiguration();
        List<TrustedServiceProvider> providers = config.getShibbolethIdpProperties().getTrustedServiceProviders();
        if (providers == null) {
            providers = new ArrayList<>();
            config.getShibbolethIdpProperties().setTrustedServiceProviders(providers);
        }
        providers.add(serviceProvider);
        updateConfiguration(config);
    }

    public void updateTrustedServiceProvider(TrustedServiceProvider serviceProvider) {
        logger.info("Updating trusted service provider: {}", serviceProvider.getEntityId());
        
        ShibbolethIdpConfiguration config = getConfiguration();
        List<TrustedServiceProvider> providers = config.getShibbolethIdpProperties().getTrustedServiceProviders();
        
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getEntityId().equals(serviceProvider.getEntityId())) {
                providers.set(i, serviceProvider);
                break;
            }
        }
        updateConfiguration(config);
    }

    public void deleteTrustedServiceProvider(String entityId) {
        logger.info("Deleting trusted service provider: {}", entityId);
        
        ShibbolethIdpConfiguration config = getConfiguration();
        List<TrustedServiceProvider> providers = config.getShibbolethIdpProperties().getTrustedServiceProviders();
        providers.removeIf(p -> p.getEntityId().equals(entityId));
        updateConfiguration(config);
    }

    private ShibbolethIdpConfiguration createDefaultConfiguration() {
        ShibbolethIdpConfiguration config = new ShibbolethIdpConfiguration();
        config.setDn(SHIBBOLETH_IDP_CONFIG_DN);
        config.setRevision(1);
        
        ShibbolethIdpConfigurationProperties props = new ShibbolethIdpConfigurationProperties();
        props.setEnabled(false);
        props.setTrustedServiceProviders(new ArrayList<>());
        props.setMetadataProviders(new ArrayList<>());
        
        config.setShibbolethIdpProperties(props);
        return config;
    }
}
