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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.remotefetch.core.dao.impl;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.identity.remotefetch.common.DeploymentRevision;
import org.wso2.carbon.identity.remotefetch.common.exceptions.RemoteFetchCoreException;
import org.wso2.carbon.identity.remotefetch.core.dao.TestConstants;
import org.wso2.carbon.identity.remotefetch.core.util.JdbcUtils;

import java.io.File;
import java.sql.Connection;

import java.util.List;

import javax.sql.DataSource;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.wso2.carbon.identity.remotefetch.core.dao.TestConstants.DB_NAME;
import static org.wso2.carbon.identity.remotefetch.core.dao.TestConstants.DEPLOYMENT_REVISION_ID;
import static org.wso2.carbon.identity.remotefetch.core.dao.TestConstants.REMOTE_FETCH_CONFIGURATION_ID;

/**
 * Unit test covering DeploymentRevisionDAOImpl.
 */
@PrepareForTest(JdbcUtils.class)
public class DeploymentRevisionDAOImplTest extends PowerMockTestCase {

    DeploymentRevisionDAOImpl deploymentRevisionDAO = new DeploymentRevisionDAOImpl();
    DeploymentRevision deploymentRevision = new DeploymentRevision(REMOTE_FETCH_CONFIGURATION_ID, null);

    @BeforeClass
    public void setUp() throws Exception {

        DAOTestUtils.initiateH2Base(DB_NAME, DAOTestUtils.getFilePath("permission.sql"));
        DAOTestUtils.createFetchConfig(DB_NAME, REMOTE_FETCH_CONFIGURATION_ID, TestConstants.TENANT_ID, true,
                TestConstants.REPO_MANAGER_TYPE, TestConstants.ACTION_LISTENER_TYPE, TestConstants.CONFIG_DEPLOYER_TYPE
                , TestConstants.getAttributesJson(), "RemoteFetchTest",
                "https://github.com/IS/Test2.git/tree/master/sp");
    }

    @AfterClass
    public void tearDown() throws Exception {

        DAOTestUtils.closeH2Base(DB_NAME);
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {

        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }

    @Test
    public void testCreateDeploymentRevision() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewTemplate()).thenReturn(new JdbcTemplate(dataSource));
        try (Connection connection = DAOTestUtils.getConnection(DB_NAME)) {
            Connection spy = DAOTestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            deploymentRevisionDAO.createDeploymentRevision(createRevision());
        }
    }

    @Test(priority = 1)
    public void testUpdateDeploymentRevision() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewTemplate()).thenReturn(new JdbcTemplate(dataSource));
        try (Connection connection = DAOTestUtils.getConnection(DB_NAME)) {
            Connection spy = DAOTestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            deploymentRevisionDAO.updateDeploymentRevision(updateRevision());
        }
    }

    @Test(expectedExceptions = RemoteFetchCoreException.class, priority = 2)
    public void testUpdateDeploymentRevisionForInvalidId() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewTemplate()).thenReturn(new JdbcTemplate(dataSource));
        try (Connection connection = DAOTestUtils.getConnection(DB_NAME)) {
            Connection spy = DAOTestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            deploymentRevisionDAO.updateDeploymentRevision(updateRevisionForInvalidId());
        }
    }

    @Test(priority = 3)
    public void testGetDeploymentRevisionsByConfigurationId() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewTemplate()).thenReturn(new JdbcTemplate(dataSource));
        try (Connection connection = DAOTestUtils.getConnection(DB_NAME)) {
            Connection spy = DAOTestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            List<DeploymentRevision> deploymentRevisionList =
                    deploymentRevisionDAO.getDeploymentRevisionsByConfigurationId(REMOTE_FETCH_CONFIGURATION_ID);
            assertNotNull(deploymentRevisionList);
            assertEquals(deploymentRevisionList.size(), 1);
        }
    }

    @Test(priority = 4)
    public void testGetDeploymentRevisionsByConfigurationId1() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewTemplate()).thenReturn(new JdbcTemplate(dataSource));
        try (Connection connection = DAOTestUtils.getConnection(DB_NAME)) {
            Connection spy = DAOTestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            DeploymentRevision deploymentRevisionNew =
                    deploymentRevisionDAO.getDeploymentRevision(REMOTE_FETCH_CONFIGURATION_ID, "NewDemoApp");
            assertNotNull(deploymentRevisionNew);
            assertEquals(deploymentRevisionNew.getDeploymentStatus(), DeploymentRevision.DeploymentStatus.SUCCESS);

        }
    }

    @Test(priority = 5)
    public void testDeleteDeploymentRevision() throws Exception {

        DataSource dataSource = mock(DataSource.class);
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewTemplate()).thenReturn(new JdbcTemplate(dataSource));
        try (Connection connection = DAOTestUtils.getConnection(DB_NAME)) {
            Connection spy = DAOTestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spy);
            deploymentRevisionDAO.deleteDeploymentRevision(REMOTE_FETCH_CONFIGURATION_ID);
        }
    }

    private DeploymentRevision createRevision() {

        long millis = System.currentTimeMillis();
        java.sql.Date date = new java.sql.Date(millis);

        deploymentRevision.setDeploymentRevisionId(DEPLOYMENT_REVISION_ID);
        deploymentRevision.setItemName("NewDemoApp");
        deploymentRevision.setFileHash("12345678");
        deploymentRevision.setFile(new File("sp/newFile.xml"));
        deploymentRevision.setConfigId(REMOTE_FETCH_CONFIGURATION_ID);
        deploymentRevision.setDeployedDate(date);
        deploymentRevision.setDeploymentStatus(DeploymentRevision.DeploymentStatus.SUCCESS);
        deploymentRevision.setErrorMessage("Test Error Message");
        deploymentRevision.setLastSynchronizedDate(date);
        return deploymentRevision;
    }

    private DeploymentRevision updateRevision() {

        deploymentRevision.setFileHash("1234567890");
        return deploymentRevision;
    }

    private DeploymentRevision updateRevisionForInvalidId() {

        deploymentRevision.setConfigId("1212121-0000-0000-0000-d29bed62f7bd");
        deploymentRevision.setFileHash("1234567");
        return deploymentRevision;
    }
}
