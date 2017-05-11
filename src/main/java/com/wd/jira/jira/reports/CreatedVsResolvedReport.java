package com.wd.jira.jira.reports;

import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.charts.Chart;
import com.atlassian.jira.charts.ChartFactory;
import com.atlassian.jira.charts.ChartFactory.ChartContext;
import com.atlassian.jira.charts.ChartFactory.PeriodName;
import com.atlassian.jira.charts.ChartFactory.VersionLabel;
import com.atlassian.jira.charts.jfreechart.TimePeriodUtils;
import com.atlassian.jira.charts.report.AbstractChartReport;
import com.atlassian.jira.charts.util.ChartUtils;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.charts.ChartFactory.REPORT_IMAGE_HEIGHT;
import static com.atlassian.jira.charts.ChartFactory.REPORT_IMAGE_WIDTH;

/**
 * A report showing issues created vs resolved for a given project or search request.
 */
@Named
public class CreatedVsResolvedReport extends AbstractChartReport {
    private static final Logger log = LoggerFactory.getLogger(CreatedVsResolvedReport.class);
    private final ChartFactory chartFactory;
    private final TimeZoneManager timeZoneManager;

    @Inject
    public CreatedVsResolvedReport(@ComponentImport JiraAuthenticationContext authenticationContext,
                                   @ComponentImport ApplicationProperties applicationProperties,
                                   @ComponentImport ProjectManager projectManager,
                                   @ComponentImport SearchRequestService searchRequestService,
                                   @ComponentImport ChartUtils chartUtils,
                                   @ComponentImport ChartFactory chartFactory,
                                   @ComponentImport TimeZoneManager timeZoneManager) {
        super(authenticationContext, applicationProperties, projectManager, searchRequestService, chartUtils);
        this.chartFactory = chartFactory;
        this.timeZoneManager = timeZoneManager;
    }

    public String generateReportHtml(final ProjectActionSupport action, final Map reqParams) throws Exception {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("report", this);
        params.put("action", action);
        params.put("user", authenticationContext.getUser());
        params.put("timePeriods", new TimePeriodUtils(timeZoneManager));

        // Retrieve the portlet parameters
        final String projectOrFilterId = (String) reqParams.get("projectOrFilterId");
        final PeriodName periodName = PeriodName.valueOf((String) reqParams.get("periodName"));

        int days = 30;

        if (reqParams.containsKey("daysprevious")) {
            days = Integer.parseInt((String) reqParams.get("daysprevious"));
        }

        VersionLabel versionLabel = VersionLabel.none;
        if (reqParams.containsKey("versionLabels")) {
            versionLabel = VersionLabel.valueOf((String) reqParams.get("versionLabels"));
        }

        boolean cumulative = false;
        if (reqParams.containsKey("cumulative")) {
            cumulative = ((String) reqParams.get("cumulative")).equalsIgnoreCase("true");
        }

        boolean showUnresolvedTrend = "true".equalsIgnoreCase((String) reqParams.get("showUnresolvedTrend"));

        try {
            final SearchRequest request = chartUtils.retrieveOrMakeSearchRequest(projectOrFilterId, params);
            params.put("projectOrFilterId", projectOrFilterId);

            final ChartContext context = new ChartContext(authenticationContext.getUser(), request, REPORT_IMAGE_WIDTH, REPORT_IMAGE_HEIGHT, true);
            Chart chart = chartFactory.generateCreatedVsResolvedChart(context, days, periodName, versionLabel, cumulative, showUnresolvedTrend);
            params.putAll(chart.getParameters());
        } catch (Exception e) {
            log.error("Could not create velocity parameters " + e.getMessage(), e);
        }

        return descriptor.getHtml("view", params);
    }
}
