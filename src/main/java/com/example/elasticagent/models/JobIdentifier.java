package com.example.elasticagent.models;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.thoughtworks.go.plugin.api.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import static java.text.MessageFormat.format;

public class JobIdentifier {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
	
    @Expose
    private String pipelineName;

    @Expose
    private Long pipelineCounter;

    @Expose
    private String pipelineLabel;

    @Expose
    private String stageName;

    @Expose
    private String stageCounter;

    @Expose
    private String jobName;

    @Expose
    private Long jobId;

    private String representation;

    public JobIdentifier(Long jobId) {
        this.jobId = jobId;
    }

    public JobIdentifier() {
    }

    public JobIdentifier(String pipelineName, Long pipelineCounter, String pipelineLabel, String stageName, String stageCounter, String jobName, Long jobId) {
        this.pipelineName = pipelineName;
        this.pipelineCounter = pipelineCounter;
        this.pipelineLabel = pipelineLabel;
        this.stageName = stageName;
        this.stageCounter = stageCounter;
        this.jobName = jobName;
        this.jobId = jobId;
    }

    public Long getJobId() {
        return jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public Long getPipelineCounter() {
        return pipelineCounter;
    }

    public String getPipelineLabel() {
        return pipelineLabel;
    }

    public String getStageName() {
        return stageName;
    }

    public String getStageCounter() {
        return stageCounter;
    }

    public String getRepresentation() {
        if (StringUtils.isBlank(representation)) {
            this.representation = format("{0}/{1}/{2}/{3}/{4}", pipelineName, pipelineCounter, stageName, stageCounter, jobName);
        }
        return representation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobIdentifier that = (JobIdentifier) o;
        return Objects.equals(pipelineName, that.pipelineName) &&
                Objects.equals(pipelineCounter, that.pipelineCounter) &&
                Objects.equals(pipelineLabel, that.pipelineLabel) &&
                Objects.equals(stageName, that.stageName) &&
                Objects.equals(stageCounter, that.stageCounter) &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineName, pipelineCounter, pipelineLabel, stageName, stageCounter, jobName, jobId);
    }

    @Override
    public String toString() {
        return "JobIdentifier{" +
                "pipelineName='" + pipelineName + '\'' +
                ", pipelineCounter=" + pipelineCounter +
                ", pipelineLabel='" + pipelineLabel + '\'' +
                ", stageName='" + stageName + '\'' +
                ", stageCounter='" + stageCounter + '\'' +
                ", jobName='" + jobName + '\'' +
                ", jobId=" + jobId +
                '}';
    }

    public String represent() {
        return String.format("%s/%d/%s/%s/%s", pipelineName, pipelineCounter, stageName, stageCounter, jobName);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static JobIdentifier fromJson(String json) {
        return GSON.fromJson(json, JobIdentifier.class);
    }
    
    public Collection<Tag> getTagsForInstance()
    {
    	ArrayList<Tag> tags = new ArrayList<Tag>();
		tags.add(Tag.builder().key("pipelineName").value(GSON.toJson(pipelineName)).build());
		tags.add(Tag.builder().key("pipelineCounter").value(GSON.toJson(pipelineCounter)).build());
		tags.add(Tag.builder().key("pipelineLabel").value(GSON.toJson(pipelineLabel)).build());
		tags.add(Tag.builder().key("stageName").value(GSON.toJson(stageName)).build());
		tags.add(Tag.builder().key("stageCounter").value(GSON.toJson(stageCounter)).build());
		tags.add(Tag.builder().key("jobName").value(GSON.toJson(jobName)).build());
		tags.add(Tag.builder().key("jobId").value(GSON.toJson(jobId)).build());
    	return tags;
    }
}
