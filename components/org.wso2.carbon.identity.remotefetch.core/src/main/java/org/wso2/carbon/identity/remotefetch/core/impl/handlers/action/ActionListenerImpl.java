/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.remotefetch.core.impl.handlers.action;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.remotefetch.common.ConfigurationFileStream;
import org.wso2.carbon.identity.remotefetch.common.DeploymentRevision;
import org.wso2.carbon.identity.remotefetch.common.actionlistener.ActionListener;
import org.wso2.carbon.identity.remotefetch.common.configdeployer.ConfigDeployer;
import org.wso2.carbon.identity.remotefetch.common.exceptions.RemoteFetchCoreException;
import org.wso2.carbon.identity.remotefetch.common.repomanager.RepositoryManager;
import org.wso2.carbon.identity.remotefetch.core.dao.DeploymentRevisionDAO;
import org.wso2.carbon.identity.remotefetch.core.dao.impl.DeploymentRevisionDAOImpl;
import org.wso2.carbon.identity.remotefetch.core.impl.deployers.config.VelocityTemplatedSPDeployer;
import org.wso2.carbon.identity.remotefetch.core.util.RemoteFetchConfigurationUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.identity.remotefetch.core.util.RemoteFetchConfigurationUtils.generateUniqueID;

/**
 * Action Listener parent class to create, manage deploy revisions.
 */
public class ActionListenerImpl implements ActionListener {

    private static final Log log = LogFactory.getLog(ActionListenerImpl.class);

    private RepositoryManager repo;
    protected Date lastIteration;
    private ConfigDeployer configDeployer;
    private DeploymentRevisionDAO deploymentRevisionDAO;
    private Map<String, DeploymentRevision> deploymentRevisionMapNotResolved = new HashMap<>();
    private Map<String, DeploymentRevision> deploymentRevisionMap = new HashMap<>();
    private String remoteFetchConfigurationId;
    private int tenantId;

    public ActionListenerImpl(RepositoryManager repo, ConfigDeployer configDeployer,
                              String remoteFetchConfigurationId, int tenantId) {

        this.repo = repo;
        this.configDeployer = configDeployer;
        this.remoteFetchConfigurationId = remoteFetchConfigurationId;
        this.tenantId = tenantId;
        this.deploymentRevisionDAO = new DeploymentRevisionDAOImpl();
        this.seedRevisions();
    }

    /**
     * Contains logic to listen for updates.
     */
    @Override
    public void execute() {

        try {
            this.repo.fetchRepository();
        } catch (RemoteFetchCoreException e) {
            log.error("Error pulling repository", e);
        }
        this.lastIteration = new Date();
        this.pollDirectory(this.configDeployer);
    }

    /**
     * Seed local map with existing DeploymentRevisions from database.
     */
    private void seedRevisions() {

        try {
            List<DeploymentRevision> deploymentRevisions = this.deploymentRevisionDAO
                    .getDeploymentRevisionsByConfigurationId(this.remoteFetchConfigurationId);

            for (DeploymentRevision deploymentRevision : deploymentRevisions) {
                this.deploymentRevisionMap.put(deploymentRevision.getItemName(), deploymentRevision);
            }

        } catch (RemoteFetchCoreException e) {
            log.info("Unable to seed DeploymentRevisions for RemoteFetchConfiguration id " +
                    this.remoteFetchConfigurationId, e);
        }
    }

    /**
     * Resolve and create / update list of revisions.
     *
     * @param configPaths list of file paths to be deployed
     */
    private void manageRevisions(List<File> configPaths) {

        configPaths.forEach((File configPath) -> {

            String resolvedName = "";
            String fileName = FilenameUtils.removeExtension(configPath.getName());

            try {
                resolvedName = this.configDeployer.resolveConfigName(this.repo.getFile(configPath));
            } catch (RemoteFetchCoreException e) {
                log.error("Unable to resolve configuration name for file " + configPath.getAbsolutePath(), e);

                if (deploymentRevisionMap.containsKey(fileName)) {
                    this.deploymentRevisionMapNotResolved.put(fileName, deploymentRevisionMap.remove(fileName));
                }
                if (this.deploymentRevisionMapNotResolved.containsKey(fileName)) {
                    updateRevisionNotResolved(fileName, configPath, e);
                } else {
                    createRevisionNotResolved(fileName, configPath, e);
                }
            }

            if (!(resolvedName.isEmpty())) {
                if (this.deploymentRevisionMapNotResolved.containsKey(fileName)) {
                    this.deploymentRevisionMap.put(resolvedName, deploymentRevisionMapNotResolved.remove(fileName));
                }
                if (this.deploymentRevisionMap.containsKey(resolvedName)) {
                    updateRevision(resolvedName, configPath);
                } else {
                    createRevision(resolvedName, configPath);
                }
            }
        });
    }

