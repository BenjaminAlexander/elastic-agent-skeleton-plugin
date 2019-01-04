/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.elasticagent;

import com.example.elasticagent.models.JobIdentifier;
import com.example.elasticagent.requests.CreateAgentRequest;
import com.thoughtworks.go.plugin.api.logging.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.ResourceType;
import software.amazon.awssdk.services.ec2.model.RunInstancesMonitoringEnabled;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagSpecification;
import org.joda.time.DateTime;
import org.joda.time.Instant;
import java.util.Base64;
import java.util.function.BiFunction;

public class ExampleInstance {

    private Instance instance;
    
    public ExampleInstance(Instance instance) {
        this.instance = instance;
    }

    public String name() {
        return instance.instanceId();
    }

    public DateTime createdAt() {
    	Instant launchInstant = Instant.ofEpochSecond(instance.launchTime().getEpochSecond());
        return new DateTime(launchInstant);
    }
    
    public String getTagValue(String key)
    {
    	for(Tag tag : instance.tags())
    	{
    		if(tag.key() == key)
    			return tag.value();
    	}
    	return null;
    }

    public String environment() {
        return getTagValue(CreateAgentRequest.AGENT_AUTO_REGISTER_ENVIRONMENTS);
    }

    public JobIdentifier jobIdentifier() {
        return new JobIdentifier(instance.tags());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExampleInstance that = (ExampleInstance) o;

        return instance != null ? instance.equals(that.instance) : that.instance == null;
    }

    @Override
    public int hashCode() {
        return instance != null ? instance.hashCode() : 0;
    }
    
	@FunctionalInterface
	public interface CommandDefinition 
	{ 
		Builder apply(Builder builder, String value); 
	}
	
	@FunctionalInterface
	public interface Command 
	{ 
		Builder apply(Builder builder); 
	}
    
    public static class Builder {
    	//TODO: the ec2 object should probably be a singleton
    	private static Ec2Client ec2 = null;
    	private static final Logger LOG = Logger.getLoggerFor(Builder.class);
    	
    	private static Ec2Client getEc2()
    	{
    		if(ec2 == null) {
    			ec2 = Ec2Client.create();
    		}
    		return ec2;
    	}
    	
    	private RunInstancesRequest.Builder runInstancesRequestBuilder;
    	private TagSpecification.Builder instanceTagSpecificationBuilder;
    	private CreateAgentRequest request = null;
    	private PluginSettings settings = null;
    	
    	public Builder()
    	{    		
    		runInstancesRequestBuilder = RunInstancesRequest.builder();
    		instanceTagSpecificationBuilder = TagSpecification.builder().resourceType(ResourceType.INSTANCE);
    	}
    	
    	public Builder createAgentRequest(CreateAgentRequest request)
    	{
    		this.request = request;
    		return this;
    	}
    	
    	public Builder pluginSettings(PluginSettings settings)
    	{
    		this.settings = settings;
    		return this;
    	}
    	
    	public RunInstancesRequest.Builder getRunInstancesRequestBuilder()
    	{
    		return runInstancesRequestBuilder;
    	}
    	
    	private String getUserData() throws Exception
    	{
    		if(request == null)
    			throw new Exception("Must provide a CreateAgentRequest to the builder.");
    		if(settings == null)
    			throw new Exception("Must provide a PluginSettings to the builder.");
    		
    		String userData = "";
    		BiFunction<String, String, String> addLine = (String string, String line) -> { return string + line + "\r\n"; };
    		
        	userData = addLine.apply(userData, "<powershell>");
        	userData = addLine.apply(userData, "mkdir \"C:\\Program Files (x86)\\Go Agent\\config\"");
        	userData = addLine.apply(userData, "$instanceId = (Invoke-WebRequest http://169.254.169.254/latest/meta-data/instance-id).Content");
        	userData = addLine.apply(userData, "$hostName = (Invoke-WebRequest http://169.254.169.254/latest/meta-data/public-hostname).Content");	
        	userData = addLine.apply(userData, "$UserInfoToFile = @\"");
        	userData = addLine.apply(userData, request.autoregisterPropertiesAsString("$instanceId", "$hostName"));
        	userData = addLine.apply(userData, "\"@");
        	userData = addLine.apply(userData, "$UserInfoToFile | Out-File -FilePath \"C:\\Program Files (x86)\\Go Agent\\config\\autoregister.properties\" -Encoding ASCII");
        	userData = addLine.apply(userData, "Invoke-WebRequest -OutFile C:\\Users\\Administrator\\Downloads\\go-agent-18.11.0-8024-jre-64bit-setup.exe https://download.gocd.org/binaries/18.11.0-8024/win/go-agent-18.11.0-8024-jre-64bit-setup.exe");
        	userData = addLine.apply(userData, "C:\\Users\\Administrator\\Downloads\\go-agent-18.11.0-8024-jre-64bit-setup.exe /S /START_AGENT=YES /SERVERURL=`\"" + settings.getGoServerUrl() + "`\"");  //TODO: set server URL correctly
        	userData = addLine.apply(userData, "</powershell>");
        	userData = Base64.getEncoder().encodeToString(userData.getBytes());
    		return userData;
    	}
    	
    	public ExampleInstance build() throws Exception 
    	{
        	for(Command command : request.getPropertyCommands())
    		{
    			command.apply(this);
    		}
        	
        	this.instanceTagSpecificationBuilder.tags(request.getTagsForInstance());
    		
    		RunInstancesRequest runInstancesRequest = runInstancesRequestBuilder
    				.tagSpecifications(instanceTagSpecificationBuilder.build())
        			.monitoring(RunInstancesMonitoringEnabled.builder().enabled(false).build())
        			.userData(this.getUserData())
        			.minCount(1)
        			.maxCount(1)
        			.build();
    		
    		LOG.info("Sending RunInstancesRequest to AWS");
    		RunInstancesResponse response = getEc2().runInstances(runInstancesRequest);
    		
    		LOG.info(Integer.toString(response.instances().size()) + "ec2 instances created");
    		for (Instance instance : response.instances())
        	{
        		LOG.info("Instance id: " + instance.instanceId());
        	}
    		
    		return new ExampleInstance(response.instances().get(0));
    	}
    }
}
