/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.remotefetch.core.ui.client;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.remotefetch.common.BasicRemoteFetchConfiguration;
import org.wso2.carbon.identity.remotefetch.common.RemoteFetchComponentRegistry;
import org.wso2.carbon.identity.remotefetch.common.RemoteFetchConfiguration;
import org.wso2.carbon.identity.remotefetch.common.ValidationReport;
import org.wso2.carbon.identity.remotefetch.common.actionlistener.ActionListenerComponent;
import org.wso2.carbon.identity.remotefetch.common.configdeployer.ConfigDeployerComponent;
import org.wso2.carbon.identity.remotefetch.common.exceptions.RemoteFetchCoreException;
import org.wso2.carbon.identity.remotefetch.common.repomanager.RepositoryManagerComponent;
import org.wso2.carbon.identity.remotefetch.core.ui.dto.RemoteFetchConfigurationRowDTO;
import org.wso2.carbon.identity.remotefetch.core.ui.internal.RemotefetchCoreUIComponentDataHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds client function for remote fetch configuration.
 */
public class RemoteFetchConfigurationClient {

    private static final Log log = LogFactory.getLog(RemoteFetchConfigurationClient.class);

    public static List<RemoteFetchConfigurationRowDTO> getConfigurations() throws RemoteFetchCoreException {

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        List<BasicRemoteFetchConfiguration> fetchConfigurations = RemotefetchCoreUIComponentDataHolder
                .getInstance().getRemoteFetchConfigurationService().getBasicRemoteFetchConfigurationList(tenantId);

        return fetchConfigurations.stream().map((basicFetchConfiguration ->
                RemoteFetchConfigurationClient.fetchConfigurationToDTO(basicFetchConfiguration)
        )).collect(Collectors.toList());
    }

    public static RemoteFetchConfigurationRowDTO fetchConfigurationToDTO(
            BasicRemoteFetchConfiguration fetchConfiguration) {

        RemoteFetchComponentRegistry componentRegistry = RemotefetchCoreUIComponentDataHolder.getInstance().
                getComponentRegistry();
        if (componentRegistry == null) {
            log.error("RemoteFetchComponentRegistry is not initialized properly");
            return null;
        }

        RepositoryManagerComponent repositoryManagerComponent = componentRegistry.getRepositoryManagerComponent(
                fetchConfiguration.getRepositoryManagerType());
        if (repositoryManagerComponent == null) {
            log.error("RepositoryManagerComponent is not initialized properly");
        }

        ActionListenerComponent actionListenerComponent = componentRegistry.getActionListenerComponent(
                fetchConfiguration.getActionListenerType());

        if (actionListenerComponent == null) {
            log.error("ActionListenerComponent is not initialized properly");
        }

        ConfigDeployerComponent configDeployerComponent = componentRegistry.
                getConfigDeployerComponent(fetchConfiguration.getConfigurationDeployerType());
        if (configDeployerComponent == null) {
            log.error("ConfigDeployerComponent is not initialized properly");
        }

        return new RemoteFetchConfigurationRowDTO(
                fetchConfiguration.getId(),
                fetchConfiguration.isEnabled(),
                repositoryManagerComponent == null ? "" : repositoryManagerComponent.getName(),
                actionListenerComponent == null ? "" : actionListenerComponent.getName(),
                configDeployerComponent == null ? "" : configDeployerComponent.getName(),
                fetchConfiguration.getRemoteFetchName(),
                fetchConfiguration.getSuccessfulDeployments(),
                fetchConfiguration.getFailedDeployments(),
                fetchConfiguration.getLastDeployed()
        );
    }

    public static RemoteFetchConfiguration getRemoteFetchConfiguration(int id) throws RemoteFetchCoreException {

        return RemotefetchCoreUIComponentDataHolder.getInstance().getRemoteFetchConfigurationService()
                .getRemoteFetchConfiguration(id);
    }

    public static ValidationReport addFetchConfiguration(String jsonObject, String currentUser)
            throws RemoteFetchCoreException {

        RemoteFetchConfiguration fetchConfiguration =
                RemoteFetchConfigurationClient.parseJsonToConfiguration(jsonObject);

        fetchConfiguration.setUserName(currentUser);

        return RemotefetchCoreUIComponentDataHolder.getInstance()
                .getRemoteFetchConfigurationService()
                .addRemoteFetchConfiguration(fetchConfiguration);
    }

    public static ValidationReport updateFetchConfiguration(String jsonObject, String currentUser)
            throws RemoteFetchCoreException {

        RemoteFetchConfiguration fetchConfiguration =
                RemoteFetchConfigurationClient.parseJsonToConfiguration(jsonObject);

        fetchConfiguration.setUserName(currentUser);

        return RemotefetchCoreUIComponentDataHolder.getInstance()
                .getRemoteFetchConfigurationService()
                .updateRemoteFetchConfiguration(fetchConfiguration);
    }

    public static void deleteRemoteFetchComponent(int id) throws RemoteFetchCoreException {

        RemotefetchCoreUIComponentDataHolder.getInstance().getRemoteFetchConfigurationService()
                .deleteRemoteFetchConfiguration(id);
    }

    private static RemoteFetchConfiguration parseJsonToConfiguration(String jsonObject) {

        Gson gson = new Gson();
        RemoteFetchConfiguration fetchConfiguration = gson.fromJson(jsonObject, RemoteFetchConfiguration.class);
        fetchConfiguration.setTenantId(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        return fetchConfiguration;
    }

}