    /**
     * Update revision if error occurred while resolving the application name.
     * @param fileName File name.
     * @param configPath Service provider file.
     * @param exception RemoteFetchCoreException
     */
    private void updateRevisionNotResolved(String fileName, File configPath, RemoteFetchCoreException exception) {

        StringBuilder exceptionStringBuilder = new StringBuilder();

        exceptionStringBuilder.append("Unable to resolve configuration name for file ").append(fileName);
        exceptionStringBuilder.append(exception.getMessage());

        DeploymentRevision currentDeploymentRevision = this.deploymentRevisionMapNotResolved.get(fileName);
        currentDeploymentRevision.setErrorMessage(
                RemoteFetchConfigurationUtils.trimErrorMessage(exceptionStringBuilder.toString(),
                        exception));
        currentDeploymentRevision.setDeploymentStatus(DeploymentRevision.DeploymentStatus.FAIL);
        currentDeploymentRevision.setDeployedDate(new Date());
        currentDeploymentRevision.setLastSynchronizedDate(this.lastIteration);
        if (!currentDeploymentRevision.getFile().equals(configPath)) {
            currentDeploymentRevision.setFile(configPath);
        }
        try {
            this.deploymentRevisionDAO.updateDeploymentRevision(currentDeploymentRevision);
        } catch (RemoteFetchCoreException e) {
            log.error("Unable to update DeploymentRevision for " + sanitize(fileName), e);
        }

    }

    /**
     * Create revision if error occurred while resolving the application name.
     * @param fileName Filename.
     * @param configPath Service Provider Path.
     * @param exception RemoteFetchCoreException
     */
    private void createRevisionNotResolved(String fileName, File configPath,
                                           RemoteFetchCoreException exception) {

        StringBuilder exceptionStringBuilder = new StringBuilder();

        exceptionStringBuilder.append("Unable to resolve configuration name for file ").append(fileName);
        exceptionStringBuilder.append(exception.getMessage());

        try {
            String deploymentRevisionId = generateUniqueID();
            if (log.isDebugEnabled()) {
                log.debug("Deployment Revision ID is  generated: " + deploymentRevisionId);
            }
            DeploymentRevision deploymentRevision = new DeploymentRevision(this.remoteFetchConfigurationId, configPath);
            deploymentRevision.setFileHash("");
            deploymentRevision.setItemName(fileName);
            deploymentRevision.setDeploymentRevisionId(deploymentRevisionId);
            deploymentRevision.setDeploymentStatus(DeploymentRevision.DeploymentStatus.FAIL);
            deploymentRevision.setErrorMessage(
                    RemoteFetchConfigurationUtils.trimErrorMessage(exceptionStringBuilder.toString(),
                            exception));
            deploymentRevision.setDeployedDate(new Date());
            deploymentRevision.setLastSynchronizedDate(this.lastIteration);
            this.deploymentRevisionDAO.createDeploymentRevision(deploymentRevision);
            this.deploymentRevisionMapNotResolved.put(deploymentRevision.getItemName(), deploymentRevision);
        } catch (RemoteFetchCoreException e) {
            log.error("Unable to add a new DeploymentRevision for " + sanitize(fileName), e);
        }
    }

    /**
     * Update DeploymentRevision if file was moved/renamed and store.
     *
     * @param resolvedName Application name
     * @param configPath Application config file path
     */
    private void updateRevision(String resolvedName, File configPath) {

        DeploymentRevision currentDeploymentRevision = this.deploymentRevisionMap.get(resolvedName);
        if (!currentDeploymentRevision.getFile().equals(configPath)) {
            try {
                currentDeploymentRevision.setFile(configPath);
                this.deploymentRevisionDAO.updateDeploymentRevision(currentDeploymentRevision);
            } catch (RemoteFetchCoreException e) {
                log.error("Unable to update DeploymentRevision for " + sanitize(resolvedName), e);
            }
        }
    }

