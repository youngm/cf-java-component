/*
 *   Copyright (c) 2013 Intellectual Reserve, Inc.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package cf.service;

import org.codehaus.jackson.annotate.JsonProperty;

import cf.common.JsonObject;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class BindRequest extends JsonObject {

	private final String serviceInstanceId;
	private final String label;
	private final String email;
	// Ignore bind_options field, it is always empty in v2

	public BindRequest(
			@JsonProperty("service_id") String serviceInstanceId,
			@JsonProperty("label") String label,
			@JsonProperty("email") String email) {
		this.serviceInstanceId = serviceInstanceId;
		this.label = label;
		this.email = email;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public String getLabel() {
		return label;
	}

	public String getEmail() {
		return email;
	}
}
