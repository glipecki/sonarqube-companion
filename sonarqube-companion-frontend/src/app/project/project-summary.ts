import {HealthStatus} from '../health/health-status';
import {Violations} from '../violations/violations';

export class ProjectSummary {

  name: string;
  key: string;
  serverId: string;
  healthStatus: HealthStatus;
  violations: Violations;

  constructor(data) {
    this.name = data.name;
    this.key = data.key;
    this.serverId = data.serverId;
    this.healthStatus = HealthStatus[data.healthStatus] as HealthStatus;
    this.violations = new Violations(data.violations || {});
  }

  get healthStatusString() {
    return this.healthStatus ? HealthStatus[this.healthStatus].toLowerCase() : '';
  }

}