    /**
     * Create DeploymentRevision and store for resolved name and path.
     *
     * @param resolvedName Application name
     * @param configPath Application config file path
     */
    private void createRevision(String resolvedName, File configPath) {

        try {
            String deploymentRevisionId = generateUniqueID();
            if (log.isDebugEnabled()) {
                log.debug("Deployment Revision ID is  generated: " + deploymentRevisionId);
            }
            DeploymentRevision deploymentRevision = new DeploymentRevision(this.remoteFetchConfigurationId, configPath);
            deploymentRevision.setFileHash("");
            deploymentRevision.setItemName(resolvedName);
            deploymentRevision.setDeploymentRevisionId(deploymentRevisionId);
            this.deploymentRevisionDAO.createDeploymentRevision(deploymentRevision);
            this.deploymentRevisionMap.put(deploymentRevision.getItemName(), deploymentRevision);
        } catch (RemoteFetchCoreException e) {
            log.error("Unable to add a new DeploymentRevision for " + sanitize(resolvedName), e);
        }
    }

    /**
     * Poll directory for new files.
     *
     * @param deployer ConfigDeployer
     */
    private void pollDirectory(ConfigDeployer deployer) {

        List<File> configFiles = null;

        try {
            configFiles = this.repo.listFiles();
        } catch (RemoteFetchCoreException e) {
            log.error("Error listing files from repository", e);
            return;
        }

        this.manageRevisions(configFiles);

        for (DeploymentRevision deploymentRevision : this.deploymentRevisionMap.values()) {

            String newHash = "";
            try {
                newHash = this.repo.getRevisionHash(deploymentRevision.getFile());
            } catch (RemoteFetchCoreException e) {
                log.error("Unable to get new hash for " + sanitize(deploymentRevision.getItemName()), e);
            }

            // Deploy if new file or updated file
            if (this.isDeploymentRevisionChanged(deploymentRevision, newHash)) {

                try {
                    deploymentRevision.setFileHash(newHash);

                    ConfigurationFileStream configurationFileStream = repo.getFile(deploymentRevision.getFile());

                    VelocityTemplatedSPDeployer velocityTemplatedSPDeployer =
                            new VelocityTemplatedSPDeployer(this.tenantId,
                                    this.remoteFetchConfigurationId);
                    velocityTemplatedSPDeployer.deploy(configurationFileStream);

                    deploymentRevision.setDeploymentStatus(DeploymentRevision.DeploymentStatus.SUCCESS);
                    deploymentRevision.setErrorMessage(null);

                } catch (RemoteFetchCoreException | IOException e) {
                    log.error("Error Deploying " + sanitize(deploymentRevision.getFile().getName()), e);
                    deploymentRevision.setDeploymentStatus(DeploymentRevision.DeploymentStatus.FAIL);
                    deploymentRevision.setErrorMessage(RemoteFetchConfigurationUtils.trimErrorMessage(e.getMessage(),
                            e));
                }

                // Set new deployment Date
                deploymentRevision.setDeployedDate(new Date());
                // Set last iteration date as synced Date
                deploymentRevision.setLastSynchronizedDate(this.lastIteration);

                try {
                    this.deploymentRevisionDAO.updateDeploymentRevision(deploymentRevision);
                } catch (RemoteFetchCoreException e) {
                    log.error("Error updating DeploymentRevision for : " + sanitize(deploymentRevision.getItemName())
                            , e);
                }
            } else {
                // Set last iteration date as synced Date
                deploymentRevision.setLastSynchronizedDate(this.lastIteration);

                try {
                    this.deploymentRevisionDAO.updateDeploymentRevision(deploymentRevision);
                } catch (RemoteFetchCoreException e) {
                    log.error("Error updating DeploymentRevision for : " + sanitize(deploymentRevision.getItemName())
                            , e);
                }
            }
        }
    }

    /**
     * Returns if the DeploymentRevision hash changed.
     *
     * @param deploymentRevision DeploymentRevision
     * @param newHash Hashfile of current file
     * @return flag used to point whether file changed or not
     */
    private boolean isDeploymentRevisionChanged(DeploymentRevision deploymentRevision, String newHash) {

        String currentHash = deploymentRevision.getFileHash();
        // Check if previous revision is none which indicates the addition of a new file and if so deploy.
        try {
            return (!newHash.isEmpty() && (currentHash.isEmpty() ||
                    !(MessageDigest.isEqual(newHash.getBytes("UTF-8"), currentHash.getBytes("UTF-8")))));
        } catch (UnsupportedEncodingException e) {
            log.error("Hash cross check failed. Error Deploying " + sanitize(deploymentRevision.getFile().getName())
                    , e);
        }
        return false;
    }

    /**
     * Used to sanitize the logs input.
     * @param input input sentences
     * @return sanitized string
     */
    private String sanitize(String input) {

        if (StringUtils.isBlank(input)) {
            return input;
        }

        return input.replaceAll("(\\r|\\n|%0D|%0A|%0a|%0d)", "");
    }

}
