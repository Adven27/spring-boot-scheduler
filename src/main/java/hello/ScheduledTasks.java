package hello;


import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Component
public class ScheduledTasks {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job job;

    @Autowired
    public JobExplorer jobExplorer;


    @GetMapping("/dash")
    public ModelAndView report() {
        Map<String, Object> params = new HashMap<>();
        params.put("cars", getJobViews(jobExplorer.findRunningJobExecutions("importUserJob")));
        return new ModelAndView("dash", params);
    }

    private List<JobView> getJobViews(Set<JobExecution> runningJobs) {
        List<JobView> views = new ArrayList<>();
        for (JobExecution runningJob : runningJobs) {
            List<StepView> stepViews = new ArrayList<>();
            for (StepExecution stepExecution : runningJob.getStepExecutions()) {
                stepViews.add(new StepView(stepExecution));
            }
            views.add(new JobView(runningJob.getJobConfigurationName(), runningJob.getStatus().name(), stepViews));
        }
        return views;
    }

    public class JobView {
        private final String name;
        private final String status;
        private List<StepView> steps;

        public JobView(String name, String status, List<StepView> stepViews) {
            this.name = name;
            this.status = status;
            this.steps = stepViews;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public List<StepView> getSteps() {
            return steps;
        }
    }

    @RequestMapping("/runit")
    @Scheduled(fixedDelay = 5000)
    public void reportCurrentTime() {
        log.info("The time is now {}", dateFormat.format(new Date()));
        try {
            Set<JobExecution> runningJobs = jobExplorer.findRunningJobExecutions("importUserJob");
            if (runningJobs.isEmpty()) {
                String dateParam = new Date().toString();
                JobParameters param = new JobParametersBuilder().addString("date", dateParam).toJobParameters();

                System.out.println(dateParam);

                JobExecution execution = jobLauncher.run(job, param);
                System.out.println("Exit Status : " + execution.getStatus());
            } else {
                System.out.println("ALREADY RUNNING " + runningJobs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class StepView {
        private final String summary;

        public StepView(StepExecution stepExecution) {
            summary = stepExecution.getSummary();

        }

        public String getSummary() {
            return summary;
        }
    }
}