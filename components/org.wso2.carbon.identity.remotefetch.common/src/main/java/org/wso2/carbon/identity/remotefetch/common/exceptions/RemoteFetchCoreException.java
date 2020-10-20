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

package org.wso2.carbon.identity.remotefetch.common.exceptions;

/**
 * Root Exception to be used for reporting anomalies in the RemoteFetchCore.
 */
public class RemoteFetchCoreException extends Exception {

    private String errorCode;

    /**
     * Core Exception to throw anomalies in RemoteFetchCore.
     * @param message Error message
     */
    public RemoteFetchCoreException(String message) {

        super(message);
    }

    /**
     * Core Exception to throw anomalies in RemoteFetchCore.
     * @param message Error Message
     * @param cause Throwable cause
     */
    public RemoteFetchCoreException(String message, Throwable cause) {

        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified error code and cause.
     *
     * @param errorCode Error code
     * @param message   Detailed message
     */
    public RemoteFetchCoreException(String errorCode, String message) {

        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new exception with the specified error code, message and cause.
     *
     * @param errorCode Error code
     * @param message   Detailed message
     * @param cause     Cause as {@link Throwable}
     */
    public RemoteFetchCoreException(String errorCode, String message, Throwable cause) {

        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Retuns the error code.
     *
     * @return Error code
     */
    public String getErrorCode() {

        return errorCode;
    }

}
