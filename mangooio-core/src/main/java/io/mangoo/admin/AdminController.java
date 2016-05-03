package io.mangoo.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.impl.matchers.GroupMatcher;

import io.mangoo.annotations.FilterWith;
import io.mangoo.cache.Cache;
import io.mangoo.core.Application;
import io.mangoo.enums.Default;
import io.mangoo.enums.Key;
import io.mangoo.enums.Template;
import io.mangoo.models.Job;
import io.mangoo.models.Metrics;
import io.mangoo.routing.Response;
import io.mangoo.routing.Router;
import io.mangoo.scheduler.Scheduler;
import io.mangoo.utils.BootstrapUtils;

/**
 * Controller class for administrative URLs
 *
 * @author svenkubiak
 *
 */
@FilterWith(AdminFilter.class)
public class AdminController {
    private static final Logger LOG = LogManager.getLogger(AdminController.class);
    private static final String JOBS = "jobs";
    private static final String STATS = "stats";
    private static final String SCHEDULER = "scheduler";
    private static final String METRICS = "metrics";
    private static final String CONFIGURATION = "configuration";
    private static final String CACHE = "cache";
    private static final String ROUTES = "routes";
    private static final String SPACE = "space";
    private static final String VERSION = "version";
    private static final int MB = 1024*1024;
    private final Map<String, String> properties = new HashMap<>();
    
    public AdminController() {
        System.getProperties().entrySet().forEach(
                entry -> this.properties.put(entry.getKey().toString(), entry.getValue().toString())
        );
    }
    
    public Response index(String space) {
        if (StringUtils.isBlank(space)) {
            return memory();
        } else if (ROUTES.equals(space)) {
            return routes(space);
        } else if (CACHE.equals(space)) {
            return cache(space);
        } else if (CONFIGURATION.equals(space)) {
            return configuration(space);
        } else if (METRICS.equals(space)) {
            return metrics(space);
        } else if (SCHEDULER.equals(space)) {
            return scheduler(space);
        }
        
        return Response.withNotFound().andEmptyBody();
    }
    
    private Response memory() {
        Runtime runtime = Runtime.getRuntime();
        double usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / MB;
        double freeMemory = runtime.freeMemory() / MB;
        double totalMemory = runtime.totalMemory() / MB;
        double maxMemory = runtime.maxMemory() / MB; 
        
        return Response.withOk()
                .andContent(VERSION, BootstrapUtils.getVersion())
                .andContent(SPACE, null)
                .andContent("properties", this.properties)
                .andContent("usedMemory", usedMemory)
                .andContent("freeMemory", freeMemory)
                .andContent("totalMemory", totalMemory)
                .andContent("maxMemory", maxMemory)
                .andTemplate(Template.DEFAULT.adminPath());
    }
    
    private Response routes(String space) {
        return Response.withOk()
                .andContent(SPACE, space)
                .andContent(VERSION, BootstrapUtils.getVersion())
                .andContent(ROUTES, Router.getRoutes())
                .andTemplate(Template.DEFAULT.routesPath());
    }

    private Response cache(String space) {
        Map<String, Object> stats = Application.getInstance(Cache.class).getStats();

        return Response.withOk()
                .andContent(SPACE, space)
                .andContent(VERSION, BootstrapUtils.getVersion())
                .andContent(STATS, stats)
                .andTemplate(Template.DEFAULT.cachePath());
    }

    private Response configuration(String space) {
        Map<String, String> configurations = Application.getConfig().getAllConfigurations();
        configurations.remove(Key.APPLICATION_SECRET.toString());

        return Response.withOk()
                .andContent(SPACE, space)
                .andContent(VERSION, BootstrapUtils.getVersion())
                .andContent(CONFIGURATION, configurations)
                .andTemplate(Template.DEFAULT.configurationPath());
    }

    private Response metrics(String space) {
        Metrics metrics = Application.getInstance(Metrics.class);

        return Response.withOk()
                .andContent(SPACE, space)
                .andContent(VERSION, BootstrapUtils.getVersion())
                .andContent(METRICS, metrics.getMetrics())
                .andTemplate(Template.DEFAULT.metricsPath());
    }

    private Response scheduler(String space)  {
        List<Job> jobs = new ArrayList<>();
        try {
            org.quartz.Scheduler scheduler = Application.getInstance(Scheduler.class).getScheduler();
            if (scheduler != null) {
                Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(Default.SCHEDULER_JOB_GROUP.toString()));
                for (JobKey jobKey : jobKeys) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    Trigger trigger = triggers.get(0);
                    TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
                    jobs.add(new Job(TriggerState.PAUSED.equals(triggerState) ? false : true, jobKey.getName(), trigger.getDescription(), trigger.getNextFireTime(), trigger.getPreviousFireTime()));
                }
            }
 
        } catch (SchedulerException e) {
            LOG.error("Failed to retrieve jobs from scheduler", e);
        }

        return Response.withOk()
                .andContent(SPACE, space)
                .andContent(VERSION, BootstrapUtils.getVersion())
                .andContent(JOBS, jobs)
                .andTemplate(Template.DEFAULT.schedulerPath());
    }
}