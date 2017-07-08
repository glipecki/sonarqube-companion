package net.lipecki.sqcompanion.sonarqube;

import lombok.Builder;
import lombok.Data;
import net.lipecki.sqcompanion.SQCompanionException;

import java.util.Date;

@Data
@Builder
public class SonarQubeMessure {

    private Date date;
    private Integer blockers;
    private Integer criticals;
    private Integer majors;
    private Integer minors;
    private Integer infos;

    public void putMetric(final String metricKey, final Integer value) {
        switch(metricKey) {
            case "blocker_violations":
                setBlockers(value);
                break;
            case "critical_violations":
                setCriticals(value);
                break;
            case "major_violations":
                setMajors(value);
                break;
            case "minor_violations":
                setMinors(value);
                break;
            case "info_violations":
                setInfos(value);
                break;
            default:
                throw new SQCompanionException("Unknown messure metric: " + metricKey);
        }
    }

}
